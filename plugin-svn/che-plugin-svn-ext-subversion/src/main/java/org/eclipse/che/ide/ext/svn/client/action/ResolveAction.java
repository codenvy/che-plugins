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

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.ext.svn.client.resolve.ResolvePresenter;

/**
 * Extension of {@link SubversionAction} for implementing the "svn resolved" command.
 */
@Singleton
public class ResolveAction extends SubversionAction {

    private final ResolvePresenter presenter;

    /**
     * Constructor.
     */
    @Inject
    public ResolveAction(final AnalyticsEventLogger eventLogger,
                         final AppContext appContext,
                         final SubversionExtensionLocalizationConstants constants,
                         final SubversionExtensionResources resources,
                         final SelectionAgent selectionAgent,
                         final ResolvePresenter presenter) {
        super(constants.resolvedTitle(), constants.resolvedDescription(), resources.resolved(), eventLogger,
              appContext, constants, resources, selectionAgent);
        this.presenter = presenter;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        presenter.showConflictsDialog();
    }

}
