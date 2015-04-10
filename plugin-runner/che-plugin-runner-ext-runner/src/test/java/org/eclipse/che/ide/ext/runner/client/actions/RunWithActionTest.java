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

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.permits.ActionDenyAccessDialog;
import org.eclipse.che.ide.api.action.permits.ActionPermit;
import org.eclipse.che.ide.api.action.permits.Run;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RunWithActionTest {

    private static final String TEXT = "some test/test/runner";

    //variables for constructor
    @Mock
    private RunnerManagerPresenter     runnerManagerPresenter;
    @Mock
    private TabContainer               tabContainer;
    @Mock
    private RunnerResources            resources;
    @Mock
    private RunnerLocalizationConstant locale;
    @Mock
    private AppContext                 appContext;
    @Mock
    private ActionPermit           runActionPermit;
    @Mock
    private ActionDenyAccessDialog runActionDenyAccessDialog;

    @Mock
    private ActionEvent actionEvent;

    @Test
    public void actionShouldBePerformed() {
        when(locale.actionRunWith()).thenReturn(TEXT);
        when(locale.runnerTabTemplates()).thenReturn(TEXT);
        when(runActionPermit.isAllowed()).thenReturn(true);

        RunWithAction runWithAction =
                new RunWithAction(runnerManagerPresenter, tabContainer, locale, appContext, resources, runActionPermit,
                                  runActionDenyAccessDialog);
        runWithAction.actionPerformed(actionEvent);

        verify(locale, times(2)).actionRunWith();
        verify(resources).runWith();
        verify(runnerManagerPresenter).setActive();
        verify(tabContainer).showTab(TEXT);
    }
}