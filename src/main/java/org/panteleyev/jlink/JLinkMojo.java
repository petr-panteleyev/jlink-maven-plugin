// Copyright © 2024-2026 Petr Panteleyev
// SPDX-License-Identifier: BSD-2-Clause
package org.panteleyev.jlink;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.panteleyev.jlink.CommandLineParameter.ADD_MODULES;
import static org.panteleyev.jlink.CommandLineParameter.BIND_SERVICES;
import static org.panteleyev.jlink.CommandLineParameter.ENDIAN;
import static org.panteleyev.jlink.CommandLineParameter.GENERATE_CDS_ARCHIVE;
import static org.panteleyev.jlink.CommandLineParameter.IGNORE_SIGNING_INFORMATION;
import static org.panteleyev.jlink.CommandLineParameter.LAUNCHER;
import static org.panteleyev.jlink.CommandLineParameter.LIMIT_MODULES;
import static org.panteleyev.jlink.CommandLineParameter.MODULE_PATH;
import static org.panteleyev.jlink.CommandLineParameter.NO_HEADER_FILES;
import static org.panteleyev.jlink.CommandLineParameter.NO_MAN_PAGES;
import static org.panteleyev.jlink.CommandLineParameter.OUTPUT;
import static org.panteleyev.jlink.CommandLineParameter.STRIP_DEBUG;
import static org.panteleyev.jlink.CommandLineParameter.STRIP_NATIVE_COMMANDS;
import static org.panteleyev.jlink.CommandLineParameter.VERBOSE;
import static org.panteleyev.jlink.OsUtil.isWindows;
import static org.panteleyev.jlink.StringUtil.isEmpty;
import static org.panteleyev.jlink.StringUtil.isNotEmpty;

/**
 * <p>Generates jlink image.<br>
 * Each plugin parameter defines <code>jlink</code> option.
 */
