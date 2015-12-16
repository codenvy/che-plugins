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
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.debug.Breakpoint;
import org.eclipse.che.ide.debug.BreakpointManager;
import org.eclipse.che.ide.debug.Debugger;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarFileNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeExtension;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeResources;
import org.eclipse.che.ide.ext.java.jdi.client.debug.changevalue.ChangeValuePresenter;
import org.eclipse.che.ide.ext.java.jdi.client.debug.expression.EvaluateExpressionPresenter;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolver;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverObserver;
import org.eclipse.che.ide.ext.java.jdi.client.marshaller.DebuggerEventListUnmarshallerWS;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakPoint;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakPointEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEventList;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerInfo;
import org.eclipse.che.ide.ext.java.jdi.shared.Location;
import org.eclipse.che.ide.ext.java.jdi.shared.StackFrameDump;
import org.eclipse.che.ide.ext.java.jdi.shared.StepEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.Value;
import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.HTTPStatus;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.exceptions.ServerException;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspaceHandler;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.OPEN;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;
import static org.eclipse.che.ide.api.notification.Notification.Type.WARNING;
import static org.eclipse.che.ide.debug.DebuggerStateEvent.createConnectedStateEvent;
import static org.eclipse.che.ide.debug.DebuggerStateEvent.createDisconnectedStateEvent;
import static org.eclipse.che.ide.debug.DebuggerStateEvent.createInitializedStateEvent;
import static org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent.BREAKPOINT;
import static org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent.STEP;

/**
 * The presenter provides debug java application.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyi
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 * @author Anatoliy Bazko
 */
