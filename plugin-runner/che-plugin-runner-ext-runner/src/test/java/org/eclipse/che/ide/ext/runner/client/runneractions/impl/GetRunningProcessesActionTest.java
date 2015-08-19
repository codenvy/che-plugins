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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.ApplicationStatus;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.app.CurrentUser;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.TestUtil;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.RunnerAction;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.container.TerminalContainer;
import org.eclipse.che.ide.ext.runner.client.util.WebSocketUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import com.google.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(MockitoJUnitRunner.class)
public class GetRunningProcessesActionTest {
    private static final String PATH_TO_PROJECT         = "somePath";
    private static final String PROJECT_NAME            = "projectName";
    private static final String MESSAGE                 = "some tested message";
    private static final String WORKSPACE_ID            = "someId";
    private static final String SOME_USER_ID            = "SOME_USER_ID";
    private static final String PROCESS_STARTED_CHANNEL = "runner:process_started:";
    private static final String CHANNEL                 =
            PROCESS_STARTED_CHANNEL + WORKSPACE_ID + ':' + PATH_TO_PROJECT + ':' + SOME_USER_ID;

    private static List<ApplicationProcessDescriptor> result;

    //mocks for constructor
    @Mock
    private NotificationManager                                                 notificationManager;
    @Mock
    private RunnerServiceClient                                                 service;
    @Mock
    private DtoUnmarshallerFactory                                              dtoUnmarshallerFactory;
    @Mock
    private AppContext                                                          appContext;
    @Mock
    private RunnerLocalizationConstant                                          locale;
    @Mock
    private Provider<AsyncCallbackBuilder<List<ApplicationProcessDescriptor>>> callbackBuilderProvider;
    @Mock
    private WebSocketUtil                                                       webSocketUtil;
    @Mock
    private RunnerActionFactory                                                 actionFactory;
    @Mock
    private RunnerManagerPresenter                                              runnerManagerPresenter;
    @Mock
    private ConsoleContainer                                                    consoleContainer;
    @Mock
    private TerminalContainer                                                   terminalContainer;
    @Mock
    private PropertiesContainer                                                 propertiesContainer;

    //captors
    @Captor
    private ArgumentCaptor<SubscriptionHandler<ApplicationProcessDescriptor>>    handlerArgumentCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<List<ApplicationProcessDescriptor>>> successCallBackCaptor;
    @Captor
    private ArgumentCaptor<Notification>                                         notificationCaptor;
    @Captor
    private ArgumentCaptor<Notification>                                         notificationCaptor2;

    //project variables
    @Mock
    private CurrentProject                                                              project;
    @Mock
    private ProjectDescriptor                                                           projectDescriptor;
    //runner variables
    @Mock
    private Runner                                                                      runner;
    @Mock
    private Runner                                                                      runner1;
    @Mock
    private Runner                                                                      runner2;
    @Mock
    private RunnerAction.StopActionListener                                             listener;
    //another variables
    @Mock
    private AsyncCallbackBuilder<List<ApplicationProcessDescriptor>>                   asyncCallbackBuilder;
    @Mock
    private Unmarshallable<List<ApplicationProcessDescriptor>>                         arrayUnmarshallable;
    @Mock
    private AsyncRequestCallback<List<ApplicationProcessDescriptor>>                   callback;
    @Mock
    private CurrentUser                                                                 currentUser;
    @Mock
    private ProfileDescriptor                                                           profileDescriptor;
    @Mock
    private org.eclipse.che.ide.websocket.rest.Unmarshallable<ApplicationProcessDescriptor> processDescriptorUnmarshallable;
    @Mock
    private ApplicationProcessDescriptor                                                processDescriptor;
    @Mock
    private ApplicationProcessDescriptor                                                processDescriptor1;
    @Mock
    private ApplicationProcessDescriptor                                                processDescriptor2;
    @Mock
    private ApplicationProcessDescriptor                                                processDescriptor3;
    @Mock
    private GetLogsAction                                                               logsAction;

    private GetRunningProcessesAction getRunningProcessesAction;

