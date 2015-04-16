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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.texteditor.UndoableEditor;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.*;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
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
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.models.EnvironmentImpl.ROOT_FOLDER;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
public class PropertiesEnvironmentPanel extends PropertiesPanelPresenter {

    private static final String DOCKER_SCRIPT_NAME    = "/Dockerfile";
    public static final  String ENVIRONMENT_ID_PREFIX = "project://";

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
    private final AsyncCallbackBuilder<Array<ItemReference>> asyncArrayCallbackBuilder;
    private final AsyncCallbackBuilder<Void>                 voidAsyncCallbackBuilder;
    private final AsyncCallbackBuilder<ProjectDescriptor>    asyncDescriptorCallbackBuilder;
    private final DialogFactory                              dialogFactory;
    private final List<RemovePanelListener>                  listeners;
    private final TemplatesContainer                         templatesContainer;

    private ProjectDescriptor                projectDescriptor;
    private Map<String, RunnerConfiguration> runnerConfigs;
    private int                              currentRam;
    private boolean                          isParameterChanged;

    @AssistedInject
    public PropertiesEnvironmentPanel(final PropertiesPanelView view,
                                      DtoFactory dtoFactory,
                                      @Named("DefaultEditorProvider")EditorProvider editorProvider,
                                      @Nonnull final FileTypeRegistry fileTypeRegistry,
                                      final DockerFileFactory dockerFileFactory,
                                      final ProjectServiceClient projectService,
                                      EventBus eventBus,
                                      AppContext appContext,
                                      DialogFactory dialogFactory,
                                      RunnerLocalizationConstant locale,
                                      GetProjectEnvironmentsAction projectEnvironmentsAction,
                                      NotificationManager notificationManager,
                                      DtoUnmarshallerFactory unmarshallerFactory,
                                      AsyncCallbackBuilder<ItemReference> asyncCallbackBuilder,
                                      AsyncCallbackBuilder<Array<ItemReference>> asyncArrayCallbackBuilder,
                                      AsyncCallbackBuilder<Void> voidAsyncCallbackBuilder,
                                      AsyncCallbackBuilder<ProjectDescriptor> asyncDescriptorCallbackBuilder,
                                      TemplatesContainer templatesContainer,
                                      @Assisted @Nonnull final Environment environment) {
        super(view, appContext);
        this.dtoFactory = dtoFactory;
        this.editorProvider = editorProvider;
        this.fileTypeRegistry = fileTypeRegistry;
        this.dockerFileFactory = dockerFileFactory;
        this.projectService = projectService;
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

        this.dialogFactory = dialogFactory;

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

        this.listeners = new ArrayList<>();

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
        boolean isConfigExist = runnerConfigs.containsKey(environmentId);

        return isConfigExist ? runnerConfigs.get(environmentId).getRam() : RAM.DEFAULT.getValue();
    }



    private void getProjectEnvironmentDocker() {
        Unmarshallable<Array<ItemReference>> unmarshaller = unmarshallerFactory.newArrayUnmarshaller(ItemReference.class);

        AsyncRequestCallback<Array<ItemReference>> arrayAsyncCallback =
                asyncArrayCallbackBuilder.unmarshaller(unmarshaller)
                                         .success(new SuccessCallback<Array<ItemReference>>() {
                                             @Override
                                             public void onSuccess(Array<ItemReference> result) {
                                                 for (ItemReference item : result.asIterable()) {
                                                     ProjectNode project = new ProjectNode(null,
                                                                                           projectDescriptor,
                                                                                           null,
                                                                                           eventBus,
                                                                                           projectService,
                                                                                           unmarshallerFactory);

                                                     FileNode file = new EnvironmentScript(project,
                                                                                           item,
                                                                                           currentProject.getCurrentTree(),
                                                                                           eventBus,
                                                                                           projectService,
                                                                                           unmarshallerFactory,
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
        DockerFile file = dockerFileFactory.newInstance(environment.getPath());
        initializeEditor(file, editorProvider, fileTypeRegistry);
    }

    /** {@inheritDoc} */
    @Override
    public void onCopyButtonClicked() {
        // get projects env
        Map<Scope, List<Environment>> envs = templatesContainer.getEnvironments();
        List<Environment> projectEnvs = envs.get(Scope.PROJECT);
        List<String> existingNames = new ArrayList<>();
        if (projectEnvs != null) {
            for (Environment env : projectEnvs) {
                existingNames.add(env.getName());
            }
        }
        // new name is based from existing one
        final String fileName = NameGenerator.generateCopy(environment.getName(), existingNames);


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
        editor.getEditorInput().getFile().getContent(new AsyncCallback<String>() {
            @Override
            public void onSuccess(String content) {
                createFile(content, fileName);
            }

            @Override
            public void onFailure(Throwable throwable) {
                notificationManager.showError(throwable.getMessage());
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
        if (isConfigExist) {
            int ram = getRam(environment.getId());

            String newEnvironmentName = getNewEnvironmentName(result.getPath());

            RunnerConfiguration newConfig = dtoFactory.createDto(RunnerConfiguration.class)
                                                      .withRam(ram);

            runnerConfigs.put(generateEnvironmentId(newEnvironmentName), newConfig);
            environment.setRam(ram);
            view.selectMemory(RAM.detect(ram));
        } else {
            view.selectMemory(RAM.detect(environment.getRam()));
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
        if ("".equals(newName)) {
            return ENVIRONMENT_ID_PREFIX + environmentName;
        }
        return newName;
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigurationChanged() {
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

        final String pathToProject = projectDescriptor.getPath();
        final String path = pathToProject + ROOT_FOLDER + environmentName;

        final AsyncRequestCallback<Void> renameAsyncRequestCallback = voidAsyncCallbackBuilder
                .success(new SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        RunnerConfiguration config = runnerConfigs.get(environmentId);

                        runnerConfigs.remove(environmentId);

                        runnerConfigs.put(generateEnvironmentId(newEnvironmentName), config);

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


        RunnerConfiguration config = dtoFactory.createDto(RunnerConfiguration.class).withRam(environment.getRam());

        runnerConfigs.put(environmentId, config);

        updateProject();
    }

    private void updateProject() {
        AsyncRequestCallback<ProjectDescriptor> asyncDescriptorCallback =
                asyncDescriptorCallbackBuilder.success(new SuccessCallback<ProjectDescriptor>() {
                    @Override
                    public void onSuccess(ProjectDescriptor result) {
                        view.setEnableSaveButton(false);
                        view.setEnableCancelButton(false);

                        projectEnvironmentsAction.perform();
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

                        updateProject();

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

        boolean isConfigExist = runnerConfigs.containsKey(environment.getId());

        RAM ram = RAM.detect(isConfigExist && !isParameterChanged ? getRam(environment.getId()) : environment.getRam());

        this.environment.setRam(ram.getValue());

        view.selectShutdown(getTimeout());
        view.selectMemory(ram);
        view.setName(environmentName);
        view.setType(environment.getType());
        view.selectScope(scope);

        String defaultRunner = currentProject.getRunner();

        view.changeSwitcherState(environment.getId().equals(defaultRunner));
    }

    /** {@inheritDoc} */
    @Override
    public void onSwitcherChanged(boolean isOn) {
        templatesContainer.setDefaultEnvironment(isOn ? environment : null);
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(@Nonnull RemovePanelListener listener) {
        listeners.add(listener);
    }
}
