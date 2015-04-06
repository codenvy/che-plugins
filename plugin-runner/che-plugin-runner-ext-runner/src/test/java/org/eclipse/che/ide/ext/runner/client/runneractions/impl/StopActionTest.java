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

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
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
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerView;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.LaunchAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEvent;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class StopActionTest {
    private static final String PATH_TO_PROJECT = "somePath";
    private static final String PROJECT_NAME    = "projectName";
    private static final String MESSAGE         = "some tested message";

    //constructor variables
    @Mock
    private RunnerServiceClient                                          service;
    @Mock
    private AppContext                                                   appContext;
    @Mock
    private Provider<AsyncCallbackBuilder<ApplicationProcessDescriptor>> callbackBuilderProvider;
    @Mock
    private RunnerLocalizationConstant                                   constant;
    @Mock
    private NotificationManager                                          notificationManager;
    @Mock
    private RunnerUtil                                                   runnerUtil;
    @Mock
    private RunnerActionFactory                                          actionFactory;
    @Mock
    private RunnerManagerPresenter                                       presenter;
    @Mock
    private ConsoleContainer                                             consoleContainer;
    @Mock
    private AnalyticsEventLogger                                         eventLogger;
    @Mock
    private EventBus                                                     eventBus;

    //action variables
    @Mock
    private GetLogsAction                                                 logsAction;
    @Mock
    private LaunchAction                                                  launchAction;
    //project variables
    @Mock
    private CurrentProject                                                project;
    @Mock
    private ProjectDescriptor                                             projectDescriptor;
    @Mock
    private Link                                                          stopLink;
    //runner variables
    @Mock
    private Runner                                                        runner;
    @Mock
    private RunnerManagerView                                             view;
    @Mock
    private RunOptions                                                    runOptions;
    //callbacks for server
    @Mock
    private AsyncCallbackBuilder<ApplicationProcessDescriptor>            asyncCallbackBuilder;
    @Mock
    private AsyncRequestCallback<ApplicationProcessDescriptor>            callback;
    @Mock
    private Throwable                                                     reason;
    @Mock
    private ApplicationProcessDescriptor                                  descriptor;
    //captors
    @Captor
    private ArgumentCaptor<FailureCallback>                               failedCallBackCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<ApplicationProcessDescriptor>> successCallBackCaptor;
    @Captor
    private ArgumentCaptor<Notification>                                  notificationCaptor;

    private StopAction stopAction;

    @Before
    public void setUp() {
        when(actionFactory.createGetLogs()).thenReturn(logsAction);
        when(presenter.getView()).thenReturn(view);
        when(actionFactory.createLaunch()).thenReturn(launchAction);

        stopAction = new StopAction(service,
                                    appContext,
                                    callbackBuilderProvider,
                                    constant,
                                    notificationManager,
                                    runnerUtil,
                                    actionFactory,
                                    consoleContainer,
                                    eventBus,
                                    eventLogger,
                                    presenter);

        when(appContext.getCurrentProject()).thenReturn(project);
        when(runner.getStopUrl()).thenReturn(stopLink);
        when(callbackBuilderProvider.get()).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.unmarshaller(ApplicationProcessDescriptor.class)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<ApplicationProcessDescriptor>>anyObject()))
                .thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.build()).thenReturn(callback);
        //preparing project data
        when(project.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getPath()).thenReturn(PATH_TO_PROJECT);
        when(projectDescriptor.getName()).thenReturn(PROJECT_NAME);
        when(constant.applicationStopped(PROJECT_NAME)).thenReturn(MESSAGE);
    }

    @Test
    public void shouldPerformWhenCurrentProjectIsNull() {
        reset(launchAction, actionFactory, presenter);
        when(appContext.getCurrentProject()).thenReturn(null);

        stopAction.perform(runner);

        verify(eventLogger).log(stopAction);
        verify(appContext).getCurrentProject();
        verifyNoMoreInteractions(service,
                                 appContext,
                                 callbackBuilderProvider,
                                 runnerUtil,
                                 actionFactory,
                                 presenter,
                                 runner);
    }

    @Test
    public void shouldFailedPerformWhenStopLinkIsNull() {
        reset(launchAction, actionFactory, presenter);
        when(runner.getStopUrl()).thenReturn(null);
        when(constant.applicationFailed(PROJECT_NAME)).thenReturn(MESSAGE);

        stopAction.perform(runner);

        verify(eventLogger).log(stopAction);
        verify(runner).getStopUrl();
        verify(runnerUtil).showError(runner, MESSAGE, null);
        verify(runner).setStatus(Runner.Status.STOPPED);
        verify(presenter).update(runner);
        verifyNoMoreInteractions(callbackBuilderProvider);
        verify(service, never()).stop(any(Link.class), Matchers.<AsyncRequestCallback<ApplicationProcessDescriptor>>anyObject());
    }

    @Test
    public void shouldFailedPerform() {
        when(constant.applicationFailed(PROJECT_NAME)).thenReturn(MESSAGE);

        when(runner.getOptions()).thenReturn(runOptions);

        stopAction.perform(runner);

        verify(eventLogger).log(stopAction);
        verify(runner).getStopUrl();

        verify(asyncCallbackBuilder).failure(failedCallBackCaptor.capture());
        FailureCallback failureCallback = failedCallBackCaptor.getValue();
        failureCallback.onFailure(reason);

        verify(runner).setStatus(Runner.Status.FAILED);
        verify(runner).setProcessDescriptor(null);
        verify(project).setIsRunningEnabled(true);
        verify(runnerUtil).showError(runner, MESSAGE, reason);

        verify(eventBus).fireEvent(Matchers.<RunnerApplicationStatusEvent>any());

        verify(service).stop(stopLink, callback);
    }

    @Test
    public void shouldSuccessPerformWithStatusRunning() {
        //set status running
        when(runner.getStatus()).thenReturn(Runner.Status.RUNNING);

        when(runner.getOptions()).thenReturn(runOptions);

        stopAction.perform(runner);

        verify(eventLogger).log(stopAction);
        verify(runner).getStopUrl();

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<ApplicationProcessDescriptor> successCallback = successCallBackCaptor.getValue();
        successCallback.onSuccess(descriptor);

        verify(runner).setProcessDescriptor(descriptor);
        verify(project).setIsRunningEnabled(true);

        verify(runner).getStatus();
        verify(runner).setStatus(Runner.Status.STOPPED);
        verify(consoleContainer).printInfo(runner, MESSAGE);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getMessage(), is(MESSAGE));
        assertThat(notification.getType(), is(Notification.Type.INFO));

        verify(presenter).update(runner);

        verify(service).stop(stopLink, callback);
        verify(eventBus).fireEvent(Matchers.<RunnerApplicationStatusEvent>any());
    }

    @Test
    public void shouldSuccessPerformWithStatusDone() {
        //set status done
        when(runner.getStatus()).thenReturn(Runner.Status.DONE);

        when(runner.getOptions()).thenReturn(runOptions);

        stopAction.perform(runner);

        verify(eventLogger).log(stopAction);
        verify(runner).getStopUrl();

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<ApplicationProcessDescriptor> successCallback = successCallBackCaptor.getValue();
        successCallback.onSuccess(descriptor);

        verify(runner).setProcessDescriptor(descriptor);
        verify(project).setIsRunningEnabled(true);

        verify(runner).getStatus();
        verify(runner).setStatus(Runner.Status.STOPPED);
        verify(consoleContainer).printInfo(runner, MESSAGE);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getMessage(), is(MESSAGE));
        assertThat(notification.getType(), is(Notification.Type.INFO));

        verify(presenter).update(runner);

        verify(service).stop(stopLink, callback);
        verify(eventBus).fireEvent(Matchers.<RunnerApplicationStatusEvent>any());
    }

    @Test
    public void shouldSuccessPerformWithStatusNotRunningOrDone() {
        //set status not running or done
        when(runner.getStatus()).thenReturn(Runner.Status.TIMEOUT);

        when(runner.getOptions()).thenReturn(runOptions);

        stopAction.perform(runner);

        verify(eventLogger).log(stopAction);
        verify(runner).getStopUrl();

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<ApplicationProcessDescriptor> successCallback = successCallBackCaptor.getValue();
        successCallback.onSuccess(descriptor);

        verify(runner).setProcessDescriptor(descriptor);
        verify(project).setIsRunningEnabled(true);

        verify(runner).getStatus();
        verify(runner).setStatus(Runner.Status.FAILED);
        verify(logsAction).perform(runner);
        verify(consoleContainer).printError(runner, MESSAGE);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        //because type of message ERROR
        assertThat(notification.getType(), is(Notification.Type.ERROR));

        verify(presenter).update(runner);

        verify(service).stop(stopLink, callback);
        verify(eventBus).fireEvent(Matchers.<RunnerApplicationStatusEvent>any());
    }

    @Test
    public void notificationStopInProgressShouldBeShown() throws Exception {
        when(constant.messageRunnerShuttingDown()).thenReturn(MESSAGE);

        stopAction.perform(runner);

        verify(constant).messageRunnerShuttingDown();
        verify(notificationManager).showNotification(notificationCaptor.capture());

        Notification notification = notificationCaptor.getValue();

        assertThat(notification.getMessage(), equalTo(MESSAGE));
        assertThat(notification.isFinished(), is(false));
    }
}