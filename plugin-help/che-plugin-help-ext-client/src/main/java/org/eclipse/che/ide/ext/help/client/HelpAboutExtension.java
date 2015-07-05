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
package org.eclipse.che.ide.ext.help.client;


import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.ext.help.client.about.ShowAboutAction;

import javax.inject.Inject;
import javax.inject.Singleton;

/** @author Vitalii Parfonov */
@Singleton
@Extension(title = "Help Extension", version = "3.0.0")
public class HelpAboutExtension {


    @Inject
    public HelpAboutExtension(ActionManager actionManager,
                              final ShowAboutAction showAboutAction,
                              final RedirectToHelpAction redirectToHelpAction,
                              final RedirectToForumsAction redirectToForumsAction,
                              final RedirectToFeedbackAction redirectToFeedbackAction) {

        // Compose Help menu
        DefaultActionGroup helpGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_HELP);
        actionManager.registerAction("showAbout", showAboutAction);
        actionManager.registerAction("redirectToHelp", redirectToHelpAction);
        actionManager.registerAction("redirectToForums", redirectToForumsAction);
        actionManager.registerAction("redirectToFeedback", redirectToFeedbackAction);


        helpGroup.add(showAboutAction);
        helpGroup.addSeparator();
        helpGroup.add(redirectToHelpAction);
        helpGroup.addSeparator();
        helpGroup.add(redirectToForumsAction);
        helpGroup.add(redirectToFeedbackAction);

    }
}
