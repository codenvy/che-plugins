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
package org.eclipse.che.ide.ext.runner.client.tabs.templates;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.actions.ChooseRunnerAction;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerView;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetSystemEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.state.PanelState;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget.FilterWidget;
import org.eclipse.che.ide.ext.runner.client.util.GetEnvironmentsUtil;
import org.eclipse.che.ide.ext.runner.client.util.NameGenerator;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.models.EnvironmentImpl.ROOT_FOLDER;
import static org.eclipse.che.ide.ext.runner.client.state.State.RUNNERS;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;

/**
 * The class contains business logic to change displaying of environments depending on scope or type.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@Singleton
public class TemplatesPresenter implements TemplatesContainer, FilterWidget.ActionDelegate, TemplatesView.ActionDelegate {
    private static final String DOCKER_SCRIPT_NAME = "/Dockerfile";

    private final TemplatesView                           view;
    private final FilterWidget                            filter;
    private final EnvironmentWidget                       defaultEnvWidget;
    private final SelectionManager                        selectionManager;
    private final GetProjectEnvironmentsAction            projectEnvironmentsAction;
    private final GetSystemEnvironmentsAction             systemEnvironmentsAction;
    private final GetEnvironmentsUtil                     environmentUtil;
    private final RunnerResources                         resources;
    private final List<Environment>                       systemEnvironments;
    private final List<Environment>                       projectEnvironments;
    private final Map<Scope, List<Environment>>           environmentMap;
    private final PropertiesContainer                     propertiesContainer;
    private final AsyncCallbackBuilder<ItemReference>     asyncCallbackBuilder;
    private final AppContext                              appContext;
    private final ProjectServiceClient                    projectService;
    private final AsyncCallbackBuilder<ProjectDescriptor> asyncDescriptorCallbackBuilder;
    private final ChooseRunnerAction                      chooseRunnerAction;
    private final RunnerManagerView                       runnerManagerView;
    private final RunnerUtil                              runnerUtil;
    private final PanelState                              panelState;
    private final NotificationManager                     notificationManager;

    private RunnerEnvironmentTree tree;
    private Environment           defaultEnvironment;
    private Environment           selectedEnvironment;
    private List<Environment>     previousProjectEnvironments;

    private CurrentProject currentProject;

    @Inject
    public TemplatesPresenter(TemplatesView view,
                              FilterWidget filter,
                              EnvironmentWidget defaultEnvWidget,
                              AppContext appContext,
                              GetProjectEnvironmentsAction projectEnvironmentsAction,
                              GetSystemEnvironmentsAction systemEnvironmentsAction,
                              GetEnvironmentsUtil environmentUtil,
                              PropertiesContainer propertiesContainer,
                              SelectionManager selectionManager,
                              AsyncCallbackBuilder<ItemReference> asyncCallbackBuilder,
                              RunnerManagerView runnerManagerView,
                              NotificationManager notificationManager,
                              RunnerUtil runnerUtil,
                              RunnerResources resources,
                              PanelState panelState,
                              ProjectServiceClient projectService,
                              AsyncCallbackBuilder<ProjectDescriptor> asyncDescriptorCallbackBuilder,
                              ChooseRunnerAction chooseRunnerAction) {
        this.filter = filter;
        this.selectionManager = selectionManager;
        this.filter.setDelegate(this);

        this.view = view;
        this.view.setDelegate(this);
        this.view.setFilterWidget(filter);

        this.defaultEnvWidget = defaultEnvWidget;

        this.projectEnvironmentsAction = projectEnvironmentsAction;
        this.systemEnvironmentsAction = systemEnvironmentsAction;
        this.environmentUtil = environmentUtil;
        this.propertiesContainer = propertiesContainer;
        this.appContext = appContext;
        this.asyncCallbackBuilder = asyncCallbackBuilder;
        this.notificationManager = notificationManager;
        this.runnerManagerView = runnerManagerView;
        this.runnerUtil = runnerUtil;
        this.panelState = panelState;
        this.projectService = projectService;
        this.asyncDescriptorCallbackBuilder = asyncDescriptorCallbackBuilder;
        this.chooseRunnerAction = chooseRunnerAction;
        this.resources = resources;

        this.projectEnvironments = new ArrayList<>();
        this.systemEnvironments = new ArrayList<>();
        this.previousProjectEnvironments = new ArrayList<>();

        this.environmentMap = new EnumMap<>(Scope.class);
        this.environmentMap.put(PROJECT, projectEnvironments);
        this.environmentMap.put(SYSTEM, systemEnvironments);
    }

    /** {@inheritDoc} */
    @Override
    public void select(@Nullable Environment environment) {
        this.selectedEnvironment = environment;
        propertiesContainer.show(environment);
        view.selectEnvironment(environment);
    }

    /** {@inheritDoc} */
    @Override
    public List<Environment> addEnvironments(@Nonnull RunnerEnvironmentTree tree, @Nonnull Scope scope) {
        ProjectDescriptor descriptor = getCurrentProject().getProjectDescription();

        List<Environment> list;

        if (SYSTEM.equals(scope)) {
            list = systemEnvironments;
            this.tree = tree;
        } else {
            list = projectEnvironments;
        }

        List<Environment> environments = environmentUtil.getEnvironmentsByProjectType(tree, descriptor.getType(), scope);
        addEnvironments(list, environments, scope);

        return environments;
    }

    @Nonnull
    private CurrentProject getCurrentProject() {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            throw new IllegalStateException("Current project is null");
        }

        return currentProject;
    }

    private void addEnvironments(@Nonnull List<Environment> sourceList,
                                 @Nonnull List<Environment> targetList,
                                 @Nonnull Scope scope) {
        sourceList.clear();
        sourceList.addAll(targetList);

        environmentMap.put(scope, sourceList);
        view.addEnvironment(environmentMap);

        if (PROJECT.equals(scope) && previousProjectEnvironments.size() < sourceList.size()) {
            selectNewProjectEnvironment(targetList);
        } else {
            selectPreviousOrFirstEnvironment();
        }

        if (PROJECT.equals(scope)) {
            previousProjectEnvironments.clear();
            previousProjectEnvironments.addAll(targetList);
        }

        if (!(RUNNERS).equals(panelState.getState())) {
            changeEnableStateRunButton();
        }
    }

    private void selectNewProjectEnvironment(@Nonnull List<Environment> currentProjectEnvironments) {
        for (Environment environment : currentProjectEnvironments) {
            if (!previousProjectEnvironments.contains(environment)) {
                select(environment);
                selectionManager.setEnvironment(environment);
            }
        }
    }

    /**
     * @return environments
     */
    public Map<Scope, List<Environment>> getEnvironments() {
        return new HashMap<>(environmentMap);
    }

    private void selectPreviousOrFirstEnvironment() {
        propertiesContainer.setVisible(true);
        Environment environment = null;

        // try to select the previous element if it still exists
        if (this.selectedEnvironment != null) {
            for (Map.Entry<Scope, List<Environment>> entry : environmentMap.entrySet()) {
                List<Environment> value = entry.getValue();
                for (Environment env : value) {
                    if (env.getName().equals(selectedEnvironment.getName()) && env.getScope().equals(selectedEnvironment.getScope())) {
                        environment = env;
                        break;
                    }
                }
            }
        }

        // else we take the first element
        if (environment == null) {
            for (Map.Entry<Scope, List<Environment>> entry : environmentMap.entrySet()) {
                List<Environment> value = entry.getValue();
                if (!value.isEmpty()) {
                    environment = value.get(0);
                    break;
                }
            }
        }

        select(environment);
        selectionManager.setEnvironment(environment);
    }

    /** {@inheritDoc} */
    @Override
    public void showEnvironments() {
        view.setDefaultProjectWidget(null);

        view.clearEnvironmentsPanel();
        systemEnvironments.clear();

        projectEnvironmentsAction.perform();

        filter.setMatchesProjectType(true);

        selectEnvironment();
    }

    /** {@inheritDoc} */
    @Override
    public void selectEnvironment() {
        Environment selectedEnvironment = selectionManager.getEnvironment();
        if (selectedEnvironment == null) {
            selectPreviousOrFirstEnvironment();
        } else {
            selectionManager.setEnvironment(selectedEnvironment);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onValueChanged() {
        view.clearEnvironmentsPanel();

        if (filter.getMatchesProjectType()) {
            projectEnvironmentsAction.perform();
            systemEnvironmentsAction.perform();
        } else {
            performProjectEnvironments();
            performSystemEnvironments();
        }
    }

    private void performProjectEnvironments() {
        projectEnvironments.clear();
        systemEnvironments.clear();

        projectEnvironmentsAction.perform();
    }

    private void performSystemEnvironments() {
        systemEnvironments.clear();
        List<RunnerEnvironmentLeaf> leaves = environmentUtil.getAllEnvironments(tree);
        List<Environment> environments = environmentUtil.getEnvironmentsFromNodes(leaves, SYSTEM);

        addEnvironments(systemEnvironments, environments, SYSTEM);
    }

    /** {@inheritDoc} */
    @Override
    public void changeEnableStateRunButton() {
        if (!runnerUtil.hasRunPermission()) {
            return;
        }

        List<Environment> projectEnvironments = environmentMap.get(PROJECT);
        List<Environment> systemEnvironments = environmentMap.get(SYSTEM);

        boolean runButtonIsEnable = !projectEnvironments.isEmpty() || !systemEnvironments.isEmpty();

        runnerManagerView.setEnableRunButton(runButtonIsEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultEnvironment(@Nullable Environment environment) {
        this.defaultEnvironment = environment;

        CurrentProject currentProject = getCurrentProject();

        ProjectDescriptor descriptor = currentProject.getProjectDescription();

        if (environment == null) {
            descriptor.getRunners().setDefault(null);

            updateProject(descriptor, null);
            return;
        }

        String defaultRunner = currentProject.getRunner();

        String environmentId = environment.getId();

        if (!environmentId.equals(defaultRunner)) {
            descriptor.getRunners().setDefault(environmentId);

            updateProject(descriptor, defaultEnvWidget);

            defaultEnvWidget.update(environment);
            return;
        }

        defaultEnvWidget.update(environment);
        view.setDefaultProjectWidget(defaultEnvWidget);
    }

    private void updateProject(@Nonnull ProjectDescriptor descriptor, @Nullable final EnvironmentWidget environmentWidget) {
        AsyncRequestCallback<ProjectDescriptor> asyncDescriptorCallback =
                asyncDescriptorCallbackBuilder.success(new SuccessCallback<ProjectDescriptor>() {
                    @Override
                    public void onSuccess(ProjectDescriptor result) {
                        view.setDefaultProjectWidget(environmentWidget);
                        chooseRunnerAction.selectDefaultRunner();
                    }
                }).failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        Log.error(getClass(), reason.getMessage());

                    }
                }).build();

        projectService.updateProject(descriptor.getPath(), descriptor, asyncDescriptorCallback);
    }

    /** {@inheritDoc} */
    @Override
    public void onDefaultRunnerMouseOver() {
        if (defaultEnvironment != null) {
            view.showDefaultEnvironmentInfo(defaultEnvironment);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void createNewEnvironment() {
        createSimpleEnvironment();
    }

    private void createSimpleEnvironment() {
        currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        final String fileName = NameGenerator.generateCustomEnvironmentName(environmentMap.get(PROJECT),
                                                                            currentProject.getProjectDescription().getName());
        String path = currentProject.getProjectDescription().getPath() + ROOT_FOLDER + fileName;

        AsyncRequestCallback<ItemReference> callback = asyncCallbackBuilder.unmarshaller(ItemReference.class)
                                                                           .success(new SuccessCallback<ItemReference>() {
                                                                               @Override
                                                                               public void onSuccess(ItemReference result) {
                                                                                   createFile(resources.dockerTemplate().getText(),
                                                                                              fileName);
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

    private void createFile(@Nonnull String content, @Nonnull String fileName) {
        String path = currentProject.getProjectDescription().getPath() + ROOT_FOLDER;

        AsyncRequestCallback<ItemReference> callback =
                asyncCallbackBuilder.unmarshaller(ItemReference.class)
                                    .success(new SuccessCallback<ItemReference>() {
                                        @Override
                                        public void onSuccess(ItemReference result) {
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

    /** {@inheritDoc} */
    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }
}