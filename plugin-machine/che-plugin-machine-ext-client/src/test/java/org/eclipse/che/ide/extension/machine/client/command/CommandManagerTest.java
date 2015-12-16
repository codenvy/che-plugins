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
package org.eclipse.che.ide.extension.machine.client.command;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineProcessDto;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.command.valueproviders.CommandPropertyValueProvider;
import org.eclipse.che.ide.extension.machine.client.command.valueproviders.CommandPropertyValueProviderRegistry;
import org.eclipse.che.ide.extension.machine.client.outputspanel.OutputsContainerPresenter;
import org.eclipse.che.ide.extension.machine.client.outputspanel.console.CommandConsoleFactory;
import org.eclipse.che.ide.extension.machine.client.outputspanel.console.OutputConsole;
import org.eclipse.che.ide.extension.machine.client.processes.ConsolesPanelPresenter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyi */
@RunWith(MockitoJUnitRunner.class)
public class CommandManagerTest {

    @Mock
    private MachineServiceClient                 machineServiceClient;
    @Mock
    private OutputsContainerPresenter            outputsContainerPresenter;
    @Mock
    private ConsolesPanelPresenter               consolesPanelPresenter;
    @Mock
    private CommandConsoleFactory                commandConsoleFactory;
    @Mock
    private NotificationManager                  notificationManager;
    @Mock
    private MachineLocalizationConstant          localizationConstant;
    @Mock
    private WorkspaceAgent                       workspaceAgent;
    @Mock
    private AppContext                           appContext;
    @Mock
    private CommandPropertyValueProviderRegistry commandPropertyValueProviderRegistry;

    @Mock
    private Promise<MachineProcessDto>                   processPromise;
    @Captor
    private ArgumentCaptor<Operation<MachineProcessDto>> processCaptor;

    @InjectMocks
    private CommandManager commandManager;

    @Test
    public void shouldShowWarningWhenNoDevMachineIsRunning() throws Exception {
        commandManager.execute(mock(CommandConfiguration.class));

        verify(localizationConstant).noDevMachine();
        verify(notificationManager).showWarning(anyString());
    }

    @Test
    public void shouldExecuteCommand() throws Exception {
        String devMachineId = "devMachineId";
        when(appContext.getDevMachineId()).thenReturn(devMachineId);
        OutputConsole outputConsole = mock(OutputConsole.class);
        when(commandConsoleFactory.create(any(CommandConfiguration.class), anyString())).thenReturn(outputConsole);
        when(machineServiceClient.executeCommand(anyString(), anyString(), anyString())).thenReturn(processPromise);

        String commandLine = "devMachineId";
        CommandConfiguration command = mock(CommandConfiguration.class);
        when(command.toCommandLine()).thenReturn(commandLine);

        MachineProcessDto process = mock(MachineProcessDto.class);
        int pid = 123;
        when(process.getPid()).thenReturn(pid);

        commandManager.execute(command);

        verify(appContext).getDevMachineId();
        verify(commandConsoleFactory).create(eq(command), eq(devMachineId));
        verify(outputConsole).listenToOutput(anyString());
        verify(consolesPanelPresenter).addCommand(eq(devMachineId), eq(command), eq(outputConsole));
        verify(workspaceAgent).setActivePart(eq(consolesPanelPresenter));
        verify(command).toCommandLine();
        verify(machineServiceClient).executeCommand(eq(devMachineId), eq(commandLine), anyString());
        verify(processPromise).then(processCaptor.capture());
        processCaptor.getValue().apply(process);
        verify(outputConsole).attachToProcess(eq(pid));
    }

    @Test
    public void testSubstituteProperties() throws Exception {
        String key = "$(project.name)";
        String value = "my_project";
        final CommandPropertyValueProvider valueProvider = mock(CommandPropertyValueProvider.class);
        when(valueProvider.getKey()).thenReturn(key);
        when(valueProvider.getValue()).thenReturn(value);
        when(commandPropertyValueProviderRegistry.getProviders()).thenReturn(Collections.singletonList(valueProvider));

        String commandLine = "mvn -f " + key + " clean install";
        final String cmd = commandManager.substituteProperties(commandLine);

        assertEquals("mvn -f " + value + " clean install", cmd);
    }
}
