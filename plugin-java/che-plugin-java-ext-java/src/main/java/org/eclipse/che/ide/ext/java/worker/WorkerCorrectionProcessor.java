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

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.collections.js.JsoStringMap;
import org.eclipse.che.ide.ext.java.jdt.MultiStatus;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.api.IProblemLocation;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.api.JavaCompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.core.JavaCore;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.AdvancedQuickAssistProcessor;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.AssistContext;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.CorrectionMessages;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.ProblemLocation;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.QuickAssistProcessorImpl;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.QuickFixProcessorImpl;
import org.eclipse.che.ide.ext.java.jdt.quickassist.api.InvocationContext;
import org.eclipse.che.ide.ext.java.jdt.quickassist.api.QuickAssistProcessor;
import org.eclipse.che.ide.ext.java.jdt.quickassist.api.QuickFixProcessor;
import org.eclipse.che.ide.ext.java.messages.ComputeCorrMessage;
import org.eclipse.che.ide.ext.java.messages.ProblemLocationMessage;
import org.eclipse.che.ide.ext.java.messages.RoutingTypes;
import org.eclipse.che.ide.ext.java.messages.WorkerProposal;
import org.eclipse.che.ide.ext.java.messages.impl.MessagesImpls;
import org.eclipse.che.ide.runtime.CoreException;
import org.eclipse.che.ide.runtime.IStatus;
import org.eclipse.che.ide.runtime.Status;
import org.eclipse.che.ide.util.UUID;
import com.google.gwt.webworker.client.messages.MessageFilter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * WorkerCorrectionProcessor 
 *
 * @author Evgen Vidolob
 */
public class WorkerCorrectionProcessor {

    private static QuickFixProcessor fixProcessor = new QuickFixProcessorImpl();

    private static QuickAssistProcessor[] assistProcessors =
            new QuickAssistProcessor[]{new QuickAssistProcessorImpl(), new AdvancedQuickAssistProcessor()};


    private String fErrorMessage;

    private JavaParserWorker      worker;
    private WorkerProposalApplier workerProposalApplier;
    private WorkerCuCache cuCache;

    public WorkerCorrectionProcessor(JavaParserWorker worker, MessageFilter messageFilter, WorkerProposalApplier workerProposalApplier,
                                     WorkerCuCache cuCache) {
        this.worker = worker;
        this.workerProposalApplier = workerProposalApplier;
        this.cuCache = cuCache;
        messageFilter.registerMessageRecipient(RoutingTypes.COMPUTE_CORRECTION, new MessageFilter.MessageRecipient<ComputeCorrMessage>() {
            @Override
            public void onMessageReceived(ComputeCorrMessage message) {
                computateProposals(message);
            }
        });
    }

    public static IStatus collectProposals(InvocationContext context,
                                           Array<ProblemLocationMessage> plMessage, boolean addQuickFixes, boolean addQuickAssists,
                                           Collection<JavaCompletionProposal> proposals)
            throws CoreException {

        ProblemLocation[] problemLocations = new ProblemLocation[plMessage.size()];
        for (int i = 0; i < plMessage.size(); i++) {
            problemLocations[i] = new ProblemLocation(plMessage.get(i));
        }
//        ProblemLocation[] problems = problems.toArray(new ProblemLocation[problems.size()]);
        MultiStatus resStatus = null;
        //
        if (addQuickFixes) {
            IStatus status = collectCorrections(context, problemLocations, proposals);
            if (!status.isOK()) {
                resStatus =
                        new MultiStatus(JavaCore.PLUGIN_ID, IStatus.ERROR,
                                        CorrectionMessages.INSTANCE.JavaCorrectionProcessor_error_quickfix_message(), null);
                resStatus.add(status);
            }
        }
        if (addQuickAssists) {
            IStatus status = collectAssists(context, problemLocations, proposals);
            if (!status.isOK()) {
                if (resStatus == null) {
                    resStatus =
                            new MultiStatus(JavaCore.PLUGIN_ID, IStatus.ERROR,
                                            CorrectionMessages.INSTANCE.JavaCorrectionProcessor_error_quickassist_message(), null);
                }
                resStatus.add(status);
            }
        }
        if (resStatus != null) {
            return resStatus;
        }
        return Status.OK_STATUS;
    }

