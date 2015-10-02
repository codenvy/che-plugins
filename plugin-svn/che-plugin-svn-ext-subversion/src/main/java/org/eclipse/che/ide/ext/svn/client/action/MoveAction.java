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
import org.eclipse.che.ide.ext.svn.client.move.MovePresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

/**
 * Extension of {@link SubversionAction} for implementing the "svn move" command.
 *
 * @author Vladyslav Zhukovskyi
 */
@Singleton
public class MoveAction extends SubversionAction {

    private MovePresenter presenter;

    @Inject
    public MoveAction(AnalyticsEventLogger eventLogger,
                      AppContext appContext,
                      SubversionExtensionLocalizationConstants constants,
                      SubversionExtensionResources resources,
                      final ProjectExplorerPresenter projectExplorerPresenter,
                      MovePresenter presenter) {
        super(constants.moveActionTitle(), constants.moveActionDescription(), resources.move(), eventLogger, appContext, constants,
              resources, projectExplorerPresenter);
        this.presenter = presenter;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        eventLogger.log(this, "IDE: Subversion 'Move' action performed");

        presenter.showMove();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isSelectionRequired() {
        return true;
    }
}
