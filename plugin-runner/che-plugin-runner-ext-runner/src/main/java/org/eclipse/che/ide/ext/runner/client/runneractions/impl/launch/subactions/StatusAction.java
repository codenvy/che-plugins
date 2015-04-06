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
package org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.subactions;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerView;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.AbstractRunnerAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.RunnerAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.GetLogsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEvent;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.ext.runner.client.util.WebSocketUtil;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * The action that checks status of a runner and changes it on UI part.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
public class StatusAction extends AbstractRunnerAction {
    /** WebSocket channel to get application's status. */
    private static final String STATUS_CHANNEL = "runner:status:";

    private final DtoUnmarshallerFactory     dtoUnmarshallerFactory;
    private final DtoFactory                 dtoFactory;
    private final WebSocketUtil              webSocketUtil;
    private final AppContext                 appContext;
    private final EventBus                   eventBus;
    private final RunnerLocalizationConstant locale;
    private final RunnerManagerPresenter     presenter;
    private final GetLogsAction              logsAction;
    private final RunnerUtil                 runnerUtil;
    private final RunnerAction               checkHealthStatusAction;
    private final Notification               notification;
    private final RunnerManagerView          view;
    private final ConsoleContainer           consoleContainer;

    private SubscriptionHandler<ApplicationProcessDescriptor> runnerStatusHandler;
    private String                                            webSocketChannel;
    private Runner                                            runner;
    private CurrentProject                                    project;

    @Inject
    public StatusAction(DtoUnmarshallerFactory dtoUnmarshallerFactory,
                        DtoFactory dtoFactory,
                        WebSocketUtil webSocketUtil,
                        AppContext appContext,
                        EventBus eventBus,
                        RunnerLocalizationConstant locale,
                        RunnerManagerPresenter presenter,
                        RunnerUtil runnerUtil,
                        ConsoleContainer consoleContainer,
                        RunnerActionFactory actionFactory,
                        @Nonnull @Assisted Notification notification) {
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.webSocketUtil = webSocketUtil;
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.locale = locale;
        this.presenter = presenter;
        this.view = presenter.getView();
        this.consoleContainer = consoleContainer;
        this.runnerUtil = runnerUtil;
        this.notification = notification;

        this.logsAction = actionFactory.createGetLogs();
        this.checkHealthStatusAction = actionFactory.createCheckHealthStatus(notification);

        addAction(logsAction);
    }

    /** {@inheritDoc} */
    @Override
    public void perform(@Nonnull final Runner runner) {
        this.runner = runner;
        project = appContext.getCurrentProject();
        webSocketChannel = STATUS_CHANNEL + runner.getProcessId();

        runnerStatusHandler = new SubscriptionHandler<ApplicationProcessDescriptor>(
                dtoUnmarshallerFactory.newWSUnmarshaller(ApplicationProcessDescriptor.class)) {
            /** {@inheritDoc} */
            @Override
            protected void onMessageReceived(ApplicationProcessDescriptor descriptor) {
                onApplicationStatusUpdated(descriptor);
            }

            /** {@inheritDoc} */
            @Override
            protected void onErrorReceived(Throwable exception) {
                runner.setStatus(Runner.Status.FAILED);

                showError(exception);

                stop();

                project.setIsRunningEnabled(true);
            }
        };

        webSocketUtil.subscribeHandler(webSocketChannel, runnerStatusHandler);

        notification.setStatus(FINISHED);
    }

    private void showError(Throwable exception) {
        String projectName = project.getProjectDescription().getName();

        if (exception instanceof ServerException && ((ServerException)exception).getHTTPStatus() == 500) {
            ServiceError error = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class);

            runnerUtil.showError(runner,
                                 locale.startApplicationFailed(projectName) + ": " + error.getMessage(),
                                 null,
                                 notification);
        } else {
            runnerUtil.showError(runner, locale.startApplicationFailed(projectName), exception, notification);
        }
    }

    private void onApplicationStatusUpdated(@Nonnull ApplicationProcessDescriptor descriptor) {
        runner.setProcessDescriptor(descriptor);

        switch (descriptor.getStatus()) {
            case RUNNING:
                processRunningMessage();
                break;

            case FAILED:
                processFailedMessage();
                break;

            case STOPPED:
                processStoppedMessage();
                break;

            case CANCELLED:
                processCancelledMessage();
                break;

            case NEW:
                runner.setStatus(Runner.Status.IN_PROGRESS);
                presenter.update(runner);
                break;

            default:
        }

        eventBus.fireEvent(new RunnerApplicationStatusEvent(runner));
    }

    private void processStoppedMessage() {
        runner.setStatus(Runner.Status.STOPPED);

        view.updateMoreInfoPopup(runner);
        view.update(runner);

        project.setIsRunningEnabled(true);

        String projectName = project.getProjectDescription().getName();
        String message = locale.applicationStopped(projectName);
        notification.update(message, INFO, FINISHED, null, true);

        consoleContainer.printInfo(runner, message);

        stop();
    }

    private void processRunningMessage() {
        runner.setStatus(Runner.Status.RUNNING);

        presenter.update(runner);

        checkHealthStatusAction.perform(runner);

        String projectName = project.getProjectDescription().getName();
        String message = locale.applicationStarting(projectName);
        notification.update(message, INFO, FINISHED, null, true);

        consoleContainer.printInfo(runner, message);
    }

    private void processFailedMessage() {
        String projectName = project.getProjectDescription().getName();
        String message = locale.applicationFailed(projectName);

        runnerUtil.showError(runner, message, null, notification);

        project.setIsRunningEnabled(true);

        logsAction.perform(runner);

        stop();
    }

    private void processCancelledMessage() {
        String projectName = project.getProjectDescription().getName();
        String message = locale.applicationCanceled(projectName);

        runnerUtil.showError(runner, message, null, notification);

        project.setIsRunningEnabled(true);

        stop();
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        if (webSocketChannel == null || runnerStatusHandler == null) {
            // It is impossible to perform stop event twice.
            return;
        }

        webSocketUtil.unSubscribeHandler(webSocketChannel, runnerStatusHandler);

        checkHealthStatusAction.stop();
        super.stop();

        webSocketChannel = null;
        runnerStatusHandler = null;
    }

}