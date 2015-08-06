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

import org.eclipse.che.api.project.server.ProjectEvent;
import org.eclipse.che.api.project.server.ProjectEventListener;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.commons.lang.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * GWT code server process.
 *
 * @author Artem Zatsarynnyy
 */
public abstract class CodeServerProcess implements ProjectEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(CodeServer.class);
    private final   String          bindAddress;
    protected final int             port;
    protected final java.io.File    startUpScriptFile;
    protected final java.io.File    workDir;
    private final   Path            extensionSourcesPath;
    private final   String          projectApiBaseUrl;
    private final   ExecutorService executor;

    protected Process process;

    protected CodeServerProcess(String bindAddress, int port, File startUpScriptFile, File workDir, Path extensionSourcesPath,
                                String projectApiBaseUrl, ExecutorService executor) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.startUpScriptFile = startUpScriptFile;
        this.workDir = workDir;
        this.extensionSourcesPath = extensionSourcesPath;
        this.projectApiBaseUrl = projectApiBaseUrl;
        this.executor = executor;
    }

    /**
     * Start CodeServerProcess
     * @throws RunnerException
     */
    public abstract void start() throws RunnerException ;

    /**
     * Stop CodeServerProcess
     * @throws RunnerException
     */
    public abstract void stop() throws RunnerException ;

    /**
     * Append some log content to output
     * @param output target output
     * @throws IOException
     * @throws RunnerException
     */
    public void appendLogs(Appendable output) throws IOException, RunnerException {
        final String url = bindAddress + ':' + port + "/log/_app";
        final String logContent = sendGet(new URL(url.startsWith("http://") ? url : "http://" + url));
        output.append(String.format("%n====> GWT-code-server.log <===="));
        output.append(logContent);
        output.append(System.lineSeparator());
    }

    private String sendGet(URL url) throws IOException, RunnerException {
        HttpURLConnection http = null;
        try {
            http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("GET");
            http.setConnectTimeout(1000);
            http.setReadTimeout(1000);
            int responseCode = http.getResponseCode();
            if (responseCode != 200) {
                responseFail(http);
            }

            try (InputStream data = http.getInputStream()) {
                return readBodyTagContent(data);
            }
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    private String readBodyTagContent(InputStream stream) throws IOException {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(stream);
            return doc.getElementsByTagName("body").item(0).getTextContent();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void responseFail(HttpURLConnection http) throws IOException, RunnerException {
        InputStream errorStream = http.getErrorStream();
        try {
            String body = null;
            if (errorStream != null) {
                body = IoUtil.readAndCloseQuietly(errorStream);
            }
            throw new RunnerException(String.format("Unable to get code server logs. %s", body == null ? "" : body));
        } catch (IOException e) {
            throw new RunnerException("Unable to get code server logs." + e.getMessage());
        }
    }

    @Override
    public void onEvent(ProjectEvent event) {
        update(event, extensionSourcesPath, projectApiBaseUrl, executor);
    }

    /**
     * Update source code after changing
     * @param event project event
     * @param projectMirrorPath project mirror path
     * @param projectApiBaseUrl base project api url
     * @param executor executor service
     */
    private void update(final ProjectEvent event, final Path projectMirrorPath, final String projectApiBaseUrl,
                               ExecutorService executor) {
        if (event.getType() == ProjectEvent.EventType.DELETED) {
            IoUtil.deleteRecursive(projectMirrorPath.resolve(event.getPath()).toFile());
        } else if (event.getType() == ProjectEvent.EventType.UPDATED || event.getType() == ProjectEvent.EventType.CREATED) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // connect to the project API URL
                    int index = projectApiBaseUrl.indexOf(event.getProject());
                    try {
                        HttpURLConnection conn = (HttpURLConnection)new URL(projectApiBaseUrl.substring(0, index)
                                                                                             .concat("/file")
                                                                                             .concat(event.getProject())
                                                                                             .concat("/")
                                                                                             .concat(event.getPath())).openConnection();
                        conn.setConnectTimeout(30 * 1000);
                        conn.setRequestMethod("GET");
                        conn.addRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.connect();

                        // if file has been found, dump the content
                        final int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            java.io.File updatedFile = new java.io.File(projectMirrorPath.toString(), event.getPath());
                            byte[] buffer = new byte[8192];
                            try (InputStream input = conn.getInputStream();
                                 OutputStream output = new FileOutputStream(updatedFile)) {
                                int bytesRead;
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOG.error("Unable to update mirror of project {} for GWT code server.", event.getProject());
                    }
                }
            });
        }
    }
}
