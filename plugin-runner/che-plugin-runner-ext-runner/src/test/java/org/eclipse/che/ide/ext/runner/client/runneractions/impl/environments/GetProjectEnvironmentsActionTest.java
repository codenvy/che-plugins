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

import com.google.inject.Provider;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
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
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
import org.eclipse.che.ide.ext.runner.client.util.GetEnvironmentsUtil;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class GetProjectEnvironmentsActionTest {
    private static final String SOME_STRING = "somePath";

    //variables for constructor
    @Mock
    private Provider<TemplatesContainer>                          templatesContainerProvider;
    @Mock
    private AppContext                                            appContext;
    @Mock
    private ProjectServiceClient                                  projectService;
    @Mock
    private NotificationManager                                   notificationManager;
    @Mock
    private Provider<AsyncCallbackBuilder<RunnerEnvironmentTree>> callbackBuilderProvider;
    @Mock
    private RunnerLocalizationConstant                            locale;
    @Mock
    private GetEnvironmentsUtil                                   environmentUtil;
    @Mock
    private RunnerUtil                                            runnerUtil;
    @Mock
    private ChooseRunnerAction                                    chooseRunnerAction;

    @Mock
    private Throwable reason;

    //callbacks for server
    @Mock
    private AsyncCallbackBuilder<RunnerEnvironmentTree> asyncCallbackBuilder;
    @Mock
    private AsyncRequestCallback<RunnerEnvironmentTree> asyncRequestCallback;
    //project variables
    @Mock
    private CurrentProject                              project;
    @Mock
    private ProjectDescriptor                           projectDescriptor;
    //runner variables
    @Mock
    private TemplatesContainer                          templatesContainer;
    @Mock
    private List<RunnerEnvironmentLeaf>                 environmentLeaves;
    @Mock
    private List<Environment>                           environments;
    @Mock
    private RunnerEnvironmentTree                       result;
    @Mock
    private CurrentProject                              currentProject;
    @Mock
    private ProjectDescriptor                           descriptor;
    @Mock
    private Environment                                 environment;

    //captors
    @Captor
    private ArgumentCaptor<FailureCallback>                        failedCallBackCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<RunnerEnvironmentTree>> successCallBackCaptor;

    private GetProjectEnvironmentsAction action;

    @Before
    public void setUp() {
        action = new GetProjectEnvironmentsAction(appContext,
                                                  projectService,
                                                  notificationManager,
                                                  callbackBuilderProvider,
                                                  locale,
                                                  environmentUtil,
                                                  runnerUtil,
                                                  chooseRunnerAction,
                                                  templatesContainerProvider);

        when(templatesContainerProvider.get()).thenReturn(templatesContainer);
        //preparing callbacks for server
        when(appContext.getCurrentProject()).thenReturn(project);
        when(project.getRunner()).thenReturn(SOME_STRING);
        when(callbackBuilderProvider.get()).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.unmarshaller(RunnerEnvironmentTree.class)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(environmentUtil.getEnvironmentsByProjectType(result, SOME_STRING, PROJECT)).thenReturn(Arrays.asList(environment));
        when(environment.getId()).thenReturn(SOME_STRING);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<RunnerEnvironmentTree>>anyObject()))
                .thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.build()).thenReturn(asyncRequestCallback);
        //preparing project data
        when(project.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getPath()).thenReturn(SOME_STRING);

        when(runnerUtil.hasRunPermission()).thenReturn(true);
    }

    @Test
    public void shouldNotPerformWhenCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.perform();

        verify(appContext).getCurrentProject();
        verifyNoMoreInteractions(appContext,
                                 projectService,
                                 notificationManager,
                                 callbackBuilderProvider,
                                 locale,
                                 environmentUtil,
                                 runnerUtil,
                                 chooseRunnerAction,
                                 templatesContainerProvider,
                                 project);
    }

    @Test
    public void shouldNotPerformWhenCurrentProjectIsNotNullAndProjectDoesNotHavePermission() {
        when(runnerUtil.hasRunPermission()).thenReturn(false);

        action.perform();

        verify(appContext).getCurrentProject();
        verify(runnerUtil).hasRunPermission();
        verifyNoMoreInteractions(appContext,
                                 projectService,
                                 notificationManager,
                                 callbackBuilderProvider,
                                 locale,
                                 environmentUtil,
                                 runnerUtil,
                                 chooseRunnerAction,
                                 templatesContainerProvider,
                                 project);
    }

    @Test
    public void shouldPerformFailure() {
        String errorMessage = "error message";
        when(locale.customRunnerGetEnvironmentFailed()).thenReturn(errorMessage);

        action.perform();

        verify(appContext).getCurrentProject();

        verify(asyncCallbackBuilder).failure(failedCallBackCaptor.capture());
        FailureCallback failureCallback = failedCallBackCaptor.getValue();
        failureCallback.onFailure(reason);

        verify(locale).customRunnerGetEnvironmentFailed();
        verify(notificationManager).showError(errorMessage);

        verify(projectService).getRunnerEnvironments(SOME_STRING, asyncRequestCallback);
    }

    @Test
    public void shouldPerformSuccessWithToRunnerEnvironment() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(currentProject.getRunner()).thenReturn(SOME_STRING);
        when(descriptor.getPath()).thenReturn(SOME_STRING);
        when(descriptor.getType()).thenReturn(SOME_STRING);

        action.perform();

        verify(appContext).getCurrentProject();

        verify(asyncCallbackBuilder).success(successCallBackCaptor.capture());
        SuccessCallback<RunnerEnvironmentTree> successCallback = successCallBackCaptor.getValue();
        successCallback.onSuccess(result);

        verify(templatesContainerProvider).get();

        verify(environmentUtil).getEnvironmentsByProjectType(result, SOME_STRING, PROJECT);

        verify(projectService).getRunnerEnvironments(SOME_STRING, asyncRequestCallback);
        verify(chooseRunnerAction).addProjectRunners(Matchers.<List<Environment>>anyObject());
        verify(templatesContainer).setDefaultEnvironment(environment);
    }
}