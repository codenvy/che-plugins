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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl;

import com.google.gwt.http.client.URL;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.texteditor.UndoableEditor;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.actions.ChooseRunnerAction;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.EnvironmentScript;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
import org.eclipse.che.ide.ext.runner.client.util.EnvironmentIdValidator;
import org.eclipse.che.ide.ext.runner.client.util.NameGenerator;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.models.EnvironmentImpl.ROOT_FOLDER;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.DEFAULT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
public class PropertiesEnvironmentPanel extends PropertiesPanelPresenter {

    private static       RegExp FILE_NAME             = RegExp.compile("^[A-Za-z0-9_\\s-\\.]+$");
    private static final String CONFIGURATION_TYPE    = "configurationType";
    private static final String DOCKER_SCRIPT_NAME    = "/Dockerfile";
    public static final  String ENVIRONMENT_ID_PREFIX = "project:/";

    private final Environment                                environment;
    private final DtoFactory                                 dtoFactory;
    private final EditorProvider                             editorProvider;
    private final FileTypeRegistry                           fileTypeRegistry;
    private final DockerFileFactory                          dockerFileFactory;
    private final ProjectServiceClient                       projectService;
    private final EventBus                                   eventBus;
    private final RunnerLocalizationConstant                 locale;
    private final GetProjectEnvironmentsAction               projectEnvironmentsAction;
    private final NotificationManager                        notificationManager;
    private final DtoUnmarshallerFactory                     unmarshallerFactory;
    private final AsyncCallbackBuilder<ItemReference>        asyncCallbackBuilder;
    private final AsyncCallbackBuilder<List<ItemReference>> asyncArrayCallbackBuilder;
    private final AsyncCallbackBuilder<Void>                 voidAsyncCallbackBuilder;
    private final AsyncCallbackBuilder<ProjectDescriptor>    asyncDescriptorCallbackBuilder;
    private final DialogFactory                              dialogFactory;
    private final ChooseRunnerAction                         chooseRunnerAction;
    private final List<RemovePanelListener>                  listeners;
    private final TemplatesContainer                         templatesContainer;
    private final EditorAgent                                editorAgent;

    private ProjectDescriptor                projectDescriptor;
    private Map<String, RunnerConfiguration> runnerConfigs;
    private int                              currentRam;
    private boolean                          isParameterChanged;

    @AssistedInject
    public PropertiesEnvironmentPanel(final PropertiesPanelView view,
                                      DtoFactory dtoFactory,
                                      @Named("DefaultEditorProvider") EditorProvider editorProvider,
                                      @Nonnull final FileTypeRegistry fileTypeRegistry,
                                      final DockerFileFactory dockerFileFactory,
                                      final ProjectServiceClient projectService,
                                      EventBus eventBus,
                                      AppContext appContext,
                                      ChooseRunnerAction chooseRunnerAction,
                                      DialogFactory dialogFactory,
                                      RunnerLocalizationConstant locale,
                                      GetProjectEnvironmentsAction projectEnvironmentsAction,
                                      NotificationManager notificationManager,
                                      DtoUnmarshallerFactory unmarshallerFactory,
                                      AsyncCallbackBuilder<ItemReference> asyncCallbackBuilder,
                                      AsyncCallbackBuilder<List<ItemReference>> asyncArrayCallbackBuilder,
                                      AsyncCallbackBuilder<Void> voidAsyncCallbackBuilder,
                                      AsyncCallbackBuilder<ProjectDescriptor> asyncDescriptorCallbackBuilder,
                                      TemplatesContainer templatesContainer,
                                      EditorAgent editorAgent,
                                      @Assisted @Nonnull final Environment environment) {
        super(view, appContext);
        this.dtoFactory = dtoFactory;
        this.editorProvider = editorProvider;
        this.fileTypeRegistry = fileTypeRegistry;
        this.dockerFileFactory = dockerFileFactory;
        this.projectService = projectService;
        this.chooseRunnerAction = chooseRunnerAction;
        this.eventBus = eventBus;
        this.environment = environment;
        this.locale = locale;
        this.projectEnvironmentsAction = projectEnvironmentsAction;
        this.notificationManager = notificationManager;
        this.unmarshallerFactory = unmarshallerFactory;
        this.asyncCallbackBuilder = asyncCallbackBuilder;
        this.asyncArrayCallbackBuilder = asyncArrayCallbackBuilder;
        this.voidAsyncCallbackBuilder = voidAsyncCallbackBuilder;
        this.asyncDescriptorCallbackBuilder = asyncDescriptorCallbackBuilder;
        this.templatesContainer = templatesContainer;
        this.editorAgent = editorAgent;

        this.dialogFactory = dialogFactory;

        this.listeners = new ArrayList<>();

        boolean isProjectScope = PROJECT.equals(environment.getScope());

        this.view.setEnableNameProperty(isProjectScope);
        this.view.setEnableRamProperty(isProjectScope);
        this.view.setEnableBootProperty(false);
        this.view.setEnableShutdownProperty(false);
        this.view.setEnableScopeProperty(false);
        this.view.setVisibleConfigLink(false);

        this.view.setVisibleSaveButton(isProjectScope);
        this.view.setVisibleDeleteButton(isProjectScope);
        this.view.setVisibleCancelButton(isProjectScope);

        if (isProjectScope) {
            getProjectEnvironmentDocker();
        } else {
            getSystemEnvironmentDocker();
        }

        projectDescriptor = currentProject.getProjectDescription();

        runnerConfigs = projectDescriptor.getRunners().getConfigs();

        currentRam = getRam(environment.getId());
    }