    public static IStatus collectAssists(InvocationContext context, IProblemLocation[] locations,
                                         Collection<JavaCompletionProposal> proposals) throws CoreException {

        for (QuickAssistProcessor curr : assistProcessors) {
            JavaCompletionProposal[] res;
            res = curr.getAssists(context, locations);
            if (res != null) {
                for (int k = 0; k < res.length; k++) {
                    proposals.add(res[k]);
                }
            }
        }
        return Status.OK_STATUS;

    }

    public static IStatus collectCorrections(InvocationContext context, IProblemLocation[] locations,
                                             Collection<JavaCompletionProposal> proposals) throws CoreException {
        JavaCompletionProposal[] res;
        res = fixProcessor.getCorrections(context, locations);
        if (res != null) {
            for (int k = 0; k < res.length; k++) {
                proposals.add(res[k]);
            }
        }
        return Status.OK_STATUS;
    }

    public static boolean hasAssists(InvocationContext context) {
        for (QuickAssistProcessor curr : assistProcessors) {
            try {
                if (curr.hasAssists(context))
                    return true;
            } catch (CoreException e) {
//                Log.error(WorkerCorrectionProcessor.class, e);
                return false;
            }
        }

        return false;
    }

//    public void setCu(CompilationUnit cu) {
//        this.cu = cu;
//    }

    public void computateProposals(ComputeCorrMessage message) {
        JsoStringMap<JavaCompletionProposal> proposalMap = JsoStringMap.create();
        int documentOffset = message.documentOffset();

        WorkerDocument document = new WorkerDocument(message.documentContent());
        AssistContext context = null;
        CompilationUnit cu = cuCache.getCompilationUnit(message.filePath());
        if (cu != null) {
            int length = message.documentSelectionLength();
            context = new AssistContext(document, documentOffset, length, cu);
        }


        fErrorMessage = null;
        Array<ProblemLocationMessage> problemLocations = message.problemLocations();
        ArrayList<JavaCompletionProposal> proposals = new ArrayList<JavaCompletionProposal>(10);
        try {
            if (context != null && problemLocations != null) {
                IStatus status =
                        collectProposals(context, problemLocations, true, !message.updatedOffset(), proposals);
                if (!status.isOK()) {
                    fErrorMessage = status.getMessage();
                    //TODO
                    //JavaPlugin.log(status);
                }
            }
        } catch (CoreException e) {
            //TODO log error
            throw new RuntimeException(e);
        }
        MessagesImpls.CAProposalsComputedMessageImpl caComputedMessage = MessagesImpls.CAProposalsComputedMessageImpl.make();
        caComputedMessage.setId(message.id());
        JsoArray<WorkerProposal> workerProposals = JsoArray.create();
        for (JavaCompletionProposal proposal : proposals) {
            MessagesImpls.WorkerProposalImpl prop = MessagesImpls.WorkerProposalImpl.make();
            prop.setAutoInsertable(proposal.isAutoInsertable()).setDisplayText(proposal.getDisplayString())
                .setImage(proposal.getImage() == null ? null : proposal.getImage().name());
            String uuid = UUID.uuid();
            prop.setId(uuid);
            proposalMap.put(uuid, proposal);
            workerProposals.add(prop);
        }
        workerProposalApplier.setQuickDocument(document);
        workerProposalApplier.setQuickProposalMap(proposalMap);
        caComputedMessage.setProposals(workerProposals);
        worker.sendMessage(caComputedMessage.serialize());
    }

    public static boolean hasCorrections(IProblemLocation annot) {
        return fixProcessor.hasCorrections(annot.getProblemId());
    }

}
