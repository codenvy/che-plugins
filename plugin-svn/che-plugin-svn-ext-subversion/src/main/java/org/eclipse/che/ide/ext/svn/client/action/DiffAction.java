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
import org.eclipse.che.ide.ext.svn.client.diff.DiffPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

/**
 * Extension of {@link org.eclipse.che.ide.ext.svn.client.action.SubversionAction} for implementing the "svn diff" command.
 */
@Singleton
public class DiffAction extends SubversionAction {

    private final DiffPresenter presenter;
    /**
     * Constructor.
     */
    @Inject
    public DiffAction(final AnalyticsEventLogger eventLogger,
                      final AppContext appContext,
                      final ProjectExplorerPresenter projectExplorerPresenter,
                      final SubversionExtensionLocalizationConstants constants,
                      final SubversionExtensionResources resources,
                      final DiffPresenter presenter) {
        super(constants.diffTitle(), constants.diffDescription(), resources.diff(), eventLogger,
              appContext, constants, resources, projectExplorerPresenter);

        this.presenter = presenter;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        eventLogger.log(this, "IDE: Subversion 'Diff' action performed");

        presenter.showDiff();
    }

}
