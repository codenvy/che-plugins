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

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.ext.java.client.editor.JavaCodeAssistClient;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.che.ide.ext.java.shared.dto.ProposalApplyResult;
import org.eclipse.che.ide.ext.java.shared.dto.Region;
import org.eclipse.che.ide.jseditor.client.codeassist.Completion;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposalExtension;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.util.loging.Log;

import java.util.List;

/**
 * @author Evgen Vidolob
 */
public class JavaCompletionProposal implements CompletionProposal, CompletionProposalExtension {

    private final int                  id;
    private final String               display;
    private final Icon                 icon;
    private final JavaCodeAssistClient client;
    private       String               sessionId;

    public JavaCompletionProposal(final int id, final String display, final Icon icon,
                                  final JavaCodeAssistClient client, String sessionId) {
        this.id = id;
        this.display = display;
        this.icon = icon;
        this.client = client;
        this.sessionId = sessionId;
    }

    /** {@inheritDoc} */
    @Override
    public Element getAdditionalProposalInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayString() {
        return display;
    }

    /** {@inheritDoc} */
    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public void getCompletion(final CompletionCallback callback) {
        getCompletion(true, callback);
    }

    @Override
    public void getCompletion(boolean insert, final CompletionCallback callback) {
        client.applyProposal(sessionId, id, insert, new AsyncCallback<ProposalApplyResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error(JavaCompletionProposal.class, caught);
            }

            @Override
            public void onSuccess(ProposalApplyResult result) {
                callback.onCompletion(new CompletionImpl(result.getChanges(), result.getSelection()));
            }
        });
    }

    private class CompletionImpl implements Completion {

        private final List<Change> changes;
        private final Region       region;

        private CompletionImpl(final List<Change> changes, final Region region) {
            this.changes = changes;
            this.region = region;
        }

        /** {@inheritDoc} */
        @Override
        public void apply(final EmbeddedDocument document) {
            for (final Change change : changes) {
                document.replace(change.getOffset(), change.getLength(), change.getText());
            }
        }

        /** {@inheritDoc} */
        @Override
        public LinearRange getSelection(final EmbeddedDocument document) {
            if (region == null) {
                return null;
            } else {
                return LinearRange.createWithStart(region.getOffset()).andLength(region.getLength());
            }
        }
    }
}
