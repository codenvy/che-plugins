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
package org.eclipse.che.ide.jseditor.java.client.editor;

import elemental.dom.Element;

import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.util.loging.Log;

/**
 * @author Evgen Vidolob
 */
public class ActionCompletonProposal implements CompletionProposal {

    private final String               display;
    private final String               actionId;
    private final Icon                 icon;

    public ActionCompletonProposal(String display, String actionId, Icon icon) {
        this.display = display;
        this.actionId = actionId;
        this.icon = icon;
    }

    @Override
    public Element getAdditionalProposalInfo() {
        return null;
    }

    @Override
    public String getDisplayString() {
        return display;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public void getCompletion(CompletionCallback callback) {
        Log.error(getClass(), "Can't run Action " + actionId);
    }
}
