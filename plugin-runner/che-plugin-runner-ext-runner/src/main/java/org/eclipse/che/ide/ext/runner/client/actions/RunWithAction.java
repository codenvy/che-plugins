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

import com.google.inject.Inject;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.permits.ActionDenyAccessDialog;
import org.eclipse.che.ide.api.action.permits.ActionPermit;
import org.eclipse.che.ide.api.action.permits.Run;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPanel;

/**
 * Action which opens multi runner panel with templates.
 *
 * @author Valeriy Svydenko
 */
public class RunWithAction extends AbstractRunnerActions {

    private final RunnerManagerPresenter     runnerManagerPresenter;
    private final TabContainer               tabContainer;
    private final RunnerLocalizationConstant locale;
    private final ActionPermit               runActionPermit;
    private final ActionDenyAccessDialog     runActionDenyAccessDialog;

    @Inject
    public RunWithAction(RunnerManagerPresenter runnerManagerPresenter,
                         @LeftPanel TabContainer tabContainer,
                         RunnerLocalizationConstant locale,
                         AppContext appContext,
                         RunnerResources resources,
                         @Run ActionPermit runActionPermit,
                         @Run ActionDenyAccessDialog runActionDenyAccessDialog) {
        super(appContext, locale.actionRunWith(), locale.actionRunWith(), resources.runWith());

        this.runnerManagerPresenter = runnerManagerPresenter;
        this.tabContainer = tabContainer;
        this.locale = locale;
        this.runActionPermit = runActionPermit;
        this.runActionDenyAccessDialog = runActionDenyAccessDialog;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (runActionPermit.isAllowed()) {
            runnerManagerPresenter.setActive();
            tabContainer.showTab(locale.runnerTabTemplates());
        } else {
            runActionDenyAccessDialog.show();
        }
    }
}