    @Nonnegative
    private int getRam(@Nonnull String environmentId) {
        boolean isConfigExist = runnerConfigs.containsKey(environmentId) || runnerConfigs.containsKey(URL.encode(environmentId));

        if (!isConfigExist) {
            return DEFAULT.getValue();
        }

        if (runnerConfigs.get(environmentId) != null) {
            return runnerConfigs.get(environmentId).getRam();
        } else {
            return runnerConfigs.get(URL.encode(environmentId)).getRam();
        }
    }

    /**
     * Gets the type of an environment. First we check if type is not defined by a runner configuration. If there is not, use type of the
     * environment
     *
     * @param environment
     *         the environment to check
     * @return the type of the provided environment
     */
    private String getType(@Nonnull Environment environment) {
        String envId = URL.encode(environment.getId());
        RunnerConfiguration runnerConfiguration = runnerConfigs.get(envId);
        if (runnerConfiguration != null) {
            return runnerConfiguration.getVariables().get(CONFIGURATION_TYPE);
        } else {
            return environment.getType();
        }
    }

    private void getProjectEnvironmentDocker() {
        Unmarshallable<List<ItemReference>> unmarshaller = unmarshallerFactory.newListUnmarshaller(ItemReference.class);

        AsyncRequestCallback<List<ItemReference>> arrayAsyncCallback =
                asyncArrayCallbackBuilder.unmarshaller(unmarshaller)
                                         .success(new SuccessCallback<List<ItemReference>>() {
                                             @Override
                                             public void onSuccess(List<ItemReference> result) {
                                                 for (ItemReference item : result) {
                                                     VirtualFile file = new EnvironmentScript(item,
                                                                                              projectService,
                                                                                              environment.getName());

                                                     initializeEditor(file, editorProvider, fileTypeRegistry);
                                                 }
                                             }
                                         })
                                         .failure(new FailureCallback() {
                                             @Override
                                             public void onFailure(@Nonnull Throwable exception) {
                                                 Log.error(getClass(), exception.getMessage());
                                             }
                                         })
                                         .build();

        projectService.getChildren(environment.getPath(), arrayAsyncCallback);

    }

    private void getSystemEnvironmentDocker() {
        VirtualFile file = dockerFileFactory.newInstance(environment.getPath());
        initializeEditor(file, editorProvider, fileTypeRegistry);
    }

    /** {@inheritDoc} */
    @Override
    public void onCopyButtonClicked() {
        List<Environment> projectEnvironments = templatesContainer.getProjectEnvironments();

        final String fileName = NameGenerator.generateCopy(environment.getName(), projectEnvironments);

        String path = projectDescriptor.getPath() + ROOT_FOLDER + fileName;

        AsyncRequestCallback<ItemReference> callback = asyncCallbackBuilder.unmarshaller(ItemReference.class)
                                                                           .success(new SuccessCallback<ItemReference>() {
                                                                               @Override
                                                                               public void onSuccess(ItemReference result) {
                                                                                   getEditorContent(fileName);
                                                                               }
                                                                           })
                                                                           .failure(new FailureCallback() {
                                                                               @Override
                                                                               public void onFailure(@Nonnull Throwable reason) {
                                                                                   notificationManager.showError(reason.getMessage());
                                                                               }
                                                                           })
                                                                           .build();

        projectService.createFolder(path, callback);
    }

    private void getEditorContent(@Nonnull final String fileName) {

        editor.getEditorInput().getFile().getContent().then(new Operation<String>() {
            @Override
            public void apply(String content) throws OperationException {
                createFile(content, fileName);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                notificationManager.showError(arg.getMessage());
            }
        });
    }

