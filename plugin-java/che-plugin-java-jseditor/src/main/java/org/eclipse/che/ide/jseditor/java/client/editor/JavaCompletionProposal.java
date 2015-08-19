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
import org.eclipse.che.ide.ext.java.client.editor.JavaParserWorker;
import org.eclipse.che.ide.ext.java.messages.Change;
import org.eclipse.che.ide.ext.java.messages.ProposalAppliedMessage;
import org.eclipse.che.ide.jseditor.client.codeassist.Completion;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;

import java.util.List;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 */
public class JavaCompletionProposal implements CompletionProposal {

    private final String id;
    private final String display;
    private final Icon icon;
    private final JavaParserWorker worker;

    public JavaCompletionProposal(final String id, final String display, final Icon icon, final JavaParserWorker worker) {
        this.id = id;
        this.display = display;
        this.icon = icon;
        this.worker = worker;
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
        worker.applyCAProposal(id, new JavaParserWorker.Callback<ProposalAppliedMessage>() {
            @Override
            public void onCallback(final ProposalAppliedMessage message) {
                callback.onCompletion(new CompletionImpl(message.changes().toList(), message.selectionRegion()));
            }
        });
    }

    private class CompletionImpl implements Completion {

        private final List<Change>                                 changes;
        private final org.eclipse.che.ide.ext.java.messages.Region region;

        private CompletionImpl(final List<Change> changes, final org.eclipse.che.ide.ext.java.messages.Region region) {
            this.changes = changes;
            this.region = region;
        }

        /** {@inheritDoc} */
        @Override
        public void apply(final EmbeddedDocument document) {
            for (final Change change : changes) {
                document.replace(change.offset(), change.length(), change.text());
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
