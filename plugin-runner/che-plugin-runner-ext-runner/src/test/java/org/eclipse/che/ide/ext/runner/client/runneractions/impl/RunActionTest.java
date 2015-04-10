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

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.ide.api.action.permits.ActionDenyAccessDialog;
import org.eclipse.che.ide.api.action.permits.ActionPermit;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.LaunchAction;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_512;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RunActionTest {
    private static final String PATH_TO_PROJECT = "somePath";
    private static final String PROJECT_NAME    = "projectName";

    /*constructor variables*/
    @Mock
    private RunnerServiceClient                                          service;
    @Mock
    private AppContext                                                   appContext;
    @Mock
    private RunnerLocalizationConstant                                   locale;
    @Mock
    private RunnerManagerPresenter                                       presenter;
    @Mock
    private Provider<AsyncCallbackBuilder<ApplicationProcessDescriptor>> callbackBuilderProvider;
    @Mock
    private RunnerUtil                                                   runnerUtil;
    @Mock
    private RunnerActionFactory                                          actionFactory;
    @Mock
    private AnalyticsEventLogger                                          eventLogger;

    @Mock
    private Throwable                                                     reason;
    //callbacks for server
    @Mock
    private AsyncCallbackBuilder<ApplicationProcessDescriptor>            asyncCallbackBuilder;
    @Mock
    private AsyncRequestCallback<ApplicationProcessDescriptor>            asyncRequestCallback;
    //project variables
    @Mock
    private CurrentProject                                                project;
    @Mock
    private ProjectDescriptor                                             projectDescriptor;
    //run variables
    @Mock
    private RunOptions                                                    runOptions;
    @Mock
    private Runner                                                        runner;
    //action variables
    @Mock
    private ApplicationProcessDescriptor                                  descriptor;
    @Mock
    private LaunchAction                                                  launchAction;
    @Mock
    private ActionPermit                                                  runActionPermit;
    @Mock
    private ActionDenyAccessDialog                                        runActionDenyAccessDialog;
    //captors
    @Captor
    private ArgumentCaptor<FailureCallback>                               failedCallBackCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<ApplicationProcessDescriptor>> successCallBackCaptor;

    private RunAction runAction;

    @Before
    public void setUp() {
        when(actionFactory.createLaunch()).thenReturn(launchAction);
        when(runActionPermit.isAllowed()).thenReturn(true);

        runAction = new RunAction(service, appContext, locale, presenter,
                                  callbackBuilderProvider, runnerUtil, actionFactory, eventLogger, runActionPermit,
                                  runActionDenyAccessDialog);

        //preparing callbacks for server
        when(appContext.getCurrentProject()).thenReturn(project);
        when(callbackBuilderProvider.get()).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.unmarshaller(ApplicationProcessDescriptor.class)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<ApplicationProcessDescriptor>>anyObject()))
                .thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.build()).thenReturn(asyncRequestCallback);
        //preparing project data
        when(project.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getPath()).thenReturn(PATH_TO_PROJECT);
    }

    @Test
    public void shouldPerformWhenCurrentProjectIsNull() {
        reset(launchAction);
        when(appContext.getCurrentProject()).thenReturn(null);

        runAction.perform(runner);

        verify(eventLogger).log(runAction);
        verify(appContext).getCurrentProject();
        verifyNoMoreInteractions(runner, service, locale, launchAction, presenter, callbackBuilderProvider);
    }

    @Test
    public void shouldSuccessPerform() {
        //preparing descriptor data
        when(descriptor.getMemorySize()).thenReturn(MB_512.getValue());
        when(descriptor.getProcessId()).thenReturn(12345678L);

        when(runner.getOptions()).thenReturn(runOptions);

        runAction.perform(runner);

        verify(eventLogger).log(runAction);
        verify(presenter).setActive();

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<ApplicationProcessDescriptor> successCallback = successCallBackCaptor.getValue();
        successCallback.onSuccess(descriptor);

        verify(runner).setProcessDescriptor(descriptor);
        verify(runner).setRAM(MB_512.getValue());
        verify(runner).setStatus(Runner.Status.IN_PROGRESS);

        verify(presenter).addRunnerId(12345678L);

        verify(launchAction).perform(runner);

        verify(service).run(PATH_TO_PROJECT, runOptions, asyncRequestCallback);
    }

    @Test
    public void shouldFailedPerform() {
        String someRunningMessage = "run information";

        when(runner.getOptions()).thenReturn(runOptions);
        when(locale.startApplicationFailed(PROJECT_NAME)).thenReturn(someRunningMessage);
        when(project.getRunner()).thenReturn(PROJECT_NAME);
        when(projectDescriptor.getName()).thenReturn(someRunningMessage);
        when(locale.startApplicationFailed(someRunningMessage)).thenReturn(someRunningMessage);

        runAction.perform(runner);

        verify(eventLogger).log(runAction);
        verify(presenter).setActive();

        verify(asyncCallbackBuilder).failure(failedCallBackCaptor.capture());
        FailureCallback failureCallback = failedCallBackCaptor.getValue();
        failureCallback.onFailure(reason);

        verify(runnerUtil).showError(runner, someRunningMessage, null);

        verify(project).getRunner();
        verify(project, times(2)).getProjectDescription();
        verify(projectDescriptor).getName();
        verify(locale).startApplicationFailed(someRunningMessage);

        verify(service).run(PATH_TO_PROJECT, runOptions, asyncRequestCallback);
    }

    @Test
    public void shouldFailedWhenDefaultRunnerAbsent() {
        String someRunningMessage = "run information";

        when(runner.getOptions()).thenReturn(runOptions);
        when(locale.startApplicationFailed(PROJECT_NAME)).thenReturn(someRunningMessage);
        when(projectDescriptor.getName()).thenReturn(someRunningMessage);
        when(locale.defaultRunnerAbsent()).thenReturn(someRunningMessage);

        runAction.perform(runner);

        verify(eventLogger).log(runAction);
        verify(presenter).setActive();

        verify(asyncCallbackBuilder).failure(failedCallBackCaptor.capture());
        FailureCallback failureCallback = failedCallBackCaptor.getValue();
        failureCallback.onFailure(reason);

        verify(runnerUtil).showError(runner, someRunningMessage, null);

        verify(project).getRunner();
        verify(locale).defaultRunnerAbsent();

        verify(service).run(PATH_TO_PROJECT, runOptions, asyncRequestCallback);
    }
}