    private void createFile(@Nonnull String content, @Nonnull String fileName) {
        String path = currentProject.getProjectDescription().getPath() + ROOT_FOLDER;

        AsyncRequestCallback<ItemReference> callback =
                asyncCallbackBuilder.unmarshaller(ItemReference.class)
                                    .success(new SuccessCallback<ItemReference>() {
                                        @Override
                                        public void onSuccess(ItemReference result) {
                                            isParameterChanged = false;
                                            setEnableSaveCancelDeleteBtn(false);

                                            updateRunnerConfig(result);

                                            projectEnvironmentsAction.perform();
                                        }
                                    })
                                    .failure(new FailureCallback() {
                                        @Override
                                        public void onFailure(@Nonnull Throwable reason) {
                                            Log.error(PropertiesPanelPresenter.class, reason.getMessage());
                                        }
                                    })
                                    .build();

        projectService.createFile(path, fileName + DOCKER_SCRIPT_NAME, content, null, callback);
    }

    private void updateRunnerConfig(@Nonnull ItemReference result) {
        boolean isConfigExist = runnerConfigs.containsKey(environment.getId());
        view.selectShutdown(getTimeout());
        String newEnvironmentName = getNewEnvironmentName(result.getPath());
        Map<String, String> variables = new HashMap<>();
        variables.put(CONFIGURATION_TYPE, environment.getType());

        if (isConfigExist) {
            int ram = getRam(environment.getId());
            RunnerConfiguration newConfig = dtoFactory.createDto(RunnerConfiguration.class)
                                                      .withVariables(variables)
                                                      .withRam(ram);

            runnerConfigs.put(generateEnvironmentId(newEnvironmentName), newConfig);
            environment.setRam(ram);
            view.selectMemory(RAM.detect(ram));
        } else {
            int ram = environment.getRam();
            RunnerConfiguration newConfig = dtoFactory.createDto(RunnerConfiguration.class)
                                                      .withVariables(variables)
                                                      .withRam(ram);
            runnerConfigs.put(generateEnvironmentId(newEnvironmentName), newConfig);
            updateProject();
            view.setType(environment.getType());
            view.selectMemory(RAM.detect(ram));
        }
    }

    @Nonnull
    private String getNewEnvironmentName(@Nonnull String path) {
        String withoutDocker = path.substring(0, path.lastIndexOf('/'));

        return withoutDocker.substring(withoutDocker.lastIndexOf('/') + 1);
    }

    private String generateEnvironmentId(@Nonnull String environmentName) {
        String newName = URL.encode(ENVIRONMENT_ID_PREFIX + environmentName);
        // with GWT mocks, native methods can be empty
        if (newName.isEmpty()) {
            return ENVIRONMENT_ID_PREFIX + environmentName;
        }
        return newName;
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigurationChanged() {
        view.incorrectName(false);

        String name = view.getName();

        if (!FILE_NAME.test(name)) {
            view.incorrectName(true);
            return;
        }

        isParameterChanged = true;

        environment.setRam(view.getRam().getValue());

        view.setEnableSaveButton(PROJECT.equals(environment.getScope()));
        view.setEnableCancelButton(true);
    }

    /** {@inheritDoc} */
    @Override
    public void onSaveButtonClicked() {
        isParameterChanged = false;

        final String newEnvironmentName = view.getName();
        final String environmentName = environment.getName();
        final String environmentId = environment.getId();
        final String encodedEnvironmentId = URL.encode(environmentId);

        final String pathToProject = projectDescriptor.getPath();
        final String path = pathToProject + ROOT_FOLDER + environmentName;

        final AsyncRequestCallback<Void> renameAsyncRequestCallback = voidAsyncCallbackBuilder
                .success(new SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        RunnerConfiguration config = runnerConfigs.get(encodedEnvironmentId);
                        runnerConfigs.remove(encodedEnvironmentId);

                        runnerConfigs.put(generateEnvironmentId(newEnvironmentName), config);

                        String defaultRunner = currentProject.getRunner();

                        if (defaultRunner != null && defaultRunner.equals(environmentId)) {
                            projectDescriptor.getRunners().setDefault(generateEnvironmentId(newEnvironmentName));
                        }

                        updateProject();
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        notificationManager.showError(reason.getMessage());
                    }
                })
                .build();


        // we save before trying to rename the file
        if (editor.isDirty()) {
            editor.doSave(new AsyncCallback<EditorInput>() {
                @Override
                public void onSuccess(EditorInput editorInput) {
                    view.setEnableSaveButton(false);
                    view.setEnableCancelButton(false);
                    // rename file
                    if (!environmentName.equals(newEnvironmentName)) {
                        projectService.rename(path, newEnvironmentName, null, renameAsyncRequestCallback);
                    }

                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.error(getClass(), throwable.getMessage());
                }
            });
        } else {
            // rename directly as nothing to save
            if (!environmentName.equals(newEnvironmentName)) {
                projectService.rename(path, newEnvironmentName, null, renameAsyncRequestCallback);
            }

        }

