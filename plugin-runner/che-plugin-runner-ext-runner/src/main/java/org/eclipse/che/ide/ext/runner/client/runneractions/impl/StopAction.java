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
package org.eclipse.che.ide.ext.runner.client.runneractions.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.AbstractRunnerAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEvent;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Action for stopping current runner.
 *
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
public class StopAction extends AbstractRunnerAction {
    private final RunnerServiceClient                                          service;
    private final AppContext                                                   appContext;
    private final Provider<AsyncCallbackBuilder<ApplicationProcessDescriptor>> callbackBuilderProvider;
    private final RunnerLocalizationConstant                                   constant;
    private final NotificationManager                                          notificationManager;
    private final RunnerUtil                                                   runnerUtil;
    private final GetLogsAction                                                logsAction;
    private final ConsoleContainer                                             consoleContainer;
    private final AnalyticsEventLogger                                         eventLogger;
    private final EventBus                                                     eventBus;

    private CurrentProject         project;
    private Runner                 runner;
    private RunnerManagerPresenter presenter;
    private Notification           notification;

    @Inject
    public StopAction(RunnerServiceClient service,
                      AppContext appContext,
                      Provider<AsyncCallbackBuilder<ApplicationProcessDescriptor>> callbackBuilderProvider,
                      RunnerLocalizationConstant constant,
                      NotificationManager notificationManager,
                      RunnerUtil runnerUtil,
                      RunnerActionFactory actionFactory,
                      ConsoleContainer consoleContainer,
                      EventBus eventBus,
                      AnalyticsEventLogger eventLogger,
                      RunnerManagerPresenter runnerManagerPresenter) {
        this.service = service;
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.callbackBuilderProvider = callbackBuilderProvider;
        this.constant = constant;
        this.notificationManager = notificationManager;
        this.runnerUtil = runnerUtil;
        this.eventLogger = eventLogger;
        this.logsAction = actionFactory.createGetLogs();
        this.consoleContainer = consoleContainer;

        presenter = runnerManagerPresenter;

        addAction(logsAction);
    }

    /** {@inheritDoc} */
    @Override
    public void perform(@Nonnull final Runner runner) {
        notification = new Notification(constant.messageRunnerShuttingDown(), PROGRESS);

        notificationManager.showNotification(notification);
        eventLogger.log(this);

        this.runner = runner;

        project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        Link stopLink = runner.getStopUrl();
        if (stopLink == null) {
            runnerUtil.showError(runner, constant.applicationFailed(project.getProjectDescription().getName()), null);

            runner.setStatus(Runner.Status.STOPPED);
            presenter.update(runner);
            return;
        }

        AsyncRequestCallback<ApplicationProcessDescriptor> callback = callbackBuilderProvider
                .get()
                .unmarshaller(ApplicationProcessDescriptor.class)
                .success(new SuccessCallback<ApplicationProcessDescriptor>() {
                    @Override
                    public void onSuccess(ApplicationProcessDescriptor result) {
                        processStoppedMessage(result);
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        runner.setStatus(Runner.Status.FAILED);
                        runner.setProcessDescriptor(null);

                        project.setIsRunningEnabled(true);

                        runnerUtil.showError(runner,
                                             constant.applicationFailed(project.getProjectDescription().getName()),
                                             reason);

                        eventBus.fireEvent(new RunnerApplicationStatusEvent(runner));
                    }
                })
                .build();

        service.stop(stopLink, callback);
    }

    private void processStoppedMessage(@Nonnull ApplicationProcessDescriptor descriptor) {
        runner.setProcessDescriptor(descriptor);

        project.setIsRunningEnabled(true);

        String projectName = project.getProjectDescription().getName();
        String message = constant.applicationStopped(projectName);

        Notification.Type notificationType;

        Runner.Status runnerStatus = runner.getStatus();

        if (Runner.Status.RUNNING.equals(runnerStatus) || Runner.Status.DONE.equals(runnerStatus) || Runner.Status.IN_PROGRESS.equals(runnerStatus)) {
            notificationType = INFO;

            runner.setStatus(Runner.Status.STOPPED);
            consoleContainer.printInfo(runner, message);
        } else {
            // this mean that application has failed to start
            notificationType = ERROR;

            runner.setStatus(Runner.Status.FAILED);
            logsAction.perform(runner);
            consoleContainer.printError(runner, message);
        }

        notification.setMessage(message);
        notification.setType(notificationType);
        notification.setStatus(FINISHED);

        notificationManager.showNotification(notification);

        presenter.update(runner);

        eventBus.fireEvent(new RunnerApplicationStatusEvent(runner));
    }

}