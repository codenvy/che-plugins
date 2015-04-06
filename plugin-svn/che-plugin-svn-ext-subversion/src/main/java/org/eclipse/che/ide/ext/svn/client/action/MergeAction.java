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

import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Extension of {@link SubversionAction} for implementing the "svn merge" command.
 */
@Singleton
public class MergeAction extends SubversionAction {

    /**
     * Constructor.
     */
    @Inject
    public MergeAction(final AnalyticsEventLogger eventLogger,
                       final AppContext appContext,
                       final SelectionAgent selectionAgent,
                       final SubversionExtensionLocalizationConstants constants,
                       final SubversionExtensionResources resources) {
        super(constants.mergeTitle(), constants.mergeDescription(), resources.merge(), eventLogger, appContext,
              constants, resources, selectionAgent);
    }

}
