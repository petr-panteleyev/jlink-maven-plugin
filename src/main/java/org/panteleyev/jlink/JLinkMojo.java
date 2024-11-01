/*
 Copyright © 2024 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jlink;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.panteleyev.jlink.CommandLineParameter.ADD_MODULES;
import static org.panteleyev.jlink.CommandLineParameter.BIND_SERVICES;
import static org.panteleyev.jlink.CommandLineParameter.ENDIAN;
import static org.panteleyev.jlink.CommandLineParameter.IGNORE_SIGNING_INFORMATION;
import static org.panteleyev.jlink.CommandLineParameter.LAUNCHER;
import static org.panteleyev.jlink.CommandLineParameter.LIMIT_MODULES;
import static org.panteleyev.jlink.CommandLineParameter.MODULE_PATH;
import static org.panteleyev.jlink.CommandLineParameter.NO_HEADER_FILES;
import static org.panteleyev.jlink.CommandLineParameter.NO_MAN_PAGES;
import static org.panteleyev.jlink.CommandLineParameter.OUTPUT;
import static org.panteleyev.jlink.CommandLineParameter.STRIP_DEBUG;
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
    public static final String GOAL = "jlink";

    private static final String TOOLCHAIN = "jdk";
    public static final String EXECUTABLE = "jlink";

    private static final String DRY_RUN_PROPERTY = "jlink.dryRun";

    @Component
    private ToolchainManager toolchainManager;

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
     * <p>--add-modules &lt;module>[,&lt;module>]</p>
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> addModules;

    /**
     * <p>--bind-services</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean bindServices;

    /**
     * <p>--endian &lt;endian></p>
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
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreSigningInformation;

    /**
     * <p>--limit-modules &lt;mod>[,&lt;mod>...]</p>
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
     * <p>--module-path &lt;path></p>
     * <p>Each module path is specified by a separate &lt;modulePath> parameter.</p>
     * <p>Example:
     * <pre>
     * &lt;modulePaths>
     *     &lt;modulePath>target/jmods&lt;/modulePath>
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
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean noHeaderFiles;

    /**
     * <p>--no-man-pages</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean noManPages;

    /**
     * <p>--output &lt;path&gt;</p>
     *
     * @since 1.0.0
     */
    @Parameter
    private File output;

    /**
     * <p>--strip-debug</p>
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean stripDebug;

    /**
     * --verbose
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    /**
     * <p>--launcher &lt;name>=&lt;module>[/&lt;mainClass>]</p>
     * <p>Adds a launcher command of the given name for the module and the main class</p>
     * <pre>
     * &lt;launchers>
     *     &lt;launcher>
     *         &lt;name>name-of-the-launcher&lt;/name>
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

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping plugin execution");
            return;
        }

        Toolchain tc = toolchainManager.getToolchainFromBuildContext(TOOLCHAIN, session);
        if (tc != null) {
            getLog().info("Toolchain in jpackage-maven-plugin: " + tc);
        }

        String executable = getJPackageExecutable(tc)
                .orElseThrow(() -> new MojoExecutionException("Failed to find " + EXECUTABLE));

        getLog().info("Using: " + executable);

        Commandline commandLine = buildParameters();
        commandLine.setExecutable(executable.contains(" ") ? ("\"" + executable + "\"") : executable);

        boolean dryRun = "true".equalsIgnoreCase(System.getProperty(DRY_RUN_PROPERTY, "false"));
        if (dryRun) {
            getLog().warn("Dry-run mode, not executing " + EXECUTABLE);
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

        getLog().debug("Looking for " + EXECUTABLE + " in " + jdkHome);

        String executable = jdkHome + File.separator + "bin" + File.separator + EXECUTABLE;
        if (isWindows()) {
            executable = executable + ".exe";
        }

        if (new File(executable).exists()) {
            return Optional.of(executable);
        } else {
            getLog().warn("File " + executable + " does not exist");
            return Optional.empty();
        }
    }

    private Optional<String> getJPackageFromToolchain(Toolchain tc) {
        if (tc == null) {
            return Optional.empty();
        }

        String executable = tc.findTool(EXECUTABLE);
        if (executable == null) {
            getLog().warn(EXECUTABLE + " is not part of configured toolchain");
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
                        getLog().error(line);
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
                        getLog().info(outputLine);
                    }
                }
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Error while executing " + EXECUTABLE + ": " + e.getMessage(), e);
        }
    }

    private Commandline buildParameters() throws MojoFailureException {
        getLog().info("jlink options:");

        Commandline commandline = new Commandline();
        addParameter(commandline, BIND_SERVICES, bindServices);
        addParameter(commandline, ENDIAN, endian);
        addParameter(commandline, IGNORE_SIGNING_INFORMATION, ignoreSigningInformation);
        addParameter(commandline, NO_HEADER_FILES, noHeaderFiles);
        addParameter(commandline, NO_MAN_PAGES, noManPages);
        addMandatoryParameter(commandline, OUTPUT, output, false);
        addParameter(commandline, STRIP_DEBUG, stripDebug);
        addParameter(commandline, VERBOSE, verbose);


        if (addModules != null && !addModules.isEmpty()) {
            addParameter(commandline, ADD_MODULES, String.join(",", addModules));
        }

        if (limitModules != null && !limitModules.isEmpty()) {
            addParameter(commandline, LIMIT_MODULES, String.join(",", limitModules));
        }

        if (modulePaths != null) {
            for (File modulePath : modulePaths) {
                addParameter(commandline, MODULE_PATH, modulePath, true);
            }
        }

        if (launchers != null) {
            for (Launcher launcher : launchers) {
                launcher.validate();
                addParameter(commandline, LAUNCHER, launcher.toString());
            }
        }

        return commandline;
    }

    private void addMandatoryParameter(
            Commandline commandline,
            @SuppressWarnings("SameParameterValue") CommandLineParameter parameter,
            String value
    ) throws MojoFailureException {
        if (value == null || value.isEmpty()) {
            throw new MojoFailureException("Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value);
    }

    private void addMandatoryParameter(
            Commandline commandline,
            @SuppressWarnings("SameParameterValue") CommandLineParameter parameter,
            File value,
            boolean checkExistence
    ) throws MojoFailureException {
        if (value == null) {
            throw new MojoFailureException("Mandatory parameter \"" + parameter.getName() + "\" cannot be null or empty");
        }
        addParameter(commandline, parameter, value, checkExistence);
    }

    private void addParameter(Commandline commandline, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + name + " " + value);
        commandline.createArg().setValue(name);
        commandline.createArg().setValue(value);
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            String value
    ) {
        if (value == null || value.isEmpty()) {
            return;
        }

        getLog().info("  " + parameter.getName() + " " + value);
        commandline.createArg().setValue(parameter.getName());
        commandline.createArg().setValue(value);
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            File value,
            boolean checkExistence
    ) throws MojoFailureException {
        addParameter(
                commandline,
                parameter,
                value,
                checkExistence,
                true
        );
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            File value,
            boolean checkExistence,
            boolean makeAbsolute
    ) throws MojoFailureException {
        if (value == null) {
            return;
        }

        String path = makeAbsolute ? value.getAbsolutePath() : value.getPath();

        if (checkExistence && !value.exists()) {
            throw new MojoFailureException("File or directory " + path + " does not exist");
        }

        addParameter(commandline, parameter.getName(), path);
    }

    private void addParameter(Commandline commandline, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }

        getLog().info("  " + name);
        commandline.createArg().setValue(name);
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            boolean value
    ) {
        if (!value) {
            return;
        }

        getLog().info("  " + parameter.getName());
        commandline.createArg().setValue(parameter.getName());
    }

    private void addParameter(
            Commandline commandline,
            CommandLineParameter parameter,
            EnumParameter value
    ) {
        if (value == null) {
            return;
        }

        addParameter(commandline, parameter, value.getValue());
    }
}