    @Before
    public void setUp() {
        when(actionFactory.createGetLogs()).thenReturn(logsAction);
        getRunningProcessesAction = new GetRunningProcessesAction(notificationManager,
                                                                  service,
                                                                  dtoUnmarshallerFactory,
                                                                  appContext,
                                                                  locale,
                                                                  callbackBuilderProvider,
                                                                  webSocketUtil,
                                                                  actionFactory,
                                                                  runnerManagerPresenter,
                                                                  propertiesContainer,
                                                                  WORKSPACE_ID);
        when(appContext.getCurrentProject()).thenReturn(project);
        when(appContext.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getProfile()).thenReturn(profileDescriptor);
        when(profileDescriptor.getId()).thenReturn(SOME_USER_ID);
        when(callbackBuilderProvider.get()).thenReturn(asyncCallbackBuilder);
        when(dtoUnmarshallerFactory.newListUnmarshaller(ApplicationProcessDescriptor.class)).thenReturn(arrayUnmarshallable);
        when(asyncCallbackBuilder.unmarshaller(arrayUnmarshallable)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<List<ApplicationProcessDescriptor>>>anyObject()))
                .thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.build()).thenReturn(callback);
        //preparing project data
        when(project.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getPath()).thenReturn(PATH_TO_PROJECT);
        when(projectDescriptor.getName()).thenReturn(PROJECT_NAME);
        when(locale.projectRunningNow(PROJECT_NAME)).thenReturn(MESSAGE);

        when(appContext.getCurrentProject()).thenReturn(project);
        when(dtoUnmarshallerFactory.newWSUnmarshaller(ApplicationProcessDescriptor.class)).thenReturn(processDescriptorUnmarshallable);
        when(processDescriptor.getProcessId()).thenReturn(1234567890L);
        when(runnerManagerPresenter.isRunnerExist(1234567890L)).thenReturn(false);

        result = new ArrayList<>();
        result.add(processDescriptor1);
        result.add(processDescriptor2);
        result.add(processDescriptor3);


        when(processDescriptor1.getStatus()).thenReturn(ApplicationStatus.RUNNING);
        when(processDescriptor2.getStatus()).thenReturn(ApplicationStatus.NEW);
        when(processDescriptor3.getStatus()).thenReturn(ApplicationStatus.CANCELLED);

        when(runnerManagerPresenter.addRunner(processDescriptor1)).thenReturn(runner1);
        when(runnerManagerPresenter.addRunner(processDescriptor2)).thenReturn(runner2);
        when(runnerManagerPresenter.addRunner(processDescriptor)).thenReturn(runner);
    }

    @Test
    public void performShouldNotBeCompletedIfProjectIsNull() {
        reset(logsAction, actionFactory);
        when(appContext.getCurrentProject()).thenReturn(null);

        getRunningProcessesAction.perform();

        verify(appContext).getCurrentProject();
        verifyNoMoreInteractions(notificationManager,
                                 service,
                                 dtoUnmarshallerFactory,
                                 appContext,
                                 locale,
                                 callbackBuilderProvider,
                                 webSocketUtil,
                                 actionFactory,
                                 runnerManagerPresenter);
    }

    @Test
    public void shouldPerformSuccessfullyCheckingAndSuccessPreparingRunnerWhenStatusIsRunning() throws Exception {
        when(processDescriptor.getStatus()).thenReturn(ApplicationStatus.RUNNING);

        getRunningProcessesAction.perform();

        verify(appContext).getCurrentProject();
        verifyCreationChannel();

        verify(webSocketUtil).subscribeHandler(anyString(), handlerArgumentCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = handlerArgumentCaptor.getValue();

        Method method = SubscriptionHandler.class.getDeclaredMethod("onMessageReceived", Object.class);
        method.setAccessible(true);
        method.invoke(processStartedHandler, processDescriptor);

        verify(runnerManagerPresenter).isRunnerExist(1234567890L);
        verify(processDescriptor).getProcessId();
        verify(processDescriptor).getStatus();
        verify(runnerManagerPresenter).addRunner(processDescriptor);
        verify(logsAction).perform(runner);

        verify(locale).projectRunningNow(PROJECT_NAME);
        verify(project, times(3)).getProjectDescription();
        verify(projectDescriptor).getName();

        verifyShowNotification();

        verify(webSocketUtil).subscribeHandler(CHANNEL, processStartedHandler);
    }

    private void verifyShowNotification() {
        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getMessage(), is(MESSAGE));
        assertThat(notification.isImportant(), is(true));
        assertThat(notification.isInfo(), is(true));

        runnerAndApplicationShouldBePrepared();
    }

    @Test
    public void shouldPerformWithSuccessfullyCheckingAndSuccessPreparingRunnerWithRunningAppWhenStatusIsNEW() throws Exception {
        when(processDescriptor.getStatus()).thenReturn(ApplicationStatus.NEW);

        getRunningProcessesAction.perform();

        verify(appContext).getCurrentProject();
        verifyCreationChannel();

        verify(webSocketUtil).subscribeHandler(anyString(), handlerArgumentCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = handlerArgumentCaptor.getValue();

        Method method = SubscriptionHandler.class.getDeclaredMethod("onMessageReceived", Object.class);
        method.setAccessible(true);
        method.invoke(processStartedHandler, processDescriptor);

        verify(runnerManagerPresenter).isRunnerExist(1234567890L);
        verify(processDescriptor).getStatus();
        verify(runnerManagerPresenter).addRunner(processDescriptor);
        verify(logsAction).perform(runner);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getMessage(), is(MESSAGE));
        assertThat(notification.isImportant(), is(true));
        assertThat(notification.isInfo(), is(true));

        verify(webSocketUtil).subscribeHandler(CHANNEL, processStartedHandler);

        runnerAndApplicationShouldBePrepared();
    }

    @Test
    public void shouldPerformWithSuccessfullyCheckingAndSuccessPreparingRunnerWhenStatusIsNotNewOrRunning() throws Exception {
        reset(logsAction);
        when(processDescriptor.getStatus()).thenReturn(null);

        getRunningProcessesAction.perform();

        verify(appContext).getCurrentProject();
        verifyCreationChannel();

        verify(webSocketUtil).subscribeHandler(anyString(), handlerArgumentCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = handlerArgumentCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", processDescriptor);

        verify(runnerManagerPresenter).isRunnerExist(1234567890L);
        verify(processDescriptor).getProcessId();
        verify(processDescriptor).getStatus();
        verifyNoMoreInteractions(runnerManagerPresenter, logsAction, notificationManager, locale);

        verify(webSocketUtil).subscribeHandler(CHANNEL, processStartedHandler);
    }

    @Test
    public void shouldPerformWithSuccessfullyCheckingAndSuccessPreparingRunnerWhenRunnerIsExist() throws Exception {
        reset(logsAction);
        when(runnerManagerPresenter.isRunnerExist(1234567890L)).thenReturn(true);
        when(processDescriptor.getStatus()).thenReturn(ApplicationStatus.RUNNING);

        getRunningProcessesAction.perform();

        verify(appContext).getCurrentProject();
        verifyCreationChannel();

        verify(webSocketUtil).subscribeHandler(anyString(), handlerArgumentCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = handlerArgumentCaptor.getValue();

        TestUtil.invokeMethodByName(processStartedHandler, "onMessageReceived", processDescriptor);

        verify(runnerManagerPresenter).isRunnerExist(1234567890L);
        verify(processDescriptor).getProcessId();
        verifyNoMoreInteractions(runnerManagerPresenter, logsAction, notificationManager, locale);

        verify(webSocketUtil).subscribeHandler(CHANNEL, processStartedHandler);

        runnerAndApplicationShouldBePrepared();
        verify(webSocketUtil).subscribeHandler(CHANNEL, processStartedHandler);
    }

    private void verifyCreationChannel() {
        verify(project, times(2)).getProjectDescription();
        verify(projectDescriptor, times(2)).getPath();
        verify(appContext).getCurrentUser();
        verify(currentUser).getProfile();
        verify(profileDescriptor).getId();
    }

    @Test
    public void runnerProcessShouldNotBeStoppedWhenChannelAndProcessStartedHandlerAndStopListenerAreNull() {
        reset(logsAction);

        getRunningProcessesAction.stop();

        verifyNoMoreInteractions(webSocketUtil, logsAction);
    }

    @Test
    public void runnerProcessShouldNotBeStoppedWhenChannelIsNullAndProcessStartedHandlerIsNullButStopListenerIsNotNull() {
        reset(logsAction);
        getRunningProcessesAction.setListener(listener);

        getRunningProcessesAction.stop();

        verify(listener, never()).onStopAction();
        verifyNoMoreInteractions(webSocketUtil, logsAction);
    }

    @Test
    public void runnerProcessShouldBeStoppedWhenChannelAndProcessStartedHandlerAndStopListenerNotNull() {
        reset(logsAction);
        when(processDescriptor.getStatus()).thenReturn(ApplicationStatus.RUNNING);
        getRunningProcessesAction.setListener(listener);
        getRunningProcessesAction.perform();

        verify(webSocketUtil).subscribeHandler(anyString(), handlerArgumentCaptor.capture());
        SubscriptionHandler<ApplicationProcessDescriptor> processStartedHandler = handlerArgumentCaptor.getValue();

        getRunningProcessesAction.stop();

        verify(webSocketUtil).unSubscribeHandler(CHANNEL, processStartedHandler);

        verify(listener).onStopAction();
    }

    public void runnerAndApplicationShouldBePrepared() {
        reset(notificationManager);

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<List<ApplicationProcessDescriptor>> successCallback = successCallBackCaptor.getValue();
        successCallback.onSuccess(result);

        verify(processDescriptor1).getStatus();
        verify(processDescriptor2).getStatus();
        verify(processDescriptor3).getStatus();

        verify(propertiesContainer).setVisible(true);

        verify(runnerManagerPresenter).addRunner(processDescriptor1);
        verify(runnerManagerPresenter).addRunner(processDescriptor2);
        verify(runnerManagerPresenter, never()).addRunner(processDescriptor3);

        verify(logsAction).perform(runner1);
        verify(logsAction).perform(runner2);

        verify(notificationManager, times(2)).showNotification(notificationCaptor2.capture());
        Notification notification2 = notificationCaptor2.getValue();

        assertThat(notification2.getMessage(), is(MESSAGE));
        assertThat(notification2.isImportant(), is(true));
        assertThat(notification2.isInfo(), is(true));

        verify(service).getRunningProcesses(PATH_TO_PROJECT, callback);
    }

    @Test
    public void runnerAndApplicationShouldNotBePreparedBecauseDescriptorsArrayIsEmpty() {
        result = new ArrayList<>();
        reset(notificationManager,logsAction);

        getRunningProcessesAction.perform();

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<List<ApplicationProcessDescriptor>> successCallback = successCallBackCaptor.getValue();

        successCallback.onSuccess(result);

        verify(service).getRunningProcesses(PATH_TO_PROJECT, callback);
        verifyNoMoreInteractions(service, propertiesContainer, notificationManager, logsAction);
    }
}
