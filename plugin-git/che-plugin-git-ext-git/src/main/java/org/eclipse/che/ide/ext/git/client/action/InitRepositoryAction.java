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
package org.eclipse.che.ide.ext.git.client.action;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ext.git.client.init.InitRepositoryPresenter;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** @author Andrey Plotnikov */
@Singleton
public class InitRepositoryAction extends GitAction {
    private final InitRepositoryPresenter presenter;
    private final AnalyticsEventLogger    eventLogger;
    private final DialogFactory           dialogFactory;
    private       GitLocalizationConstant constant;

    @Inject
    public InitRepositoryAction(InitRepositoryPresenter presenter,
                                GitResources resources,
                                GitLocalizationConstant constant,
                                AppContext appContext,
                                AnalyticsEventLogger eventLogger,
                                SelectionAgent selectionAgent,
                                DialogFactory dialogFactory) {
        super(constant.initControlTitle(), constant.initControlPrompt(), resources.initRepo(), appContext, selectionAgent);
        this.presenter = presenter;
        this.eventLogger = eventLogger;
        this.constant = constant;
        this.dialogFactory = dialogFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        dialogFactory.createConfirmDialog(constant.createTitle(),
                                          constant.messagesInitRepoQuestion(appContext.getCurrentProject().getRootProject().getName()),
                                          new ConfirmCallback() {
                                              @Override
                                              public void accepted() {
                                                  presenter.initRepository();
                                              }
                                          }, null).show();
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        e.getPresentation().setVisible(getActiveProject() != null);
        e.getPresentation().setEnabled(!isGitRepository());
    }
}
