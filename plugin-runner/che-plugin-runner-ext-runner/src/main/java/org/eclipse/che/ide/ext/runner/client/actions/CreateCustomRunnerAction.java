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
package org.eclipse.che.ide.ext.runner.client.actions;

import com.google.inject.Inject;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesPresenter;
import org.eclipse.che.ide.ext.runner.client.util.NameGenerator;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPanel;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.ext.runner.client.models.EnvironmentImpl.ROOT_FOLDER;

/**
 * Action which allows user create environment from scratch.
 *
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
public class CreateCustomRunnerAction extends AbstractRunnerActions {
    private static final String DOCKER_SCRIPT_NAME = "/Dockerfile";

    private final ProjectServiceClient                projectService;
    private final AsyncCallbackBuilder<ItemReference> asyncCallbackBuilder;
    private final GetProjectEnvironmentsAction        getProjectEnvironmentsAction;
    private final AppContext                          appContext;
    private final NotificationManager                 notificationManager;
    private final RunnerResources                     resources;
    private final RunnerManagerPresenter              runnerManagerPresenter;
    private final TabContainer                        tabContainer;
    private final RunnerLocalizationConstant          locale;
    private final TemplatesPresenter                  templatesPresenter;

    private CurrentProject currentProject;

    @Inject
    public CreateCustomRunnerAction(RunnerLocalizationConstant locale,
                                    AppContext appContext,
                                    RunnerResources resources,
                                    RunnerManagerPresenter runnerManagerPresenter,
                                    NotificationManager notificationManager,
                                    GetProjectEnvironmentsAction getProjectEnvironmentsAction,
                                    AsyncCallbackBuilder<ItemReference> asyncCallbackBuilder,
                                    ProjectServiceClient projectService,
                                    TemplatesPresenter templatesPresenter,
                                    @LeftPanel TabContainer tabContainer) {
        super(appContext, locale.createCustomRunner(), locale.createCustomRunner(), resources.runWith());

        this.locale = locale;
        this.getProjectEnvironmentsAction = getProjectEnvironmentsAction;
        this.notificationManager = notificationManager;
        this.templatesPresenter = templatesPresenter;
        this.asyncCallbackBuilder = asyncCallbackBuilder;
        this.projectService = projectService;
        this.resources = resources;
        this.appContext = appContext;
        this.runnerManagerPresenter = runnerManagerPresenter;
        this.tabContainer = tabContainer;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        runnerManagerPresenter.setActive();

        tabContainer.showTab(locale.runnerTabTemplates());

        createSimpleEnvironment();
    }

    private void createSimpleEnvironment() {
        currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        final String fileName = NameGenerator.generateCustomEnvironmentName(templatesPresenter.getProjectEnvironments(),
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
                                            getProjectEnvironmentsAction.perform();
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
}