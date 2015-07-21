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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.InvalidRecipeException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.DockerFileException;
import org.eclipse.che.plugin.docker.client.Dockerfile;
import org.eclipse.che.plugin.docker.client.DockerfileParser;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Docker implementation of {@link InstanceProvider}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerInstanceProvider implements InstanceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DockerInstanceProvider.class);

    private final DockerConnector                  docker;
    private final Set<String>                      supportedRecipeTypes;
    private final DockerMachineFactory             dockerMachineFactory;
    private final Map<String, String>              containerLabels;
    private final Map<String, Map<String, String>> portsToExpose;
    private final Set<String>                      systemVolumes;

    @Inject
    public DockerInstanceProvider(DockerConnector docker,
                                  DockerMachineFactory dockerMachineFactory,
                                  Set<ServerConf> machineServers,
                                  @Named("machine.docker.system_volumes") Set<String> systemVolumes) {
        this.docker = docker;
        this.dockerMachineFactory = dockerMachineFactory;
        this.systemVolumes = systemVolumes;
        this.supportedRecipeTypes = Collections.unmodifiableSet(Collections.singleton("Dockerfile"));

        this.portsToExpose = new HashMap<>();
        this.containerLabels = new HashMap<>();
        for (ServerConf serverConf : machineServers) {
            portsToExpose.put(serverConf.getPort(), Collections.<String, String>emptyMap());
            containerLabels.put("che:server:" + serverConf.getPort() + ":ref", serverConf.getRef());
            containerLabels.put("che:server:" + serverConf.getPort() + ":protocol", serverConf.getProtocol());
        }
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
    public Instance createInstance(Recipe recipe,
                                   String machineId,
                                   String userId,
                                   String workspaceId,
                                   boolean bindWorkspace,
                                   String displayName,
                                   int memorySizeMB,
                                   LineConsumer creationLogsOutput) throws MachineException {
        final Dockerfile dockerfile = parseRecipe(recipe);

        final String dockerImage = buildImage(dockerfile, creationLogsOutput);

        return createInstance(dockerImage,
                              machineId,
                              userId,
                              workspaceId,
                              bindWorkspace,
                              displayName,
                              recipe.getScript(),
                              memorySizeMB,
                              creationLogsOutput);
    }

    private Dockerfile parseRecipe(Recipe recipe) throws InvalidRecipeException {
        final Dockerfile dockerfile = getDockerFile(recipe);
        if (dockerfile.getImages().isEmpty()) {
            throw new InvalidRecipeException("Unable build docker based machine, Dockerfile found but it doesn't contain base image.");
        }
        return dockerfile;
    }

    private Dockerfile getDockerFile(Recipe recipe) throws InvalidRecipeException {
        if (recipe.getScript() == null) {
            throw new InvalidRecipeException("Unable build docker based machine, recipe isn't set or doesn't provide Dockerfile and " +
                                             "no Dockerfile found in the list of files attached to this builder.");
        }
        try {
            return DockerfileParser.parse(recipe.getScript());
        } catch (DockerFileException e) {
            LOG.debug(e.getLocalizedMessage(), e);
            throw new InvalidRecipeException(String.format("Unable build docker based machine. %s", e.getMessage()));
        }
    }

    private String buildImage(Dockerfile dockerfile, final LineConsumer creationLogsOutput) throws MachineException {
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
                        creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            };
            return docker.buildImage(null, progressMonitor, files.toArray(new File[files.size()]));
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getMessage(), e);
        } finally {
            if (workDir != null) {
                FileCleaner.addFile(workDir);
            }
        }
    }

    @Override
    public Instance createInstance(InstanceKey instanceKey,
                                   String machineId,
                                   String userId,
                                   String workspaceId,
                                   boolean bindWorkspace,
                                   String displayName,
                                   String script,
                                   int memorySizeMB,
                                   LineConsumer creationLogsOutput) throws NotFoundException, MachineException {
        final String imageId = pullImage(instanceKey, creationLogsOutput);

        return createInstance(imageId,
                              machineId,
                              userId,
                              workspaceId,
                              bindWorkspace,
                              displayName,
                              script,
                              memorySizeMB,
                              creationLogsOutput);
    }

    private String pullImage(InstanceKey instanceKey, final LineConsumer creationLogsOutput) throws MachineException {
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

        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }

        return imageId;
    }

    @Override
    public void removeInstanceSnapshot(InstanceKey instanceKey) throws SnapshotException {
        // use registry API directly because docker doesn't have such API yet
        // https://github.com/docker/docker-registry/issues/45
        final DockerInstanceKey dockerInstanceKey = new DockerInstanceKey(instanceKey);
        String registry = dockerInstanceKey.getRegistry();
        String repository = dockerInstanceKey.getRepository();
        if (registry == null || repository == null) {
            throw new SnapshotException("Snapshot removing failed. Snapshot attributes are not valid");
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
                    throw new SnapshotException("Internal server error occurs. Can't remove snapshot");
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private Instance createInstance(String imageId,
                                    String machineId,
                                    String creator,
                                    String workspaceId,
                                    boolean bindWorkspace,
                                    String displayName,
                                    String script,
                                    int memorySizeMB,
                                    LineConsumer outputConsumer)
            throws MachineException {
        try {
            final ContainerConfig config = new ContainerConfig().withImage(imageId)
                                                                .withMemorySwap(-1)
                                                                .withMemory((long)memorySizeMB * 1024 * 1024)
                                                                .withLabels(containerLabels)
                                                                .withExposedPorts(portsToExpose);

            final String containerId = docker.createContainer(config, null).getId();

            final DockerNode node = dockerMachineFactory.createNode(containerId);
            String hostProjectsFolder = node.getProjectsFolder();

            if (bindWorkspace) {
                node.bindWorkspace(workspaceId, hostProjectsFolder);
            }

            final ArrayList<String> volumes = new ArrayList<>(systemVolumes.size() + 1);
            volumes.addAll(systemVolumes);
            volumes.add(String.format("%s:%s", hostProjectsFolder, "/projects"));

            docker.startContainer(containerId,
                                  new HostConfig().withPublishAllPorts(true)
                                                  .withBinds(volumes.toArray(new String[volumes.size()])),
                                  new LogMessagePrinter(outputConsumer));

            return dockerMachineFactory.createInstance(machineId,
                                                       workspaceId,
                                                       bindWorkspace,
                                                       creator,
                                                       displayName,
                                                       containerId,
                                                       node,
                                                       outputConsumer,
                                                       script,
                                                       memorySizeMB);
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }
}
