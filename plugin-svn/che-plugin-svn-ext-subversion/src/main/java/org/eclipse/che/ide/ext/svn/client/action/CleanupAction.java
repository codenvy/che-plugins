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
import org.eclipse.che.ide.ext.svn.client.cleanup.CleanupPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

/**
 * Extension of {@link SubversionAction} for implementing the "svn cleanup" command.
 */
@Singleton
public class CleanupAction extends SubversionAction {

    private final CleanupPresenter cleanupPresenter;

    /**
     * Constructor.
     */
    @Inject
    public CleanupAction(final AnalyticsEventLogger eventLogger,
                         final AppContext appContext,
                         final CleanupPresenter cleanupPresenter,
                         final ProjectExplorerPresenter projectExplorerPresenter,
                         final SubversionExtensionLocalizationConstants constants,
                         final SubversionExtensionResources resources) {
        super(constants.cleanupTitle(), constants.cleanupDescription(), resources.cleanup(), eventLogger,
              appContext, constants, resources, projectExplorerPresenter);
        this.cleanupPresenter = cleanupPresenter;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        this.eventLogger.log(this, "IDE: Subversion 'Cleanup' action performed");
        this.cleanupPresenter.cleanup();
    }

}
