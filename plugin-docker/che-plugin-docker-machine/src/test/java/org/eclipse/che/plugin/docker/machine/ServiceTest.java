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

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.machine.server.MachineImpl;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.MachineRegistry;
import org.eclipse.che.api.machine.server.MachineService;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.SnapshotImpl;
import org.eclipse.che.api.machine.server.SnapshotStorage;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.shared.MachineState;
import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromRecipe;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromSnapshot;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.plugin.docker.client.AuthConfig;
import org.eclipse.che.plugin.docker.client.AuthConfigs;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.client.json.PortBinding;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;

import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

// TODO rework authentication
// TODO check removeSnapshotTest with https and password
// TODO bind, unbind
// TODO should we check result of tests with native calls?
/**
 * @author Alexander Garagatyi
 */
@Listeners(value = {MockitoTestNGListener.class})
public class ServiceTest {
    private static final String       USER         = "userId";
    private static final String       SNAPSHOT_ID  = "someSnapshotId";
    private static       LineConsumer lineConsumer = new StdErrLineConsumer();

    // set in method {@link saveSnapshotTest}
    // used in methods {@link createMachineFromSnapshotTest} and {@link removeSnapshotTest}
    private DockerInstanceKey pushedImage;

    private SnapshotStorage   snapshotStorage;
    private MemberDao         memberDao;
    private DockerNodeFactory dockerNodeFactory;
    private DockerNode        dockerNode;
    private MachineRegistry   machineRegistry;
    private DockerConnector   docker;
    private InstanceProvider  dockerInstanceProvider;
    private MachineManager    machineManager;
    private MachineService    machineService;
    private String            registryContainerId;
    private AuthConfigs       authConfigs;
    private EventService      eventService;

    private DtoFactory dtoFactory = DtoFactory.getInstance();

    @BeforeClass
    public void setUpClass() throws Exception {
        //authConfigs = new AuthConfigs(Collections.singleton(new AuthConfig("localhost:5000", "codenvy", "password1")));
        authConfigs = new AuthConfigs(Collections.<AuthConfig>emptySet());

        docker = new DockerConnector(authConfigs);

        machineRegistry = new MachineRegistry();

        assertTrue(pull("registry", "latest", null));

        registryContainerId = docker.createContainer(new ContainerConfig().withImage("registry").withExposedPorts(
                Collections.singletonMap("5000/tcp", Collections.<String, String>emptyMap())), null).getId();

        docker.startContainer(registryContainerId, new HostConfig()
                                      .withPortBindings(Collections.singletonMap("5000/tcp", new PortBinding[]{
                                              new PortBinding().withHostPort("5000")})),
                              new LogMessagePrinter(lineConsumer));
    }

    @AfterClass
    public void tearDownClass() throws IOException {
        docker.killContainer(registryContainerId);
        docker.removeContainer(registryContainerId, true, false);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        memberDao = mock(MemberDao.class);

        snapshotStorage = mock(SnapshotStorage.class);

        dockerNodeFactory = mock(DockerNodeFactory.class);

        dockerNode = mock(DockerNode.class);

        eventService = mock(EventService.class);

        dockerInstanceProvider = new DockerInstanceProvider(docker, "localhost:5000", dockerNodeFactory);

        machineManager = new MachineManager(snapshotStorage,
                                            Collections.singleton(dockerInstanceProvider),
                                            machineRegistry,
                                            "/tmp",
                                            eventService);

        Field field = MachineService.class.getDeclaredField("defaultLineConsumer");
        field.setAccessible(true);
        field.set(null, lineConsumer);

        machineService = spy(new MachineService(machineManager, memberDao));

        EnvironmentContext.getCurrent().setUser(new User() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean isMemberOf(String s) {
                return false;
            }

            @Override
            public String getToken() {
                return null;
            }

            @Override
            public String getId() {
                return USER;
            }

            @Override
            public boolean isTemporary() {
                return false;
            }
        });

