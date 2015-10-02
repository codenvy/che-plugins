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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager.MachineOperationType;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateEvent;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.events.MessageHandler;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspaceHandler;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.MachineOperationType.RESTART;

/**
 * Notifies about changing machine state.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
class MachineStatusNotifier {

    /** WebSocket channel to receive messages about changing machine state. */
    public static final String MACHINE_STATUS_WS_CHANNEL = "machine:status:";

    private final EventBus                    eventBus;
    private final DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    private final AppContext                  appContext;
    private final NotificationManager         notificationManager;
    private final MachineLocalizationConstant locale;

    private MessageBus messageBus;

    @Inject
    MachineStatusNotifier(EventBus eventBus,
                          DtoUnmarshallerFactory dtoUnmarshallerFactory,
                          AppContext appContext,
                          final MessageBusProvider messageBusProvider,
                          NotificationManager notificationManager,
                          MachineLocalizationConstant locale) {
        this.eventBus = eventBus;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.locale = locale;

        eventBus.addHandler(StartWorkspaceEvent.TYPE, new StartWorkspaceHandler() {
            @Override
            public void onWorkspaceStarted(UsersWorkspaceDto workspace) {
                messageBus = messageBusProvider.getMessageBus();
            }
        });
    }

    /**
     * Start tracking machine state and notify about state changing.
     *
     * @param machine
     *         machine to track
     */
    void trackMachine(@NotNull final Machine machine, @NotNull final MachineOperationType operationType) {
        trackMachine(machine, null, operationType);
    }

    /**
     * Start tracking machine state and notify about state changing.
     *
     * @param machine
     *         machine to track
     * @param runningListener
     *         listener that will be notified when machine is running
     */
    void trackMachine(@NotNull final Machine machine,
                      @Nullable final RunningListener runningListener,
                      @NotNull final MachineOperationType operationType) {

        final String machineName = machine.getDisplayName();
        final String workspaceId = appContext.getWorkspace().getId();
        final String wsChannel = MACHINE_STATUS_WS_CHANNEL + workspaceId + ":" + machineName;
        final Notification notification = new Notification("", INFO, true);

        final Unmarshallable<MachineStatusEvent> unmarshaller = dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class);
        final MessageHandler messageHandler = new SubscriptionHandler<MachineStatusEvent>(unmarshaller) {
            @Override
            protected void onMessageReceived(MachineStatusEvent result) {
                switch (result.getEventType()) {
                    case RUNNING:
                        unsubscribe(wsChannel, this);

                        if (runningListener != null) {
                            runningListener.onRunning();
                        }

                        showInfo(RESTART.equals(operationType) ? locale.machineRestarted(machineName)
                                                               : locale.notificationMachineIsRunning(machineName), notification);
                        eventBus.fireEvent(MachineStateEvent.createMachineRunningEvent(machine));
                        break;
                    case DESTROYED:
                        unsubscribe(wsChannel, this);
                        showInfo(locale.notificationMachineDestroyed(machineName), notification);
                        eventBus.fireEvent(MachineStateEvent.createMachineDestroyedEvent(machine));
                        break;
                    case ERROR:
                        unsubscribe(wsChannel, this);
                        showError(result.getError(), notification);
                        break;
                }
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                unsubscribe(wsChannel, this);
                showError(exception.getMessage(), notification);
            }
        };

        switch (operationType) {
            case START:
                notification.setMessage(locale.notificationCreatingMachine(machineName));
                break;
            case RESTART:
                notification.setMessage(locale.notificationMachineRestarting(machineName));
                break;
            case DESTROY:
                notification.setMessage(locale.notificationDestroyingMachine(machineName));
                break;
        }

        notification.setStatus(PROGRESS);
        notificationManager.showNotification(notification);

        subscribe(wsChannel, messageHandler);
    }

    private void showInfo(@NotNull String message, @NotNull Notification notification) {
        notification.setMessage(message);
        notification.setStatus(FINISHED);
        notification.setType(INFO);
    }

    private void showError(@NotNull String message, @NotNull Notification notification) {
        notification.setMessage(message);
        notification.setStatus(FINISHED);
        notification.setType(ERROR);
    }

    private void subscribe(@NotNull String wsChannel, @NotNull MessageHandler handler) {
        try {
            messageBus.subscribe(wsChannel, handler);
        } catch (WebSocketException e) {
            Log.error(getClass(), e);
        }
    }

    private void unsubscribe(@NotNull String wsChannel, @NotNull MessageHandler handler) {
        try {
            messageBus.unsubscribe(wsChannel, handler);
        } catch (WebSocketException e) {
            Log.error(getClass(), e);
        }
    }

    /** Listener's method will be invoked when machine is running. */
    interface RunningListener {
        void onRunning();
    }
}
