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
package org.eclipse.che.plugin.docker.client;

import org.eclipse.che.plugin.docker.client.connection.DockerConnection;
import org.eclipse.che.plugin.docker.client.connection.DockerResponse;
import org.eclipse.che.plugin.docker.client.connection.TcpConnection;
import org.eclipse.che.plugin.docker.client.connection.UnixSocketConnection;
import org.eclipse.che.plugin.docker.client.json.ContainerCommited;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.ContainerCreated;
import org.eclipse.che.plugin.docker.client.json.ContainerExitStatus;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.client.json.ContainerProcesses;
import org.eclipse.che.plugin.docker.client.json.ContainerResource;
import org.eclipse.che.plugin.docker.client.json.ExecConfig;
import org.eclipse.che.plugin.docker.client.json.ExecCreated;
import org.eclipse.che.plugin.docker.client.json.ExecInfo;
import org.eclipse.che.plugin.docker.client.json.ExecStart;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.client.json.Image;
import org.eclipse.che.plugin.docker.client.json.ImageInfo;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.ptr.LongByReference;

import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonNameConvention;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.Size;
import org.eclipse.che.commons.lang.TarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.eclipse.che.plugin.docker.client.CLibraryFactory.getCLibrary;
import static java.io.File.separatorChar;

/**
 * Connects to the docker daemon.
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@Singleton
public class DockerConnector {
    private static final Logger LOG = LoggerFactory.getLogger(DockerConnector.class);

    public static final String UNIX_SOCKET_SCHEME    = "unix";
    public static final String UNIX_SOCKET_PATH      = "/var/run/docker.sock";
    public static final URI    UNIX_SOCKET_URI       = URI.create(UNIX_SOCKET_SCHEME + "://" + UNIX_SOCKET_PATH);
    public static final URI    BOOT2DOCKER_URI       = URI.create("https://192.168.59.103:2376");
    public static final String BOOT2DOCKER_CERTS_DIR = System.getProperty("user.home")
                                                       + separatorChar + ".boot2docker"
                                                       + separatorChar + "certs"
                                                       + separatorChar + "boot2docker-vm";

    private final URI                      dockerDaemonUri;
    private final DockerCertificates       dockerCertificates;
    private final AuthConfigs              authConfigs;
    private final ExecutorService          executor;
    private final Map<String, OOMDetector> oomDetectors;

    public DockerConnector(AuthConfigs authConfigs) {
        this(new DockerConnectorConfiguration(authConfigs));
    }

    public DockerConnector(URI dockerDaemonUri, DockerCertificates dockerCertificates, AuthConfigs authConfigs) {
        this.dockerDaemonUri = dockerDaemonUri;
        this.dockerCertificates = dockerCertificates;
        this.authConfigs = authConfigs;
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("DockerApiConnector-%d").setDaemon(true).build());
        oomDetectors = new ConcurrentHashMap<>();
    }

    @Inject
    private DockerConnector(DockerConnectorConfiguration connectorConfiguration) {
        this(connectorConfiguration.getDockerDaemonUri(),
             connectorConfiguration.getDockerCertificates(),
             connectorConfiguration.getAuthConfigs());
    }

    /**
     * Gets system-wide information.
     *
     * @return system-wide information
     * @throws IOException
     */
    public org.eclipse.che.plugin.docker.client.json.SystemInfo getSystemInfo() throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response = connection.method("GET").path("/info").request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), org.eclipse.che.plugin.docker.client.json.SystemInfo.class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    /**
     * Gets system-wide information.
     *
     * @return system-wide information
     * @throws IOException
     */
    public org.eclipse.che.plugin.docker.client.json.SystemInfo getVersion() throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response = connection.method("GET").path("/version").request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), org.eclipse.che.plugin.docker.client.json.SystemInfo.class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    /**
     * Lists docker images.
     *
     * @return list of docker images
     * @throws IOException
     */
    public Image[] listImages() throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response = connection.method("GET").path("/images/json").request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), Image[].class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    /**
     * Builds new docker image from specified dockerfile.
     *
     * @param repository
     *         full repository name to be applied to newly created image
     * @param progressMonitor
     *         ProgressMonitor for images creation process
     * @param files
     *         files that are needed for creation docker images (e.g. file of directories used in ADD instruction in Dockerfile), one of
     *         them must be Dockerfile.
     * @return image id
     * @throws IOException
     * @throws InterruptedException
     *         if build process was interrupted
     */
    public String buildImage(String repository, ProgressMonitor progressMonitor, File... files) throws IOException, InterruptedException {
        final File tar = Files.createTempFile(null, ".tar").toFile();
        try {
            createTarArchive(tar, files);
            return buildImage(repository, tar, progressMonitor);
        } finally {
            FileCleaner.addFile(tar);
        }
    }

    protected String buildImage(String repository,
                                File tar,
                                final ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        return doBuildImage(repository, tar, progressMonitor, dockerDaemonUri);
    }


    private String getBuildImageId(ProgressStatus progressStatus) {
        final String stream = progressStatus.getStream();
        if (stream != null && stream.startsWith("Successfully built ")) {
            int endSize = 19;
            while (endSize < stream.length() && Character.digit(stream.charAt(endSize), 16) != -1) {
                endSize++;
            }
            return stream.substring(19, endSize);
        }
        return null;
    }


    /**
     * Gets detailed information about docker image.
     *
     * @param image
     *         id or full repository name of docker image
     * @return detailed information about {@code image}
     * @throws IOException
     */
    public ImageInfo inspectImage(String image) throws IOException {
        return doInspectImage(image, dockerDaemonUri);
    }

    protected ImageInfo doInspectImage(String image, URI dockerDaemonUri) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response = connection.method("GET").path(String.format("/images/%s/json", image)).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ImageInfo.class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    public void removeImage(String image, boolean force) throws IOException {
        doRemoveImage(image, force, dockerDaemonUri);
    }

    public void tag(String image, String repository, String tag) throws IOException {
        doTag(image, repository, tag, dockerDaemonUri);
    }

    public void push(String repository,
                     String tag,
                     String registry,
                     final ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        doPush(repository, tag, registry, progressMonitor, dockerDaemonUri);
    }


    /**
     * See <a href="https://docs.docker.com/reference/api/docker_remote_api_v1.16/#create-an-image">Docker remote API # Create an
     * image</a>.
     * To pull from private registry use registry.address:port/image as image. This is not documented.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void pull(String image,
                     String tag,
                     String registry,
                     ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        doPull(image, tag, registry, progressMonitor, dockerDaemonUri);
    }

    public ContainerCreated createContainer(ContainerConfig containerConfig, String containerName) throws IOException {
        return doCreateContainer(containerConfig, containerName, dockerDaemonUri);
    }

    public void startContainer(String container, HostConfig hostConfig, LogMessageProcessor startContainerLogProcessor) throws IOException {
        doStartContainer(container, hostConfig, startContainerLogProcessor, dockerDaemonUri);
    }

    /**
     * Stops container.
     *
     * @param container
     *         container identifier, either id or name
     * @param timeout
     *         time to wait for the container to stop before killing it
     * @param timeunit
     *         time unit of the timeout parameter
     * @throws IOException
     */
    public void stopContainer(String container, long timeout, TimeUnit timeunit) throws IOException {
        stopOOMDetector(container);
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            final DockerResponse response =
                    connection.method("POST").path(String.format("/containers/%s/stop?t=%d", container, timeunit.toSeconds(timeout)))
                              .headers(headers).request();
            final int status = response.getStatus();
            if (!(204 == status || 304 == status)) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Kills running container Kill a running container using specified signal.
     *
     * @param container
     *         container identifier, either id or name
     * @param signal
     *         code of signal, e.g. 9 in case of SIGKILL
     * @throws IOException
     */
    public void killContainer(String container, int signal) throws IOException {
        stopOOMDetector(container);
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            final DockerResponse response = connection.method("POST")
                                                      .path(String.format("/containers/%s/kill?signal=%d", container, signal))
                                                      .headers(headers).request();
            final int status = response.getStatus();
            if (204 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Kills container with SIGKILL signal.
     *
     * @param container
     *         container identifier, either id or name
     * @throws IOException
     */
    public void killContainer(String container) throws IOException {
        killContainer(container, 9);
    }


    /**
     * Removes container.
     *
     * @param container
     *         container identifier, either id or name
     * @param force
     *         if {@code true} kills the running container then remove it
     * @param removeVolumes
     *         if {@code true} removes volumes associated to the container
     * @throws IOException
     */
    public void removeContainer(String container, boolean force, boolean removeVolumes) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response =
                    connection.method("DELETE")
                              .path(String.format("/containers/%s?force=%d&v=%d", container, force ? 1 : 0, removeVolumes ? 1 : 0))
                              .request();
            final int status = response.getStatus();
            if (204 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Blocks until {@code container} stops, then returns the exit code
     *
     * @param container
     *         container identifier, either id or name
     * @return exit code
     * @throws IOException
     */
    public int waitContainer(String container) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            final DockerResponse response =
                    connection.method("POST").path(String.format("/containers/%s/wait", container)).headers(headers).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ContainerExitStatus.class, null, FIRST_LETTER_LOWERCASE).getStatusCode();
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }


    /**
     * Gets detailed information about docker container.
     *
     * @param container
     *         id of container
     * @return detailed information about {@code container}
     * @throws IOException
     */
    public ContainerInfo inspectContainer(String container) throws IOException {
        return doInspectContainer(container, dockerDaemonUri);
    }

    protected ContainerInfo doInspectContainer(String container, URI dockerDaemonUri) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response = connection.method("GET").path(String.format("/containers/%s/json", container)).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ContainerInfo.class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }


    /**
     * Attaches to the container with specified id.
     *
     * @param container
     *         id of container
     * @param containerLogsProcessor
     *         output for container logs
     * @param stream
     *         if {@code true} then get 'live' stream from container. Typically need to run this method in separate thread, if {@code
     *         stream} is {@code true} since this method blocks until container is running.
     * @throws java.io.IOException
     */
    public void attachContainer(String container, LogMessageProcessor containerLogsProcessor, boolean stream) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            final String path = String.format("/containers/%s/attach?stream=%d&logs=%d&stdout=%d&stderr=%d", container, (stream ? 1 : 0),
                                              (stream ? 0 : 1), 1, 1);
            final DockerResponse response = connection.method("POST").path(path).headers(headers).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            new LogMessagePumper(response.getInputStream(), containerLogsProcessor).start();
        } finally {
            connection.close();
        }
    }


    public String commit(String container, String repository, String tag, String comment, String author) throws IOException {
        // todo: pause container
        return doCommit(container, repository, tag, comment, author, dockerDaemonUri);
    }

    /**
     * Copies file or directory {@code path} from {@code container} to the {code hostPath}.
     *
     * @param container
     *         container id
     * @param path
     *         path to file or directory inside container
     * @param hostPath
     *         path to the directory on host filesystem
     * @throws IOException
     */
    public void copy(String container, String path, File hostPath) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/json"));
            final String entity = JsonHelper.toJson(new ContainerResource().withResource(path), FIRST_LETTER_LOWERCASE);
            headers.add(Pair.of("Content-Length", entity.getBytes().length));
            final DockerResponse response = connection.method("POST").path(String.format("/containers/%s/copy", container))
                                                      .headers(headers).entity(entity).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            // TarUtils uses apache commons compress library for working with tar archive and it fails
            // (e.g. doesn't unpack all files from archive in case of coping directory) when we try to use stream from docker remote API.
            // Docker sends tar contents as sequence of chunks and seems that causes problems for apache compress library.
            // The simplest solution is spool content to temporary file and then unpack it to destination folder.
            final Path spoolFilePath = Files.createTempFile("docker-copy-spool-", ".tar");
            try {
                Files.copy(response.getInputStream(), spoolFilePath, StandardCopyOption.REPLACE_EXISTING);
                try (InputStream tarStream = Files.newInputStream(spoolFilePath)) {
                    TarUtils.untar(tarStream, hostPath);
                }
            } finally {
                FileCleaner.addFile(spoolFilePath.toFile());
            }
        } finally {
            connection.close();
        }
    }


    public Exec createExec(String container, boolean detach, String... cmd) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/json"));
            final ExecConfig execConfig = new ExecConfig().withCmd(cmd);
            if (!detach) {
                execConfig.withAttachStderr(true).withAttachStdout(true);
            }
            final String entity = JsonHelper.toJson(execConfig, FIRST_LETTER_LOWERCASE);
            headers.add(Pair.of("Content-Length", entity.getBytes().length));
            final DockerResponse response = connection.method("POST").path(String.format("/containers/%s/exec", container))
                                                      .headers(headers).entity(entity).request();
            final int status = response.getStatus();
            if (status / 100 != 2) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            String execId = JsonHelper.fromJson(response.getInputStream(), ExecCreated.class, null, FIRST_LETTER_LOWERCASE).getId();
            return new Exec(cmd, execId);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    public void startExec(String execId, LogMessageProcessor execOutputProcessor) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/json"));
            final ExecStart execStart = new ExecStart().withDetach(execOutputProcessor == null);
            final String entity = JsonHelper.toJson(execStart, FIRST_LETTER_LOWERCASE);
            headers.add(Pair.of("Content-Length", entity.getBytes().length));
            final DockerResponse response = connection.method("POST")
                                                      .path(String.format("/exec/%s/start", execId))
                                                      .headers(headers)
                                                      .entity(entity)
                                                      .request();
            final int status = response.getStatus();
            // According to last doc (https://docs.docker.com/reference/api/docker_remote_api_v1.15/#exec-start) status must be 201 but
            // in fact docker API returns 200 or 204 status.
            if (status / 100 != 2) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            if (status != 204 && execOutputProcessor != null) {
                new LogMessagePumper(response.getInputStream(), execOutputProcessor).start();
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Gets detailed information about exec
     *
     * @return detailed information about {@code execId}
     * @throws IOException
     */
    public ExecInfo getExecInfo(String execId) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response = connection.method("GET").path(String.format("/exec/%s/json", execId)).request();

            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ExecInfo.class, null, FIRST_LETTER_LOWERCASE);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }


    public ContainerProcesses top(String container, String... psArgs) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            final String path;
            if (psArgs == null || psArgs.length == 0) {
                path = String.format("/containers/%s/top", container);
            } else {
                final StringBuilder pathBuilder = new StringBuilder();
                pathBuilder.append("/containers/").append(container).append("/top?ps_args=");
                for (int i = 0, l = psArgs.length; i < l; i++) {
                    if (i > 0) {
                        pathBuilder.append('+');
                    }
                    pathBuilder.append(URLEncoder.encode(psArgs[i], "UTF-8"));
                }
                path = pathBuilder.toString();
            }
            final DockerResponse response = connection.method("GET").path(path).headers(headers).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ContainerProcesses.class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    protected String doBuildImage(String repository,
                                  File tar,
                                  final ProgressMonitor progressMonitor,
                                  URI dockerDaemonUri) throws IOException, InterruptedException {
        DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/x-compressed-tar"));
            headers.add(Pair.of("Content-Length", tar.length()));
            final DockerResponse response;
            try (InputStream tarInput = new FileInputStream(tar)) {
                response = connection.method("POST").path(String.format("/build?t=%s&rm=%d&pull=%d", repository, 1, 1)).headers(headers)
                                     .entity(tarInput).request();
            }
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            final ValueHolder<IOException> errorHolder = new ValueHolder<>();
            final ValueHolder<String> imageIdHolder = new ValueHolder<>();
            final ProgressStatusReader progressReader = new ProgressStatusReader(response.getInputStream());
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressStatus progressStatus;
                        while ((progressStatus = progressReader.next()) != null) {
                            final String buildImageId = getBuildImageId(progressStatus);
                            if (buildImageId != null) {
                                imageIdHolder.set(buildImageId);
                            }
                            progressMonitor.updateProgress(progressStatus);
                        }
                    } catch (IOException e) {
                        errorHolder.set(e);
                    }
                    synchronized (this) {
                        notify();
                    }
                }
            };
            executor.execute(runnable);
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (runnable) {
                runnable.wait();
            }
            final IOException ioe = errorHolder.get();
            if (ioe != null) {
                throw ioe;
            }
            if (imageIdHolder.get() == null) {
                throw new IOException("Docker image build failed");
            }
            return imageIdHolder.get();
        } finally {
            connection.close();
        }
    }

    protected void doRemoveImage(String image, boolean force, URI dockerDaemonUri) throws IOException {
        DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final DockerResponse response =
                    connection.method("DELETE").path(String.format("/images/%s?force=%d", image, force ? 1 : 0)).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            String output = CharStreams.toString(new InputStreamReader(response.getInputStream()));
            LOG.debug("remove image: {}", output);
        } finally {
            connection.close();
        }
    }

    protected void doTag(String image, String repository, String tag, URI dockerDaemonUri) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(3);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            final StringBuilder pathBuilder = new StringBuilder("/images/");
            pathBuilder.append(image);
            pathBuilder.append("/tag");
            pathBuilder.append("?repo=");
            pathBuilder.append(repository);
            pathBuilder.append("&force=");
            pathBuilder.append(0);
            if (tag != null) {
                pathBuilder.append("&tag=");
                pathBuilder.append(tag);
            }
            final DockerResponse response = connection.method("POST").path(pathBuilder.toString()).headers(headers).request();
            final int status = response.getStatus();
            if (201 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
        } finally {
            connection.close();
        }
    }

    protected void doPush(String repository,
                          String tag,
                          String registry,
                          final ProgressMonitor progressMonitor,
                          URI dockerDaemonUri) throws IOException, InterruptedException {
        DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(3);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            headers.add(Pair.of("X-Registry-Auth", authConfigs.getAuthHeader(registry)));
            final StringBuilder pathBuilder = new StringBuilder("/images/");
            if (registry != null) {
                pathBuilder.append(registry).append("/").append(repository);
            } else {
                pathBuilder.append(repository);
            }
            pathBuilder.append("/push");
            if (tag != null) {
                pathBuilder.append("?tag=");
                pathBuilder.append(tag);
            }
            final DockerResponse response = connection.method("POST").path(pathBuilder.toString()).headers(headers).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            final ValueHolder<IOException> errorHolder = new ValueHolder<>();
            final ProgressStatusReader progressReader = new ProgressStatusReader(response.getInputStream());
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressStatus progressStatus;
                        while ((progressStatus = progressReader.next()) != null) {
                            progressMonitor.updateProgress(progressStatus);
                        }
                    } catch (IOException e) {
                        errorHolder.set(e);
                    }
                    synchronized (this) {
                        notify();
                    }
                }
            };
            executor.execute(runnable);
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (runnable) {
                runnable.wait();
            }
            final IOException ioe = errorHolder.get();
            if (ioe != null) {
                throw ioe;
            }
        } finally {
            connection.close();
        }
    }

    protected String doCommit(String container,
                              String repository,
                              String tag,
                              String comment,
                              String author,
                              URI dockerDaemonUri) throws IOException {
        DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final StringBuilder pathBuilder = new StringBuilder("/commit?container=");
            pathBuilder.append(container);
            pathBuilder.append("&repo=");
            pathBuilder.append(repository);
            if (tag != null) {
                pathBuilder.append("&tag=");
                pathBuilder.append(tag);
            }
            if (comment != null) {
                pathBuilder.append("&comment=");
                pathBuilder.append(URLEncoder.encode(comment, "UTF-8"));
            }
            if (author != null) {
                pathBuilder.append("&author=");
                pathBuilder.append(URLEncoder.encode(author, "UTF-8"));
            }
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/json"));
            final String entity = "{}";
            headers.add(Pair.of("Content-Length", entity.getBytes().length));
            final DockerResponse response =
                    connection.method("POST").path(pathBuilder.toString()).headers(headers).entity(entity).request();
            final int status = response.getStatus();
            if (201 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ContainerCommited.class, null, FIRST_LETTER_LOWERCASE).getId();
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    protected void doPull(String image,
                          String tag,
                          String registry,
                          final ProgressMonitor progressMonitor,
                          URI dockerDaemonUri) throws IOException, InterruptedException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(3);
            headers.add(Pair.of("Content-Type", "text/plain"));
            headers.add(Pair.of("Content-Length", 0));
            headers.add(Pair.of("X-Registry-Auth", authConfigs.getAuthHeader(registry)));
            final StringBuilder pathBuilder = new StringBuilder("/images/create?fromImage=");
            if (registry != null) {
                pathBuilder.append(registry).append("/");
            }
            pathBuilder.append(image);
            if (tag != null) {
                pathBuilder.append("&tag=");
                pathBuilder.append(tag);
            }
            final DockerResponse response = connection.method("POST").path(pathBuilder.toString()).headers(headers).request();
            final int status = response.getStatus();
            if (200 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            final ValueHolder<IOException> errorHolder = new ValueHolder<>();
            final ProgressStatusReader progressReader = new ProgressStatusReader(response.getInputStream());
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressStatus progressStatus;
                        while ((progressStatus = progressReader.next()) != null) {
                            progressMonitor.updateProgress(progressStatus);
                        }
                    } catch (IOException e) {
                        errorHolder.set(e);
                    }
                    synchronized (this) {
                        notify();
                    }
                }
            };
            executor.execute(runnable);
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (runnable) {
                runnable.wait();
            }
            final IOException ioe = errorHolder.get();
            if (ioe != null) {
                throw ioe;
            }
        } finally {
            connection.close();
        }
    }

    protected ContainerCreated doCreateContainer(ContainerConfig containerConfig,
                                                 String containerName,
                                                 URI dockerDaemonUri) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/json"));
            final String entity = JsonHelper.toJson(containerConfig, FIRST_LETTER_LOWERCASE);
            headers.add(Pair.of("Content-Length", entity.getBytes().length));
            final StringBuilder pathBuilder = new StringBuilder("/containers/create");
            if (containerName != null) {
                pathBuilder.append("?name=");
                pathBuilder.append(containerName);
            }
            final DockerResponse response =
                    connection.method("POST").path(pathBuilder.toString()).headers(headers).entity(entity).request();
            final int status = response.getStatus();
            if (201 != status) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            return JsonHelper.fromJson(response.getInputStream(), ContainerCreated.class, null, FIRST_LETTER_LOWERCASE);
        } catch (JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            connection.close();
        }
    }

    protected void doStartContainer(String container,
                                    HostConfig hostConfig,
                                    LogMessageProcessor startContainerLogProcessor,
                                    URI dockerDaemonUri) throws IOException {
        final DockerConnection connection = openConnection(dockerDaemonUri);
        try {
            final List<Pair<String, ?>> headers = new ArrayList<>(2);
            headers.add(Pair.of("Content-Type", "application/json"));
            final String entity = hostConfig == null ? "{}" : JsonHelper.toJson(hostConfig, FIRST_LETTER_LOWERCASE);
            headers.add(Pair.of("Content-Length", entity.getBytes().length));
            final DockerResponse response = connection.method("POST").path(String.format("/containers/%s/start", container))
                                                      .headers(headers).entity(entity).request();
            final int status = response.getStatus();
            if (!(204 == status || 304 == status)) {
                final String msg = CharStreams.toString(new InputStreamReader(response.getInputStream()));
                throw new DockerException(String.format("Error response from docker API, status: %d, message: %s", status, msg), status);
            }
            if (204 == status) {
                startOOMDetector(container, startContainerLogProcessor);
            }
        } finally {
            connection.close();
        }
    }

    // Unfortunately we can't use generated DTO here.
    // Docker uses uppercase in first letter in names of json objects, e.g. {"Id":"123"} instead of {"id":"123"}
    protected static JsonNameConvention FIRST_LETTER_LOWERCASE = new JsonNameConvention() {
        @Override
        public String toJsonName(String javaName) {
            return Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1);
        }

        @Override
        public String toJavaName(String jsonName) {
            return Character.toLowerCase(jsonName.charAt(0)) + jsonName.substring(1);
        }
    };


    protected DockerConnection openConnection(URI dockerDaemonUri) {
        if (isUnixSocketUri(dockerDaemonUri)) {
            return new UnixSocketConnection(dockerDaemonUri.getPath());
        } else {
            return new TcpConnection(dockerDaemonUri, dockerCertificates);
        }
    }


    private boolean isUnixSocketUri(URI uri) {
        return UNIX_SOCKET_SCHEME.equals(uri.getScheme());
    }


    private void createTarArchive(File tar, File... files) throws IOException {
        TarUtils.tarFiles(tar, 0, files);
    }


    // OOM detect

    private void startOOMDetector(String container, LogMessageProcessor containerLogProcessor) {
        if (needStartOOMDetector()) {
            if (cgroupMount == null) {
                LOG.warn("System doesn't support OOM events");
                return;
            }
            final OOMDetector oomDetector = new OOMDetector(container, containerLogProcessor);
            oomDetectors.put(container, oomDetector);
            oomDetector.start();
        }
    }

    private boolean needStartOOMDetector() {
        if (isUnixSocketUri(dockerDaemonUri)) {
            return true;
        }
        if (SystemInfo.isLinux()) {
            final String dockerDaemonHost = dockerDaemonUri.getHost();
            if ("localhost".equals(dockerDaemonHost) || "127.0.0.1".equals(dockerDaemonHost)) {
                return true;
            }
        }
        return false;
    }

    private void stopOOMDetector(String container) {
        final OOMDetector oomDetector = oomDetectors.remove(container);
        if (oomDetector != null) {
            oomDetector.stop();
        }
    }


    /*
     * Need detect OOM errors and notify users about them. Without such notification if application is killed by oom-killer client often can
     * see message "Killed" and there is no any why to see why. Unfortunately for now docker doesn't provide clear mechanism how to control
     * OOM errors, with docker event mechanism can get something like that:
     * {"status":"die","id":"dfdf82bd3881","from":"base:latest","time":1374067970}
     * That is not enough.
     * Found two ways how to control OOM errors.
     *
     *     1. With parsing output of 'dmesg' command
     * ----
     * andrew@andrey:~> dmesg | grep oom-killer
     * [41313.629018] java invoked oom-killer: gfp_mask=0xd0, order=0, oom_score_adj=0
     * [41631.391818] java invoked oom-killer: gfp_mask=0xd0, order=0, oom_score_adj=0
     * ...
     * -----
     * Problem here is in timestamp format. Unfortunately dmesg doesn't provide real time correctly with -T option. Here is a piece of man
     * page:
     * -----
     * -T, --ctime
     *         Print human readable timestamps.  The timestamp could be inaccurate!
     *
     *         The time source used for the logs is not updated after system SUSPEND/RESUME.
     * -----
     * So it's complicated to detect time when oom-killer was activated and link its activity with failed docker container.
     *
     *     2. Usage of cgroup notification mechanism.
     * Good article about this: https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Resource_Management_Guide/sec-Using_the_Notification_API.html
     */
    private static String  cgroupMount;
    private static boolean systemd;

    static {
        if (SystemInfo.isLinux()) {
            final String mounts = "/proc/mounts";
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(mounts), Charset.forName("UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] a = line.split("\\s+");
                    // line has format: "DEVICE PATH FILESYSTEM FLAGS_DELIMITED_BY_COMMAS ??? ???"
                    String filesystem = a[2];
                    if ("cgroup".equals(filesystem)) {
                        String path = a[1];
                        if (path.endsWith("cpu")
                            || path.endsWith("cpuacct")
                            || path.endsWith("cpuset")
                            || path.endsWith("memory")
                            || path.endsWith("devices")
                            || path.endsWith("freezer")) {
                            cgroupMount = Paths.get(path).getParent().toString();
                        } else if (path.endsWith("systemd")) {
                            systemd = true;
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Detects OOM with cgroup notification mechanism.
     * <p/>
     * https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Resource_Management_Guide/sec-Using_the_Notification_API.html
     */
    private class OOMDetector implements Runnable {
        private final String              container;
        private final LogMessageProcessor containerLogProcessor;
        private final CLibrary            cLib;
        private final String              containerCgroup;

        private volatile boolean stopped = false;

        OOMDetector(String container, LogMessageProcessor containerLogProcessor) {
            this.container = container;
            this.containerLogProcessor = containerLogProcessor;
            cLib = getCLibrary();

            if (systemd) {
                containerCgroup = cgroupMount + "/memory/system.slice/docker-" + container + ".scope/";
            } else {
                containerCgroup = cgroupMount + "/memory/docker/" + container + "/";
            }
        }

        @Override
        public void run() {
            final String cf = containerCgroup + "cgroup.event_control";
            final String oomf = containerCgroup + "memory.oom_control";
            int efd = -1;
            int oomfd = -1;
            try {
                if ((efd = cLib.eventfd(0, 1)) == -1) {
                    LOG.error("Unable create a file descriptor for event notification");
                    return;
                }
                int cfd;
                if ((cfd = cLib.open(cf, CLibrary.O_WRONLY)) == -1) {
                    LOG.error("Unable open event control file '{}' for write", cf);
                    return;
                }
                if ((oomfd = cLib.open(oomf, CLibrary.O_RDONLY)) == -1) {
                    LOG.error("Unable open OOM event file '{}' for read", oomf);
                    return;
                }
                final byte[] data = String.format("%d %d", efd, oomfd).getBytes();
                if (cLib.write(cfd, data, data.length) != data.length) {
                    LOG.error("Unable write event control data to file '{}'", cf);
                    return;
                }
                if (cLib.close(cfd) == -1) {
                    LOG.error("Error closing of event control file '{}'", cf);
                    return;
                }
                final LongByReference eventHolder = new LongByReference();
                if (cLib.eventfd_read(efd, eventHolder) == 0) {
                    if (stopped) {
                        return;
                    }
                    LOG.warn("OOM event received for container '{}'", container);
                    if (readCgroupValue("memory.failcnt") > 0) {
                        try {
                            containerLogProcessor.process(new LogMessage(LogMessage.Type.DOCKER,
                                                                         "[ERROR] The processes in this machine need more RAM. This machine started with " +
                                                                         Size.toHumanSize(
                                                                                 inspectContainer(container).getConfig().getMemory())));
                            containerLogProcessor.process(new LogMessage(LogMessage.Type.DOCKER,
                                                                         "[ERROR] Create a new machine configuration that allocates additional RAM or increase" +
                                                                         " the workspace RAM limit in the user dashboard."));
                        } catch (/*IOException*/ Exception e) {
                            LOG.warn(e.getMessage(), e);
                        }
                    }
                }
            } finally {
                if (!stopped) {
                    stopOOMDetector(container);
                }
                close(oomfd);
                close(efd);
            }
        }

        private void close(int fd) {
            if (fd != -1) {
                cLib.close(fd);
            }
        }

        long readCgroupValue(String cgroupFile) {
            final String failCntf = containerCgroup + cgroupFile;
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(failCntf), Charset.forName("UTF-8"))) {
                return Long.parseLong(reader.readLine().trim());
            } catch (IOException e) {
                LOG.warn("Unable read content of file '{}'", failCntf);
            } catch (NumberFormatException e) {
                LOG.error("Unable parse content of file '{}'", failCntf);
            }
            return 0;
        }

        void start() {
            executor.execute(this);
        }

        void stop() {
            stopped = true;
        }
    }
}
