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
import org.eclipse.che.ide.ext.svn.client.copy.CopyPresenter;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 *
 * Extension of {@link SubversionAction} for implementing the "svn copy" (copy a file or directory) command.
 *
 * @author Vladyslav Zhukovskyi
 */
@Singleton
public class CopyAction extends SubversionAction {

    private       SelectionAgent selectionAgent;
    private final CopyPresenter  presenter;

    @Inject
    public CopyAction(final AnalyticsEventLogger eventLogger,
                      final AppContext appContext,
                      final SelectionAgent selectionAgent,
                      final SubversionExtensionLocalizationConstants constants,
                      final SubversionExtensionResources resources,
                      final CopyPresenter presenter) {
        super(constants.copyTitle(), constants.copyDescription(), resources.copy(), eventLogger, appContext, constants, resources,
              selectionAgent);
        this.selectionAgent = selectionAgent;

        this.presenter = presenter;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
        eventLogger.log(this, "IDE: Subversion 'Copy' action performed");

        presenter.showCopy(getSelectedNode());
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isSelectionRequired() {
        return true;
    }

    private TreeNode<?> getSelectedNode() {
        Object selectedNode = selectionAgent.getSelection().getFirstElement();
        return selectedNode != null && selectedNode instanceof StorableNode ? (StorableNode)selectedNode : null;
    }
}
