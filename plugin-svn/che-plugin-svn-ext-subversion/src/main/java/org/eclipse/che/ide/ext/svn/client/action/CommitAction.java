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
package org.eclipse.che.ide.ext.svn.client.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;
import org.eclipse.che.ide.ext.svn.client.commit.CommitPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

/**
 * Extension of {@link SubversionAction} for implementing the "svn commit" command.
 */
@Singleton
public class CommitAction extends SubversionAction {

    private final CommitPresenter presenter;

    /**
     * Constructor.
     */
    @Inject
    public CommitAction(final AnalyticsEventLogger eventLogger,
                        final AppContext appContext,
                        final CommitPresenter presenter,
                        final ProjectExplorerPresenter projectExplorerPresenter,
                        final SubversionExtensionLocalizationConstants constants,
                        final SubversionExtensionResources resources) {
        super(constants.commitTitle(), constants.commitDescription(), resources.commit(), eventLogger, appContext,
              constants, resources, projectExplorerPresenter);
        this.presenter = presenter;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        this.eventLogger.log(this, "IDE: Subversion 'Commit' action performed");
        this.presenter.show();
    }
}
