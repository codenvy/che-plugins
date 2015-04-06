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

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ChooseRunnerActionTest {

    private static final String TEXT        = "some test/test/runner";
    private static final String SELECTED_ID = "should select this runner";
    private List<Environment> projectEnvList;
    private List<Environment> systemEnvList;

    //variables for constructor
    @Mock
    private RunnerResources            resources;
    @Mock
    private RunnerLocalizationConstant locale;
    @Mock
    private AppContext                 appContext;

    @Mock
    private RunnerResources.RunnerCss css;
    @Mock
    private Environment               projectEnv1;
    @Mock
    private Environment               projectEnv2;
    @Mock
    private Environment               systemEnv1;
    @Mock
    private Environment               systemEnv2;

    private ChooseRunnerAction action;

    @Before
    public void setUp() {
        when(locale.actionChooseRunner()).thenReturn(TEXT);
        when(resources.runnerCss()).thenReturn(css);
        when(css.fontStyle()).thenReturn(TEXT);
        when(css.runnersAction()).thenReturn(TEXT);

        action = new ChooseRunnerAction(resources, locale, appContext);

        projectEnvList = Arrays.asList(projectEnv1, projectEnv2);
        systemEnvList = Arrays.asList(systemEnv1, systemEnv2);

        when(projectEnv1.getName()).thenReturn(TEXT);
        when(projectEnv2.getName()).thenReturn(TEXT);
        when(projectEnv1.getName()).thenReturn(TEXT);
        when(projectEnv2.getName()).thenReturn(SELECTED_ID);
    }

    @Test
    public void prepareActionShouldBePerformed() throws Exception {
        verify(locale, times(2)).actionChooseRunner();
        verify(resources, times(2)).runnerCss();
        verify(css).runnersAction();
        verify(css).runnerFontStyle();
    }

    @Test
    public void projectEnvironmentsShouldBeAdded() {
        //current project not null
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(projectEnv1.getId()).thenReturn(TEXT);

        action.addProjectRunners(projectEnvList);

        verify(appContext).getCurrentProject();

        verify(projectEnv1).getId();
        verify(projectEnv1, times(2)).getName();
        verify(projectEnv2).getName();
    }

    @Test
    public void systemEnvironmentsShouldBeAdded() {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.addSystemRunners(systemEnvList);

        verify(appContext).getCurrentProject();

        verify(systemEnv1).getName();
        verify(systemEnv2).getName();
    }

    @Test
    public void systemEnvironmentsShouldBeAddedWhenProjectRunnerAreAlreadyAdded() {
        when(appContext.getCurrentProject()).thenReturn(null);
        action.addProjectRunners(projectEnvList);

        action.addSystemRunners(systemEnvList);

        verify(appContext, times(2)).getCurrentProject();

        verify(projectEnv1, times(2)).getName();
        verify(projectEnv2, times(2)).getName();
        verify(systemEnv1).getName();
        verify(systemEnv2).getName();
    }

    @Test
    public void onlyProjectEnvironmentsShouldBeAdded() {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.addProjectRunners(projectEnvList);

        verify(appContext).getCurrentProject();

        verify(projectEnv1).getName();
        verify(projectEnv2).getName();
    }

    @Test
    public void projectEnvironmentsShouldBeAddedWhenSystemRunnerAreAlreadyAdded() {
        when(appContext.getCurrentProject()).thenReturn(null);
        action.addSystemRunners(systemEnvList);

        action.addProjectRunners(projectEnvList);

        verify(appContext, times(2)).getCurrentProject();

        verify(systemEnv1, times(2)).getName();
        verify(systemEnv2, times(2)).getName();
        verify(projectEnv1).getName();
        verify(projectEnv2).getName();
    }

    @Test
    public void defaultRunnerShouldBeSelected() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(systemEnv1.getId()).thenReturn(TEXT + TEXT);
        when(systemEnv2.getId()).thenReturn(TEXT);
        when(systemEnv2.getName()).thenReturn(TEXT);

        action.addSystemRunners(systemEnvList);

        action.addProjectRunners(projectEnvList);

        verify(appContext, times(2)).getCurrentProject();

        verify(systemEnv1, times(2)).getId();
        verify(systemEnv2, times(2)).getId();
        verify(systemEnv2, times(4)).getName();
    }

    @Test
    public void defaultRunnerShouldBeNull() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(projectEnv1.getId()).thenReturn(TEXT + TEXT);
        when(projectEnv2.getId()).thenReturn(TEXT + TEXT);

        action.addProjectRunners(projectEnvList);

        verify(appContext).getCurrentProject();
        verify(projectEnv1).getId();
        verify(projectEnv2).getId();
    }
}