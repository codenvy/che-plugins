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
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.LogMessageProcessor;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.ContainerCreated;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Listeners(MockitoTestNGListener.class)
public class DockerInstanceProviderTest {

    private static final String API_ENDPOINT_VALUE = "apiEndpoint";

    @Mock
    private DockerConnector dockerConnector;

    @Mock
    private DockerMachineFactory dockerMachineFactory;

    @Mock
    private DockerNode dockerNode;

    private DockerInstanceProvider dockerInstanceProvider;

    @BeforeMethod
    public void setUp() throws Exception {
        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.<ServerConf>emptySet(),
                                                            Collections.<ServerConf>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerMachineFactory.createNode(anyString())).thenReturn(dockerNode);
    }

    @Test
    public void shouldReturnTypeDocker() throws Exception {
        assertEquals(dockerInstanceProvider.getType(), "docker");
    }

    @Test
    public void shouldReturnRecipeTypesDockerfile() throws Exception {
        assertEquals(dockerInstanceProvider.getRecipeTypes(), Collections.singleton("Dockerfile"));
    }

    // TODO add tests for instance snapshot removal

    @Test
    public void shouldBuildDockerfileOnInstanceCreationFromRecipe() throws Exception {
        when(dockerConnector.buildImage(anyString(), any(ProgressMonitor.class), any(AuthConfigs.class), anyVararg()))
                .thenReturn("builtImageId");
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromRecipe();


        verify(dockerConnector).buildImage(anyString(), any(ProgressMonitor.class), any(AuthConfigs.class), anyVararg());
    }

    @Test
    public void shouldPullDockerImageOnInstanceCreationFromSnapshot() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        String repo = "repo";
        String tag = "tag";
        String registry = "localhost:1234";


        createInstanceFromSnapshot(repo, tag, registry);


        verify(dockerConnector).pull(eq(repo), eq(tag), eq(registry), any(ProgressMonitor.class));
    }

    @Test
    public void shouldCreateContainerOnInstanceCreationFromRecipe() throws Exception {
        String builtImageId = "builtImageId";
        when(dockerConnector.buildImage(anyString(), any(ProgressMonitor.class), any(AuthConfigs.class), anyVararg()))
                .thenReturn(builtImageId);
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromRecipe();


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getImage(), builtImageId);
    }

    @Test
    public void shouldStartContainerOnCreateInstanceFromRecipe() throws Exception {
        String containerId = "containerId";
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated(containerId, new String[0]));


        createInstanceFromRecipe();


        verify(dockerConnector).startContainer(eq(containerId), any(HostConfig.class), any(LogMessageProcessor.class));
    }

    @Test
    public void shouldCreateContainerOnInstanceCreationFromSnapshot() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromSnapshot();


        verify(dockerConnector).createContainer(any(ContainerConfig.class), anyString());
    }

    @Test
    public void shouldStartContainerOnCreateInstanceFromSnapshot() throws Exception {
        String containerId = "containerId";
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated(containerId, new String[0]));


        createInstanceFromSnapshot();


        verify(dockerConnector).startContainer(eq(containerId), any(HostConfig.class), any(LogMessageProcessor.class));
    }

    @Test
    public void shouldCallCreationDockerInstanceWithFactoryOnCreateInstanceFromSnapshot() throws Exception {
        final String machineId = "machineId";
        final String workspaceId = "workspaceId";
        final String userId = "userId";
        final boolean isDev = false;
        final int memorySizeInMB = 64;
        final String displayName = "Display name";
        final String containerId = "containerId";
        final Recipe recipe = new RecipeImpl().withType("Dockerfile").withScript("FROM busybox");

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated(containerId, new String[0]));


        createInstanceFromSnapshot(isDev, memorySizeInMB, machineId, userId, workspaceId, displayName, recipe);


        verify(dockerMachineFactory).createInstance(eq(machineId),
                                                    eq(workspaceId),
                                                    eq(isDev),
                                                    eq(userId),
                                                    eq(displayName),
                                                    eq(containerId),
                                                    eq(dockerNode),
                                                    any(LineConsumer.class),
                                                    eq(recipe),
                                                    eq(memorySizeInMB));
    }

    @Test
    public void shouldCallCreationDockerInstanceWithFactoryOnCreateInstanceFromRecipe() throws Exception {
        final String machineId = "machineId";
        final String workspaceId = "workspaceId";
        final String userId = "userId";
        final boolean isDev = false;
        final int memorySizeInMB = 64;
        final String displayName = "Display name";
        final String containerId = "containerId";
        final Recipe recipe = new RecipeImpl().withType("Dockerfile").withScript("FROM busybox");

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated(containerId, new String[0]));


        createInstanceFromRecipe(isDev, memorySizeInMB, machineId, userId, workspaceId, displayName, recipe);


        verify(dockerMachineFactory).createInstance(eq(machineId),
                                                    eq(workspaceId),
                                                    eq(isDev),
                                                    eq(userId),
                                                    eq(displayName),
                                                    eq(containerId),
                                                    eq(dockerNode),
                                                    any(LineConsumer.class),
                                                    eq(recipe),
                                                    eq(memorySizeInMB));
    }

    @Test
    public void shouldBindWorkspaceOnDevInstanceCreationFromRecipe() throws Exception {
        final boolean isDev = true;
        final String workspaceId = "workspaceId";
        final String hostProjectsFolder = "/tmp/projects";

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(hostProjectsFolder);

        createInstanceFromRecipe(isDev, workspaceId);

        verify(dockerNode).bindWorkspace(eq(workspaceId), eq(hostProjectsFolder));
    }

    @Test
    public void shouldBindWorkspaceOnDevInstanceCreationFromSnapshot() throws Exception {
        final boolean isDev = true;
        final String workspaceId = "workspaceId";
        final String hostProjectsFolder = "/tmp/projects";

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(hostProjectsFolder);

        createInstanceFromSnapshot(isDev, workspaceId);

        verify(dockerNode).bindWorkspace(eq(workspaceId), eq(hostProjectsFolder));
    }

    @Test
    public void shouldNotBindWorkspaceOnNonDevInstanceCreationFromRecipe() throws Exception {
        final boolean isDev = false;
        final String workspaceId = "workspaceId";

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn("/tmp/projects");

        createInstanceFromRecipe(isDev, workspaceId);

        verify(dockerNode, never()).bindWorkspace(anyString(), anyString());
    }

    @Test
    public void shouldNotBindWorkspaceOnNonDevInstanceCreationFromSnapshot() throws Exception {
        final boolean isDev = false;
        final String workspaceId = "workspaceId";

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn("/tmp/projects");

        createInstanceFromSnapshot(isDev, workspaceId);

        verify(dockerNode, never()).bindWorkspace(anyString(), anyString());
    }

    @Test
    public void shouldAddApiEndpointEnvVariableOnDevInstanceCreationFromRecipe() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromRecipe(true);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getEnv(),
                     new String[]{DockerInstanceProvider.API_ENDPOINT_URL_VARIABLE + "=" + API_ENDPOINT_VALUE});
    }

    @Test
    public void shouldAddApiEndpointEnvVariableOnDevInstanceCreationFromSnapshot() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromSnapshot(true);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getEnv(),
                     new String[]{DockerInstanceProvider.API_ENDPOINT_URL_VARIABLE + "=" + API_ENDPOINT_VALUE});
    }

    @Test
    public void shouldNotAddApiEndpointEnvVariableOnNonDevInstanceCreationFromRecipe() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromRecipe(false);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getEnv(), new String[0]);
    }

    @Test
    public void shouldNotAddApiEndpointEnvVariableOnNonDevInstanceCreationFromSnapshot() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromSnapshot(false);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getEnv(), new String[0]);
    }

    @Test
    public void shouldSetMemorySizeInContainersOnInstanceCreationFromRecipe() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        int memorySizeMB = 234;


        createInstanceFromRecipe(memorySizeMB);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        // docker accepts memory size in bytes
        assertEquals(argumentCaptor.getValue().getMemory(), memorySizeMB * 1024 * 1024);
    }

    @Test
    public void shouldSetMemorySizeInContainersOnInstanceCreationFromSnapshot() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        int memorySizeMB = 234;


        createInstanceFromSnapshot(memorySizeMB);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        // docker accepts memory size in bytes
        assertEquals(argumentCaptor.getValue().getMemory(), memorySizeMB * 1024 * 1024);
    }

    @Test
    public void shouldDisableSwapMemorySizeInContainersOnInstanceCreationFromRecipe() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromRecipe();


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getMemorySwap(), -1);
    }

    @Test
    public void shouldDisableSwapMemorySizeInContainersOnInstanceCreationFromSnapshot() throws Exception {
        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));


        createInstanceFromSnapshot();


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getMemorySwap(), -1);
    }

    @Test
    public void shouldAddCommonAndDevLabelsToContainerOnDevInstanceCreationFromRecipe() throws Exception {
        final Map<String, String> expectedLabels = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedLabels.put("che:server:" + server.getPort() + ":ref", server.getRef());
            expectedLabels.put("che:server:" + server.getPort() + ":protocol", server.getProtocol());
        }

        final Set<ServerConf> devServers = new HashSet<>(asList(new ServerConf("reference3", "8082", "https"),
                                                                new ServerConf("reference4", "8083", "sftp")));
        for (ServerConf server : devServers) {
            expectedLabels.put("che:server:" + server.getPort() + ":ref", server.getRef());
            expectedLabels.put("che:server:" + server.getPort() + ":protocol", server.getProtocol());
        }

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            devServers,
                                                            commonServers,
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = true;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getLabels(), expectedLabels);
    }

    @Test
    public void shouldAddOnlyCommonLabelsToContainerOnNonDevInstanceCreationFromRecipe() throws Exception {
        final Map<String, String> expectedLabels = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedLabels.put("che:server:" + server.getPort() + ":ref", server.getRef());
            expectedLabels.put("che:server:" + server.getPort() + ":protocol", server.getProtocol());
        }

        final Set<ServerConf> devServers = new HashSet<>(asList(new ServerConf("reference3", "8082", "https"),
                                                                new ServerConf("reference4", "8083", "sftp")));

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            devServers,
                                                            commonServers,
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = false;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getLabels(), expectedLabels);
    }

    @Test
    public void shouldAddCommonAndDevLabelsToContainerOnDevInstanceCreationFromSnapshot() throws Exception {
        final Map<String, String> expectedLabels = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedLabels.put("che:server:" + server.getPort() + ":ref", server.getRef());
            expectedLabels.put("che:server:" + server.getPort() + ":protocol", server.getProtocol());
        }

        final Set<ServerConf> devServers = new HashSet<>(asList(new ServerConf("reference3", "8082", "https"),
                                                                new ServerConf("reference4", "8083", "sftp")));
        for (ServerConf server : devServers) {
            expectedLabels.put("che:server:" + server.getPort() + ":ref", server.getRef());
            expectedLabels.put("che:server:" + server.getPort() + ":protocol", server.getProtocol());
        }

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            devServers,
                                                            commonServers,
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = true;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getLabels(), expectedLabels);
    }

    @Test
    public void shouldAddOnlyCommonLabelsToContainerOnNonDevInstanceCreationFromSnapshot() throws Exception {
        final Map<String, String> expectedLabels = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedLabels.put("che:server:" + server.getPort() + ":ref", server.getRef());
            expectedLabels.put("che:server:" + server.getPort() + ":protocol", server.getProtocol());
        }

        final Set<ServerConf> devServers = new HashSet<>(asList(new ServerConf("reference3", "8082", "https"),
                                                                new ServerConf("reference4", "8083", "sftp")));

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            devServers,
                                                            commonServers,
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = false;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());
        assertEquals(argumentCaptor.getValue().getLabels(), expectedLabels);
    }

    @Test
    public void shouldExposeCommonAndDevPortsToContainerOnDevInstanceCreationFromRecipe() throws Exception {
        Map<String, Map<String, String>> expectedExposedPorts = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedExposedPorts.put(server.getPort(), Collections.<String, String>emptyMap());
        }

        final Set<ServerConf> devServers = new HashSet<>(asList(new ServerConf("reference3", "8082", "https"),
                                                                new ServerConf("reference4", "8083", "sftp")));
        for (ServerConf server : devServers) {
            expectedExposedPorts.put(server.getPort(), Collections.<String, String>emptyMap());
        }

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            devServers,
                                                            commonServers,
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = true;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());

        assertEquals(argumentCaptor.getValue().getExposedPorts(), expectedExposedPorts);
    }

    @Test
    public void shouldExposeOnlyCommonPortsToContainerOnNonDevInstanceCreationFromRecipe() throws Exception {
        Map<String, Map<String, String>> expectedExposedPorts = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedExposedPorts.put(server.getPort(), Collections.<String, String>emptyMap());
        }

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            commonServers,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = false;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());

        assertEquals(argumentCaptor.getValue().getExposedPorts(), expectedExposedPorts);
    }

    @Test
    public void shouldExposeCommonAndDevPortsToContainerOnDevInstanceCreationFromSnapshot() throws Exception {
        Map<String, Map<String, String>> expectedExposedPorts = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedExposedPorts.put(server.getPort(), Collections.<String, String>emptyMap());
        }

        final Set<ServerConf> devServers = new HashSet<>(asList(new ServerConf("reference3", "8082", "https"),
                                                                new ServerConf("reference4", "8083", "sftp")));
        for (ServerConf server : devServers) {
            expectedExposedPorts.put(server.getPort(), Collections.<String, String>emptyMap());
        }

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            devServers,
                                                            commonServers,
                                                            Collections.<String>emptySet(),
                                                            Collections.<String>emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = true;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());

        assertEquals(argumentCaptor.getValue().getExposedPorts(), expectedExposedPorts);
    }

    @Test
    public void shouldExposeOnlyCommonPortsToContainerOnNonDevInstanceCreationFromSnapshot() throws Exception {
        Map<String, Map<String, String>> expectedExposedPorts = new HashMap<>();
        final Set<ServerConf> commonServers = new HashSet<>(asList(new ServerConf("reference1", "8080", "http"),
                                                                   new ServerConf("reference2", "8081", "ftp")));
        for (ServerConf server : commonServers) {
            expectedExposedPorts.put(server.getPort(), Collections.<String, String>emptyMap());
        }

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            commonServers,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));

        final boolean isDev = false;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<ContainerConfig> argumentCaptor = ArgumentCaptor.forClass(ContainerConfig.class);
        verify(dockerConnector).createContainer(argumentCaptor.capture(), anyString());

        assertEquals(argumentCaptor.getValue().getExposedPorts(), expectedExposedPorts);
    }

    @Test
    public void shouldBindProjectsFSVolumeToContainerOnDevInstanceCreationFromRecipe() throws Exception {
        final String expectedHostPathOfProjects = "/tmp/projects";
        String[] expectedVolumes = new String[]{expectedHostPathOfProjects + ":/projects"};

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(expectedHostPathOfProjects);

        final boolean isDev = true;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        assertEquals(argumentCaptor.getValue().getBinds(), expectedVolumes);
    }

    @Test
    public void shouldBindProjectsFSVolumeToContainerOnDevInstanceCreationFromSnapshot() throws Exception {
        final String expectedHostPathOfProjects = "/tmp/projects";
        final String[] expectedVolumes = new String[]{expectedHostPathOfProjects + ":/projects"};

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(expectedHostPathOfProjects);

        final boolean isDev = true;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        assertEquals(argumentCaptor.getValue().getBinds(), expectedVolumes);
    }

    @Test
    public void shouldNotBindProjectsFSVolumeToContainerOnNonDevInstanceCreationFromRecipe() throws Exception {
        String[] expectedVolumes = new String[0];

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn("/tmp/projects");

        final boolean isDev = false;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        assertEquals(argumentCaptor.getValue().getBinds(), expectedVolumes);
    }

    @Test
    public void shouldNotBindProjectsFSVolumeToContainerOnNonDevInstanceCreationFromSnapshot() throws Exception {
        String[] expectedVolumes = new String[0];

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn("/tmp/projects");

        final boolean isDev = false;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        assertEquals(argumentCaptor.getValue().getBinds(), expectedVolumes);
    }

    @Test
    public void shouldBindCommonAndDevVolumesToContainerOnDevInstanceCreationFromRecipe() throws Exception {
        final String expectedHostPathOfProjects = "/tmp/projects";
        Set<String> devVolumes = new HashSet<>(asList("/etc:/tmp/etc:ro", "/some/thing:/home/some/thing"));
        Set<String> commonVolumes = new HashSet<>(asList("/some/thing/else:/home/some/thing/else", "/other/path:/home/other/path"));

        final ArrayList<String> expectedVolumes = new ArrayList<>();
        expectedVolumes.addAll(devVolumes);
        expectedVolumes.addAll(commonVolumes);
        expectedVolumes.add(expectedHostPathOfProjects + ":/projects");

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            devVolumes,
                                                            commonVolumes,
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(expectedHostPathOfProjects);

        final boolean isDev = true;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        final String[] actualBinds = argumentCaptor.getValue().getBinds();
        assertEquals(actualBinds.length, expectedVolumes.size());
        assertEquals(new HashSet<>(asList(actualBinds)), new HashSet<>(expectedVolumes));
    }

    @Test
    public void shouldBindCommonAndDevVolumesToContainerOnDevInstanceCreationFromSnapshot() throws Exception {
        final String expectedHostPathOfProjects = "/tmp/projects";
        Set<String> devVolumes = new HashSet<>(asList("/etc:/tmp/etc:ro", "/some/thing:/home/some/thing"));
        Set<String> commonVolumes = new HashSet<>(asList("/some/thing/else:/home/some/thing/else", "/other/path:/home/other/path"));

        final ArrayList<String> expectedVolumes = new ArrayList<>();
        expectedVolumes.addAll(devVolumes);
        expectedVolumes.addAll(commonVolumes);
        expectedVolumes.add(expectedHostPathOfProjects + ":/projects");

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            devVolumes,
                                                            commonVolumes,
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(expectedHostPathOfProjects);

        final boolean isDev = true;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        final String[] actualBinds = argumentCaptor.getValue().getBinds();
        assertEquals(actualBinds.length, expectedVolumes.size());
        assertEquals(new HashSet<>(asList(actualBinds)), new HashSet<>(expectedVolumes));
    }

    @Test
    public void shouldBindCommonVolumesOnlyToContainerOnNonDevInstanceCreationFromRecipe() throws Exception {
        final String expectedHostPathOfProjects = "/tmp/projects";
        Set<String> devVolumes = new HashSet<>(asList("/etc:/tmp/etc:ro", "/some/thing:/home/some/thing"));
        Set<String> commonVolumes = new HashSet<>(asList("/some/thing/else:/home/some/thing/else", "/other/path:/home/other/path"));

        final ArrayList<String> expectedVolumes = new ArrayList<>();
        expectedVolumes.addAll(commonVolumes);

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            devVolumes,
                                                            commonVolumes,
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(expectedHostPathOfProjects);

        final boolean isDev = false;


        createInstanceFromRecipe(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        final String[] actualBinds = argumentCaptor.getValue().getBinds();
        assertEquals(actualBinds.length, expectedVolumes.size());
        assertEquals(new HashSet<>(asList(actualBinds)), new HashSet<>(expectedVolumes));
    }

    @Test
    public void shouldBindCommonVolumesOnlyToContainerOnNonDevInstanceCreationFromSnapshot() throws Exception {
        final String expectedHostPathOfProjects = "/tmp/projects";
        Set<String> devVolumes = new HashSet<>(asList("/etc:/tmp/etc:ro", "/some/thing:/home/some/thing"));
        Set<String> commonVolumes = new HashSet<>(asList("/some/thing/else:/home/some/thing/else", "/other/path:/home/other/path"));

        final ArrayList<String> expectedVolumes = new ArrayList<>();
        expectedVolumes.addAll(commonVolumes);

        dockerInstanceProvider = new DockerInstanceProvider(dockerConnector,
                                                            dockerMachineFactory,
                                                            Collections.emptySet(),
                                                            Collections.emptySet(),
                                                            devVolumes,
                                                            commonVolumes,
                                                            API_ENDPOINT_VALUE);

        when(dockerConnector.createContainer(any(ContainerConfig.class), anyString()))
                .thenReturn(new ContainerCreated("containerId", new String[0]));
        when(dockerNode.getProjectsFolder()).thenReturn(expectedHostPathOfProjects);

        final boolean isDev = false;


        createInstanceFromSnapshot(isDev);


        ArgumentCaptor<HostConfig> argumentCaptor = ArgumentCaptor.forClass(HostConfig.class);
        verify(dockerConnector).startContainer(anyString(), argumentCaptor.capture(), any(LogMessageProcessor.class));

        final String[] actualBinds = argumentCaptor.getValue().getBinds();
        assertEquals(actualBinds.length, expectedVolumes.size());
        assertEquals(new HashSet<>(asList(actualBinds)), new HashSet<>(expectedVolumes));
    }

    private void createInstanceFromRecipe() throws Exception {
        createInstanceFromRecipe(false, 64, "machineId", "userId", "workspaceId", "Display Name", new RecipeImpl().withType("Dockerfile")
                                                                                                                  .withScript(
                                                                                                                          "FROM busybox"));
    }

    private void createInstanceFromRecipe(boolean isDev) throws Exception {
        createInstanceFromRecipe(isDev, null, null, null, null, null, null);
    }

    private void createInstanceFromRecipe(boolean isDev, String workspaceId) throws Exception {
        createInstanceFromRecipe(isDev, null, null, null, workspaceId, null, null);
    }

    private void createInstanceFromRecipe(int memorySizeInMB) throws Exception {
        createInstanceFromRecipe(null, memorySizeInMB, null, null, null, null, null);
    }

    private void createInstanceFromRecipe(Boolean isDev,
                                          Integer memorySizeInMB,
                                          String machineId,
                                          String userId,
                                          String workspaceId,
                                          String displayName,
                                          Recipe recipe) throws Exception {

        createInstanceFromRecipe(isDev == null ? false : isDev,
                                 memorySizeInMB == null ? 64 : memorySizeInMB,
                                 machineId == null ? "machineId" : machineId,
                                 userId == null ? "userId" : userId,
                                 workspaceId == null ? "workspaceId" : workspaceId,
                                 displayName == null ? "Display Name" : displayName,
                                 recipe == null ? new RecipeImpl().withType("Dockerfile").withScript("FROM busybox") : recipe);
    }

    private void createInstanceFromRecipe(boolean isDev,
                                          int memorySizeInMB,
                                          String machineId,
                                          String userId,
                                          String workspaceId,
                                          String displayName,
                                          Recipe recipe) throws Exception {

        dockerInstanceProvider.createInstance(recipe,
                                              machineId,
                                              userId,
                                              workspaceId,
                                              isDev,
                                              displayName,
                                              memorySizeInMB,
                                              LineConsumer.DEV_NULL);
    }

    private void createInstanceFromSnapshot() throws NotFoundException, MachineException {
        createInstanceFromSnapshot(null, null, null, null, null, null, null, null, null, null);
    }

    private void createInstanceFromSnapshot(int memorySizeInMB) throws NotFoundException, MachineException {
        createInstanceFromSnapshot(null, null, null, null, memorySizeInMB, null, null, null, null, null);
    }

    private void createInstanceFromSnapshot(boolean isDev) throws NotFoundException, MachineException {
        createInstanceFromSnapshot(null, null, null, isDev, null, null, null, null, null, null);
    }

    private void createInstanceFromSnapshot(boolean isDev, String workspaceId) throws NotFoundException, MachineException {
        createInstanceFromSnapshot(null, null, null, isDev, null, null, null, workspaceId, null, null);
    }

    private void createInstanceFromSnapshot(String repo, String tag, String registry) throws NotFoundException, MachineException {
        createInstanceFromSnapshot(repo, tag, registry, null, null, null, null, null, null, null);
    }

    private void createInstanceFromSnapshot(Boolean isDev,
                                            Integer memorySizeInMB,
                                            String machineId,
                                            String userId,
                                            String workspaceId,
                                            String displayName,
                                            Recipe recipe) throws NotFoundException, MachineException {
        createInstanceFromSnapshot(null, null, null, isDev, memorySizeInMB, machineId, userId, workspaceId, displayName, recipe);
    }

    private void createInstanceFromSnapshot(String repo,
                                            String tag,
                                            String registry,
                                            Boolean isDev,
                                            Integer memorySizeInMB,
                                            String machineId,
                                            String userId,
                                            String workspaceId,
                                            String displayName,
                                            Recipe recipe) throws NotFoundException, MachineException {

        createInstanceFromSnapshot(repo == null ? "repo" : repo,
                                   tag == null ? "tag" : tag,
                                   registry == null ? "localhost:1234" : registry,
                                   isDev == null ? false : isDev,
                                   memorySizeInMB == null ? 64 : memorySizeInMB,
                                   machineId == null ? "machineId" : machineId,
                                   userId == null ? "userId" : userId,
                                   workspaceId == null ? "workspaceId" : workspaceId,
                                   displayName == null ? "Display Name" : displayName,
                                   recipe == null ? new RecipeImpl().withType("Dockerfile")
                                                                    .withScript("FROM busybox") : recipe);
    }

    private void createInstanceFromSnapshot(String repo,
                                            String tag,
                                            String registry,
                                            boolean isDev,
                                            int memorySizeInMB,
                                            String machineId,
                                            String userId,
                                            String workspaceId,
                                            String displayName,
                                            Recipe recipe) throws NotFoundException, MachineException {

        dockerInstanceProvider.createInstance(new DockerInstanceKey(repo, tag, "imageId", registry),
                                              machineId,
                                              userId,
                                              workspaceId,
                                              isDev,
                                              displayName,
                                              recipe,
                                              memorySizeInMB,
                                              LineConsumer.DEV_NULL);
    }
}