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
package org.eclipse.che.runner.webapps;

import com.google.inject.Singleton;

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
import org.eclipse.che.api.runner.internal.DeploymentSources;
import org.eclipse.che.commons.lang.FlushingStreamWriter;
import org.jvnet.winp.WinProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ApplicationServer} implementation to deploy application to Apache Tomcat servlet container for windows system.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class WindowsTomcatServer extends BaseTomcatServer {
    private static final Logger LOG        = LoggerFactory.getLogger(WindowsTomcatServer.class);
    private static final String LOG_FOLDER = "logs";
    private static final String LOG_FILE   = "output.log";

    @Inject
    public WindowsTomcatServer(@Named(MEM_SIZE_PARAMETER) int memSize,
                               @Named(TOMCAT_HOME_PARAMETER) File tomcatHome,
                               EventService eventService) {
        super(memSize, tomcatHome, eventService);
    }

    @Override
    public ApplicationProcess deploy(File appDir, DeploymentSources toDeploy, ApplicationServerRunnerConfiguration runnerConfiguration,
                                     ApplicationProcess.Callback callback) throws RunnerException {
        prepare(appDir, toDeploy, runnerConfiguration);
        final File logsDir = new File(appDir, LOG_FOLDER);
        final File startUpScriptFile;
        try {
            generateSetEnvScript(appDir, runnerConfiguration);
            startUpScriptFile = generateStartUpScript(appDir, runnerConfiguration);
            Files.createDirectory(logsDir.toPath());
        } catch (IOException e) {
            throw new RunnerException(e);
        }
        final List<File> logFiles = new ArrayList<>(1);
        logFiles.add(new File(logsDir, LOG_FILE));

        return new TomcatProcess(appDir, startUpScriptFile, logFiles, runnerConfiguration, callback, eventService);
    }

    private void generateSetEnvScript(File appDir, ApplicationServerRunnerConfiguration runnerConfiguration)
            throws IOException {
        int memory = runnerConfiguration.getMemory();
        if (memory <= 0) {
            memory = getMemSize();
        }
        final String setEnvScript = "@echo off\r\n" +
                                    String.format("set \"CATALINA_OPTS=-server -Xms%dm -Xmx%dm\"\r\n", memory, memory) +
                                    "set \"CLASSPATH=%CATALINA_HOME%/conf/;\"";
        final File setEnvScriptFile = new File(appDir.toPath() + "/tomcat/bin", "setenv.bat");
        Files.write(setEnvScriptFile.toPath(), setEnvScript.getBytes());
        if (!setEnvScriptFile.setExecutable(true, false)) {
            throw new IOException("Unable to update attributes of the setenv script");
        }
    }

    private File generateStartUpScript(File appDir, ApplicationServerRunnerConfiguration runnerConfiguration)
            throws IOException {
        final String startupScript = "@echo off\r\n" +
                                     "setlocal\r\n" +
                                     setDebugVariables(runnerConfiguration) +
                                     "cd tomcat\r\n" +
                                     setCatalinaVariables(runnerConfiguration);
        final File startUpScriptFile = new File(appDir, "startup.bat");
        Files.write(startUpScriptFile.toPath(), startupScript.getBytes());
        if (!startUpScriptFile.setExecutable(true, false)) {
            throw new IOException("Unable to update attributes of the startup script");
        }
        return startUpScriptFile;
    }

    private String setDebugVariables(ApplicationServerRunnerConfiguration runnerConfiguration) {
        final int debugPort = runnerConfiguration.getDebugPort();
        if (debugPort > 0) {
            return "set \"JPDA_ADDRESS=" + debugPort + "\"\r\n" +
                   "set \"JPDA_TRANSPORT=dt_socket\"\r\n" +
                   "set \"JPDA_SUSPEND=" + (runnerConfiguration.isDebugSuspend() ? "y" : "n") + "\"\r\n";
        }
        return "";
    }

    private String setCatalinaVariables(ApplicationServerRunnerConfiguration runnerConfiguration) {
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
        final int          httpPort;
        final List<File>   logFiles;
        final int          debugPort;
        final File         startUpScriptFile;
        final File         workDir;
        final Callback     callback;
        final EventService eventService;
        final String       workspace;
        final String       project;
        final long         id;

        ApplicationLogsPublisher logger;
        LineConsumer             logFileConsumer;
        Process                  process;
        StreamPump               output;
        WinProcess               winProcess;

        TomcatProcess(File appDir, File startUpScriptFile, List<File> logFiles,
                      ApplicationServerRunnerConfiguration runnerConfiguration, Callback callback, EventService eventService) {
            this.httpPort = runnerConfiguration.getHttpPort();
            this.logFiles = logFiles;
            this.debugPort = runnerConfiguration.getDebugPort();
            this.startUpScriptFile = startUpScriptFile;
            this.workDir = appDir;
            this.callback = callback;
            this.eventService = eventService;
            this.workspace = runnerConfiguration.getRequest().getWorkspace();
            this.project = runnerConfiguration.getRequest().getProject();
            this.id = runnerConfiguration.getRequest().getId();
        }

        @Override
        public synchronized void start() throws RunnerException {
            if (process != null && isAlive()) {
                throw new IllegalStateException("Process is already started");
            }
            File logFile = Paths.get(workDir.getAbsolutePath(), LOG_FOLDER, LOG_FILE).toFile();
            try {
                OutputStreamWriter flashingStream = new FlushingStreamWriter(new FileOutputStream(logFile));
                logger = new ApplicationLogsPublisher(new TomcatLogger(logFiles), eventService, id, workspace, project);
                logFileConsumer = new IndentWrapperLineConsumer(new WritableLineConsumer(flashingStream));
                LineConsumer logComposite = new CompositeLineConsumer(logFileConsumer, logger);

                process = Runtime.getRuntime().exec(new CommandLine(startUpScriptFile.getAbsolutePath()).toShellCommand(), null, workDir);
                winProcess = new WinProcess(process);
                StreamPump output = new StreamPump();
                output.start(process, logComposite);
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
            callback.stopped();
            LOG.debug("Stop Tomcat at port {}, application {}", httpPort, workDir);
            try {
                logFileConsumer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void killProcess() throws RunnerException {
            if (isAlive()) {
                winProcess.killRecursively();
                winProcess = null;
            }
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

        public boolean isAlive() {
            return winProcess != null && winProcess.getPid() > 0;
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