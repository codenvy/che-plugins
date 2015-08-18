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
import org.eclipse.che.ide.ext.git.client.remove.RemoveFromIndexPresenter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a> */
@Singleton
public class RemoveFromIndexAction extends GitAction {
    private final RemoveFromIndexPresenter presenter;
    private final AnalyticsEventLogger     eventLogger;

    @Inject
    public RemoveFromIndexAction(RemoveFromIndexPresenter presenter,
                                 AppContext appContext,
                                 GitResources resources,
                                 GitLocalizationConstant constant,
                                 AnalyticsEventLogger eventLogger,
                                 SelectionAgent selectionAgent) {
        super(constant.removeFromIndexTitle(), constant.removeFromIndexTitle(), resources.removeFiles(), appContext, selectionAgent);
        this.presenter = presenter;
        this.eventLogger = eventLogger;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.showDialog();
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        e.getPresentation().setVisible(getActiveProject() != null);
        e.getPresentation().setEnabled(isGitRepository() && isItemSelected());
    }
}
