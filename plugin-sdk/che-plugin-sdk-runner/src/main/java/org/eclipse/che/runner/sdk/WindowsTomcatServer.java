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

import com.google.common.collect.ImmutableList;
import org.eclipse.che.api.core.notification.EventService;

import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.IndentWrapperLineConsumer;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.StreamPump;
import org.eclipse.che.api.core.util.WritableLineConsumer;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.internal.ApplicationLogger;
import org.eclipse.che.api.runner.internal.ApplicationLogsPublisher;
import org.eclipse.che.api.runner.internal.ApplicationProcess;
import org.eclipse.che.commons.lang.FlushingStreamWriter;
import org.jvnet.winp.WinProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * {@link ApplicationServer} implementation to deploy application to Apache Tomcat servlet container on os Windows.
 *
 * @author Artem Zatsarynnyy
 * @author Eugene Voevodin
 * @author Alexander Andrienko
 */
@Singleton
public class WindowsTomcatServer extends AbstractTomcatServer {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsTomcatServer.class);
    private static final String LOG_FOLDER = "logs";
    private static final String LOG_FILE   = "output.log";

    @Inject
    public WindowsTomcatServer(@Named(MEM_SIZE_PARAMETER) int memSize, ApplicationUpdaterRegistry applicationUpdaterRegistry,
                               EventService eventService) {
        super(memSize, applicationUpdaterRegistry, eventService);
    }

    @Override
    protected ApplicationProcess start(java.io.File appDir, SDKRunnerConfiguration runnerCfg,
                                       CodeServerProcess codeServerProcess, ApplicationProcess.Callback callback)
            throws RunnerException {
        final java.io.File startUpScriptFile;
        final java.io.File logsDir = new java.io.File(appDir, LOG_FOLDER);
        try {
            generateSetEnvScript(appDir, runnerCfg);
            startUpScriptFile = generateStartUpScript(appDir, runnerCfg);
            Files.createDirectory(logsDir.toPath());
        } catch (IOException e) {
            throw new RunnerException(e);
        }
        final List<java.io.File> logFiles = ImmutableList.of(new java.io.File(logsDir, LOG_FILE));
        return new TomcatProcess(appDir, startUpScriptFile, logFiles, runnerCfg, callback, codeServerProcess, eventService);
    }

    private void generateSetEnvScript(java.io.File appDir, SDKRunnerConfiguration runnerConfiguration)
            throws IOException {
        int memory = runnerConfiguration.getMemory();
        if (memory <= 0) {
            memory = getMemSize();
        }
        final String setEnvScript = "@echo off\r\n" +
                                    String.format("set \"CATALINA_OPTS=-server -Xms%dm -Xmx%dm\"\r\n", memory, memory) +
                                    "set \"CLASSPATH=%CATALINA_HOME%/conf/;\"";
        final java.io.File setEnvScriptFile = new java.io.File(appDir.toPath() + "/tomcat/bin", "setenv.bat");
        Files.write(setEnvScriptFile.toPath(), setEnvScript.getBytes());
        if (!setEnvScriptFile.setExecutable(true, false)) {
            throw new IOException("Unable to update attributes of the setenv script");
        }
    }

    private java.io.File generateStartUpScript(java.io.File appDir, SDKRunnerConfiguration runnerConfiguration)
            throws IOException {
        final String startupScript = "@echo off\r\n" +
                                     "setlocal\r\n" +
                                     setDebugVariables(runnerConfiguration) +
                                     "cd tomcat\r\n" +
                                     setCatalinaVariables(runnerConfiguration);
        final java.io.File startUpScriptFile = new java.io.File(appDir, "startup.bat");
        Files.write(startUpScriptFile.toPath(), startupScript.getBytes());
        if (!startUpScriptFile.setExecutable(true, false)) {
            throw new IOException("Unable to update attributes of the startup script");
        }
        return startUpScriptFile;
    }

    private String setDebugVariables(SDKRunnerConfiguration runnerConfiguration) {
        final int debugPort = runnerConfiguration.getDebugPort();
        if (debugPort > 0) {
            return "set \"JPDA_ADDRESS=" + debugPort + "\"\r\n" +
                   "set \"JPDA_TRANSPORT=dt_socket\"\r\n" +
                   "set \"JPDA_SUSPEND=" + (runnerConfiguration.isDebugSuspend() ? "y" : "n") + "\"\r\n";
        }
        return "";
    }

    private String setCatalinaVariables(SDKRunnerConfiguration runnerConfiguration) {
        String catalinaOpts = "set \"CATALINA_HOME=%cd%\"\r\n" +
                              "set \"CATALINA_BASE=%cd%\"\r\n" +
                              "set \"CATALINA_TMPDIR=%cd%\\temp\"\r\n";
        final boolean debug = runnerConfiguration.getDebugPort() > 0;
        if (debug) {
            return catalinaOpts + "call bin/catalina.bat jpda run 2>&1\r\n";
        }
        return catalinaOpts + "call bin/catalina.bat run 2>&1\r\n";
    }

    private static class TomcatProcess extends ApplicationProcess {
        final int                          httpPort;
        final List<java.io.File>           logFiles;
        final int                          debugPort;
        final java.io.File                 startUpScriptFile;
        final java.io.File                 workDir;
        final CodeServerProcess codeServerProcess;
        final Callback                     callback;
        final String                       workspace;
        final String                       project;
        final long                         id;
        final EventService                 eventService;

        ApplicationLogger logger;
        Process           process;
        StreamPump        output;
        WinProcess        winProcess;
        LineConsumer      logFileConsumer;

        TomcatProcess(java.io.File appDir, java.io.File startUpScriptFile, List<java.io.File> logFiles,
                      SDKRunnerConfiguration runnerCfg, Callback callback, CodeServerProcess codeServerProcess,
                      EventService eventService) {
            this.httpPort = runnerCfg.getHttpPort();
            this.logFiles = logFiles;
            this.debugPort = runnerCfg.getDebugPort();
            this.startUpScriptFile = startUpScriptFile;
            this.codeServerProcess = codeServerProcess;
            this.workDir = appDir;
            this.callback = callback;
            this.eventService = eventService;
            this.workspace = runnerCfg.getRequest().getWorkspace();
            this.project = runnerCfg.getRequest().getProject();
            this.id = runnerCfg.getRequest().getId();
        }

        @Override
        public synchronized void start() throws RunnerException {
            if (process != null && isAlive()) {
                throw new IllegalStateException("Process is already started");
            }
            java.io.File logFile = Paths.get(workDir.getAbsolutePath(), LOG_FOLDER, LOG_FILE).toFile();
            try {
                OutputStreamWriter flashingStream = new FlushingStreamWriter(new FileOutputStream(logFile));
                logger = new ApplicationLogsPublisher(new TomcatLogger(logFiles, codeServerProcess), eventService, id, workspace, project);
                logFileConsumer = new IndentWrapperLineConsumer(new WritableLineConsumer(flashingStream));
                LineConsumer logComposite = new CompositeLineConsumer(logFileConsumer, logger);

                process = Runtime.getRuntime().exec(new CommandLine(startUpScriptFile.getAbsolutePath()).toShellCommand(), null, workDir);
                winProcess = new WinProcess(process);
                output = new StreamPump();
                output.start(process, logComposite);
                try {
                    codeServerProcess.start();
                } catch (RunnerException e) {
                    killProcess();
                    LOG.error(e.getMessage(), e);
                }
                LOG.debug("Start Tomcat at port {}, application {}", httpPort, workDir);
            } catch (IOException e) {
                throw new RunnerException(e);
            }
        }

        @Override
        public synchronized void stop() throws RunnerException {
            if (process == null) {
                throw new IllegalStateException("Process is not started yet");
            }
            killProcess();
            if (output != null) {
                output.stop();
            }
            try {
                codeServerProcess.stop();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            callback.stopped();
            LOG.debug("Stop Tomcat at port {}, application {}", httpPort, workDir);
            try {
                logFileConsumer.close();
            } catch (IOException e) {
                LOG.error("Can't close LineConsumer for file: " + LOG_FILE + " port {}, application {}", httpPort, workDir);
            }
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

        @Override
        public int waitFor() throws RunnerException {
            synchronized (this) {
                if (process == null) {
                    throw new IllegalStateException("Process is not started yet");
                }
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.interrupted();
                killProcess();
            } finally {
                if (output != null) {
                    output.stop();
                }
            }
            return process.exitValue();
        }

        @Override
        public synchronized int exitCode() throws RunnerException {
            if (process == null || isAlive()) {
                return -1;
            }
            return process.exitValue();
        }

        @Override
        public synchronized boolean isRunning() throws RunnerException {
            return process != null && isAlive();
        }

        @Override
        public synchronized ApplicationLogger getLogger() throws RunnerException {
            if (logger == null) {
                // is not started yet
                return ApplicationLogger.DUMMY;
            }
            return logger;
        }
    }
}
