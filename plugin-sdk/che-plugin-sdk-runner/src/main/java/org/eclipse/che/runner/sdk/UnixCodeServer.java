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
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.runner.RunnerException;
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
 * Unix GWT code server.
 *
 * @author Artem Zatsarynnyi
 * @author Alexander Andrienko
 */
@Singleton
public class UnixCodeServer extends AbstractCodeServer {

    private static final Logger LOG = LoggerFactory.getLogger(UnixCodeServer.class);

    @Inject
    public UnixCodeServer() {
    }

    @Override
    protected CodeServerProcess createProcess(File codeServerWorkDir, SDKRunnerConfiguration runnerConfiguration, Path extensionSourcesPath,
                                              String projectApiBaseUrl, ExecutorService executor) throws RunnerException {
        java.io.File startUpScriptFile = genStartUpScriptUnix(codeServerWorkDir);
        return new UnixCodeServerProcess(runnerConfiguration.getCodeServerBindAddress(),
                                         runnerConfiguration.getCodeServerPort(),
                                         startUpScriptFile,
                                         codeServerWorkDir,
                                         extensionSourcesPath,
                                         projectApiBaseUrl,
                                         executor);
    }

    private java.io.File genStartUpScriptUnix(java.io.File workDir) throws RunnerException {
        final String startupScript = "#!/bin/sh\n" +
                                     "cd war\n" +
                                     generateSources() +
                                     "PID=$!\n" +
                                     "echo \"$PID\" > run.pid\n" +
                                     "wait $PID";
        final java.io.File startUpScriptFile = new java.io.File(workDir, "startup.sh");
        try {
            Files.write(startUpScriptFile.toPath(), startupScript.getBytes());
        } catch (IOException e) {
            throw new RunnerException(e);
        }
        if (!startUpScriptFile.setExecutable(true, false)) {
            throw new RunnerException(UNABELE_UPDATE_SCRIPT_ATTRIBUTE);
        }
        return startUpScriptFile;
    }

    public static class UnixCodeServerProcess extends CodeServerProcess {

        protected UnixCodeServerProcess(String bindAddress, int port, File startUpScriptFile, File workDir, Path extensionSourcesPath,
                                        String projectApiBaseUrl, ExecutorService executor) {
            super(bindAddress, port, startUpScriptFile, workDir, extensionSourcesPath, projectApiBaseUrl, executor);
        }

        @Override
        public synchronized void start() throws RunnerException {
            if (process != null && ProcessUtil.isAlive(process)) {
                throw new IllegalStateException("Code server process is already started");
            }

            try {
                process = Runtime.getRuntime().exec(new CommandLine(startUpScriptFile.getAbsolutePath()).toShellCommand(), null, workDir);
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
            ProcessUtil.kill(process);
            LOG.debug("Stop GWT code server at port {}, working directory {}", port, workDir);
        }
    }
}
