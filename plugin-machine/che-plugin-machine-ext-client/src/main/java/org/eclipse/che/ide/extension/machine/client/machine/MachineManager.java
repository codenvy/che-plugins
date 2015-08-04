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
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.OutputMessageUnmarshaller;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.machine.MachineStatusNotifier.RunningListener;
import org.eclipse.che.ide.extension.machine.client.machine.console.MachineConsolePresenter;
import org.eclipse.che.ide.extension.machine.client.outputspanel.OutputsContainerPresenter;
import org.eclipse.che.ide.extension.machine.client.outputspanel.console.CommandConsoleFactory;
import org.eclipse.che.ide.extension.machine.client.outputspanel.console.OutputConsole;
import org.eclipse.che.ide.extension.machine.client.util.RecipeProvider;
import org.eclipse.che.ide.extension.machine.client.watcher.SystemFileWatcher;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.GET;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.OperationType.DESTROY;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.OperationType.RESTART;
import static org.eclipse.che.ide.extension.machine.client.machine.MachineManager.OperationType.START;

/**
 * Manager for machine operations.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class MachineManager implements ProjectActionHandler {

    private final MachineServiceClient        machineServiceClient;
    private final MessageBus                  messageBus;
    private final MachineConsolePresenter     machineConsolePresenter;
    private final OutputsContainerPresenter   outputsContainerPresenter;
    private final CommandConsoleFactory       commandConsoleFactory;
    private final NotificationManager         notificationManager;
    private final MachineLocalizationConstant localizationConstant;
    private final WorkspaceAgent              workspaceAgent;
    private final MachineStatusNotifier       machineStatusNotifier;
    private final DialogFactory               dialogFactory;
    private final RecipeProvider              recipeProvider;
    private final EntityFactory               entityFactory;
    private final SystemFileWatcher           systemFileWatcher;

    /** Stores ID of the developer machine (where workspace or current project is bound). */
    private String devMachineId;

    @Inject
    public MachineManager(MachineServiceClient machineServiceClient,
                          MessageBus messageBus,
                          MachineConsolePresenter machineConsolePresenter,
                          OutputsContainerPresenter outputsContainerPresenter,
                          CommandConsoleFactory commandConsoleFactory,
                          NotificationManager notificationManager,
                          MachineLocalizationConstant localizationConstant,
                          WorkspaceAgent workspaceAgent,
                          MachineStatusNotifier machineStatusNotifier,
                          DialogFactory dialogFactory,
                          RecipeProvider recipeProvider,
                          EntityFactory entityFactory,
                          SystemFileWatcher systemFileWatcher) {
        this.machineServiceClient = machineServiceClient;
        this.messageBus = messageBus;
        this.machineConsolePresenter = machineConsolePresenter;
        this.outputsContainerPresenter = outputsContainerPresenter;
        this.commandConsoleFactory = commandConsoleFactory;
        this.notificationManager = notificationManager;
        this.localizationConstant = localizationConstant;
        this.workspaceAgent = workspaceAgent;
        this.machineStatusNotifier = machineStatusNotifier;
        this.dialogFactory = dialogFactory;
        this.recipeProvider = recipeProvider;
        this.entityFactory = entityFactory;
        this.systemFileWatcher = systemFileWatcher;
    }

    @Override
    public void onProjectOpened(final ProjectActionEvent event) {
        machineServiceClient.getMachinesStates(null).then(new Operation<List<MachineStateDescriptor>>() {
            @Override
            public void apply(List<MachineStateDescriptor> arg) throws OperationException {
                for (MachineStateDescriptor machineStateDescriptor : arg) {
                    if (machineStateDescriptor.isWorkspaceBound()) {
                        devMachineId = machineStateDescriptor.getId();
                        return;
                    }
                }
                startAndBindMachine(event.getProject().getRecipe(), "Dev");
            }
        });
    }

    @Override
    public void onProjectClosing(ProjectActionEvent event) {
    }

    @Override
    public void onProjectClosed(ProjectActionEvent event) {
        devMachineId = null;
        machineConsolePresenter.clear();
    }

    public void restartMachine(@Nonnull final Machine machine) {
        machineServiceClient.destroyMachine(machine.getId()).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                String recipeUrl = recipeProvider.getRecipeUrl();
                String displayName = machine.getDisplayName();

                boolean isWSBound = machine.isWorkspaceBound();

                startMachine(recipeUrl, displayName, isWSBound, RESTART);
            }
        });
    }

    /** Start new machine. */
    public void startMachine(String recipeURL, @Nonnull String displayName) {
        startMachine(recipeURL, displayName, false, START);
    }

    /** Start new machine and bind workspace to running machine. */
    public void startAndBindMachine(String recipeURL, @Nonnull String displayName) {
        startMachine(recipeURL, displayName, true, START);
    }

    private void startMachine(@Nonnull final String recipeURL,
                              @Nonnull final String displayName,
                              final boolean bindWorkspace,
                              @Nonnull final OperationType operationType) {
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
                final Machine machine = entityFactory.createMachine(machineDescriptor);

                RunningListener runningListener = null;

                if (bindWorkspace) {
                    runningListener = new RunningListener() {
                        @Override
                        public void onRunning() {
                            devMachineId = machine.getId();

                            systemFileWatcher.registerWatcher(machineDescriptor.getWorkspaceId());
                        }
                    };
                }

                machineStatusNotifier.trackMachine(machine, runningListener, operationType);
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
                if (devMachineId != null && machine.getId().equals(devMachineId)) {
                    devMachineId = null;
                }
            }
        });
    }

    /** Returns ID of the developer machine (where workspace or current project is bound). */
    @Nullable
    public String getDeveloperMachineId() {
        return devMachineId;
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

    /** Execute the the given command configuration on the developer machine. */
    public void execute(@Nonnull CommandConfiguration configuration) {
        if (devMachineId == null) {
            notificationManager.showWarning(localizationConstant.noDevMachine());
            return;
        }

        final String outputChannel = "process:output:" + UUID.uuid();

        final OutputConsole console = commandConsoleFactory.create(configuration, devMachineId);
        console.listenToOutput(outputChannel);
        outputsContainerPresenter.addConsole(console);
        workspaceAgent.setActivePart(outputsContainerPresenter);

        final Promise<ProcessDescriptor> processPromise = machineServiceClient.executeCommand(devMachineId,
                                                                                              configuration.toCommandLine(),
                                                                                              outputChannel);
        processPromise.then(new Operation<ProcessDescriptor>() {
            @Override
            public void apply(ProcessDescriptor arg) throws OperationException {
                console.attachToProcess(arg.getPid());
            }
        });
    }

    enum OperationType {
        START, RESTART, DESTROY
    }
}
