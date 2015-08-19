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
import com.google.inject.name.Named;

import org.eclipse.che.api.runner.ApplicationStatus;
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
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.util.WebSocketUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;

import javax.annotation.Nonnull;

import java.util.List;

import static org.eclipse.che.api.runner.ApplicationStatus.NEW;
import static org.eclipse.che.api.runner.ApplicationStatus.RUNNING;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * This action executes a request on the server side for getting runner processes by project name.
 *
 * @author Valeriy Svydenko
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
public class GetRunningProcessesAction extends AbstractRunnerAction {

    private static final String PROCESS_STARTED_CHANNEL = "runner:process_started:";

    private final NotificationManager                                                 notificationManager;
    private final RunnerServiceClient                                                 service;
    private final DtoUnmarshallerFactory                                              dtoUnmarshallerFactory;
    private final AppContext                                                          appContext;
    private final RunnerLocalizationConstant                                          locale;
    private final GetLogsAction                                                       logsAction;
    private final Provider<AsyncCallbackBuilder<List<ApplicationProcessDescriptor>>> callbackBuilderProvider;
    private final WebSocketUtil                                                       webSocketUtil;
    private final RunnerManagerPresenter                                              runnerManagerPresenter;
    private final String                                                              workspaceId;
    private final PropertiesContainer                                                 propertiesContainer;

    private String                                            channel;
    private SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler;
    private CurrentProject                                    project;

    @Inject
    public GetRunningProcessesAction(NotificationManager notificationManager,
                                     RunnerServiceClient service,
                                     DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                     AppContext appContext,
                                     RunnerLocalizationConstant locale,
                                     Provider<AsyncCallbackBuilder<List<ApplicationProcessDescriptor>>> callbackBuilderProvider,
                                     WebSocketUtil webSocketUtil,
                                     RunnerActionFactory actionFactory,
                                     RunnerManagerPresenter runnerManagerPresenter,
                                     PropertiesContainer propertiesContainer,
                                     @Named("workspaceId") String workspaceId) {
        this.notificationManager = notificationManager;
        this.service = service;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
        this.locale = locale;
        this.logsAction = actionFactory.createGetLogs();
        this.callbackBuilderProvider = callbackBuilderProvider;
        this.webSocketUtil = webSocketUtil;
        this.runnerManagerPresenter = runnerManagerPresenter;
        this.propertiesContainer = propertiesContainer;
        this.workspaceId = workspaceId;

        addAction(logsAction);
    }

    /** {@inheritDoc} */
    @Override
    public void perform() {
        project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        startCheckingNewProcesses();

        Unmarshallable<List<ApplicationProcessDescriptor>> unmarshallable = dtoUnmarshallerFactory
                                                                            .newListUnmarshaller(ApplicationProcessDescriptor.class);

        AsyncRequestCallback<List<ApplicationProcessDescriptor>> callback = callbackBuilderProvider
                .get()
                .unmarshaller(unmarshallable)
                .success(new SuccessCallback<List<ApplicationProcessDescriptor>>() {
                    @Override
                    public void onSuccess(List<ApplicationProcessDescriptor> result) {
                        if (result.isEmpty()) {
                            return;
                        }

                        propertiesContainer.setVisible(true);

                        for (ApplicationProcessDescriptor processDescriptor : result) {
                            if (isNewOrRunningProcess(processDescriptor)) {
                                prepareRunnerWithRunningApp(processDescriptor);
                            }
                        }
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        Log.error(GetRunningProcessesAction.class, reason);
                    }
                })
                .build();

        service.getRunningProcesses(project.getProjectDescription().getPath(), callback);
    }

    private boolean isNewOrRunningProcess(@Nonnull ApplicationProcessDescriptor processDescriptor) {
        ApplicationStatus status = processDescriptor.getStatus();
        return status == NEW || status == RUNNING;
    }

    private void startCheckingNewProcesses() {
        processStartedHandler = new SubscriptionHandler<ApplicationProcessDescriptor>(
                dtoUnmarshallerFactory.newWSUnmarshaller(ApplicationProcessDescriptor.class)) {
            @Override
            protected void onMessageReceived(ApplicationProcessDescriptor processDescriptor) {
                if (!runnerManagerPresenter.isRunnerExist(processDescriptor.getProcessId()) && isNewOrRunningProcess(processDescriptor)) {
                    prepareRunnerWithRunningApp(processDescriptor);
                }
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                Log.error(GetRunningProcessesAction.class, exception);
            }
        };

        channel = PROCESS_STARTED_CHANNEL + workspaceId + ':' + project.getProjectDescription().getPath() + ':' +
                  appContext.getCurrentUser().getProfile().getId();
        webSocketUtil.subscribeHandler(channel, processStartedHandler);
    }

    private void prepareRunnerWithRunningApp(@Nonnull ApplicationProcessDescriptor processDescriptor) {
        Runner runner = runnerManagerPresenter.addRunner(processDescriptor);

        logsAction.perform(runner);

        Notification notification = new Notification(locale.projectRunningNow(project.getProjectDescription().getName()), INFO, true);
        notificationManager.showNotification(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        if (channel == null || processStartedHandler == null) {
            return;
        }

        webSocketUtil.unSubscribeHandler(channel, processStartedHandler);

        super.stop();

        channel = null;
        processStartedHandler = null;
    }

}