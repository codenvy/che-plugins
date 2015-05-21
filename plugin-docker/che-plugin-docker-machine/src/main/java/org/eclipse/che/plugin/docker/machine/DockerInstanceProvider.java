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
package org.eclipse.che.plugin.docker.machine;

import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.DockerFileException;
import org.eclipse.che.plugin.docker.client.Dockerfile;
import org.eclipse.che.plugin.docker.client.DockerfileParser;
import org.eclipse.che.plugin.docker.client.Exec;
import org.eclipse.che.plugin.docker.client.LogMessage;
import org.eclipse.che.plugin.docker.client.LogMessageProcessor;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.InvalidInstanceSnapshotException;
import org.eclipse.che.api.machine.server.InvalidRecipeException;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.UnsupportedRecipeException;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.commons.lang.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Docker implementation of {@link InstanceProvider}
 *
 * @author andrew00x
 */
@Singleton
public class DockerInstanceProvider implements InstanceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DockerInstanceProvider.class);

    private final DockerConnector   docker;
    private final String            registry;
    private final DockerNodeFactory dockerNodeFactory;
    private final Set<String>       supportedRecipeTypes;

    @Inject
    public DockerInstanceProvider(DockerConnector docker,
                                  @Named("machine.docker.registry") String registry,
                                  DockerNodeFactory dockerNodeFactory) {
        this.docker = docker;
        this.registry = registry;
        this.dockerNodeFactory = dockerNodeFactory;
        final Set<String> recipeTypes = new LinkedHashSet<>(2);
        recipeTypes.add("Dockerfile");
        supportedRecipeTypes = Collections.unmodifiableSet(recipeTypes);
    }

    @Override
    public String getType() {
        return "docker";
    }

    @Override
    public Set<String> getRecipeTypes() {
        return supportedRecipeTypes;
    }

    @Override
    public DockerInstance createInstance(Recipe recipe, final LineConsumer creationLogsOutput)
            throws UnsupportedRecipeException, InvalidRecipeException, MachineException {
        final Dockerfile dockerfile = getDockerFile(recipe);
        if (dockerfile.getImages().isEmpty()) {
            throw new InvalidRecipeException("Unable build docker based machine, Dockerfile found but it doesn't contain base image.");
        }
        File workDir = null;
        try {
            // build docker image
            workDir = Files.createTempDirectory(null).toFile();
            final File dockerfileFile = new File(workDir, "Dockerfile");
            dockerfile.writeDockerfile(dockerfileFile);
            final List<File> files = new LinkedList<>();
            Collections.addAll(files, workDir.listFiles());
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            final ProgressMonitor progressMonitor = new ProgressMonitor() {
                @Override
                public void updateProgress(ProgressStatus currentProgressStatus) {
                    try {
                        // format/beatify logs
                        creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            };
            // don't need repository for now
            final String dockerImage = docker.buildImage(null, progressMonitor, files.toArray(new File[files.size()]));

            return createInstance(dockerImage, creationLogsOutput);

            /*// don't need repository, registry, tag for now
            return new DockerImage(docker,
                                   registry,
                                   dockerNodeFactory,
                                   new DockerInstanceSnapshotKey(null, null, dockerImage, null),
                                   creationLogsOutput);*/
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getMessage(), e);
        } finally {
            if (workDir != null) {
                FileCleaner.addFile(workDir);
            }
        }
    }

    @Override
    public DockerInstance createInstance(InstanceKey instanceKey, final LineConsumer creationLogsOutput)
            throws NotFoundException, InvalidInstanceSnapshotException, MachineException {
        final DockerInstanceKey dockerInstanceKey = new DockerInstanceKey(instanceKey);
        final String repository = dockerInstanceKey.getRepository();
        final String imageId = dockerInstanceKey.getImageId();
        if (repository == null || imageId == null) {
            throw new MachineException("Machine creation failed. Image attributes are not valid");
        }
        try {
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            docker.pull(repository, dockerInstanceKey.getTag(), dockerInstanceKey.getRegistry(), new ProgressMonitor() {
                @Override
                public void updateProgress(ProgressStatus currentProgressStatus) {
                    try {
                        creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            });

            return createInstance(imageId, creationLogsOutput);

            /*return new DockerImage(docker,
                                   registry,
                                   dockerNodeFactory,
                                   new DockerInstanceSnapshotKey(repository,
                                                      dockerInstanceKey.getTag(),
                                                                         imageId,
                                                      dockerInstanceKey.getRegistry()),

                                   creationLogsOutput);*/
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void removeInstanceSnapshot(InstanceKey instanceKey) throws MachineException {
        // use registry API directly because docker doesn't have such API yet
        // https://github.com/docker/docker-registry/issues/45
        final DockerInstanceKey dockerImageKey = new DockerInstanceKey(instanceKey);
        String registry = dockerImageKey.getRegistry();
        String repository = dockerImageKey.getRepository();
        if (registry == null || repository == null) {
            throw new MachineException("Snapshot removing failed. Snapshot attributes are not valid");
        }

        StringBuilder sb = new StringBuilder("http://");// TODO make possible to use https here
        sb.append(registry).append("/v1/repositories/");
        sb.append(repository);
        sb.append("/");// do not remove! Doesn't work without this slash
        try {
            final HttpURLConnection conn = (HttpURLConnection)new URL(sb.toString()).openConnection();
            try {
                conn.setConnectTimeout(30 * 1000);
                conn.setRequestMethod("DELETE");
                // fixme add auth header for secured registry
//                conn.setRequestProperty("Authorization", authHeader);
                final int responseCode = conn.getResponseCode();
                if ((responseCode / 100) != 2) {
                    InputStream in = conn.getErrorStream();
                    if (in == null) {
                        in = conn.getInputStream();
                    }
                    LOG.error(IoUtil.readAndCloseQuietly(in));
                    throw new MachineException("Internal server error occurs. Can't remove snapshot");
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private Dockerfile getDockerFile(Recipe recipe) throws InvalidRecipeException {
        if (recipe.getScript() != null) {
            try {
                return DockerfileParser.parse(recipe.getScript());
            } catch (DockerFileException e) {
                LOG.debug(e.getLocalizedMessage(), e);
                throw new InvalidRecipeException(String.format("Unable build docker based machine. %s", e.getMessage()));
            }
        }
        throw new InvalidRecipeException("Unable build docker based machine, recipe isn't set or doesn't provide Dockerfile and " +
                                         "no Dockerfile found in the list of files attached to this builder.");
    }

    private DockerInstance createInstance(String imageId, LineConsumer outputConsumer) throws MachineException {
        try {
            final ContainerConfig config = new ContainerConfig().withImage(imageId)
                                                                .withMemorySwap(-1)
                                                                .withExposedPorts(Collections.singletonMap("4300", Collections
                                                                        .<String, String>emptyMap()));

            LOG.debug("Creating container from image {}", imageId);

            final String containerId = docker.createContainer(config, null).getId();

            LOG.debug("Container {} has been created successfully", containerId);

            final DockerNode node = dockerNodeFactory.createNode(containerId);
            String hostProjectsFolder = node.getProjectsFolder();

            LOG.debug("Starting container {}", containerId);
            docker.startContainer(containerId,
                                  new HostConfig().withPublishAllPorts(true)
                                                  .withBinds(String.format("%s:%s", hostProjectsFolder, "/projects"),
                                                             "/usr/local/codenvy/terminal:/usr/local/codenvy/terminal"),//TODO add :ro
                                  new LogMessagePrinter(outputConsumer));
            LOG.debug("Container {} has been started successfully", containerId);

            startTerminal(containerId);

            return new DockerInstance(docker, registry, containerId, outputConsumer, node);
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    private void startTerminal(final String container) {
        try {
            final Exec exec = docker.createExec(container, true, "/bin/bash", "-c",
                                                "/usr/local/codenvy/terminal/terminal -addr :4300 -cmd /bin/sh -static /usr/local/codenvy/terminal/");
            docker.startExec(exec.getId(), new LogMessageProcessor() {
                @Override
                public void process(LogMessage logMessage) {
                    LOG.error(String.format("Terminal error in container %s. %s", container, logMessage.getContent()));
                }
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }
}
