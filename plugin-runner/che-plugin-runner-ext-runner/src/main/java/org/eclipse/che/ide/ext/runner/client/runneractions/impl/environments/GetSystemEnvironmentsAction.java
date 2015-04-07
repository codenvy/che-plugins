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

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
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

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;

/**
 * The class contains business logic to get system environments which are added on templates panel.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class GetSystemEnvironmentsAction extends AbstractRunnerAction {

    private final RunnerUtil                                            runnerUtil;
    private final Provider<TemplatesContainer>                          templatesPanelProvider;
    private final RunnerServiceClient                                   runnerService;
    private final NotificationManager                                   notificationManager;
    private final Provider<AsyncCallbackBuilder<RunnerEnvironmentTree>> callbackBuilderProvider;
    private final RunnerLocalizationConstant                            locale;
    private final GetEnvironmentsUtil                                   environmentUtil;
    private final ChooseRunnerAction                                    chooseRunnerAction;
    private final AppContext                                            appContext;
    private final AnalyticsEventLogger                                  eventLogger;

    private RunnerEnvironmentTree environmentTree;

    @Inject
    public GetSystemEnvironmentsAction(RunnerServiceClient runnerService,
                                       NotificationManager notificationManager,
                                       Provider<AsyncCallbackBuilder<RunnerEnvironmentTree>> callbackBuilderProvider,
                                       RunnerLocalizationConstant locale,
                                       GetEnvironmentsUtil environmentUtil,
                                       RunnerUtil runnerUtil,
                                       ChooseRunnerAction chooseRunnerAction,
                                       AppContext appContext,
                                       Provider<TemplatesContainer> templatesPanelProvider,
                                       AnalyticsEventLogger eventLogger) {
        this.runnerUtil = runnerUtil;
        this.templatesPanelProvider = templatesPanelProvider;
        this.runnerService = runnerService;
        this.notificationManager = notificationManager;
        this.chooseRunnerAction = chooseRunnerAction;
        this.callbackBuilderProvider = callbackBuilderProvider;
        this.locale = locale;
        this.environmentUtil = environmentUtil;
        this.appContext = appContext;
        this.eventLogger = eventLogger;
    }

    /** {@inheritDoc} */
    @Override
    public void perform() {
        if (!runnerUtil.hasRunPermission()) {
            return;
        }

        eventLogger.log(this);

        AsyncRequestCallback<RunnerEnvironmentTree> callback = callbackBuilderProvider
                .get()
                .unmarshaller(RunnerEnvironmentTree.class)
                .success(new SuccessCallback<RunnerEnvironmentTree>() {
                    @Override
                    public void onSuccess(RunnerEnvironmentTree result) {
                        environmentTree = result;

                        getEnvironments(result);
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        notificationManager.showError(locale.customRunnerGetEnvironmentFailed());
                    }
                })
                .build();

        if (environmentTree == null) {
            runnerService.getRunners(callback);
        } else {
            getEnvironments(environmentTree);
        }
    }

    private void getEnvironments(@Nonnull RunnerEnvironmentTree tree) {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return;
        }

        String projectType = currentProject.getProjectDescription().getType();

        List<Environment> environments = environmentUtil.getEnvironmentsByProjectType(tree, projectType, SYSTEM);

        TemplatesContainer container = templatesPanelProvider.get();

        container.addEnvironments(tree, SYSTEM);

        String defaultRunner = currentProject.getRunner();

        chooseRunnerAction.addSystemRunners(environments);

        for (Environment environment : environments) {
            if (environment.getId().equals(defaultRunner)) {
                container.setDefaultEnvironment(environment);

                break;
            }
        }
    }
}