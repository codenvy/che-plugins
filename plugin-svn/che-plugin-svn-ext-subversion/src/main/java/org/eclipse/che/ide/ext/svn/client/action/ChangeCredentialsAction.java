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

import static org.eclipse.che.ide.ext.svn.shared.SubversionTypeConstant.SUBVERSION_ATTRIBUTE_REPOSITORY_URL;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;
import org.eclipse.che.ide.ext.svn.client.askcredentials.AskCredentialsPresenter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Extension of {@link SubversionAction} for changing username/password.
 */
@Singleton
public class ChangeCredentialsAction extends SubversionAction {

    private final AskCredentialsPresenter presenter;

    /**
     * Constructor.
     */
    @Inject
    public ChangeCredentialsAction(final AnalyticsEventLogger eventLogger,
                                   final AppContext appContext,
                                   final SelectionAgent selectionAgent,
                                   final SubversionExtensionLocalizationConstants constants,
                                   final SubversionExtensionResources resources,
                                   final AskCredentialsPresenter presenter) {
        super(constants.changeCredentialsTitle(), constants.changeCredentialsDescription(), resources.add(),
              eventLogger, appContext, constants, resources, selectionAgent);

        this.presenter = presenter;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        eventLogger.log(this, "IDE: Changing credentials for Subversion repository");
        final CurrentProject project = appContext.getCurrentProject();

        if (project != null) {
            final String repositoryUrl = appContext.getCurrentProject().getAttributeValue(SUBVERSION_ATTRIBUTE_REPOSITORY_URL);
            presenter.askCredentials(repositoryUrl);
        }
    }
}
