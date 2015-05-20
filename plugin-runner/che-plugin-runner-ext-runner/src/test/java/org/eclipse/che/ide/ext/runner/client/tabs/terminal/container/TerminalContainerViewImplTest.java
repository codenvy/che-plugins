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
package org.eclipse.che.ide.ext.runner.client.tabs.terminal.container;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class TerminalContainerViewImplTest {
    @Mock
    private RunnerResources resources;
    @Mock
    private IsWidget        terminal;

    @InjectMocks
    private TerminalContainerViewImpl view;

    @Test
    public void widgetShouldBeAddedIntoMainPanel() {
        view.addWidget(terminal);

        verify(view.mainPanel).add(terminal);
    }

    @Test
    public void widgetShouldBeRemoved() {
        view.removeWidget(terminal);

        verify(view.mainPanel).remove(terminal);
    }

    @Test
    public void dummyContentShouldBeShowed() throws Exception {
        view.setVisibleNoRunnerLabel(true);

        verify(view.noRunnerLabel).setVisible(true);
    }

}