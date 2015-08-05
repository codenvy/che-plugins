/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.runner.sdk;

import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.runner.RunnerException;
import org.jvnet.winp.WinProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * Windows GWT code server.
 *
 * @author Artem Zatsarynnyy
 * @author Alexander Andrienko
 */
@Singleton
public class WindowsCodeServer extends AbstractCodeServer {

    private static final Logger LOG = LoggerFactory.getLogger(UnixCodeServer.class);

    @Inject
    public WindowsCodeServer() {
    }

    @Override
    protected CodeServerProcess createProcess(File codeServerWorkDir, SDKRunnerConfiguration runnerConfiguration, Path extensionSourcesPath,
                                              String projectApiBaseUrl, ExecutorService executor) throws RunnerException {
        java.io.File startUpScriptFile = getStartUpScriptWindows(codeServerWorkDir);
        return new WindowsCodeServerProcess(runnerConfiguration.getCodeServerBindAddress(),
                                            runnerConfiguration.getCodeServerPort(),
                                            startUpScriptFile,
                                            codeServerWorkDir,
                                            extensionSourcesPath,
                                            projectApiBaseUrl,
                                            executor);
    }

    private java.io.File getStartUpScriptWindows(java.io.File workDir) throws RunnerException {
        final String startUpScript = "@echo off\r\n" +
                                     "cd war\r\n" +
                                     "call " + generateSources();
        final java.io.File startUpScriptFile = new java.io.File(workDir, "startup.bat");
        try {
            Files.write(startUpScriptFile.toPath(), startUpScript.getBytes());
        } catch (IOException e) {
            throw new RunnerException(e);
        }
        if (!startUpScriptFile.setExecutable(true, false)) {
            throw new RunnerException(UNABELE_UPDATE_SCRIPT_ATTRIBUTE);
        }
        return startUpScriptFile;
    }

    private class WindowsCodeServerProcess extends CodeServerProcess {

        private WinProcess winProcess;

        public WindowsCodeServerProcess(String bindAddress, int port, File startUpScriptFile, File workDir, Path extensionSourcesPath,
                                        String projectApiBaseUrl, ExecutorService executor) {
            super(bindAddress, port, startUpScriptFile, workDir, extensionSourcesPath, projectApiBaseUrl, executor);
        }

        @Override
        public synchronized void start() throws RunnerException {
            if (process != null && isAlive()) {
                throw new IllegalStateException("Code server process is already started");
            }

            try {
                process = Runtime.getRuntime().exec(new CommandLine(startUpScriptFile.getAbsolutePath()).toShellCommand(), null, workDir);
                winProcess = new WinProcess(process);
                LOG.debug("Start GWT code server at port {}, working directory {}", port, workDir);
            } catch (IOException e) {
                throw new RunnerException(e);
            }
        }

        @Override
        public synchronized void stop() throws RunnerException {
            if (process == null) {
                throw new IllegalStateException("Code server process is not started yet");
            }
            killProcess();
            LOG.debug("Stop GWT code server at port {}, working directory {}", port, workDir);
        }

        private void killProcess() throws RunnerException {
            if (isAlive()) {
                winProcess.killRecursively();
                winProcess = null;
            }
        }

        public boolean isAlive() {
            return winProcess != null && winProcess.getPid() > 0;
        }
    }
}
