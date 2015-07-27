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
import com.google.inject.Singleton;

import org.eclipse.che.env.local.client.location.WorkspaceLocationPresenter;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.extension.Extension;

/**
 * @author Vitaly Parfonov
 */
@Singleton
@Extension(title = "Che Local Env", version = "3.0.0")
public class LocalEnvironmentExtension {

    @Inject
    public LocalEnvironmentExtension(WorkspaceLocationPresenter presenter,
                                     ActionManager actionManager,
                                     ShowWorkspaceMappingDirAction action) {
        DefaultActionGroup fileGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_FILE);
        actionManager.registerAction("showWorkspaceMappingDirAction", action);
        fileGroup.add(action);
        presenter.init();
    }
}
