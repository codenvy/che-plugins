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

import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.MachineState;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.machine.server.exception.InvalidRecipeException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.inject.CheBootstrap;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.DockerFileException;
import org.eclipse.che.plugin.docker.client.Dockerfile;
import org.eclipse.che.plugin.docker.client.DockerfileParser;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.machine.ext.DockerExtConfBindingProvider;
import org.eclipse.che.plugin.docker.machine.node.DockerNode;
import org.eclipse.che.plugin.docker.machine.node.WorkspaceFolderPathProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Docker implementation of {@link InstanceProvider}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerInstanceProvider implements InstanceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DockerInstanceProvider.class);

    public static final String API_ENDPOINT_URL_VARIABLE = "CHE_API_ENDPOINT";

    /**
     * Default HOSTNAME that will be added in all docker containers that are started.
     * This host will container the Docker host's ip reachable inside the container.
     */
    public static final String CHE_HOST = "che-host";

    /**
     * Environment variable that will be setup in developer machine
     * will contain ID of a workspace for which this machine has been created
     */
    public static final String CHE_WORKSPACE_ID = "CHE_WORKSPACE_ID";

    /**
     * Environment variable that will be setup in developer machine and contains user token.
     */
    public static final String USER_TOKEN = "USER_TOKEN";

    private final DockerConnector                  docker;
    private final DockerInstanceStopDetector       dockerInstanceStopDetector;
    private final WorkspaceFolderPathProvider      workspaceFolderPathProvider;
    private final boolean                          doForcePullOnBuild;
    private final Set<String>                      supportedRecipeTypes;
    private final DockerMachineFactory             dockerMachineFactory;
    private final Map<String, String>              devMachineContainerLabels;
    private final Map<String, String>              machineContainerLabels;
    private final Map<String, Map<String, String>> portsToExposeOnDevMachine;
    private final Map<String, Map<String, String>> portsToExposeOnMachine;
    private final Set<String>                      systemVolumesForDevMachine;
    private final Set<String>                      systemVolumesForMachine;
    private final String[]                         devMachineEnvVariables;
    private final String[]                         commonEnvVariables;
    private final String[]                         machineExtraHosts;
    private final String                           projectFolderPath;

    @Inject
    public DockerInstanceProvider(DockerConnector docker,
                                  DockerMachineFactory dockerMachineFactory,
                                  DockerInstanceStopDetector dockerInstanceStopDetector,
                                  @Named("machine.docker.dev_machine.machine_servers") Set<ServerConf> devMachineServers,
                                  @Named("machine.docker.machine_servers") Set<ServerConf> allMachineServers,
                                  @Named("machine.docker.dev_machine.machine_volumes_all") Set<String> systemVolumesForDevMachine,
                                  @Named("machine.docker.machine_volumes") Set<String> machinesSystemVolumes,
                                  @Nullable @Named("machine.docker.machine_extra_hosts") String machineExtraHosts,
                                  @Named("machine.docker.che_api.endpoint") String apiEndpoint,
                                  WorkspaceFolderPathProvider workspaceFolderPathProvider,
                                  @Named("che.projects.root") String projectFolderPath,
                                  @Named("machine.docker.pull_image") boolean doForcePullOnBuild)
            throws IOException {

        if (SystemInfo.isWindows()) {
            machinesSystemVolumes = escapePaths(machinesSystemVolumes);
            systemVolumesForDevMachine = escapePaths(systemVolumesForDevMachine);
        }

        this.docker = docker;
        this.dockerMachineFactory = dockerMachineFactory;
        this.dockerInstanceStopDetector = dockerInstanceStopDetector;
        this.workspaceFolderPathProvider = workspaceFolderPathProvider;
        this.doForcePullOnBuild = doForcePullOnBuild;
        this.supportedRecipeTypes = Collections.singleton("Dockerfile");

        this.systemVolumesForDevMachine = Sets.newHashSetWithExpectedSize(machinesSystemVolumes.size()
                                                                          + systemVolumesForDevMachine.size());
        this.systemVolumesForDevMachine.addAll(machinesSystemVolumes);
        this.systemVolumesForDevMachine.addAll(systemVolumesForDevMachine);

        this.systemVolumesForMachine = machinesSystemVolumes;
        this.portsToExposeOnDevMachine = Maps.newHashMapWithExpectedSize(allMachineServers.size() + devMachineServers.size());
        this.portsToExposeOnMachine = Maps.newHashMapWithExpectedSize(allMachineServers.size());
        this.devMachineContainerLabels = Maps.newHashMapWithExpectedSize(2 * allMachineServers.size() + 2 * devMachineServers.size());
        this.machineContainerLabels = Maps.newHashMapWithExpectedSize(2 * allMachineServers.size());
        this.projectFolderPath = projectFolderPath;

        for (ServerConf serverConf : devMachineServers) {
            portsToExposeOnDevMachine.put(serverConf.getPort(), Collections.<String, String>emptyMap());
            devMachineContainerLabels.put("che:server:" + serverConf.getPort() + ":ref", serverConf.getRef());
            devMachineContainerLabels.put("che:server:" + serverConf.getPort() + ":protocol", serverConf.getProtocol());
        }

        for (ServerConf serverConf : allMachineServers) {
            portsToExposeOnMachine.put(serverConf.getPort(), Collections.<String, String>emptyMap());
            portsToExposeOnDevMachine.put(serverConf.getPort(), Collections.<String, String>emptyMap());
            machineContainerLabels.put("che:server:" + serverConf.getPort() + ":ref", serverConf.getRef());
            devMachineContainerLabels.put("che:server:" + serverConf.getPort() + ":ref", serverConf.getRef());
            machineContainerLabels.put("che:server:" + serverConf.getPort() + ":protocol", serverConf.getProtocol());
            devMachineContainerLabels.put("che:server:" + serverConf.getPort() + ":protocol", serverConf.getProtocol());
        }

        commonEnvVariables = new String[0];
        devMachineEnvVariables = new String[] {API_ENDPOINT_URL_VARIABLE + '=' + apiEndpoint,
                                               DockerInstanceMetadata.PROJECTS_ROOT_VARIABLE + '=' + projectFolderPath,
                                               CheBootstrap.CHE_LOCAL_CONF_DIR + '=' + DockerExtConfBindingProvider.EXT_CHE_LOCAL_CONF_DIR};

        // always add the docker host
        String dockerHost = CHE_HOST.concat(":").concat(docker.getDockerHostIp());
        if (isNullOrEmpty(machineExtraHosts)) {
            this.machineExtraHosts = new String[] {dockerHost};
        } else {
            this.machineExtraHosts = ObjectArrays.concat(machineExtraHosts.split(","), dockerHost);
        }
    }

    /**
     * Escape paths for Windows system with boot@docker according to rules given here :
     * https://github.com/boot2docker/boot2docker/blob/master/README.md#virtualbox-guest-additions
     *
     * @param paths
     *         set of paths to escape
     * @return set of escaped path
     */
    protected Set<String> escapePaths(Set<String> paths) {
        return paths.stream().map(this::escapePath).collect(Collectors.toSet());
    }

    /**
     * Escape path for Windows system with boot@docker according to rules given here :
     * https://github.com/boot2docker/boot2docker/blob/master/README.md#virtualbox-guest-additions
     *
     * @param path
     *         path to escape
     * @return escaped path
     */
    protected String escapePath(String path) {
        String esc;
        if (path.indexOf(":") == 1) {
            //check and replace only occurrence of ":" after disk label on Windows host (e.g. C:/)
            // but keep other occurrences it can be marker for docker mount volumes
            // (e.g. /path/dir/from/host:/name/of/dir/in/container                                               )
            esc = path.replaceFirst(":", "").replace('\\', '/');
            esc = Character.toLowerCase(esc.charAt(0)) + esc.substring(1); //letter of disk mark must be lower case
        } else {
            esc = path.replace('\\', '/');
        }
        if (!esc.startsWith("/")) {
            esc = "/" + esc;
        }
        return esc;
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
                                   MachineState machineState,
                                   LineConsumer creationLogsOutput) throws MachineException {
        final Dockerfile dockerfile = parseRecipe(recipe);

        final String machineContainerName = generateContainerName(machineState.getWorkspaceId(), machineState.getName());
        final String machineImageName = "eclipse-che/" + machineContainerName;

        buildImage(dockerfile, creationLogsOutput, machineImageName);

        return createInstance(machineContainerName,
                              machineState,
                              machineImageName,
                              creationLogsOutput);
    }

    @Override
    public Instance createInstance(InstanceKey instanceKey,
                                   MachineState machineState,
                                   LineConsumer creationLogsOutput) throws NotFoundException, MachineException {
        final DockerInstanceKey dockerInstanceKey = new DockerInstanceKey(instanceKey);

        pullImage(dockerInstanceKey, creationLogsOutput);

        final String machineContainerName = generateContainerName(machineState.getWorkspaceId(), machineState.getName());
        final String machineImageName = "eclipse-che/" + machineContainerName;
        final String fullNameOfPulledImage = dockerInstanceKey.getFullName();
        try {
            // tag image with generated name to allow sysadmin recognize it
            docker.tag(fullNameOfPulledImage, machineImageName, null);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new MachineException("Can't create machine from snapshot.");
        }
        try {
            // remove unneeded tag
            docker.removeImage(fullNameOfPulledImage, false);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        return createInstance(machineContainerName,
                              machineState,
                              machineImageName,
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

    private void buildImage(Dockerfile dockerfile, final LineConsumer creationLogsOutput, String imageName) throws MachineException {
        File workDir = null;
        try {
            // build docker image
            workDir = Files.createTempDirectory(null).toFile();
            final File dockerfileFile = new File(workDir, "Dockerfile");
            dockerfile.writeDockerfile(dockerfileFile);
            final List<File> files = new LinkedList<>();
            Collections.addAll(files, workDir.listFiles());
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            final ProgressMonitor progressMonitor = currentProgressStatus -> {
                try {
                    creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            };
            docker.buildImage(imageName,
                              progressMonitor,
                              null,
                              doForcePullOnBuild,
                              files.toArray(new File[files.size()]));
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getMessage(), e);
        } finally {
            if (workDir != null) {
                FileCleaner.addFile(workDir);
            }
        }
    }

    private void pullImage(DockerInstanceKey dockerInstanceKey, final LineConsumer creationLogsOutput) throws MachineException {
        if (dockerInstanceKey.getRepository() == null) {
            throw new MachineException("Machine creation failed. Snapshot state is invalid. Please, contact support.");
        }
        try {
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            docker.pull(dockerInstanceKey.getRepository(),
                        dockerInstanceKey.getTag(),
                        dockerInstanceKey.getRegistry(),
                        currentProgressStatus -> {
                            try {
                                creationLogsOutput.writeLine(progressLineFormatter.format(currentProgressStatus));
                            } catch (IOException e) {
                                LOG.error(e.getLocalizedMessage(), e);
                            }
                        });
        } catch (IOException | InterruptedException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    // TODO rework in accordance with v2 docker registry API
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

    private Instance createInstance(String containerName,
                                    MachineState machineState,
                                    String imageName,
                                    LineConsumer outputConsumer)
            throws MachineException {
        try {
            final Map<String, String> labels;
            final Map<String, Map<String, String>> portsToExpose;
            final Set<String> volumes;
            final String[] env;
            if (machineState.isDev()) {
                labels = devMachineContainerLabels;
                portsToExpose = portsToExposeOnDevMachine;

                // 1 extra element that contains workspace FS folder will be added further
                volumes = Sets.newHashSetWithExpectedSize(systemVolumesForDevMachine.size() + 1);
                volumes.addAll(systemVolumesForDevMachine);
                String[] vars = {CHE_WORKSPACE_ID + '=' + machineState.getWorkspaceId(),
                                 USER_TOKEN + '=' + EnvironmentContext.getCurrent().getUser().getToken()};
                env = ObjectArrays.concat(devMachineEnvVariables, vars, String.class);

                // add workspace FS folder to volumes
                final String projectFolderVolume = String.format("%s:%s",
                                                                 workspaceFolderPathProvider.getPath(machineState.getWorkspaceId()),
                                                                 projectFolderPath);
                volumes.add(SystemInfo.isWindows() ? escapePath(projectFolderVolume) : projectFolderVolume);
            } else {
                labels = machineContainerLabels;
                portsToExpose = portsToExposeOnMachine;
                volumes = systemVolumesForMachine;
                env = commonEnvVariables;
            }

            final HostConfig hostConfig = new HostConfig().withBinds(volumes.toArray(new String[volumes.size()]))
                                                          .withExtraHosts(machineExtraHosts)
                                                          .withPublishAllPorts(true)
                                                          .withMemorySwap(-1)
                                                          .withMemory((long)machineState.getLimits().getMemory() * 1024 * 1024);
            final ContainerConfig config = new ContainerConfig().withImage(imageName)
                                                                .withLabels(labels)
                                                                .withExposedPorts(portsToExpose)
                                                                .withHostConfig(hostConfig)
                                                                .withEnv(env);

            final String containerId = docker.createContainer(config, containerName).getId();

            docker.startContainer(containerId, null);

            final DockerNode node = dockerMachineFactory.createNode(machineState.getWorkspaceId(), containerId);
            if (machineState.isDev()) {
                node.bindWorkspace();
            }

            dockerInstanceStopDetector.startDetection(containerId, machineState.getId());

            return dockerMachineFactory.createInstance(machineState,
                                                       containerId,
                                                       node,
                                                       outputConsumer);
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    String generateContainerName(String workspaceId, String displayName) {
        String userName = EnvironmentContext.getCurrent().getUser().getName();
        final String containerName = userName + '_' + workspaceId + '_' + displayName + '_';

        // removing all not allowed characters + generating random name suffix
        return NameGenerator.generate(containerName.replaceAll("[^a-zA-Z0-9_-]+", ""), 5);
    }
}
