/*
 Copyright © 2024-2025 Petr Panteleyev
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jlink;

import org.apache.maven.plugin.MojoFailureException;

public class Launcher {
    private String command;
    private String module;
    private String mainClass;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void validate() throws MojoFailureException {
        if (command == null || command.isEmpty()) {
            throw new MojoFailureException("Launcher name cannot be null or empty");
        }
        if (module == null || module.isEmpty()) {
            throw new MojoFailureException("Launcher module cannot be null or empty");
        }
        if (mainClass != null && mainClass.isEmpty()) {
            throw new MojoFailureException("Launcher main class cannot be empty");
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder(command)
                .append("=")
                .append(module);
        if (mainClass != null && !mainClass.isEmpty()) {
            b.append("/").append(mainClass);
        }
        return b.toString();
    }
}
