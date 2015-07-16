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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.internal.ApplicationLogger;
import org.eclipse.che.api.runner.internal.DeploymentSources;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.ZipUtils;
import com.google.common.io.CharStreams;
import com.google.inject.Singleton;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstract base implementation to deploy application to Apache Tomcat servlet container.
 *
 * @author Artem Zatsarynnyy
 * @author Roman Nikitenko
 */
@Singleton
public abstract class BaseTomcatServer implements ApplicationServer {
    public static final    String TOMCAT_HOME_PARAMETER = "runner.tomcat.tomcat_home";
    public static final    String MEM_SIZE_PARAMETER    = "runner.tomcat.memory";
    protected static final String SERVER_XML            =
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

    protected final int          memSize;
    protected final File         tomcatHome;
    protected final EventService eventService;

    @Inject
    public BaseTomcatServer(@Named(MEM_SIZE_PARAMETER) int memSize,
                            @Named(TOMCAT_HOME_PARAMETER) File tomcatHome,
                            EventService eventService) {
        this.memSize = memSize;
        this.tomcatHome = tomcatHome;
        this.eventService = eventService;
    }

    @Override
    public final String getName() {
        return "tomcat7";
    }

    @Override
    public String getDescription() {
        return "Apache Tomcat 7.0 is an implementation of the Java Servlet and JavaServer Pages technologies.\n" +
               "Home page: http://tomcat.apache.org/";
    }

    protected void generateServerXml(File tomcatDir, ApplicationServerRunnerConfiguration runnerConfiguration) throws IOException {
        final String cfg = SERVER_XML.replace("${PORT}", Integer.toString(runnerConfiguration.getHttpPort()));
        final File serverXmlFile = new File(new File(tomcatDir, "conf"), "server.xml");
        Files.write(serverXmlFile.toPath(), cfg.getBytes());
    }

    public File getTomcatHome() {
        return tomcatHome;
    }

    public int getMemSize() {
        return memSize;
    }

    @Override
    public String toString() {
        return "Tomcat Server";
    }

    protected void prepare(File appDir, DeploymentSources toDeploy, ApplicationServerRunnerConfiguration runnerConfiguration)
            throws RunnerException {
        final File myTomcatHome = getTomcatHome();
        try {
            final Path tomcatPath = Files.createDirectory(appDir.toPath().resolve("tomcat"));
            IoUtil.copy(myTomcatHome, tomcatPath.toFile(), null);
            final Path webappsPath = tomcatPath.resolve("webapps");
            if (Files.exists(webappsPath)) {
                IoUtil.deleteRecursive(webappsPath.toFile());
            }
            Files.createDirectory(webappsPath);
            final Path rootPath = Files.createDirectory(webappsPath.resolve("ROOT"));
            if (toDeploy.isZipArchive()) {
                ZipUtils.unzip(toDeploy.getFile(), rootPath.toFile());
            } else {
                IoUtil.copy(toDeploy.getFile(), rootPath.toFile(), null);
            }
            generateServerXml(tomcatPath.toFile(), runnerConfiguration);
        } catch (IOException e) {
            throw new RunnerException(e);
        }
    }

    protected static class TomcatLogger implements ApplicationLogger {

        final List<File> logFiles;

        TomcatLogger(List<File> logFiles) {
            this.logFiles = logFiles;
        }

        @Override
        public void getLogs(Appendable output) throws IOException {
            for (File logFile : logFiles) {
                output.append(String.format("%n====> %1$s <====%n%n", logFile.getName()));
                try (FileReader r = new FileReader(logFile)) {
                    CharStreams.copy(r, output);
                }
                output.append(System.lineSeparator());
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