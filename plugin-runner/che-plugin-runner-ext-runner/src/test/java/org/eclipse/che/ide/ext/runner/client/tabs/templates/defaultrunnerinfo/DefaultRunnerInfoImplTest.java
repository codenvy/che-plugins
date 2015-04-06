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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.defaultrunnerinfo;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class DefaultRunnerInfoImplTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock
    private RunnerResources            resources;
    @Mock
    private RunnerLocalizationConstant locale;

    //additional mocks
    @Mock
    private Environment environment;

    @InjectMocks
    private DefaultRunnerInfoImpl view;

    @Before
    public void setUp() {
        when(environment.getName()).thenReturn(SOME_TEXT);
        when(environment.getType()).thenReturn(SOME_TEXT);
        when(environment.getRam()).thenReturn(512);
    }

    @Test
    public void infoWidgetShouldBeUpdated() throws Exception {
        view.update(environment);

        verify(view.name).setText(SOME_TEXT);
        verify(view.type).setText(SOME_TEXT);
        verify(view.ram).setText("512 MB");

        verify(environment).getName();
        verify(environment).getType();
        verify(environment).getRam();
    }

}