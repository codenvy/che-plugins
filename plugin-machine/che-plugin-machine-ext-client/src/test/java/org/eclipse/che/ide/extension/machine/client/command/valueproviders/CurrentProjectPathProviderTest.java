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
package org.eclipse.che.ide.extension.machine.client.command.valueproviders;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.MachineMetadataDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
@RunWith(MockitoJUnitRunner.class)
public class CurrentProjectPathProviderTest {

    private static final String PROJECTS_ROOT = "/projects";
    private static final String PROJECT_PATH  = "/my_project";

    @Mock
    private EventBus             eventBus;
    @Mock
    private AppContext           appContext;
    @Mock
    private MachineServiceClient machineServiceClient;

    @InjectMocks
    private CurrentProjectPathProvider currentProjectPathProvider;

    @Mock
    private Promise<MachineDescriptor>                   machinePromise;
    @Captor
    private ArgumentCaptor<Operation<MachineDescriptor>> machineCaptor;

    @Before
    public void setUp() {
        CurrentProject currentProjectMock = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProjectMock);

        ProjectDescriptor projectDescriptorMock = mock(ProjectDescriptor.class);
        when(projectDescriptorMock.getPath()).thenReturn(PROJECT_PATH);
        when(currentProjectMock.getProjectDescription()).thenReturn(projectDescriptorMock);
    }

    @Test
    public void shouldBeRegisteredOnEventBus() throws Exception {
        verify(eventBus).addHandler(MachineStateEvent.TYPE, currentProjectPathProvider);
        verify(eventBus).addHandler(ProjectActionEvent.TYPE, currentProjectPathProvider);
    }

    @Test
    public void shouldReturnEmptyValueAfterClosingProject() throws Exception {
        currentProjectPathProvider.onProjectClosed(mock(ProjectActionEvent.class));

        assertTrue(currentProjectPathProvider.getValue().isEmpty());
    }

    @Test
    public void shouldReturnPathAfterRunningMachine() throws Exception {
        final Machine machineMock = mock(Machine.class);
        when(machineMock.isDev()).thenReturn(Boolean.TRUE);
        when(machineMock.getProjectsRoot()).thenReturn(PROJECTS_ROOT);
        final MachineStateEvent machineStateEvent = mock(MachineStateEvent.class);
        when(machineStateEvent.getMachine()).thenReturn(machineMock);

        currentProjectPathProvider.onMachineRunning(machineStateEvent);

        assertThat(currentProjectPathProvider.getValue(), equalTo(PROJECTS_ROOT + PROJECT_PATH));
    }

    @Test
    public void shouldReturnEmptyValueAfterDestroyingMachine() throws Exception {
        final Machine machineMock = mock(Machine.class);
        when(machineMock.isDev()).thenReturn(Boolean.FALSE);
        final MachineStateEvent machineStateEvent = mock(MachineStateEvent.class);
        when(machineStateEvent.getMachine()).thenReturn(machineMock);

        currentProjectPathProvider.onMachineDestroyed(machineStateEvent);

        assertTrue(currentProjectPathProvider.getValue().isEmpty());
    }

    @Test
    public void shouldReturnValueAfterOpeningProject() throws Exception {
        final String devMachineId = "dev1";
        when(appContext.getDevMachineId()).thenReturn(devMachineId);
        when(machineServiceClient.getMachine(anyString())).thenReturn(machinePromise);

        final MachineMetadataDto machineMetadataMock = mock(MachineMetadataDto.class);
        when(machineMetadataMock.projectsRoot()).thenReturn(PROJECTS_ROOT);
        final MachineDescriptor machineDescriptorMock = mock(MachineDescriptor.class);
        when(machineDescriptorMock.getMetadata()).thenReturn(machineMetadataMock);
        when(machinePromise.then(Matchers.any(Operation.class))).thenReturn(machinePromise);

        currentProjectPathProvider.onProjectReady(mock(ProjectActionEvent.class));

        verify(machinePromise).then(machineCaptor.capture());
        machineCaptor.getValue().apply(machineDescriptorMock);
        assertThat(currentProjectPathProvider.getValue(), equalTo(PROJECTS_ROOT + PROJECT_PATH));
    }
}
