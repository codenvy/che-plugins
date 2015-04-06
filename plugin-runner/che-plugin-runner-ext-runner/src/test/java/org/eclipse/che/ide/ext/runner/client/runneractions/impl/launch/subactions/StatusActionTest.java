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
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.ApplicationStatus;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.TestUtil;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerView;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.GetLogsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEvent;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.ext.runner.client.util.WebSocketUtil;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import com.google.gwt.http.client.Response;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class StatusActionTest {
    private static final String                   STATUS_CHANNEL       = "runner:status:";
    private static final long                     PROCESS_ID           = 1234567L;
    private static final String                   WEB_SOCKET_CHANNEL   = STATUS_CHANNEL + PROCESS_ID;
    private static final String                   PROJECT_NAME         = "SomeProject";
    private static final String                   MESSAGE              = "some message";
    private static final String                   SERVER_ERROR_MESSAGE = "server internal error";
    private static final String                   FailedMessage        = "{\"message\": \"some error\"}";
    private static final IllegalArgumentException simpleException      = new IllegalArgumentException();

    //variables for constructor
    @Mock
    private DtoUnmarshallerFactory     dtoUnmarshallerFactory;
    @Mock
    private DtoFactory                 dtoFactory;
    @Mock
    private WebSocketUtil              webSocketUtil;
    @Mock
    private AppContext                 appContext;
    @Mock
    private EventBus                   eventBus;
    @Mock
    private RunnerLocalizationConstant locale;
    @Mock
    private RunnerManagerPresenter     presenter;
    @Mock
    private RunnerUtil                 runnerUtil;
    @Mock
    private ConsoleContainer    consoleContainer;
    @Mock
    private RunnerManagerView   view;
    @Mock
    private RunnerActionFactory actionFactory;
    @Mock
    private Notification        notification;

    @Mock
    private ServerException                                                   serverException;
    @Mock
    private Response                                                          response;
    @Mock
    private ServiceError                                                      error;
    @Mock
    private GetLogsAction                                                     logsAction;
    @Mock
    private CheckHealthStatusAction                                           checkHealthStatusAction;
    @Mock
    private Runner                                                            runner;
    @Mock
    private CurrentProject                                                    project;
    @Mock
    private ProjectDescriptor                                                 projectDescriptor;
    @Mock
    private ApplicationProcessDescriptor                                      descriptor;
    //captors
    @Captor
    private ArgumentCaptor<SubscriptionHandler<ApplicationProcessDescriptor>> subscriptionHandlerCaptor;
    @Captor
    private ArgumentCaptor<RunnerApplicationStatusEvent>                      runnerApplicationStatusEventCaptor;

    private StatusAction statusAction;

    @Before
    public void setUp() {
        when(actionFactory.createGetLogs()).thenReturn(logsAction);
        when(actionFactory.createCheckHealthStatus(notification)).thenReturn(checkHealthStatusAction);
        when(presenter.getView()).thenReturn(view);

        statusAction = new StatusAction(dtoUnmarshallerFactory,
                                        dtoFactory,
                                        webSocketUtil,
                                        appContext,
                                        eventBus,
                                        locale,
                                        presenter,
                                        runnerUtil,
                                        consoleContainer,
                                        actionFactory,
                                        notification);

        when(project.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getName()).thenReturn(PROJECT_NAME);
        when(appContext.getCurrentProject()).thenReturn(project);
        when(runner.getProcessId()).thenReturn(PROCESS_ID);
        when(locale.applicationStarting(PROJECT_NAME)).thenReturn(MESSAGE);

        when(locale.applicationCanceled(PROJECT_NAME)).thenReturn(MESSAGE);
    }

    @Test
    public void shouldCheckPerformActionBeforeSendCallBackOnServer() {
        statusAction.perform(runner);

        verify(appContext).getCurrentProject();
        verify(runner).getProcessId();
    }

    @Test
    public void shouldCheckPerformActionAfterSendCallBackOnServer() throws Exception {
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.RUNNING);

        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        verify(webSocketUtil).subscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(notification).setStatus(Notification.Status.FINISHED);
    }

    @Test
    public void shouldOnPerformWithStatusRunning() throws Exception {
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.RUNNING);

        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setProcessDescriptor(descriptor);

        verify(descriptor).getStatus();

        verify(runner).setStatus(Runner.Status.RUNNING);

        verify(presenter).update(runner);

        verify(checkHealthStatusAction).perform(runner);

        verify(project).getProjectDescription();
        verify(projectDescriptor).getName();
        verify(locale).applicationStarting(PROJECT_NAME);

        verify(notification).update(MESSAGE, Notification.Type.INFO, Notification.Status.FINISHED, null, true);

        verify(consoleContainer).printInfo(runner, MESSAGE);

        verify(eventBus).fireEvent(any(RunnerApplicationStatusEvent.class));
    }

    @Test
    public void shouldOnPerformWithStatusFailed() throws Exception {
        when(locale.applicationFailed(PROJECT_NAME)).thenReturn(FailedMessage);
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.FAILED);

        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setProcessDescriptor(descriptor);

        verify(descriptor).getStatus();

        verify(runnerUtil).showError(runner, FailedMessage, null, notification);

        verify(logsAction).perform(runner);
        verify(project).setIsRunningEnabled(true);

        verify(project).getProjectDescription();
        verify(projectDescriptor).getName();
        verify(locale).applicationFailed(PROJECT_NAME);

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(eventBus).fireEvent(any(RunnerApplicationStatusEvent.class));
    }

    @Test
    public void shouldOnPerformWithStatusStopped() throws Exception {
        when(locale.applicationStopped(PROJECT_NAME)).thenReturn(FailedMessage);
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.STOPPED);

        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setProcessDescriptor(descriptor);

        verify(descriptor).getStatus();

        verify(runner).setStatus(Runner.Status.STOPPED);
        verify(view).updateMoreInfoPopup(runner);
        verify(view).update(runner);

        verify(project).setIsRunningEnabled(true);

        verify(project).getProjectDescription();
        verify(projectDescriptor).getName();
        verify(locale).applicationStopped(PROJECT_NAME);

        verify(notification).update(FailedMessage, Notification.Type.INFO, Notification.Status.FINISHED, null, true);

        verify(consoleContainer).printInfo(runner, FailedMessage);

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(eventBus).fireEvent(any(RunnerApplicationStatusEvent.class));
    }

    @Test
    public void shouldOnPerformWithStatusCancelled() throws Exception {
        when(locale.applicationFailed(PROJECT_NAME)).thenReturn(FailedMessage);
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.CANCELLED);

        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setProcessDescriptor(descriptor);

        verify(descriptor).getStatus();

        verify(runnerUtil).showError(runner, MESSAGE, null, notification);

        verify(project).setIsRunningEnabled(true);

        verify(project).getProjectDescription();
        verify(projectDescriptor).getName();
        verify(locale).applicationCanceled(PROJECT_NAME);

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(eventBus).fireEvent(any(RunnerApplicationStatusEvent.class));
    }

    @Test
    public void shouldOnPerformWithStatusNEW() throws Exception {
        when(locale.applicationFailed(PROJECT_NAME)).thenReturn(FailedMessage);
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.NEW);

        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setProcessDescriptor(descriptor);

        verify(descriptor).getStatus();

        verify(runner).setStatus(Runner.Status.IN_PROGRESS);
        verify(presenter).update(runner);

        verify(eventBus).fireEvent(any(RunnerApplicationStatusEvent.class));
    }

    @Test
    public void shouldOnPerformWithErrorReceivedAndServerExceptionWithStatus500() throws Exception {
        when(dtoFactory.createDtoFromJson(SERVER_ERROR_MESSAGE, ServiceError.class)).thenReturn(error);
        when(locale.startApplicationFailed(PROJECT_NAME)).thenReturn(MESSAGE);
        when(error.getMessage()).thenReturn(SERVER_ERROR_MESSAGE);
        when(serverException.getMessage()).thenReturn(SERVER_ERROR_MESSAGE);
        when(serverException.getHTTPStatus()).thenReturn(500);

        statusAction.perform(runner);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onErrorReceived", Throwable.class, serverException);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setStatus(Runner.Status.FAILED);

        verify(dtoFactory).createDtoFromJson(serverException.getMessage(), ServiceError.class);

        verify(locale).startApplicationFailed(PROJECT_NAME);
        verify(runnerUtil).showError(runner,
                                     MESSAGE + ": " + SERVER_ERROR_MESSAGE,
                                     null,
                                     notification);

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(project).setIsRunningEnabled(true);
    }

    @Test
    public void shouldOnPerformWithErrorReceivedAndServerExceptionWithStatus503() throws Exception {
        when(dtoFactory.createDtoFromJson(SERVER_ERROR_MESSAGE, ServiceError.class)).thenReturn(error);
        when(locale.startApplicationFailed(PROJECT_NAME)).thenReturn(MESSAGE);
        when(error.getMessage()).thenReturn(SERVER_ERROR_MESSAGE);
        when(serverException.getHTTPStatus()).thenReturn(503);

        statusAction.perform(runner);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onErrorReceived", Throwable.class, serverException);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setStatus(Runner.Status.FAILED);

        verify(locale).startApplicationFailed(PROJECT_NAME);
        verify(runnerUtil).showError(runner,
                                     MESSAGE,
                                     serverException,
                                     notification);

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(project).setIsRunningEnabled(true);
    }

    @Test
    public void shouldOnPerformWithErrorReceivedAndNotServerException() throws Exception {
        when(dtoFactory.createDtoFromJson(SERVER_ERROR_MESSAGE, ServiceError.class)).thenReturn(error);
        when(locale.startApplicationFailed(PROJECT_NAME)).thenReturn(MESSAGE);
        when(error.getMessage()).thenReturn(SERVER_ERROR_MESSAGE);

        statusAction.perform(runner);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onErrorReceived", Throwable.class, simpleException);

        verify(dtoUnmarshallerFactory).newWSUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runner).setStatus(Runner.Status.FAILED);

        verify(locale).startApplicationFailed(PROJECT_NAME);
        verify(runnerUtil).showError(runner,
                                     MESSAGE,
                                     simpleException,
                                     notification);

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(project).setIsRunningEnabled(true);
    }

    @Test
    public void shouldStopWhenRunnerStatusHandlerAndWebSocketChannelAreNull() {
        reset(logsAction);
        statusAction.stop();
        verifyNoMoreInteractions(webSocketUtil, checkHealthStatusAction, logsAction);
    }

    @Test
    public void shouldStopWhenRunnerStatusHandlerAndWebSocketChannelNotNull() throws Exception {
        when(locale.applicationFailed(PROJECT_NAME)).thenReturn(MESSAGE);
        when(descriptor.getStatus()).thenReturn(ApplicationStatus.NEW);

        //launch this method for initialize webSocketChannel and runnerStatusHandler
        statusAction.perform(runner);

        verify(webSocketUtil).subscribeHandler(eq(WEB_SOCKET_CHANNEL), subscriptionHandlerCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = subscriptionHandlerCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", descriptor);

        reset(logsAction);
        statusAction.stop();

        verify(webSocketUtil).unSubscribeHandler(WEB_SOCKET_CHANNEL, processStartedHandler);
        verify(checkHealthStatusAction).stop();

        //action for super.stop
        verify(logsAction).removeListener();
        verify(logsAction).stop();

        verify(eventBus).fireEvent(any(RunnerApplicationStatusEvent.class));
    }
}