@Mojo(name = JLinkMojo.GOAL, defaultPhase = LifecyclePhase.NONE)
public class JLinkMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(JLinkMojo.class);

    public static final String GOAL = "jlink";

    private static final String TOOLCHAIN = "jdk";
    public static final String EXECUTABLE = "jlink";

    private static final String DRY_RUN_PROPERTY = "jlink.dryRun";

    private final ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    /**
     * Skips plugin execution.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * <p>--add-modules <i>mod</i>[,<i>mod</i>]</p>
     * <p>Adds the named modules, <i>mod</i>, to the default set of root modules. The default set of root modules is
     * empty.</p>
     * <p>Example:
     * <pre>
     * &lt;addModules>
     *     &lt;module>app.module&lt;/module>
     * &lt;/addModules>
     * </pre>
     * </p>
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> addModules;

    /**
     * <p>--bind-services</p>
     * <p>Link service provider modules and their dependencies.</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean bindServices;

    /**
     * <p>--endian {little|big}</p>
     * <p>Specifies the byte order of the generated image. The default value is the format of your system's
     * architecture.</p>
     * <p>Possible values:</p>
     * <table>
     *     <tr>
     *         <th>Plugin</th><th>jlink</th>
     *     </tr>
     *     <tr><td>LITTLE</td><td>little</td></tr>
     *     <tr><td>BIG</td><td>big</td></tr>
     * </table>
     *
     * @since 1.0.0
     */
    @Parameter
    private Endian endian;

    /**
     * <p>--ignore-signing-information</p>
     * <p>Suppresses a fatal error when signed modular JARs are linked in the runtime image. The signature-related
     * files of the signed modular JARs aren't copied to the runtime image.</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreSigningInformation;

    /**
     * <p>--generate-cds-archive</p>
     * <p>Generate CDS archive if the runtime image supports the CDS feature.</p>
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "false")
    private boolean generateCdsArchive;

    /**
     * <p>--limit-modules <i>mod</i>[,<i>mod</i>...]</p>
     * <p>Limits the universe of observable modules to those in the transitive closure of the named modules,
     * <i>mod</i>,
     * plus the main module, if any, plus any further modules specified in the <code>addModules</code> option.</p>
     * <p>Each module is specified by a separate &lt;limitModule> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;limitModules>
     *     &lt;limitModule>java.base&lt;/limitModule>
     * &lt;/limitModules>
     * </pre>
     * </p>
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> limitModules;

    /**
     * <p>--module-path <i>modulepath</i></p>
     * <p>Specifies the module path.<br>
     * If this option is not specified, then the default module path is <code>$JAVA_HOME/jmods</code>. This directory
     * contains the <code>java.base</code> module and the other standard and JDK modules. If this option is specified
     * but the <code>java.base</code> module cannot be resolved from it, then the jlink command appends
     * <code>$JAVA_HOME/jmods</code> to the module path.</p>
     * <p>Each module path is specified by a separate &lt;modulePath> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;modulePaths>
     *     &lt;modulePath>>${project.build.directory}/jmods&lt;/modulePath>
     * &lt;/modulePaths>
     * </pre>
     * </p>
     *
     * @since 1.0.0
     */
    @Parameter
    private List<File> modulePaths;

    /**
     * <p>--no-header-files</p>
     * <p>Excludes header files.</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean noHeaderFiles;

    /**
     * <p>--no-man-pages</p>
     * <p>Excludes man pages.</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean noManPages;

    /**
     * <p>--output <i>path</i></p>
     * <p>Specifies the location of the generated runtime image.</p>
     *
     * @since 1.0.0
     */
    @Parameter(required = true)
    private File output;

    /**
     * <p>--strip-debug</p>
     * <p>Strips debug information from the output image.</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean stripDebug;

    /**
     * <p>--strip-native-commands</p>
     * <p>Excludes native commands (such as java/java.exe) from the image.</p>
     *
     * @since 1.3.0
     */
    @Parameter(defaultValue = "false")
    private boolean stripNativeCommands;

    /**
     * --verbose
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    /**
     * <p>--launcher <i>command=module</i> or --launcher <i>command=module/main</i></p>
     * <p>Specifies the launcher command name for the module or the command name for the module and main class.</p>
     * <pre>
     * &lt;launchers>
     *     &lt;launcher>
     *         &lt;command>command&lt;/name>
     *         &lt;module>module&lt;/module>
     *         &lt;mainClass>optionalMainClass&lt;/mainClass>
     *     &lt;/launcher>
     * &lt;/launchers>
     * </pre>
     *
     * @since 1.0.0
     */
    @Parameter
    private List<Launcher> launchers;

    @Inject
    public JLinkMojo(ToolchainManager toolchainManager) {
        this.toolchainManager = toolchainManager;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            logger.info("Skipping plugin execution");
            return;
        }

        Toolchain tc = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (tc != null) {
            logger.info("Toolchain in jpackage-maven-plugin: {}", tc);
        }

        String executable = getJPackageExecutable(tc)
                .orElseThrow(() -> new MojoExecutionException("Failed to find " + EXECUTABLE));

        logger.info("Using: {}", executable);

        Commandline commandLine = buildParameters();
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        boolean dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            logger.warn("Dry-run mode, not executing " + EXECUTABLE);
        } else {
            try {
                execute(commandLine);
            } catch (Exception ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        }
    }

    private Optional<String> getJPackageFromJdkHome(String jdkHome) {
        if (jdkHome == null || jdkHome.isEmpty()) {
            return Optional.empty();
        }

        logger.debug("Looking for " + EXECUTABLE + " in {}", jdkHome);

        String executable = jdkHome + File.separator + "bin" + File.separator + EXECUTABLE;
        if (isWindows()) {
            executable = executable + ".exe";
        }

        if (new File(executable).exists()) {
            return Optional.of(executable);
        } else {
            logger.warn("File {} does not exist", executable);
            return Optional.empty();
        }
    }

    private Optional<String> getJPackageFromToolchain(Toolchain tc) {
        if (tc == null) {
            return Optional.empty();
        }

        String executable = tc.findTool(EXECUTABLE);
        if (executable == null) {
            logger.warn(EXECUTABLE + " is not part of configured toolchain");
        }

        return Optional.ofNullable(executable);
    }

    private Optional<String> getJPackageExecutable(Toolchain tc) {
        Optional<String> executable = getJPackageFromToolchain(tc);
        return executable.isPresent() ?
                executable : getJPackageFromJdkHome(System.getProperty("java.home"));
    }

    private void execute(Commandline commandline) throws Exception {
        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        try {
            int exitCode = CommandLineUtils.executeCommandLine(commandline, out, err);

            String output = (isEmpty(out.getOutput()) ? null : '\n' + out.getOutput().trim());

            if (exitCode != 0) {
                if (isNotEmpty(output)) {
                    for (String line : output.split("\n")) {
                        logger.error(line);
                    }
                }

                StringBuilder msg = new StringBuilder("\nExit code: ")
                        .append(exitCode);
                String errOutput = err.getOutput();
                if (isNotEmpty(errOutput)) {
                    msg.append(" - ").append(errOutput);
                }
                msg.append('\n');
                msg.append("Command line was: ").append(commandline).append('\n').append('\n');

                throw new MojoExecutionException(msg.toString());
            } else {
                if (isNotEmpty(output)) {
                    for (String outputLine : output.split("\n")) {
                        logger.info(outputLine);
                    }
                }
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Error while executing " + EXECUTABLE + ": " + e.getMessage(), e);
        }
    }

    private Commandline buildParameters() throws MojoFailureException {
        logger.info("jlink options:");

        Commandline commandline = new Commandline();
        addParameter(commandline, BIND_SERVICES, bindServices);
        addParameter(commandline, ENDIAN, endian);
        addParameter(commandline, IGNORE_SIGNING_INFORMATION, ignoreSigningInformation);
        addParameter(commandline, GENERATE_CDS_ARCHIVE, generateCdsArchive);
        addParameter(commandline, NO_HEADER_FILES, noHeaderFiles);
        addParameter(commandline, NO_MAN_PAGES, noManPages);
        addMandatoryParameter(commandline, OUTPUT, output, false);
        addParameter(commandline, STRIP_DEBUG, stripDebug);
        addParameter(commandline, STRIP_NATIVE_COMMANDS, stripNativeCommands);
        addParameter(commandline, VERBOSE, verbose);

        if (modulePaths != null && !modulePaths.isEmpty()) {
            List<String> pathStrings = modulePaths.stream()
                    .filter(Objects::nonNull)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            addParameter(commandline, MODULE_PATH, String.join(File.pathSeparator, pathStrings));
        }

        if (addModules != null && !addModules.isEmpty()) {
            addParameter(commandline, ADD_MODULES,
                    addModules.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(",")));
        }

        if (limitModules != null && !limitModules.isEmpty()) {
            addParameter(commandline, LIMIT_MODULES,
                    limitModules.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(",")));
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                if (launcher == null) continue;
                launcher.validate();
                addParameter(commandline, LAUNCHER, launcher.toString());
            }
        }

        return commandline;
    }

    @SuppressWarnings("SameParameterValue")
    private void addMandatoryParameter(Commandline commandline, CommandLineParameter parameter, File value,
            boolean checkExistence) throws MojoFailureException
    {
        if (value == null) {
            throw new MojoFailureException(
                    "Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value, checkExistence);
    }

    private void addParameter(Commandline commandline, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        logger.info("  {} {}", name, value);
        commandline.createArg().setValue(name);
        commandline.createArg().setValue(value);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        logger.info("  {} {}", parameter.getName(), value);
        commandline.createArg().setValue(parameter.getName());
        commandline.createArg().setValue(value);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, File value,
            boolean checkExistence) throws MojoFailureException
    {
        addParameter(
                commandline,
                parameter,
                value,
                checkExistence,
                true
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void addParameter(Commandline commandline, CommandLineParameter parameter, File value,
            boolean checkExistence, boolean makeAbsolute) throws MojoFailureException
    {
        if (value == null) {
            return;
        }

        String path = makeAbsolute ? value.getAbsolutePath() : value.getPath();

        if (checkExistence && !value.exists()) {
            throw new MojoFailureException("File or directory " + path + " does not exist");
        }

        addParameter(commandline, parameter.getName(), path);
    }

    private void addParameter(Commandline commandline, CommandLineParameter parameter, boolean value) {
        if (!value) {
            return;
        }

        logger.info("  {}", parameter.getName());
        commandline.createArg().setValue(parameter.getName());
    }

    @SuppressWarnings("SameParameterValue")
    private void addParameter(Commandline commandline, CommandLineParameter parameter, EnumParameter value) {
        if (value == null) {
            return;
        }

        addParameter(commandline, parameter, value.getValue());
    }
}
