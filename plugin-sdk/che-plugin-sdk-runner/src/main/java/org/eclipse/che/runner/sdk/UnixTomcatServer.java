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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.core.util.StreamPump;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.internal.ApplicationLogger;
import org.eclipse.che.api.runner.internal.ApplicationLogsPublisher;
import org.eclipse.che.api.runner.internal.ApplicationProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ApplicationServer} implementation to deploy application to Apache Tomcat servlet container on os Unix.
 *
 * @author Artem Zatsarynnyy
 * @author Eugene Voevodin
 * @author Alexander Andrienko
 */
@Singleton
public class UnixTomcatServer extends TomcatServer {

    private static final Logger LOG = LoggerFactory.getLogger(UnixTomcatServer.class);

    @Inject
    public UnixTomcatServer(@Named(MEM_SIZE_PARAMETER) int memSize,
                            ApplicationUpdaterRegistry applicationUpdaterRegistry,
                            EventService eventService) {
        super(memSize, applicationUpdaterRegistry, eventService);
    }

    @Override
    protected ApplicationProcess start(java.io.File appDir, SDKRunnerConfiguration runnerCfg,
                                       CodeServerProcess codeServerProcess, ApplicationProcess.Callback callback)
            throws RunnerException {
        final java.io.File startUpScriptFile;
        final java.io.File logsDir = new java.io.File(appDir, "logs");
        try {
            startUpScriptFile = genStartUpScript(appDir, runnerCfg);
            updateSetenvFileUnix(appDir, runnerCfg);
            Files.createDirectory(logsDir.toPath());
        } catch (IOException e) {
            throw new RunnerException(e);
        }
        final List<File> logFiles = new ArrayList<>(1);
        logFiles.add(new java.io.File(logsDir, "output.log"));
        return new TomcatProcess(appDir, startUpScriptFile, logFiles, runnerCfg, callback, codeServerProcess, eventService);
    }

    private java.io.File genStartUpScript(java.io.File appDir, SDKRunnerConfiguration runnerConfiguration) throws IOException {
        final String startupScript = "#!/bin/sh\n" +
                                     exportEnvVariablesUnix(runnerConfiguration) +
                                     "cd tomcat\n" +
                                     "chmod +x bin/*.sh\n" +
                                     setCatalinaVariables(runnerConfiguration) +
                                     "PID=$!\n" +
                                     "echo \"$PID\" > ../run.pid\n" +
                                     "wait $PID";
        final java.io.File startUpScriptFile = new java.io.File(appDir, "startup.sh");
        Files.write(startUpScriptFile.toPath(), startupScript.getBytes());
        if (!startUpScriptFile.setExecutable(true, false)) {
            throw new IOException("Unable to update attributes of the startup script");
        }
        return startUpScriptFile;
    }

    private void updateSetenvFileUnix(java.io.File tomcatDir, SDKRunnerConfiguration runnerCfg) throws IOException {
        final Path setenvShPath = tomcatDir.toPath().resolve("tomcat/bin/setenv.sh");
        final String setenvShContent =
                new String(Files.readAllBytes(setenvShPath)).replace("${PORT}", Integer.toString(runnerCfg.getHttpPort()));
        Files.write(setenvShPath, setenvShContent.getBytes());
    }

    private String exportEnvVariablesUnix(SDKRunnerConfiguration runnerConfiguration) {
        int memory = runnerConfiguration.getMemory();
        if (memory <= 0) {
            memory = getMemSize();
        }
        final StringBuilder export = new StringBuilder();
        export.append(String.format("export CATALINA_OPTS=\"-Xms%dm -Xmx%dm\"%n", memory, memory));
        export.append(String.format("export SERVER_PORT=%d%n", runnerConfiguration.getHttpPort()));
        final int debugPort = runnerConfiguration.getDebugPort();
        if (debugPort > 0) {
            /*
            From catalina.sh:
            -agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND
             */
            export.append(String.format("export JPDA_ADDRESS=%d%n", debugPort));
            export.append(String.format("export JPDA_TRANSPORT=%s%n", "dt_socket"));
            export.append(String.format("export JPDA_SUSPEND=%s%n", runnerConfiguration.isDebugSuspend() ? "y" : "n"));
        }
        return export.toString();
    }

    private String setCatalinaVariables(SDKRunnerConfiguration runnerConfiguration) {
        final boolean debug = runnerConfiguration.getDebugPort() > 0;
        if (debug) {
            return "./bin/catalina.sh jpda run 2>&1 | tee ../logs/output.log &\n";
        }
        return "./bin/catalina.sh run 2>&1 | tee ../logs/output.log &\n";
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
            if (process != null && ProcessUtil.isAlive(process)) {
                throw new IllegalStateException("Process is already started");
            }
            try {
                process = Runtime.getRuntime().exec(new CommandLine(startUpScriptFile.getAbsolutePath()).toShellCommand(), null, workDir);
                logger = new ApplicationLogsPublisher(new TomcatLogger(logFiles, codeServerProcess), eventService, id, workspace, project);
                output = new StreamPump();
                output.start(process, logger);
                try {
                    codeServerProcess.start();
                } catch (RunnerException e) {
                    ProcessUtil.kill(process);
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
            // Use ProcessUtil.kill(process) because java.lang.Process.destroy() method doesn't
            // kill all child processes (see http://bugs.sun.com/view_bug.do?bug_id=4770092).
            ProcessUtil.kill(process);
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
                ProcessUtil.kill(process);
            } finally {
                if (output != null) {
                    output.stop();
                }
            }
            return process.exitValue();
        }

        @Override
        public synchronized int exitCode() throws RunnerException {
            if (process == null || ProcessUtil.isAlive(process)) {
                return -1;
            }
            return process.exitValue();
        }

        @Override
        public synchronized boolean isRunning() throws RunnerException {
            return process != null && ProcessUtil.isAlive(process);
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
