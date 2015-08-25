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
package org.eclipse.che.ide.ext.tutorials.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;

/**
 * Controls a tutorial page state: shows or hides it.
 * Automatically shows a tutorial page when project opening and closes page when project closing.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class GuidePageController {
    private WorkspaceAgent workspaceAgent;
    private GuidePage      guidePage;

    @Inject
    public GuidePageController(WorkspaceAgent workspaceAgent, GuidePage guidePage) {
        this.workspaceAgent = workspaceAgent;
        this.guidePage = guidePage;
    }

    /** Open tutorial guide page. */
    public void openTutorialGuide() {
        workspaceAgent.openPart(guidePage, PartStackType.EDITING);
    }
}