@Singleton
public class DebuggerPresenter extends BasePresenter implements DebuggerView.ActionDelegate,
                                                                Debugger,
                                                                FqnResolverObserver {
    private static final String TITLE = "Debug";

    private final DtoFactory                             dtoFactory;
    private final DtoUnmarshallerFactory                 dtoUnmarshallerFactory;
    private final AppContext                             appContext;
    private final ProjectExplorerPresenter               projectExplorer;
    private final JavaNodeManager                        javaNodeManager;
    private final JavaRuntimeResources                   javaRuntimeResources;
    /** Channel identifier to receive events from debugger over WebSocket. */
    private       String                                 debuggerEventsChannel;
    /** Channel identifier to receive event when debugger will disconnected. */
    private       String                                 debuggerDisconnectedChannel;
    private       DebuggerView                           view;
    private       EventBus                               eventBus;
    private       DebuggerServiceClient                  service;
    private       JavaRuntimeLocalizationConstant        constant;
    private       DebuggerInfo                           debuggerInfo;
    private       MessageBus                             messageBus;
    private       BreakpointManager                      breakpointManager;
    private       WorkspaceAgent                         workspaceAgent;
    private       FqnResolverFactory                     resolverFactory;
    private       EditorAgent                            editorAgent;
    private       DebuggerVariable                       selectedVariable;
    private       EvaluateExpressionPresenter            evaluateExpressionPresenter;
    private       ChangeValuePresenter                   changeValuePresenter;
    private       NotificationManager                    notificationManager;
    /** Handler for processing events which is received from debugger over WebSocket connection. */
    private       SubscriptionHandler<DebuggerEventList> debuggerEventsHandler;
    private       SubscriptionHandler<Void>              debuggerDisconnectedHandler;
    private       List<DebuggerVariable>                 variables;
    private       Location                               executionPoint;

    private String host;
    private int    port;

    @Inject
    public DebuggerPresenter(DebuggerView view,
                             final DebuggerServiceClient service,
                             final EventBus eventBus,
                             final JavaRuntimeLocalizationConstant constant,
                             WorkspaceAgent workspaceAgent,
                             final BreakpointManager breakpointManager,
                             FqnResolverFactory resolverFactory,
                             EditorAgent editorAgent,
                             final EvaluateExpressionPresenter evaluateExpressionPresenter,
                             ChangeValuePresenter changeValuePresenter,
                             final NotificationManager notificationManager,
                             final DtoFactory dtoFactory,
                             DtoUnmarshallerFactory dtoUnmarshallerFactory,
                             final AppContext appContext,
                             ProjectExplorerPresenter projectExplorer,
                             final MessageBusProvider messageBusProvider,
                             final JavaNodeManager javaNodeManager,
                             JavaRuntimeResources javaRuntimeResources) {
        this.view = view;
        this.eventBus = eventBus;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
        this.projectExplorer = projectExplorer;
        this.javaRuntimeResources = javaRuntimeResources;
        this.view.setDelegate(this);
        this.view.setTitle(TITLE);
        this.service = service;
        this.constant = constant;
        this.workspaceAgent = workspaceAgent;
        this.breakpointManager = breakpointManager;
        this.resolverFactory = resolverFactory;
        this.resolverFactory.addFqnResolverObserver(this);
        this.variables = new ArrayList<>();
        this.editorAgent = editorAgent;
        this.evaluateExpressionPresenter = evaluateExpressionPresenter;
        this.changeValuePresenter = changeValuePresenter;
        this.notificationManager = notificationManager;
        this.javaNodeManager = javaNodeManager;

        eventBus.addHandler(ExtServerStateEvent.TYPE, new ExtServerStateHandler() {
            @Override
            public void onExtServerStarted(ExtServerStateEvent event) {
                messageBus = messageBusProvider.getMachineMessageBus();
            }

            @Override
            public void onExtServerStopped(ExtServerStateEvent event) {
            }
        });

        this.debuggerEventsHandler = new SubscriptionHandler<DebuggerEventList>(new DebuggerEventListUnmarshallerWS(dtoFactory)) {
            @Override
            public void onMessageReceived(DebuggerEventList result) {
                onEventListReceived(result);
            }

            @Override
            public void onErrorReceived(Throwable exception) {
                try {
                    messageBus.unsubscribe(debuggerEventsChannel, this);
                } catch (WebSocketException e) {
                    Log.error(DebuggerPresenter.class, e);
                }
                closeView();

                if (exception instanceof ServerException) {
                    ServerException serverException = (ServerException)exception;
                    if (HTTPStatus.INTERNAL_ERROR == serverException.getHTTPStatus()
                        && serverException.getMessage() != null
                        && serverException.getMessage().contains("not found")) {

                        onDebuggerDisconnected();
                        return;
                    }
                }
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
            }
        };

        this.debuggerDisconnectedHandler = new SubscriptionHandler<Void>() {
            @Override
            protected void onMessageReceived(Void result) {
                try {
                    messageBus.unsubscribe(debuggerDisconnectedChannel, this);
                } catch (WebSocketException e) {
                    Log.error(DebuggerPresenter.class, e);
                }

                evaluateExpressionPresenter.closeDialog();
                closeView();
                onDebuggerDisconnected();
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                try {
                    messageBus.unsubscribe(debuggerDisconnectedChannel, this);
                } catch (WebSocketException e) {
                    Log.error(DebuggerPresenter.class, e);
                }
            }
        };

        eventBus.addHandler(StartWorkspaceEvent.TYPE, new StartWorkspaceHandler() {
            @Override
            public void onWorkspaceStarted(UsersWorkspaceDto workspace) {
                messageBus = messageBusProvider.getMessageBus();
            }
        });

        eventBus.addHandler(CloseCurrentProjectEvent.TYPE, new CloseCurrentProjectHandler() {
            @Override
            public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
                // application will be stopped after closing a project
                if (debuggerInfo != null) {
                    changeButtonsEnableState(false);
                    onDebuggerDisconnected();
                    closeView();
                }
            }
        });

        eventBus.fireEvent(createInitializedStateEvent(this));
    }

    /** {@inheritDoc} */
    @Override
    @NotNull
    public String getTitle() {
        return TITLE;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return javaRuntimeResources.debug();
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return "Debug";
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        view.setBreakpoints(breakpointManager.getBreakpointList());
        view.setVariables(variables);
        container.setWidget(view);
    }

    private void onEventListReceived(@NotNull DebuggerEventList eventList) {
        if (eventList.getEvents().size() == 0) {
            return;
        }

        VirtualFile activeFile = null;
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor != null) {
            activeFile = activeEditor.getEditorInput().getFile();
        }
        Location location;
        List<DebuggerEvent> events = eventList.getEvents();
        for (DebuggerEvent event : events) {
            switch (event.getType()) {
                case STEP:
                    location = ((StepEvent)event).getLocation();
                    break;
                case BREAKPOINT:
                    location = ((BreakPointEvent)event).getBreakPoint().getLocation();
                    break;
                default:
                    Log.error(DebuggerPresenter.class, "Unknown type of debugger event: " + event.getType());
                    return;
            }
            this.executionPoint = location;

            List<String> filePaths = resolveFilePathByLocation(location);

            if (activeFile == null || !filePaths.contains(activeFile.getPath())) {
                final Location finalLocation = location;
                openFile(location, filePaths, 0, new AsyncCallback<VirtualFile>() {
                    @Override
                    public void onSuccess(VirtualFile result) {
                        breakpointManager.setCurrentBreakpoint(finalLocation.getLineNumber() - 1);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Notification notification =
                                new Notification(constant.errorSourceNotFoundForClass(finalLocation.getClassName()), WARNING);
                        notificationManager.showNotification(notification);
                    }
                });
            } else {
                breakpointManager.setCurrentBreakpoint(location.getLineNumber() - 1);
            }
            getStackFrameDump();
            changeButtonsEnableState(true);
        }
    }

    /**
     * Create file path from {@link Location}.
     *
     * @param location
     *         location of class
     * @return file path
     */
    @NotNull
    private List<String> resolveFilePathByLocation(@NotNull Location location) {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return Collections.emptyList();
        }

        String pathSuffix = location.getClassName().replace(".", "/") + ".java";

        List<String> sourceFolders = JavaSourceFolderUtil.getSourceFolders(currentProject);
        List<String> filePaths = new ArrayList<>(sourceFolders.size());

        for (String sourceFolder : sourceFolders) {
            filePaths.add(sourceFolder + pathSuffix);
        }

        return filePaths;
    }

    /**
     * Tries to open file from the project.
     * If fails then method will try to find resource from external dependencies.
     */
    private void openFile(@NotNull final Location location,
                          final List<String> filePaths,
                          final int pathNumber,
                          final AsyncCallback<VirtualFile> callback) {

        String filePath = filePaths.get(pathNumber);

        projectExplorer.getNodeByPath(new HasStorablePath.StorablePath(filePath)).then(new Operation<Node>() {
            public HandlerRegistration handlerRegistration;

            @Override
            public void apply(final Node node) throws OperationException {
                if (!(node instanceof FileReferenceNode)) {
                    return;
                }

                handlerRegistration = eventBus.addHandler(ActivePartChangedEvent.TYPE, new ActivePartChangedHandler() {
                    @Override
                    public void onActivePartChanged(ActivePartChangedEvent event) {
                        if (event.getActivePart() instanceof EditorPartPresenter) {
                            final VirtualFile openedFile = ((EditorPartPresenter)event.getActivePart()).getEditorInput().getFile();
                            if (((FileReferenceNode)node).getStorablePath().equals(openedFile.getPath())) {
                                handlerRegistration.removeHandler();
                                // give the editor some time to fully render it's view
                                new Timer() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess((VirtualFile)node);
                                    }
                                }.schedule(300);
                            }
                        }
                    }
                });
                eventBus.fireEvent(new FileEvent((VirtualFile)node, OPEN));
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                if (pathNumber + 1 == filePaths.size()) {
                    openExternalResource(location);
                } else {
                    // try another path
                    openFile(location, filePaths, pathNumber + 1, callback);
                }
            }
        });
    }


    private void openExternalResource(Location location) {
        String className = location.getClassName();

        JarEntry jarEntry = dtoFactory.createDto(JarEntry.class);
        jarEntry.setPath(className);
        jarEntry.setName(className.substring(className.lastIndexOf(".") + 1) + ".class");
        jarEntry.setType(JarEntry.JarEntryType.CLASS_FILE);

        JarFileNode jarFileNode = javaNodeManager.getJavaNodeFactory()
                                                 .newJarFileNode(jarEntry,
                                                                 null,
                                                                 appContext.getCurrentProject().getProjectConfig(),
                                                                 javaNodeManager.getJavaSettingsProvider().getSettings());

        openFile(jarFileNode);
    }

    private void openFile(VirtualFile result) {
        editorAgent.openEditor(result, new EditorAgent.OpenEditorCallback() {
            @Override
            public void onEditorOpened(EditorPartPresenter editor) {
            }
        });
    }

    private void getStackFrameDump() {
        service.getStackFrameDump(debuggerInfo.getId(),
                                  new AsyncRequestCallback<StackFrameDump>(dtoUnmarshallerFactory.newUnmarshaller(StackFrameDump.class)) {
                                      @Override
                                      protected void onSuccess(StackFrameDump result) {
                                          List<Variable> variables = new ArrayList<>();
                                          variables.addAll(result.getFields());
                                          variables.addAll(result.getLocalVariables());

                                          List<DebuggerVariable> debuggerVariables = getDebuggerVariables(variables);

                                          DebuggerPresenter.this.variables = debuggerVariables;
                                          view.setVariables(debuggerVariables);
                                          if (!variables.isEmpty()) {
                                              view.setExecutionPoint(variables.get(0).isExistInformation(), executionPoint);
                                          }
                                      }

                                      @Override
                                      protected void onFailure(Throwable exception) {
                                          Notification notification = new Notification(exception.getMessage(), ERROR);
                                          notificationManager.showNotification(notification);
                                      }
                                  }
                                 );
    }

    @NotNull
    private List<DebuggerVariable> getDebuggerVariables(@NotNull List<Variable> variables) {
        List<DebuggerVariable> debuggerVariables = new ArrayList<>();

        for (Variable variable : variables) {
            debuggerVariables.add(new DebuggerVariable(variable));
        }

        return debuggerVariables;
    }

    /** Change enable state of all buttons (except Disconnect button) on Debugger panel. */
    private void changeButtonsEnableState(boolean isEnable) {
        view.setEnableResumeButton(isEnable);
        view.setEnableStepIntoButton(isEnable);
        view.setEnableStepOverButton(isEnable);
        view.setEnableStepReturnButton(isEnable);
        view.setEnableEvaluateExpressionButtonEnable(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void onResumeButtonClicked() {
        changeButtonsEnableState(false);
        breakpointManager.removeCurrentBreakpoint();

        service.resume(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoveAllBreakpointsButtonClicked() {
        if (debuggerInfo != null) {
            service.deleteAllBreakpoints(debuggerInfo.getId(), new AsyncRequestCallback<String>() {
                @Override
                protected void onSuccess(String result) {
                    breakpointManager.removeAllBreakpoints();
                    updateBreakPoints();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    Notification notification = new Notification(exception.getMessage(), ERROR);
                    notificationManager.showNotification(notification);
                }
            });
        } else {
            breakpointManager.removeAllBreakpoints();
            updateBreakPoints();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onDisconnectButtonClicked() {
        disconnectDebugger();
    }

    /** {@inheritDoc} */
    @Override
    public void onStepIntoButtonClicked() {
        if (!view.resetStepIntoButton(false)) {
            return;
        }
        breakpointManager.removeCurrentBreakpoint();

        service.stepInto(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
                view.resetStepIntoButton(true);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
                view.resetStepIntoButton(true);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onStepOverButtonClicked() {
        if (!view.resetStepOverButton(false)) {
            return;
        }
        breakpointManager.removeCurrentBreakpoint();

        service.stepOver(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
                view.resetStepOverButton(true);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
                view.resetStepOverButton(true);
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    public void onStepReturnButtonClicked() {
        if (!view.resetStepReturnButton(false)) {
            return;
        }
        breakpointManager.removeCurrentBreakpoint();

        service.stepReturn(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
                view.resetStepReturnButton(true);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
                view.resetStepReturnButton(true);
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    public void onChangeValueButtonClicked() {
        if (selectedVariable == null) {
            return;
        }

        changeValuePresenter.showDialog(debuggerInfo, selectedVariable.getVariable(), new AsyncCallback<String>() {
            @Override
            public void onSuccess(String s) {
                getStackFrameDump();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.error(DebuggerPresenter.class, throwable);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onEvaluateExpressionButtonClicked() {
        evaluateExpressionPresenter.showDialog(debuggerInfo);
    }

    /** {@inheritDoc} */
    @Override
    public void onExpandVariablesTree() {
        List<DebuggerVariable> rootVariables = selectedVariable.getVariables();
        if (rootVariables.size() == 0) {
            service.getValue(debuggerInfo.getId(), selectedVariable.getVariable(),
                             new AsyncRequestCallback<Value>(dtoUnmarshallerFactory.newUnmarshaller(Value.class)) {
                                 @Override
                                 protected void onSuccess(Value result) {
                                     List<Variable> variables = result.getVariables();

                                     List<DebuggerVariable> debuggerVariables = getDebuggerVariables(variables);

                                     view.setVariablesIntoSelectedVariable(debuggerVariables);
                                     view.updateSelectedVariable();
                                 }

                                 @Override
                                 protected void onFailure(Throwable exception) {
                                     Notification notification = new Notification(exception.getMessage(), ERROR);
                                     notificationManager.showNotification(notification);
                                 }
                             }
                            );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectedVariableElement(@NotNull DebuggerVariable variable) {
        this.selectedVariable = variable;
        updateChangeValueButtonEnableState();
    }

    /** Update enable state for 'Change value' button. */
    private void updateChangeValueButtonEnableState() {
        view.setEnableChangeValueButtonEnable(selectedVariable != null);
    }

    private void resetStates() {
        variables.clear();
        view.setVariables(variables);
        selectedVariable = null;
        updateChangeValueButtonEnableState();
    }

    private void showDialog(@NotNull DebuggerInfo debuggerInfo) {
        view.setVMName(debuggerInfo.getVmName() + " " + debuggerInfo.getVmVersion());
        selectedVariable = null;
        updateChangeValueButtonEnableState();
        changeButtonsEnableState(false);
        view.setEnableDisconnectButton(false);

        workspaceAgent.openPart(this, PartStackType.INFORMATION);
        PartPresenter activePart = partStack.getActivePart();
        if (activePart != null && !activePart.equals(this)) {
            partStack.setActivePart(this);
        }
    }

    private void closeView() {
        variables.clear();
        view.setVariables(variables);
        view.setExecutionPoint(true, null);
        view.setEnableDisconnectButton(false);
        workspaceAgent.hidePart(this);
    }

    /**
     * Attached debugger via special host and port for current project.
     *
     * @param host
     *         host which need to connect to debugger
     * @param port
     *         port which need to connect to debugger
     */
    public void attachDebugger(@NotNull final String host, @Min(1) final int port) {
        this.host = host;
        this.port = port;

        service.connect(host, port, new AsyncRequestCallback<DebuggerInfo>(dtoUnmarshallerFactory.newUnmarshaller(DebuggerInfo.class)) {
                            @Override
                            public void onSuccess(DebuggerInfo result) {
                                debuggerInfo = result;
                                Notification notification = new Notification(constant.debuggerConnected(host + ':' + port), INFO);
                                notificationManager.showNotification(notification);
                                view.setEnableDisconnectButton(true);
                                startCheckingEvents();

                                eventBus.fireEvent(createConnectedStateEvent(DebuggerPresenter.this));
                            }

                            @Override
                            protected void onFailure(Throwable exception) {
                                Notification notification = new Notification(exception.getMessage(), ERROR);
                                notificationManager.showNotification(notification);
                            }
                        }
                       );
    }

    private void disconnectDebugger() {
        if (debuggerInfo != null) {
            stopCheckingDebugEvents();
            service.disconnect(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    changeButtonsEnableState(false);
                    onDebuggerDisconnected();
                    closeView();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    Notification notification = new Notification(exception.getMessage(), ERROR);
                    notificationManager.showNotification(notification);
                }
            });
        } else {
            changeButtonsEnableState(false);
        }
    }

    private void startCheckingEvents() {
        debuggerEventsChannel = JavaRuntimeExtension.EVENTS_CHANNEL + debuggerInfo.getId();
        try {
            messageBus.subscribe(debuggerEventsChannel, debuggerEventsHandler);
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }

        try {
            debuggerDisconnectedChannel = JavaRuntimeExtension.DISCONNECT_CHANNEL + debuggerInfo.getId();
            messageBus.subscribe(debuggerDisconnectedChannel, debuggerDisconnectedHandler);
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }
    }

    private void stopCheckingDebugEvents() {
        try {
            if (messageBus.isHandlerSubscribed(debuggerEventsHandler, debuggerEventsChannel)) {
                messageBus.unsubscribe(debuggerEventsChannel, debuggerEventsHandler);
            }

            if (messageBus.isHandlerSubscribed(debuggerDisconnectedHandler, debuggerDisconnectedChannel)) {
                messageBus.unsubscribe(debuggerDisconnectedChannel, debuggerDisconnectedHandler);
            }
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }
    }

    /** Perform some action after disconnecting a debugger. */
    private void onDebuggerDisconnected() {
        debuggerInfo = null;
        Notification notification = new Notification(constant.debuggerDisconnected(host + ':' + port), INFO);
        notificationManager.showNotification(notification);

        eventBus.fireEvent(createDisconnectedStateEvent(this));
    }

    /**
     * Updates breakpoints list.
     * The main idea is to display FQN instead of file path.
     */
    private void updateBreakPoints() {
        List<Breakpoint> breakpoints = breakpointManager.getBreakpointList();
        List<Breakpoint> breakpoints2Display = new ArrayList<Breakpoint>(breakpoints.size());

        for (Breakpoint breakpoint : breakpoints) {
            FqnResolver resolver = resolverFactory.getResolver(breakpoint.getFile().getMediaType());

            breakpoints2Display.add(new Breakpoint(breakpoint.getType(),
                                                   breakpoint.getLineNumber(),
                                                   resolver == null ? breakpoint.getPath() : resolver.resolveFqn(breakpoint.getFile()),
                                                   breakpoint.getFile(),
                                                   breakpoint.getMessage(),
                                                   breakpoint.isActive()));
        }

        view.setEnableRemoveAllBreakpointsButton(!breakpoints.isEmpty());
        view.setBreakpoints(breakpoints2Display);
    }

    /** {@inheritDoc} */
    @Override
    public void addBreakpoint(@NotNull final VirtualFile file, final int lineNumber, final AsyncCallback<Breakpoint> callback) {
        if (debuggerInfo != null) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);
            final FqnResolver resolver = resolverFactory.getResolver(file.getMediaType());
            if (resolver != null) {
                location.setClassName(resolver.resolveFqn(file));
            } else {
                Log.warn(DebuggerPresenter.class, "FqnResolver is not found");
            }

            BreakPoint breakPoint = dtoFactory.createDto(BreakPoint.class);
            breakPoint.setLocation(location);
            breakPoint.setEnabled(true);
            service.addBreakpoint(debuggerInfo.getId(), breakPoint, new AsyncRequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    if (resolver != null) {
                        Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, true);
                        callback.onSuccess(breakpoint);
                        updateBreakPoints();
                    }
                }

                @Override
                protected void onFailure(Throwable exception) {
                    callback.onFailure(exception);
                    updateBreakPoints();

                }
            });
        } else {
            callback.onFailure(new IllegalStateException("Debugger not attached"));
            showDialog(EmptyDebuggerInfo.INSTANCE);
            updateBreakPoints();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteBreakpoint(@NotNull VirtualFile file, int lineNumber, final AsyncCallback<Void> callback) {
        if (debuggerInfo != null) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);
            FqnResolver resolver = resolverFactory.getResolver(file.getMediaType());
            if (resolver != null) {
                location.setClassName(resolver.resolveFqn(file));
            } else {
                Log.warn(DebuggerPresenter.class, "FqnResolver is not found");
            }

            BreakPoint point = dtoFactory.createDto(BreakPoint.class);
            point.setLocation(location);
            point.setEnabled(true);

            service.deleteBreakpoint(debuggerInfo.getId(), point, new AsyncRequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    callback.onSuccess(null);
                    updateBreakPoints();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    callback.onFailure(exception);
                    updateBreakPoints();
                }
            });
        } else {
            callback.onFailure(new IllegalStateException("Debugger not attached"));
            showDialog(EmptyDebuggerInfo.INSTANCE);
            updateBreakPoints();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onFqnResolverAdded(FqnResolver fqnResolver) {
        updateBreakPoints();
    }
}
