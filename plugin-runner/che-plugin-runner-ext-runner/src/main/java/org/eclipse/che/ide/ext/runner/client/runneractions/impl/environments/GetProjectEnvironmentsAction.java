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
package org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.actions.ChooseRunnerAction;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.AbstractRunnerAction;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
import org.eclipse.che.ide.ext.runner.client.util.GetEnvironmentsUtil;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;
import java.util.List;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;

/**
 * The class contains business logic to get project environments which are added on templates panel.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@Singleton
public class GetProjectEnvironmentsAction extends AbstractRunnerAction {

    private final RunnerUtil                                            runnerUtil;
    private final Provider<TemplatesContainer>                          templatesPanelProvider;
    private final AppContext                                            appContext;
    private final ProjectServiceClient                                  projectService;
    private final NotificationManager                                   notificationManager;
    private final Provider<AsyncCallbackBuilder<RunnerEnvironmentTree>> callbackBuilderProvider;
    private final RunnerLocalizationConstant                            locale;
    private final ChooseRunnerAction                                    chooseRunnerAction;

    @Inject
    public GetProjectEnvironmentsAction(AppContext appContext,
                                        ProjectServiceClient projectService,
                                        NotificationManager notificationManager,
                                        Provider<AsyncCallbackBuilder<RunnerEnvironmentTree>> callbackBuilderProvider,
                                        RunnerLocalizationConstant locale,
                                        RunnerUtil runnerUtil,
                                        ChooseRunnerAction chooseRunnerAction,
                                        Provider<TemplatesContainer> templatesPanelProvider) {
        this.runnerUtil = runnerUtil;
        this.templatesPanelProvider = templatesPanelProvider;
        this.chooseRunnerAction = chooseRunnerAction;
        this.appContext = appContext;
        this.projectService = projectService;
        this.notificationManager = notificationManager;
        this.callbackBuilderProvider = callbackBuilderProvider;
        this.locale = locale;
    }

    /** {@inheritDoc} */
    @Override
    public void perform() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null || !runnerUtil.hasRunPermission()) {
            return;
        }

        final ProjectDescriptor descriptor = currentProject.getProjectDescription();

        AsyncRequestCallback<RunnerEnvironmentTree> callback = callbackBuilderProvider
                .get()
                .unmarshaller(RunnerEnvironmentTree.class)
                .success(new SuccessCallback<RunnerEnvironmentTree>() {
                    @Override
                    public void onSuccess(RunnerEnvironmentTree result) {
                        TemplatesContainer panel = templatesPanelProvider.get();

                        List<Environment> projectEnvironments = panel.addEnvironments(result, PROJECT);

                        String defaultRunner = currentProject.getRunner();

                        for (Environment environment : projectEnvironments) {
                            if (environment.getId().equals(defaultRunner)) {
                                panel.setDefaultEnvironment(environment);

                                break;
                            }
                        }

                        chooseRunnerAction.addProjectRunners(projectEnvironments);
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        notificationManager.showError(locale.customRunnerGetEnvironmentFailed());
                    }
                })
                .build();

        projectService.getRunnerEnvironments(descriptor.getPath(), callback);
    }
}