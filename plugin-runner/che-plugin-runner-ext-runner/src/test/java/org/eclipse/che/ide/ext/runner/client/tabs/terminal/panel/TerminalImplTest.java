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
package org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel;

import com.google.gwt.user.client.Element;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.DONE;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_PROGRESS;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_QUEUE;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class TerminalImplTest {

    private static final String SOME_TEXT = "some text";

    @Mock
    private Runner       runner;
    @Mock
    private Element      element;
    @InjectMocks
    private TerminalImpl terminal;

    @Before
    public void setUp() throws Exception {
        when(terminal.terminal.getElement()).thenReturn(element);
    }

    @Test
    public void terminalShouldBeHiddenIfRunnerIsNull() throws Exception {
        terminal.update(null);

        verify(terminal.unavailableLabel).setVisible(true);
        verify(element).removeAttribute("src");
    }

    @Test
    public void terminalContentShouldBeUpdated() throws Exception {
        when(runner.getStatus()).thenReturn(DONE);
        when(runner.isAlive()).thenReturn(true);
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);

        terminal.update(runner);

        verify(terminal.unavailableLabel).setVisible(false);
        verify(element).focus();

        when(runner.getTerminalURL()).thenReturn(SOME_TEXT + SOME_TEXT);

        terminal.update(runner);

        verify(terminal.terminal).setUrl(SOME_TEXT + SOME_TEXT);
    }

    @Test
    public void unavailableLabelShouldBeShowed() throws Exception {
        when(runner.getStatus()).thenReturn(DONE);
        when(runner.isAlive()).thenReturn(false);
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);

        terminal.update(runner);

        verify(element).focus();
        verify(element).removeAttribute("src");
        verify(terminal.unavailableLabel).setVisible(true);

        when(runner.getTerminalURL()).thenReturn(SOME_TEXT + SOME_TEXT);

        terminal.update(runner);

        verify(terminal.terminal, never()).setUrl(anyString());
    }

    @Test
    public void terminalShouldNotBeUpdateWhenRunnerIsAlive() throws Exception {
        when(runner.getStatus()).thenReturn(DONE);
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);
        when(runner.isAlive()).thenReturn(true);

        terminal.update(runner);

        verify(element).focus();
        verify(terminal.terminal).setUrl(anyString());
        verify(element, never()).removeAttribute("src");
    }

    @Test
    public void terminalShouldNotBeUpdateWhenLaunchingStatus() throws Exception {
        when(runner.getStatus()).thenReturn(IN_PROGRESS);
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);
        when(runner.isAlive()).thenReturn(true);

        terminal.update(runner);

        verify(element).focus();
        verify(terminal.unavailableLabel, never()).setVisible(true);
        verify(terminal.terminal, never()).setUrl(anyString());
        verify(element, never()).removeAttribute("src");
    }

    @Test
    public void terminalShouldNotBeUpdateWhenLaunchingStatus2() throws Exception {
        when(runner.getStatus()).thenReturn(IN_QUEUE);
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);
        when(runner.isAlive()).thenReturn(true);

        terminal.update(runner);

        verify(element).focus();
        verify(terminal.unavailableLabel, never()).setVisible(true);
        verify(terminal.terminal, never()).setUrl(anyString());
        verify(element, never()).removeAttribute("src");
    }

    @Test
    public void terminalShouldBeVisible() throws Exception {
        terminal.setVisible(true);

        verify(terminal.terminal).setVisible(true);
    }

    @Test
    public void unavailableLabelShouldBeVisible() throws Exception {
        terminal.setUnavailableLabelVisible(true);

        verify(terminal.unavailableLabel).setVisible(true);
    }

    @Test
    public void urlShouldBeSet() throws Exception {
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);

        terminal.setUrl(runner);

        verify(terminal.terminal).setUrl(eq(SOME_TEXT));
    }

    @Test
    public void urlShouldNotBeSet() throws Exception {
        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);

        terminal.setUrl(runner);

        when(runner.getTerminalURL()).thenReturn(SOME_TEXT);

        terminal.setUrl(runner);

        verify(terminal.terminal, times(1)).setUrl(anyString());
    }
}