        when(dockerNodeFactory.createNode(anyString())).thenReturn(dockerNode);
        when(dockerNode.getProjectsFolder()).thenReturn(System.getProperty("user.dir"));
        when(memberDao.getWorkspaceMember("wsId", USER)).thenReturn(new Member()
                                                                            .withWorkspaceId("wsId")
                                                                            .withUserId(USER)
                                                                            .withRoles(Collections.singletonList("workspace/developer")));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        for (MachineImpl machine : new ArrayList<>(machineManager.getMachines())) {
            machineManager.destroy(machine.getId());
        }
    }

    @Test
    public void createFromRecipeTest() throws Exception {
        final MachineDescriptor machine = machineService.createMachineFromRecipe(
                dtoFactory.createDto(CreateMachineFromRecipe.class)
                          .withType("docker")
                          .withWorkspaceId("wsId")
                          .withRecipeDescriptor(dtoFactory.createDto(RecipeDescriptor.class)
                                                          .withType("Dockerfile")
                                                          .withScript("FROM ubuntu\nCMD tail -f /dev/null\n")));

        waitMachineIsRunning(machine.getId());
    }

    @Test(dependsOnMethods = "saveSnapshotTest", enabled = false)
    public void createMachineFromSnapshotTest() throws Exception {
        // remove local copy of image to check pulling
        docker.removeImage(pushedImage.getImageId(), true);

        SnapshotImpl snapshot = mock(SnapshotImpl.class);
        when(snapshotStorage.getSnapshot(SNAPSHOT_ID)).thenReturn(snapshot);
        when(snapshot.getInstanceType()).thenReturn("docker");
        when(snapshot.getWorkspaceId()).thenReturn("wsId");
        when(snapshot.getInstanceKey()).thenReturn(pushedImage);
        when(snapshot.getOwner()).thenReturn(USER);

        final MachineDescriptor machine = machineService
                .createMachineFromSnapshot(dtoFactory.createDto(CreateMachineFromSnapshot.class).withSnapshotId(SNAPSHOT_ID));

        waitMachineIsRunning(machine.getId());
    }

    @Test
    public void getMachineTest() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        final MachineDescriptor machineById = machineService.getMachineById(machine.getId());

        assertEquals(machineById.getId(), machine.getId());
    }

    @Test
    public void getMachinesTest() throws Exception {
        Set<String> expected = new HashSet<>();
        expected.add(createMachineAndWaitRunningState().getId());
        expected.add(createMachineAndWaitRunningState().getId());

        Set<String> actual = new HashSet<>();
        for (MachineImpl machine : machineManager.getMachines()) {
            actual.add(machine.getId());
        }
        assertEquals(actual, expected);
    }

    @Test
    public void destroyMachineTest() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        machineService.destroyMachine(machine.getId());

        assertEquals(MachineState.DESTROYING, machine.getState());

        int counter = 0;
        while (++counter < 10) {
            try {
                machineManager.getMachine(machine.getId());
            } catch (NotFoundException e) {
                return;
            }
            Thread.sleep(500);
        }
        fail();
    }

    @Test(enabled = false)// TODO Add ability to check when snapshot creation is finishes or fails
    public void saveSnapshotTest() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        // use machine manager instead of machine service because it returns future with snapshot
        // that allows check operation result
        final SnapshotImpl snapshot = machineManager.save(machine.getId(), USER, "label","test description");

        for (int i = 0; snapshot.getInstanceKey() == null && i < 10; ++i) {
            Thread.sleep(500);
        }
        assertNotNull(snapshot.getInstanceKey());

        final DockerInstanceKey imageKey = (DockerInstanceKey)snapshot.getInstanceKey();

        final boolean pullIsSuccessful = pull(imageKey.getRepository(), imageKey.getTag(), imageKey.getRegistry());

        assertTrue(pullIsSuccessful);

        pushedImage = imageKey;
    }

    // depends on saveSnapshotTest to be able to remove image from registry
    // actually doesn't depend on createMachineFromSnapshotTest,
    // but this test will fail createMachineFromSnapshotTest if called before
    @Test(dependsOnMethods = {"saveSnapshotTest", "createMachineFromSnapshotTest"}, enabled = false)// TODO
    public void removeSnapshotTest() throws Exception {
        SnapshotImpl snapshot = mock(SnapshotImpl.class);
        when(snapshotStorage.getSnapshot(SNAPSHOT_ID)).thenReturn(snapshot);
        when(snapshot.getInstanceType()).thenReturn("docker");
        when(snapshot.getOwner()).thenReturn(USER);
        when(snapshot.getInstanceKey()).thenReturn(pushedImage);

        machineService.removeSnapshot(SNAPSHOT_ID);

        verify(snapshotStorage).removeSnapshot(SNAPSHOT_ID);

        try {
            final boolean isPullSuccessful = pull(pushedImage.getRepository(), pushedImage.getTag(), pushedImage.getRegistry());
            assertFalse(isPullSuccessful);
        } catch (Exception e) {
            fail(e.getLocalizedMessage(), e);
        }
    }

    @Test
    public void executeTest() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        String commandInMachine = "echo \"command in machine\" && tail -f /dev/null";
        machineService
                .executeCommandInMachine(machine.getId(), dtoFactory.createDto(CommandDescriptor.class).withCommandLine(commandInMachine));

        Thread.sleep(500);

        final List<ProcessDescriptor> processes = machineService.getProcesses(machine.getId());
        assertEquals(processes.size(), 1);
        assertEquals(processes.get(0).getCommandLine(), commandInMachine);
    }

    @Test
    public void getProcessesTest() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        Set<String> commands = new HashSet<>(2);
        commands.add("tail -f /dev/null");
        commands.add("sleep 10000");

        for (String command : commands) {
            machineService.executeCommandInMachine(machine.getId(), dtoFactory.createDto(CommandDescriptor.class).withCommandLine(command));
        }

        Thread.sleep(500);

        final List<ProcessDescriptor> processes = machineService.getProcesses(machine.getId());
        assertEquals(processes.size(), 2);
        Set<String> actualCommandLines = new HashSet<>(2);
        for (ProcessDescriptor process : processes) {
            assertTrue(process.getPid() > 0);
            actualCommandLines.add(process.getCommandLine());
        }
        assertEquals(actualCommandLines, commands);
    }

    @Test
    public void stopProcessTest() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        String commandInMachine = "echo \"command in machine\" && tail -f /dev/null";
        machineService
                .executeCommandInMachine(machine.getId(), dtoFactory.createDto(CommandDescriptor.class).withCommandLine(commandInMachine));

        Thread.sleep(500);

        final List<ProcessDescriptor> processes = machineService.getProcesses(machine.getId());
        assertEquals(processes.size(), 1);
        assertEquals(processes.get(0).getCommandLine(), commandInMachine);

        machineService.stopProcess(machine.getId(), processes.get(0).getPid());

        assertTrue(machineService.getProcesses(machine.getId()).isEmpty());
    }

    @Test(expectedExceptions = NotFoundException.class, expectedExceptionsMessageRegExp = "Process with pid .* not found")
    public void shouldThrowNotFoundExceptionOnProcessKillIfProcessPidMissing() throws Exception {
        final MachineImpl machine = createMachineAndWaitRunningState();

        String commandInMachine = "echo \"command in machine\" && tail -f /dev/null";
        machineService
                .executeCommandInMachine(machine.getId(), dtoFactory.createDto(CommandDescriptor.class).withCommandLine(commandInMachine));

        Thread.sleep(500);

        final List<ProcessDescriptor> processes = machineService.getProcesses(machine.getId());
        assertEquals(processes.size(), 1);
        assertEquals(processes.get(0).getCommandLine(), commandInMachine);

        machineService.stopProcess(machine.getId(), processes.get(0).getPid() + 100);
    }

    private MachineImpl createMachineAndWaitRunningState()
            throws ServerException, NotFoundException, ForbiddenException, InterruptedException {
        final MachineImpl machine = machineManager
                .create("docker", new RecipeImpl().withId(null).withType("Dockerfile").withScript("FROM ubuntu\nCMD tail -f /dev/null\n"),
                        "wsId", EnvironmentContext.getCurrent().getUser().getId(), new StdErrLineConsumer());
        while (MachineState.RUNNING != machineManager.getMachine(machine.getId()).getState()) {
            Thread.sleep(500);
        }
        return machine;
    }

    private void waitMachineIsRunning(String machineId) throws NotFoundException, InterruptedException {
        while (MachineState.RUNNING != machineManager.getMachine(machineId).getState()) {
            Thread.sleep(500);
        }
    }

    private boolean pull(String image, String tag, String registry) throws Exception {
        final ValueHolder<Boolean> isSuccessfulValueHolder = new ValueHolder<>(true);
        final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
        docker.pull(image, tag, registry, new ProgressMonitor() {
            @Override
            public void updateProgress(ProgressStatus currentProgressStatus) {
                try {
                    if (currentProgressStatus.getError() != null) {
                        isSuccessfulValueHolder.set(false);
                    }
                    lineConsumer.writeLine(progressLineFormatter.format(currentProgressStatus));
                } catch (IOException ignored) {
                }
            }
        });

        return isSuccessfulValueHolder.get();
    }

    private static class StdErrLineConsumer implements LineConsumer {
        @Override
        public void writeLine(String line) throws IOException {
            System.err.println(line);
        }

        @Override
        public void close() throws IOException {
        }
    }
}
