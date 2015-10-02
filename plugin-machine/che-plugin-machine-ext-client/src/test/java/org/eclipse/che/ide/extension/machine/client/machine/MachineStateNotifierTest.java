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
package org.eclipse.che.ide.extension.machine.client.machine;

import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachinePanelPresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.events.MessageHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.junit.Before;
import org.junit.Ignore;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class MachineStateNotifierTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock
    private MessageBus                  messageBus;
    @Mock
    private AppContext                  appContext;
    @Mock
    private DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    @Mock
    private NotificationManager         notificationManager;
    @Mock
    private MachineLocalizationConstant locale;
    @Mock
    private MachinePanelPresenter       machinePanelPresenter;

    //additional mocks
    @Mock
    private Unmarshallable<MachineStatusEvent> unmarshaller;
    @Mock
    private Machine                            machine;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @InjectMocks
    private MachineStatusNotifier stateNotifier;

    @Before
    public void setUp() {
        when(dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class)).thenReturn(unmarshaller);

        when(locale.notificationCreatingMachine(SOME_TEXT)).thenReturn(SOME_TEXT);
        when(locale.notificationDestroyingMachine(SOME_TEXT)).thenReturn(SOME_TEXT);
    }

    @Test
    @Ignore
    public void machineShouldBeTrackedWhenMachineStateIsCreating() throws Exception {
        UsersWorkspaceDto workspace = mock(UsersWorkspaceDto.class);
        when(appContext.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(SOME_TEXT);
        when(machine.getDisplayName()).thenReturn(SOME_TEXT);
        stateNotifier.trackMachine(machine, MachineManager.MachineOperationType.START);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();

        assertThat(notification.isFinished(), equalTo(false));
        assertThat(notification.getMessage(), equalTo(SOME_TEXT));

        verify(locale).notificationCreatingMachine(SOME_TEXT);
        verify(locale, never()).notificationDestroyingMachine(SOME_TEXT);

        verify(messageBus).subscribe(anyString(), Matchers.<MessageHandler>anyObject());
    }

    @Test
    @Ignore
    public void machineShouldBeTrackedWhenMachineStateIsDestroying() throws Exception {
        UsersWorkspaceDto workspace = mock(UsersWorkspaceDto.class);
        when(appContext.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(SOME_TEXT);
        when(machine.getDisplayName()).thenReturn(SOME_TEXT);
        stateNotifier.trackMachine(machine, MachineManager.MachineOperationType.DESTROY);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();

        assertThat(notification.isFinished(), equalTo(false));
        assertThat(notification.getMessage(), equalTo(SOME_TEXT));

        verify(locale).notificationDestroyingMachine(SOME_TEXT);
        verify(locale, never()).notificationCreatingMachine(SOME_TEXT);

        verify(messageBus).subscribe(anyString(), Matchers.<MessageHandler>anyObject());
    }
}