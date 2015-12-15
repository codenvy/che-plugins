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
package org.eclipse.che.ide.extension.machine.client.processes;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateHandler;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.inject.factories.TerminalFactory;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.outputspanel.console.OutputConsole;
import org.eclipse.che.ide.extension.machine.client.perspective.terminal.TerminalPresenter;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.extension.machine.client.processes.ProcessTreeNode.ProcessNodeType.ROOT_NODE;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Roman Nikitenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ConsolesPanelPresenterTest {
    private static final String MACHINE_ID   = "machineID";
    private static final String WORKSPACE_ID = "workspaceID";
    private static final String PROCESS_ID   = "processID";

    @Mock
    private DialogFactory               dialogFactory;
    @Mock
    private NotificationManager         notificationManager;
    @Mock
    private MachineLocalizationConstant localizationConstant;
    @Mock
    private TerminalFactory             terminalFactory;
    @Mock
    private ConsolesPanelView           view;
    @Mock
    private MachineResources            resources;
    @Mock
    private AppContext                  appContext;
    @Mock
    private MachineServiceClient        machineService;
    @Mock
    private EntityFactory               entityFactory;
    @Mock
    private EventBus                    eventBus;
    @Mock
    private UsersWorkspaceDto           workspace;

    @Mock
    private Promise<List<MachineDto>> machinesPromise;

    @Mock
    private Promise<MachineDto> machinePromise;

    @Captor
    private ArgumentCaptor<AcceptsOneWidget>            acceptsOneWidgetCaptor;
    @Captor
    private ArgumentCaptor<Operation<List<MachineDto>>> machinesCaptor;
    @Captor
    private ArgumentCaptor<Operation<MachineDto>>       machineCaptor;
    @Captor
    private ArgumentCaptor<DevMachineStateHandler>      devMachineStateHandlerCaptor;
    @Captor
    private ArgumentCaptor<Operation<PromiseError>>     errorOperation;

    private ConsolesPanelPresenter presenter;

    @Before
    public void setUp() {
        when(appContext.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);

        when(machineService.getWorkspaceMachines(anyString())).thenReturn(machinesPromise);
        when(machineService.getMachine(anyString())).thenReturn(machinePromise);
        when(machinePromise.then(Matchers.<Operation<MachineDto>>anyObject())).thenReturn(machinePromise);

        presenter = new ConsolesPanelPresenter(view, eventBus, terminalFactory, dialogFactory, notificationManager, localizationConstant,
                                               machineService, entityFactory, resources, appContext);
    }

    @Test
    public void shouldFetchMachines() throws Exception {
        MachineDto machineDto = mock(MachineDto.class);
        when(machineDto.isDev()).thenReturn(true);
        List<MachineDto> machines = new ArrayList<>(2);
        machines.add(machineDto);

        when(appContext.getWorkspace()).thenReturn(workspace);
        DevMachineStateEvent devMachineStateEvent = mock(DevMachineStateEvent.class);
        verify(eventBus).addHandler(anyObject(), devMachineStateHandlerCaptor.capture());

        DevMachineStateHandler devMachineStateHandler = devMachineStateHandlerCaptor.getValue();
        devMachineStateHandler.onMachineStarted(devMachineStateEvent);

        verify(appContext, times(2)).getWorkspace();
        verify(workspace, times(2)).getId();
        verify(machineService, times(2)).getWorkspaceMachines(eq(WORKSPACE_ID));
        verify(machinesPromise, times(2)).then(machinesCaptor.capture());
        machinesCaptor.getValue().apply(machines);
        verify(view).setProcessesData(anyObject());
    }

    @Test
    public void shouldShowErrorWhenMachineNodeIsNull() throws Exception {
        List<ProcessTreeNode> children = new ArrayList<>();
        presenter.rootNode = new ProcessTreeNode(ROOT_NODE, null, null, children);

        CommandConfiguration commandConfiguration = mock(CommandConfiguration.class);
        OutputConsole outputConsole = mock(OutputConsole.class);

        presenter.addCommand(MACHINE_ID, commandConfiguration, outputConsole);

        verify(notificationManager).showError(anyString());
        verify(localizationConstant, times(2)).machineNotFound(eq(MACHINE_ID));
    }

    @Test
    public void shouldAddCommand() throws Exception {
        ProcessTreeNode machineNode = mock(ProcessTreeNode.class);
        when(machineNode.getId()).thenReturn(MACHINE_ID);
        List<ProcessTreeNode> children = new ArrayList<>();
        children.add(machineNode);
        presenter.rootNode = new ProcessTreeNode(ROOT_NODE, null, null, children);

        CommandConfiguration commandConfiguration = mock(CommandConfiguration.class);
        when(commandConfiguration.getName()).thenReturn(PROCESS_ID);
        OutputConsole outputConsole = mock(OutputConsole.class);

        presenter.addCommand(MACHINE_ID, commandConfiguration, outputConsole);

        verify(view).addProcessNode(anyObject());
        verify(view, never()).hideProcessOutput(anyString());

        verify(outputConsole).go(acceptsOneWidgetCaptor.capture());
        IsWidget widget = mock(IsWidget.class);
        acceptsOneWidgetCaptor.getValue().setWidget(widget);

        verify(view).addProcessWidget(eq(PROCESS_ID), eq(widget));
        verify(view, times(2)).selectNode(anyObject());
        verify(view).setProcessesData(anyObject());
        verify(view).getNodeById(eq(PROCESS_ID));
    }

    @Test
    public void shouldReplaceCommandOutput() throws Exception {
        ProcessTreeNode machineNode = mock(ProcessTreeNode.class);
        when(machineNode.getId()).thenReturn(MACHINE_ID);
        List<ProcessTreeNode> children = new ArrayList<>();
        children.add(machineNode);
        OutputConsole outputConsole = mock(OutputConsole.class);

        presenter.rootNode = new ProcessTreeNode(ROOT_NODE, null, null, children);
        presenter.commandConsoles.put(PROCESS_ID, outputConsole);

        CommandConfiguration commandConfiguration = mock(CommandConfiguration.class);
        when(commandConfiguration.getName()).thenReturn(PROCESS_ID);


        presenter.addCommand(MACHINE_ID, commandConfiguration, outputConsole);

        verify(view, never()).addProcessNode(anyObject());
        verify(view, never()).setProcessesData(anyObject());

        verify(outputConsole).go(acceptsOneWidgetCaptor.capture());
        IsWidget widget = mock(IsWidget.class);
        acceptsOneWidgetCaptor.getValue().setWidget(widget);

        verify(view).hideProcessOutput(eq(PROCESS_ID));
        verify(view).addProcessWidget(eq(PROCESS_ID), eq(widget));
        verify(view).selectNode(anyObject());
        verify(view).getNodeById(eq(PROCESS_ID));
    }

    @Test
    public void shouldAddTerminal() throws Exception {
        MachineDto machineDto = mock(MachineDto.class);
        when(machineDto.isDev()).thenReturn(true);

        ProcessTreeNode machineNode = mock(ProcessTreeNode.class);
        when(machineNode.getId()).thenReturn(MACHINE_ID);
        when(machineNode.getId()).thenReturn(MACHINE_ID);
        List<ProcessTreeNode> children = new ArrayList<>();
        children.add(machineNode);
        presenter.rootNode = new ProcessTreeNode(ROOT_NODE, null, null, children);

        Machine machine = mock(Machine.class);
        when(entityFactory.createMachine(anyObject())).thenReturn(machine);
        TerminalPresenter terminal = mock(TerminalPresenter.class);
        when(terminalFactory.create(machine)).thenReturn(terminal);
        IsWidget terminalWidget = mock(IsWidget.class);
        when(terminal.getView()).thenReturn(terminalWidget);

        presenter.onAddTerminal(MACHINE_ID);

        verify(machinePromise).then(machineCaptor.capture());
        machineCaptor.getValue().apply(machineDto);

        verify(entityFactory).createMachine(anyObject());
        verify(terminalFactory).create(eq(machine));
        verify(terminal).getView();
        verify(view).setProcessesData(anyObject());
        verify(view).selectNode(anyObject());
        verify(view).addProcessWidget(anyString(), eq(terminalWidget));
        verify(view).addProcessNode(anyObject());
        verify(terminal).setVisible(eq(true));
        verify(terminal).connect();
        verify(terminal).setListener(anyObject());

    }
    
    @Test
    public void shouldShowCommanOutputWhenCommandSelected() throws Exception {
        presenter.onCommandSelected(PROCESS_ID);
        
        verify(view).showProcessOutput(eq(PROCESS_ID));
    }
    
    @Test
    public void shouldCloseCommanOutputWhenCommandHasFinished() throws Exception {
        ProcessTreeNode machineNode = mock(ProcessTreeNode.class);
        ProcessTreeNode commandNode = mock(ProcessTreeNode.class);
        when(machineNode.getId()).thenReturn(MACHINE_ID);
        List<ProcessTreeNode> children = new ArrayList<>();
        children.add(machineNode);
        presenter.rootNode = new ProcessTreeNode(ROOT_NODE, null, null, children);

        OutputConsole outputConsole = mock(OutputConsole.class);
        when(outputConsole.isFinished()).thenReturn(true);
        presenter.commandConsoles.put(PROCESS_ID, outputConsole);
        machineNode.getChildren().add(commandNode);
        
        when(commandNode.getId()).thenReturn(PROCESS_ID);
        when(view.getNodeIndex(anyString())).thenReturn(0);
        when(machineNode.getChildren()).thenReturn(children);
        when(commandNode.getParent()).thenReturn(machineNode);
        
        presenter.onCloseCommandConsole(commandNode);
        
        verify(commandNode, times(2)).getId();
        verify(commandNode).getParent();
        verify(view).getNodeIndex(eq(PROCESS_ID));
        verify(view).hideProcessOutput(eq(PROCESS_ID));
        verify(view).removeProcessNode(eq(commandNode));
        verify(view).setProcessesData(anyObject());
    }
    
    @Test
    public void shouldShowConfirmDialogWhenCommandHasNotFinished() throws Exception {
        ConfirmDialog confirmDialog = mock(ConfirmDialog.class);
        ProcessTreeNode commandNode = mock(ProcessTreeNode.class);

        OutputConsole outputConsole = mock(OutputConsole.class);
        when(outputConsole.isFinished()).thenReturn(false);
        presenter.commandConsoles.put(PROCESS_ID, outputConsole);
        
        when(commandNode.getId()).thenReturn(PROCESS_ID);
        when(dialogFactory.createConfirmDialog(anyString(), anyString(), anyObject(), anyObject())).thenReturn(confirmDialog);
        
        presenter.onCloseCommandConsole(commandNode);
        
        verify(commandNode).getId();
        verify(view, never()).hideProcessOutput(anyString());
        verify(view, never()).removeProcessNode(anyObject());
        verify(dialogFactory).createConfirmDialog(anyString(), anyString(), anyObject(), anyObject());
        verify(confirmDialog).show();
    }
    
    @Test
    public void shouldShowTerminalWhenTerminalNodeSelected() throws Exception {
        presenter.onTerminalSelected(PROCESS_ID);
        
        verify(view).showProcessOutput(eq(PROCESS_ID));
    }
    
    @Test
    public void shouldCloseTerminal() throws Exception {
        TerminalPresenter terminal = mock(TerminalPresenter.class);
        ProcessTreeNode machineNode = mock(ProcessTreeNode.class);
        ProcessTreeNode terminalNode = mock(ProcessTreeNode.class);
        when(machineNode.getId()).thenReturn(MACHINE_ID);
        List<ProcessTreeNode> children = new ArrayList<>();
        children.add(machineNode);
        presenter.rootNode = new ProcessTreeNode(ROOT_NODE, null, null, children);
        presenter.terminals.put(PROCESS_ID, terminal);
        
        when(terminalNode.getId()).thenReturn(PROCESS_ID);
        when(view.getNodeIndex(anyString())).thenReturn(0);
        when(machineNode.getChildren()).thenReturn(children);
        when(terminalNode.getParent()).thenReturn(machineNode);
        
        presenter.onCloseTerminal(terminalNode);
        
        verify(terminal).stopTerminal();
        verify(terminalNode, times(2)).getId();
        verify(terminalNode).getParent();
        verify(view).getNodeIndex(eq(PROCESS_ID));
        verify(view).hideProcessOutput(eq(PROCESS_ID));
        verify(view).removeProcessNode(eq(terminalNode));
        verify(view).setProcessesData(anyObject());
    }

    @Test
    public void shouldReturnTitle() throws Exception {
        presenter.getTitle();

        verify(localizationConstant, times(2)).viewConsolesTitle();
    }

    @Test
    public void shouldReturnTitleToolTip() throws Exception {
        presenter.getTitleToolTip();

        verify(localizationConstant).viewProcessesTooltip();
    }

    @Test
    public void shouldSetViewVisible() throws Exception {
        presenter.setVisible(true);

        verify(view).setVisible(eq(true));
    }

    @Test
    public void shouldReturnTitleSVGImage() {
        presenter.getTitleSVGImage();

        verify(resources).terminal();
    }

    @Test
    public void testGo() throws Exception {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(eq(view));
    }
}
