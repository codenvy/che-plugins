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
package org.eclipse.che.ide.ext.java.worker;

import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.collections.js.JsoStringMap;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.api.JavaCompletionProposal;
import org.eclipse.che.ide.ext.java.messages.ApplyProposalMessage;
import org.eclipse.che.ide.ext.java.messages.Change;
import org.eclipse.che.ide.ext.java.messages.RoutingTypes;
import org.eclipse.che.ide.ext.java.messages.impl.MessagesImpls;
import org.eclipse.che.ide.ext.java.jdt.text.Document;
import org.eclipse.che.ide.ext.java.jdt.text.DocumentEvent;
import org.eclipse.che.ide.ext.java.jdt.text.DocumentListener;
import org.eclipse.che.ide.api.text.Region;
import com.google.gwt.webworker.client.messages.MessageFilter;

public class WorkerProposalApplier {

    private final JavaParserWorker worker;
    private JsoStringMap<JavaCompletionProposal> caProposalMap    = JsoStringMap.create();
    private JsoStringMap<JavaCompletionProposal> quickProposalMap = JsoStringMap.create();
    private Document caDocument;
    private Document   quickDocument;


    public WorkerProposalApplier(JavaParserWorker worker, MessageFilter messageFilter) {
        this.worker = worker;
        messageFilter.registerMessageRecipient(RoutingTypes.APPLY_CA_PROPOSAL, new MessageFilter.MessageRecipient<ApplyProposalMessage>() {
            @Override
            public void onMessageReceived(ApplyProposalMessage message) {
                handleApply(message.id());
            }
        });
    }

    void handleApply(String id) {
        if (caProposalMap.containsKey(id)) {
            JavaCompletionProposal proposal = caProposalMap.get(id);
            apply(id, proposal, caDocument);
        } else if (quickProposalMap.containsKey(id)) {
            JavaCompletionProposal proposal = quickProposalMap.get(id);
            apply(id, proposal, quickDocument);
        }
    }

    private void apply(String id, JavaCompletionProposal proposal, Document document) {
        final JsoArray<Change> changes = JsoArray.create();
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                MessagesImpls.ChangeImpl change = MessagesImpls.ChangeImpl.make();
                change.setOffset(event.getOffset()).setLength(event.getLength()).setText(event.getText());
                changes.add(change);
            }
        });
        proposal.apply(document);
        MessagesImpls.ProposalAppliedMessageImpl message = MessagesImpls.ProposalAppliedMessageImpl.make();
        message.setChanges(changes);
        Region selection = proposal.getSelection(document);
        if (selection != null) {
            MessagesImpls.RegionImpl region = MessagesImpls.RegionImpl.make();
            region.setLength(selection.getLength()).setOffset(selection.getOffset());
            message.setSelectionRegion(region);
        }
        message.setId(id);
        worker.sendMessage(message.serialize());
    }

    void setCaDocument(Document caDocument){
        this.caDocument = caDocument;
    }

    void setQuickDocument(Document quickDocument) {
        this.quickDocument = quickDocument;
    }

    public void setCaProposalMap(JsoStringMap<JavaCompletionProposal> caProposalMap) {
        this.caProposalMap = caProposalMap;
    }

    public void setQuickProposalMap(JsoStringMap<JavaCompletionProposal> quickProposalMap) {
        this.quickProposalMap = quickProposalMap;
    }
}