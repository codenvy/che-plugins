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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.container;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesContainerViewImplTest {
    @Mock
    private RunnerResources resources;

    @InjectMocks
    private PropertiesContainerViewImpl view;

    @Test
    public void widgetShouldBeShown() {
        PropertiesPanel panel = mock(PropertiesPanel.class);

        view.showWidget(panel);

        verify(panel).go(view.mainPanel);
    }

    @Test
    public void dummyContentShouldBeShowed() throws Exception {
        view.setVisibleNoRunnerLabel(true);

        verify(view.noRunnerLabel).setVisible(true);
    }
}