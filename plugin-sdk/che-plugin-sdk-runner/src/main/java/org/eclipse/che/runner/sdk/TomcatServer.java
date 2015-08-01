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
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.internal.ApplicationLogger;
import org.eclipse.che.api.runner.internal.ApplicationProcess;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.ZipUtils;

import com.google.common.io.CharStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * {@link ApplicationServer} implementation to deploy application to Apache Tomcat servlet container.
 *
 * @author Artem Zatsarynnyy
 * @author Eugene Voevodin
 */
public abstract class TomcatServer implements ApplicationServer {
    public static final  String MEM_SIZE_PARAMETER = "runner.tomcat.memory";
    private static final Logger LOG                = LoggerFactory.getLogger(TomcatServer.class);
    private static final String SERVER_XML         =
            "<?xml version='1.0' encoding='utf-8'?>\n" +
            "<Server port=\"-1\">\n" +
            "  <Listener className=\"org.apache.catalina.core.JreMemoryLeakPreventionListener\" />\n" +
            "  <Listener className=\"org.apache.catalina.mbeans.GlobalResourcesLifecycleListener\" />\n" +
            "  <Listener className=\"org.apache.catalina.core.ThreadLocalLeakPreventionListener\" />\n" +
            "  <Service name=\"Catalina\">\n" +
            "    <Connector port=\"${PORT}\" protocol=\"HTTP/1.1\"\n" +
            "               connectionTimeout=\"20000\" />\n" +
            "    <Engine name=\"Catalina\" defaultHost=\"localhost\">\n" +
            "      <Host name=\"localhost\"  appBase=\"webapps\"\n" +
            "            unpackWARs=\"true\" autoDeploy=\"true\">\n" +
            "      </Host>\n" +
            "    </Engine>\n" +
            "  </Service>\n" +
            "</Server>\n";

    private final int                        memSize;
    private final ApplicationUpdaterRegistry applicationUpdaterRegistry;
    protected final EventService eventService;

    @Inject
    public TomcatServer(@Named(MEM_SIZE_PARAMETER) int memSize, ApplicationUpdaterRegistry applicationUpdaterRegistry,
                        EventService eventService) {
        this.memSize = memSize;
        this.applicationUpdaterRegistry = applicationUpdaterRegistry;
        this.eventService = eventService;
    }

    @Override
    public final String getName() {
        return "tomcat8";
    }

    @Override
    public String getDescription() {
        return "Apache Tomcat 8.0 is an implementation of the Java Servlet and JavaServer Pages technologies.\n" +
               "Home page: http://tomcat.apache.org/";
    }

    @Override
    public ApplicationProcess deploy(final java.io.File workDir,
                                     ZipFile warToDeploy,
                                     final java.io.File extensionJar,
                                     final SDKRunnerConfiguration runnerConfiguration,
                                     CodeServerProcess codeServerProcess,
                                     ApplicationProcess.Callback callback) throws RunnerException {
        final Path tomcatPath = workDir.toPath().resolve("tomcat");
        final Path webappsPath = tomcatPath.resolve("webapps");
        final Path apiAppContextPath = webappsPath.resolve("che");

        try {
            Files.createDirectory(tomcatPath);
            ZipUtils.unzip(Utils.getTomcatBinaryDistribution().openStream(), tomcatPath.toFile());
            ZipUtils.unzip(new java.io.File(warToDeploy.getName()), apiAppContextPath.toFile());
            generateServerXml(tomcatPath.toFile(), runnerConfiguration);
            //add JAR with extension to 'api' application's 'lib' directory
            IoUtil.copy(extensionJar, apiAppContextPath.resolve("WEB-INF/lib").resolve(extensionJar.getName()).toFile(), null);
        } catch (IOException e) {
            throw new RunnerException(e);
        }

        ApplicationProcess process = start(workDir, runnerConfiguration, codeServerProcess, callback);

        // TODO: unregister updater
        registerUpdater(process, new ApplicationUpdater() {
            @Override
            public void update() throws RunnerException {
                try {
                    final ProjectDescriptor projectDescriptor = runnerConfiguration.getRequest().getProjectDescriptor();
                    final java.io.File destinationDir = Files.createTempDirectory(workDir.toPath(), "sources-").toFile();
                    final java.io.File exportProject = Utils.exportProject(projectDescriptor, destinationDir);
                    final java.io.File sourcesDir = Files.createTempDirectory(workDir.toPath(), "sources-build-").toFile();
                    ZipUtils.unzip(exportProject, sourcesDir);
                    ZipFile artifact = Utils.buildProjectFromSources(sourcesDir.toPath(), extensionJar.getName());
                    // add JAR with extension to 'api' application's 'lib' directory
                    IoUtil.copy(new java.io.File(artifact.getName()),
                                apiAppContextPath.resolve("WEB-INF/lib").resolve(extensionJar.getName()).toFile(), null);
                    LOG.debug("Extension {} updated", workDir);
                } catch (Exception e) {
                    LOG.error("Unable to update extension: {}", workDir);
                    throw new RunnerException(e);
                }
            }
        });

        return process;
    }

    protected void registerUpdater(ApplicationProcess process, ApplicationUpdater updater) {
        applicationUpdaterRegistry.registerUpdater(process, updater);
    }

    protected void generateServerXml(java.io.File tomcatDir, SDKRunnerConfiguration runnerConfiguration) throws IOException {
        final String cfg = SERVER_XML.replace("${PORT}", Integer.toString(runnerConfiguration.getHttpPort()));
        final java.io.File serverXmlFile = new java.io.File(new java.io.File(tomcatDir, "conf"), "server.xml");
        Files.write(serverXmlFile.toPath(), cfg.getBytes());
    }

    public int getMemSize() {
        return memSize;
    }

    @Override
    public String toString() {
        return "Tomcat Server";
    }

    /**
     * Start application process
     * @param appDir application directory
     * @param runnerCfg runner configuration
     * @param codeServerProcess code server process
     * @param callback some actions after start application process
     * @return launched application process
     */
    protected abstract ApplicationProcess start(java.io.File appDir,
                                      SDKRunnerConfiguration runnerCfg,
                                      CodeServerProcess codeServerProcess,
                                      ApplicationProcess.Callback callback) throws RunnerException;

    protected static class TomcatLogger implements ApplicationLogger {
        final List<File>                   logFiles;
        final CodeServerProcess codeServerProcess;

        TomcatLogger(List<java.io.File> logFiles, CodeServerProcess codeServerProcess) {
            this.logFiles = logFiles;
            this.codeServerProcess = codeServerProcess;
        }

        @Override
        public void getLogs(Appendable output) throws IOException {
            for (java.io.File logFile : logFiles) {
                output.append(String.format("%n====> %1$s <====%n%n", logFile.getName()));
                try (FileReader r = new FileReader(logFile)) {
                    CharStreams.copy(r, output);
                }
                output.append(System.lineSeparator());
            }
            try {
                codeServerProcess.getLogs(output);
            } catch (Exception ignore) {
            }
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public void writeLine(String line) throws IOException {
            // noop since logs already redirected to the file
        }

        @Override
        public void close() throws IOException {
        }
    }
}