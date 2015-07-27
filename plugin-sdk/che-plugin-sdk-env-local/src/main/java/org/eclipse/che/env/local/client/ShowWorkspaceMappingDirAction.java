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
package org.eclipse.che.env.local.client;

import com.google.inject.Inject;

import org.eclipse.che.env.local.client.location.WorkspaceLocationPresenter;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;

/**
 * Action for showing About application information.
 *
 * @author Ann Shumilova
 */
public class ShowWorkspaceMappingDirAction extends Action {

    private final WorkspaceLocationPresenter presenter;

    @Inject
    public ShowWorkspaceMappingDirAction(WorkspaceLocationPresenter presenter, LocalizationConstant locale) {
        super(locale.actionWorkspaceLocation(), locale.actionWorkspaceLocationTitle());
        this.presenter = presenter;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        presenter.show();
    }

}
