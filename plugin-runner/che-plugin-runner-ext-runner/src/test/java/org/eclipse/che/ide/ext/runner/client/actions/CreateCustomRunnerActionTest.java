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
package org.eclipse.che.ide.ext.runner.client.actions;

import com.google.gwt.resources.client.TextResource;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Valeriy Svydenko */
@RunWith(GwtMockitoTestRunner.class)
public class CreateCustomRunnerActionTest {
    private static final String DOCKER_TEMPLATE = "docker";
    private static final String TEXT            = "text";

    //variables for constructor
    @Mock
    private RunnerLocalizationConstant          locale;
    @Mock
    private AppContext                          appContext;
    @Mock
    private RunnerResources                     resources;
    @Mock
    private RunnerManagerPresenter              runnerManagerPresenter;
    @Mock
    private NotificationManager                 notificationManager;
    @Mock
    private GetProjectEnvironmentsAction        getProjectEnvironmentsAction;
    @Mock
    private AsyncCallbackBuilder<ItemReference> asyncCallbackBuilder;
    @Mock
    private ProjectServiceClient                projectService;
    @Mock
    private TemplatesPresenter                  templatesPresenter;
    @Mock
    private TabContainer                        tabContainer;

    @Mock
    private ActionEvent                         actionEvent;
    @Mock
    private CurrentProject                      currentProject;
    @Mock
    private ItemReference                       itemReference1;
    @Mock
    private Environment                         environment;
    @Mock
    private TextResource                        textResource;
    @Mock
    private Throwable                           exception;
    @Mock
    private ProjectDescriptor                   projectDescriptor;
    @Mock
    private AsyncRequestCallback<ItemReference> asyncRequestCallback;

    @Captor
    private ArgumentCaptor<SuccessCallback<ItemReference>> successCallbackArgumentCaptor;
    @Captor
    private ArgumentCaptor<FailureCallback>                failureCallbackArgumentCaptor;

    private CreateCustomRunnerAction action;

    @Before
    public void setUp() {
        when(currentProject.getProjectDescription()).thenReturn(projectDescriptor);
        when(environment.getName()).thenReturn("environmentName");
        when(projectDescriptor.getPath()).thenReturn("/path");
        when(projectDescriptor.getName()).thenReturn("project");
        when(resources.dockerTemplate()).thenReturn(textResource);
        when(textResource.getText()).thenReturn(DOCKER_TEMPLATE);
        when(exception.getMessage()).thenReturn(TEXT);

        when(asyncCallbackBuilder.unmarshaller(ItemReference.class)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<ItemReference>>anyObject())).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.build()).thenReturn(asyncRequestCallback);

        List<Environment> projectEnvironments = Collections.singletonList(environment);
        Map<Scope, List<Environment>> environments = new EnumMap<>(Scope.class);
        environments.put(PROJECT, projectEnvironments);

        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(templatesPresenter.getProjectEnvironments()).thenReturn(environments.get(PROJECT));

        action = new CreateCustomRunnerAction(locale,
                                              appContext,
                                              resources,
                                              runnerManagerPresenter,
                                              notificationManager,
                                              getProjectEnvironmentsAction,
                                              asyncCallbackBuilder,
                                              projectService,
                                              templatesPresenter,
                                              tabContainer);
    }

    @Test
    public void actionShouldBePerformed() throws Exception {
        action.actionPerformed(actionEvent);

        verify(runnerManagerPresenter).setActive();
        verify(locale).runnerTabTemplates();
    }

    @Test
    public void environmentShouldNotBeCreatedIfCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.actionPerformed(actionEvent);

        verify(runnerManagerPresenter).setActive();
        verify(locale).runnerTabTemplates();
        verify(projectService, never()).createFolder(anyString(), (AsyncRequestCallback<ItemReference>)any());
    }

    @Test
    public void environmentShouldBeCreated() throws Exception {
        action.actionPerformed(actionEvent);

        verify(runnerManagerPresenter).setActive();
        verify(locale).runnerTabTemplates();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgumentCaptor.capture());

        successCallbackArgumentCaptor.getValue().onSuccess(itemReference1);

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallback));
        verify(projectDescriptor, times(2)).getPath();

        verify(asyncCallbackBuilder, times(2)).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder, times(2)).success(successCallbackArgumentCaptor.capture());

        successCallbackArgumentCaptor.getValue().onSuccess(itemReference1);

        verify(getProjectEnvironmentsAction).perform();
    }

    @Test
    public void notificationMessageShouldBeShowedIfParentFolderDoesNotCreate() throws Exception {
        action.actionPerformed(actionEvent);

        verify(runnerManagerPresenter).setActive();
        verify(locale).runnerTabTemplates();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).failure(failureCallbackArgumentCaptor.capture());

        failureCallbackArgumentCaptor.getValue().onFailure(exception);

        verify(notificationManager).showError(TEXT);
    }

    @Test
    public void environmentDoesNotCreatedIfDockerFileNotCrested() throws Exception {
        action.actionPerformed(actionEvent);

        verify(runnerManagerPresenter).setActive();
        verify(locale).runnerTabTemplates();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgumentCaptor.capture());

        successCallbackArgumentCaptor.getValue().onSuccess(itemReference1);

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallback));
        verify(projectDescriptor, times(2)).getPath();

        verify(asyncCallbackBuilder, times(2)).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder, times(2)).failure(failureCallbackArgumentCaptor.capture());

        failureCallbackArgumentCaptor.getValue().onFailure(exception);

        verify(getProjectEnvironmentsAction, never()).perform();
    }
}