        Map<String, String> variables = new HashMap<>();
        variables.put(CONFIGURATION_TYPE, environment.getType());
        RunnerConfiguration config = dtoFactory.createDto(RunnerConfiguration.class).withRam(environment.getRam()).withVariables(variables);

        runnerConfigs.put(encodedEnvironmentId, config);

        updateProject();

        projectEnvironmentsAction.perform();
    }

    private void updateProject() {
        AsyncRequestCallback<ProjectDescriptor> asyncDescriptorCallback =
                asyncDescriptorCallbackBuilder.success(new SuccessCallback<ProjectDescriptor>() {
                    @Override
                    public void onSuccess(ProjectDescriptor result) {
                        view.setEnableSaveButton(false);
                        view.setEnableCancelButton(false);
                    }
                }).failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        Log.error(getClass(), reason.getMessage());

                    }
                }).build();

        projectService.updateProject(projectDescriptor.getPath(), projectDescriptor, asyncDescriptorCallback);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteButtonClicked() {
        if (PROJECT.equals(environment.getScope())) {
            showDialog();
        }
    }

    private void showDialog() {
        dialogFactory.createConfirmDialog(locale.removeEnvironment(),
                                          locale.removeEnvironmentMessage(environment.getName()),
                                          new ConfirmCallback() {
                                              @Override
                                              public void accepted() {
                                                  removeSelectedEnvironment();
                                              }
                                          }, null).show();
    }

    private void removeSelectedEnvironment() {
        AsyncRequestCallback<Void> asyncRequestCallback = voidAsyncCallbackBuilder
                .success(new SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        String environmentId = environment.getId();

                        runnerConfigs.remove(environmentId);

                        String defaultRunner = currentProject.getRunner();

                        if (defaultRunner != null && defaultRunner.equals(environmentId)) {
                            templatesContainer.setDefaultEnvironment(null);
                            chooseRunnerAction.setEmptyDefaultRunner();
                        }

                        updateProject();

                        projectEnvironmentsAction.perform();

                        notifyListeners(environment);
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        notificationManager.showError(reason.getMessage());
                    }
                })
                .build();

        projectService.delete(environment.getPath(), asyncRequestCallback);
    }

    private void notifyListeners(@Nonnull Environment environment) {
        for (RemovePanelListener listener : listeners) {
            listener.onPanelRemoved(environment);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelButtonClicked() {
        view.incorrectName(false);
        isParameterChanged = false;
        Scope scope = environment.getScope();

        view.setEnableSaveButton(false);
        view.setEnableCancelButton(false);
        view.setEnableDeleteButton(PROJECT.equals(scope));

        if (editor instanceof UndoableEditor) {
            HandlesUndoRedo undoRedo = ((UndoableEditor)editor).getUndoRedo();
            while (editor.isDirty() && undoRedo.undoable()) {
                undoOperations++;
                undoRedo.undo();
            }
        }

        environment.setRam(currentRam);

        view.setName(environment.getName());
        view.selectMemory(RAM.detect(currentRam));
        view.selectScope(environment.getScope());
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull Environment environment) {
        view.setEnableCancelButton(isParameterChanged);
        view.setEnableSaveButton(isParameterChanged);

        Scope scope = environment.getScope();

        view.setEnableDeleteButton(PROJECT.equals(scope));

        String environmentName = environment.getName();
        String environmentId = environment.getId();
        if (!EnvironmentIdValidator.isValid(environmentId)) {
            environmentId = URL.encode(environmentId);
        }

        boolean isConfigExist = runnerConfigs.containsKey(environmentId);

        RAM ram = RAM.detect(isConfigExist && !isParameterChanged ? getRam(environmentId) : environment.getRam());

        if (DEFAULT.getValue() != currentRam && DEFAULT.equals(ram)) {
            view.addRamValue(currentRam);
            view.selectMemory(currentRam);
            this.environment.setRam(currentRam);
        } else {
            this.environment.setRam(ram.getValue());
            view.selectMemory(ram);
        }

        view.selectShutdown(getTimeout());
        view.setName(environmentName);
        String type = getType(environment);
        view.setType(type);
        view.selectScope(scope);

        String defaultRunner = currentProject.getRunner();

        view.changeSwitcherState(environmentId.equals(defaultRunner));
    }

    /** {@inheritDoc} */
    @Override
    public void onSwitcherChanged(boolean isOn) {
        if (isOn) {
            templatesContainer.setDefaultEnvironment(environment);
        } else {
            templatesContainer.setDefaultEnvironment(null);
            chooseRunnerAction.setEmptyDefaultRunner();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(@Nonnull RemovePanelListener listener) {
        listeners.add(listener);
    }
}