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
package org.eclipse.che.ide.ext.java.jdi.client.actions;

import com.google.common.collect.ImmutableList;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.shared.dto.AttributeDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.client.debug.remotedebug.RemoteDebugPresenter;
import org.eclipse.che.ide.ext.java.shared.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RemoteDebugActionTest {
    private static final String PROJECT_TYPE = "maven";

    //constructor mocks
    @Mock
    private AppContext                      appContext;
    @Mock
    private RemoteDebugPresenter            presenter;
    @Mock
    private AnalyticsEventLogger            eventLogger;
    @Mock
    private ProjectTypeRegistry             typeRegistry;
    @Mock
    private JavaRuntimeLocalizationConstant locale;

    //additional mocks
    @Mock(answer = RETURNS_DEEP_STUBS)
    private ActionEvent           actionEvent;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private CurrentProject        currentProject;
    @Mock
    private ProjectTypeDefinition definition;
    @Mock
    private AttributeDescriptor   attributeDescriptor;

    @InjectMocks
    private RemoteDebugAction action;

    @Before
    public void setUp() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription().getType()).thenReturn(PROJECT_TYPE);
        when(typeRegistry.getProjectType(PROJECT_TYPE)).thenReturn(definition);
        when(definition.getAttributeDescriptors()).thenReturn(ImmutableList.of(attributeDescriptor));
        when(attributeDescriptor.getName()).thenReturn(Constants.LANGUAGE);
        when(attributeDescriptor.getValues()).thenReturn(ImmutableList.of("java"));
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(locale).connectToRemote();
        verify(locale).connectToRemoteDescription();
    }

    @Test
    public void projectActionShouldNotBeUpdatedWhenCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.updateProjectAction(actionEvent);

        verify(typeRegistry, never()).getProjectType(PROJECT_TYPE);
        verify(actionEvent, never()).getPresentation();
    }

    @Test
    public void actionShouldBeUpdatedWhenProjectTypeIsJava() {
        action.updateProjectAction(actionEvent);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getProjectDescription();
        verify(typeRegistry).getProjectType(PROJECT_TYPE);
        verify(actionEvent.getPresentation()).setEnabledAndVisible(true);
    }

    @Test
    public void actionShouldBeUpdatedWhenProjectTypeIsNotJava() {
        when(attributeDescriptor.getValues()).thenReturn(ImmutableList.of("cpp"));

        action.updateProjectAction(actionEvent);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getProjectDescription();
        verify(typeRegistry).getProjectType(PROJECT_TYPE);
        verify(actionEvent.getPresentation(), never()).setEnabledAndVisible(true);
    }

    @Test
    public void actionShouldBePerformed() {
        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(presenter).showDialog();
    }
}