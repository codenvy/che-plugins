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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesStubPanelTest {
    @Mock
    private PropertiesPanelView view;
    @Mock
    private AppContext          appContext;
    @Mock
    private WorkspaceDescriptor         currentWorkspace;

    @Before
    public void setUp() {
        when(appContext.getWorkspace()).thenReturn(currentWorkspace);
        when(appContext.getWorkspace().getAttributes()).thenReturn(new HashMap<String, String>());


        new PropertiesStubPanel(view, appContext);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        verify(view).setName("");
        verify(view).setType("");

        verify(view).setEnableNameProperty(false);
        verify(view).setEnableRamProperty(false);
        verify(view).setEnableBootProperty(false);
        verify(view).setEnableShutdownProperty(false);
        verify(view).setEnableScopeProperty(false);

        verify(view).hideButtonsPanel();
        verify(view).hideSwitcher();
    }
}