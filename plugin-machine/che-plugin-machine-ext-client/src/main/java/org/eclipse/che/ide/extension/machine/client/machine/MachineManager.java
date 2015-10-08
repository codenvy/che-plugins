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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.ExtServerStateController;
import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.gwt.client.OutputMessageUnmarshaller;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateHandler;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.machine.MachineStatusNotifier.RunningListener;
import org.eclipse.che.ide.extension.machine.client.machine.console.MachineConsolePresenter;
import org.eclipse.che.ide.extension.machine.client.util.RecipeProvider;
import org.eclipse.che.ide.extension.machine.client.watcher.SystemFileWatcher;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspaceHandler;

import javax.validation.constraints.NotNull;

import static com.google.gwt.http.client.RequestBuilder.GET;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.MachineOperationType.DESTROY;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.MachineOperationType.RESTART;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.MachineOperationType.START;

/**
 * Manager for machine operations.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class MachineManager {

    private final ExtServerStateController extServerStateController;
    private final MachineServiceClient     machineServiceClient;
    private final MachineConsolePresenter  machineConsolePresenter;
    private final NotificationManager      notificationManager;
    private final MachineStatusNotifier    machineStatusNotifier;
    private final DialogFactory            dialogFactory;
    private final RecipeProvider           recipeProvider;
    private final EntityFactory            entityFactory;
    private final AppContext               appContext;

    private MessageBus messageBus;
    private Machine    devMachine;

    @Inject
    public MachineManager(ExtServerStateController extServerStateController,
                          MachineServiceClient machineServiceClient,
                          MachineConsolePresenter machineConsolePresenter,
                          NotificationManager notificationManager,
                          MachineStatusNotifier machineStatusNotifier,
                          final MessageBusProvider messageBusProvider,
                          DialogFactory dialogFactory,
                          RecipeProvider recipeProvider,
                          EntityFactory entityFactory,
                          EventBus eventBus,
                          AppContext appContext,
                          Provider<SystemFileWatcher> systemFileWatcherProvider) {
        this.extServerStateController = extServerStateController;
        this.machineServiceClient = machineServiceClient;
        this.machineConsolePresenter = machineConsolePresenter;
        this.notificationManager = notificationManager;
        this.machineStatusNotifier = machineStatusNotifier;
        this.dialogFactory = dialogFactory;
        this.recipeProvider = recipeProvider;
        this.entityFactory = entityFactory;
        this.appContext = appContext;

        this.messageBus = messageBusProvider.getMessageBus();

        systemFileWatcherProvider.get();

        eventBus.addHandler(StartWorkspaceEvent.TYPE, new StartWorkspaceHandler() {
            @Override
            public void onWorkspaceStarted(UsersWorkspaceDto workspace) {
                messageBus = messageBusProvider.getMessageBus();
            }
        });

        eventBus.addHandler(DevMachineStateEvent.TYPE, new DevMachineStateHandler() {
            @Override
            public void onMachineStarted(DevMachineStateEvent event) {
                onMachineRunning(event.getMachineId());
            }

            @Override
            public void onMachineDestroyed(DevMachineStateEvent event) {

            }
        });
    }

    public void restartMachine(@NotNull final Machine machine) {
        machineServiceClient.destroyMachine(machine.getId()).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                final String recipeUrl = recipeProvider.getRecipeUrl();
                final String displayName = machine.getDisplayName();
                final boolean isDev = machine.isDev();

                startMachine(recipeUrl, displayName, isDev, RESTART);
            }
        });
    }

    /** Start new machine. */
    public void startMachine(String recipeURL, @NotNull String displayName) {
        startMachine(recipeURL, displayName, false, START);
    }

    /** Start new machine as dev-machine (bind workspace to running machine). */
    public void startDevMachine(String recipeURL, @NotNull String displayName) {
        startMachine(recipeURL, displayName, true, START);
    }

    private void startMachine(@NotNull final String recipeURL,
                              @NotNull final String displayName,
                              final boolean bindWorkspace,
                              @NotNull final MachineOperationType operationType) {
        downloadRecipe(recipeURL).thenPromise(new Function<String, Promise<MachineDescriptor>>() {
            @Override
            public Promise<MachineDescriptor> apply(String recipeScript) throws FunctionException {
                final String outputChannel = "machine:output:" + UUID.uuid();
                subscribeToOutput(outputChannel);

                return machineServiceClient.createMachineFromRecipe("docker",
                                                                    "Dockerfile",
                                                                    recipeScript,
                                                                    displayName,
                                                                    bindWorkspace,
                                                                    outputChannel);
            }
        }).then(new Operation<MachineDescriptor>() {
            @Override
            public void apply(final MachineDescriptor machineDescriptor) throws OperationException {
                RunningListener runningListener = null;

                if (bindWorkspace) {
                    runningListener = new RunningListener() {
                        @Override
                        public void onRunning() {
                            onMachineRunning(machineDescriptor.getId());
                        }
                    };
                }

                final Machine machine = entityFactory.createMachine(machineDescriptor);
                machineStatusNotifier.trackMachine(machine, runningListener, operationType);
            }
        });
    }

    public void onMachineRunning(final String machineId) {
        machineServiceClient.getMachine(machineId).then(new Operation<MachineDescriptor>() {
            @Override
            public void apply(MachineDescriptor machineDescriptor) throws OperationException {
                appContext.setDevMachineId(machineId);
                devMachine = entityFactory.createMachine(machineDescriptor);
                extServerStateController.initialize(devMachine.getWsServerExtensionsUrl() + "/" + appContext.getWorkspace().getId());
            }
        });
    }

    private Promise<String> downloadRecipe(String recipeURL) {
        final RequestBuilder builder = new RequestBuilder(GET, recipeURL);
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<String>() {
            @Override
            public void makeCall(final AsyncCallback<String> callback) {
                try {
                    builder.sendRequest("", new RequestCallback() {
                        public void onResponseReceived(Request request, Response response) {
                            final String text = response.getText();
                            if (text.isEmpty()) {
                                callback.onFailure(new Exception("Unable to fetch recipe"));
                            } else {
                                callback.onSuccess(text);
                            }
                        }

                        public void onError(Request request, Throwable exception) {
                            callback.onFailure(exception);
                        }
                    });
                } catch (RequestException e) {
                    callback.onFailure(e);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                dialogFactory.createMessageDialog("", arg.toString(), null).show();
            }
        });
    }

    public void destroyMachine(final Machine machine) {
        machineServiceClient.destroyMachine(machine.getId()).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                machineStatusNotifier.trackMachine(machine, DESTROY);

                final String devMachineId = appContext.getDevMachineId();
                if (devMachineId != null && machine.getId().equals(devMachineId)) {
                    appContext.setDevMachineId(null);
                }
            }
        });
    }

    private void subscribeToOutput(final String channel) {
        try {
            messageBus.subscribe(
                    channel,
                    new SubscriptionHandler<String>(new OutputMessageUnmarshaller()) {
                        @Override
                        protected void onMessageReceived(String result) {
                            machineConsolePresenter.print(result);
                        }

                        @Override
                        protected void onErrorReceived(Throwable exception) {
                            notificationManager.showError(exception.getMessage());
                        }
                    });
        } catch (WebSocketException e) {
            Log.error(MachineManager.class, e);
            notificationManager.showError(e.getMessage());
        }
    }

    enum MachineOperationType {
        START, RESTART, DESTROY
    }
}
