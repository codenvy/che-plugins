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
import org.eclipse.che.ide.ext.java.jdt.codeassistant.AbstractJavaCompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.CompletionProposalCollector;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.FillArgumentNamesCompletionProposalCollector;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.JavaContentAssistInvocationContext;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.LazyGenericTypeProposal;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.TemplateCompletionProposalComputer;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.api.JavaCompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.core.IJavaElement;
import org.eclipse.che.ide.ext.java.jdt.core.IType;
import org.eclipse.che.ide.ext.java.jdt.core.JavaCore;
import org.eclipse.che.ide.ext.java.jdt.core.Signature;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.che.ide.ext.java.messages.ComputeCAProposalsMessage;
import org.eclipse.che.ide.ext.java.messages.RoutingTypes;
import org.eclipse.che.ide.ext.java.messages.WorkerProposal;
import org.eclipse.che.ide.ext.java.messages.impl.MessagesImpls;
import org.eclipse.che.ide.runtime.AssertionFailedException;
import org.eclipse.che.ide.util.UUID;
import com.google.gwt.webworker.client.messages.MessageFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @version $Id:
 */
public class WorkerCodeAssist {


    private final WorkerProposalApplier workerProposalApplier;
    private Comparator<JavaCompletionProposal> comparator = new Comparator<JavaCompletionProposal>() {

        @Override
        public int compare(JavaCompletionProposal o1, JavaCompletionProposal o2) {
            int r1 = o1.getRelevance();
            int r2 = o2.getRelevance();
            int relevanceDif = r2 - r1;
            if (relevanceDif != 0) {
                return relevanceDif;
            }

            return getSortKey(o1).compareToIgnoreCase(getSortKey(o2));
        }

        private String getSortKey(JavaCompletionProposal p) {
            if (p instanceof AbstractJavaCompletionProposal)
                return ((AbstractJavaCompletionProposal) p).getSortString();
            return p.getDisplayString();
        }
    };

    private JavaParserWorker                   worker;
    private INameEnvironment                   nameEnvironment;
    private TemplateCompletionProposalComputer templateCompletionProposalComputer;
    private String                             projectPath;
    private String                             docContext;
    private WorkerCuCache                      cuCache;
    private String                             vfsId;
    private String                             documentContent;
    private WorkerDocument                     document;

    public WorkerCodeAssist(JavaParserWorker worker, MessageFilter messageFilter, WorkerProposalApplier workerProposalApplier,
                            INameEnvironment nameEnvironment,
                            TemplateCompletionProposalComputer templateCompletionProposalComputer, String docContext,
                            WorkerCuCache cuCache) {
        this.worker = worker;
        this.workerProposalApplier = workerProposalApplier;
        this.nameEnvironment = nameEnvironment;
        this.templateCompletionProposalComputer = templateCompletionProposalComputer;
        this.docContext = docContext;
        this.cuCache = cuCache;
        messageFilter.registerMessageRecipient(RoutingTypes.CA_COMPUTE_PROPOSALS,
                                               new MessageFilter.MessageRecipient<ComputeCAProposalsMessage>() {
                                                   @Override
                                                   public void onMessageReceived(final ComputeCAProposalsMessage message) {
                                                       handleCAMessage(message);
                                                   }
                                               });
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    private void handleCAMessage(ComputeCAProposalsMessage message) {
        setProjectPath(message.projectPath());
        nameEnvironment.setProjectPath(message.projectPath());
        Map<String, JavaCompletionProposal> proposalMap = new HashMap<>();
        documentContent = message.docContent();

        JavaCompletionProposal[] proposals =
                computeCompletionProposals(cuCache.getCompilationUnit(message.filePath()), message.offset(), documentContent, message.fileName());

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
        workerProposalApplier.setCaDocument(document);
        workerProposalApplier.setCaProposalMap(proposalMap);
        caComputedMessage.setProposals(workerProposals);
        worker.sendMessage(caComputedMessage.serialize());
    }

    public JavaCompletionProposal[] computeCompletionProposals(CompilationUnit unit, int offset, String documentContent,
                                                               String fileName) {
        if (unit == null) {
            return null;
        }
        document = new WorkerDocument(documentContent);
        CompletionProposalCollector collector =
                new FillArgumentNamesCompletionProposalCollector(unit, document, offset, projectPath, docContext, vfsId);

        collector
                .setAllowsRequiredProposals(
                        CompletionProposal.CONSTRUCTOR_INVOCATION,
                        CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION,
                                             CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_DECLARATION,
                                             CompletionProposal.TYPE_REF,
                                             true);

        collector.setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, false);
        collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, false);
        collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, false);
        collector.setIgnored(CompletionProposal.FIELD_REF, false);
        collector.setIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, false);
        collector.setIgnored(CompletionProposal.KEYWORD, false);
        collector.setIgnored(CompletionProposal.LABEL_REF, false);
        collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
        collector.setIgnored(CompletionProposal.METHOD_DECLARATION, false);
        collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, false);
        collector.setIgnored(CompletionProposal.METHOD_REF, false);
        collector.setIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, false);
        collector.setIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, false);
        collector.setIgnored(CompletionProposal.PACKAGE_REF, false);
        collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, false);
        collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, false);
        collector.setIgnored(CompletionProposal.TYPE_REF, false);

        CompletionEngine e = new CompletionEngine(nameEnvironment, collector, JavaCore.getOptions());
        try {
            e.complete(new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(
                    documentContent.toCharArray(),
                    fileName.substring(0, fileName.lastIndexOf('.')), "UTF-8"), offset, 0);

            JavaCompletionProposal[] javaCompletionProposals = collector.getJavaCompletionProposals();
            List<JavaCompletionProposal> types =
                    new ArrayList<>(Arrays.asList(javaCompletionProposals));
            if (types.size() > 0 && collector.getInvocationContext().computeIdentifierPrefix().length() == 0) {
                IType expectedType = collector.getInvocationContext().getExpectedType();
                if (expectedType != null) {
                    // empty prefix completion - insert LRU types if known, but prune if they already occur in the core list

                    // compute minmimum relevance and already proposed list
                    int relevance = Integer.MAX_VALUE;
                    Set<String> proposed = new HashSet<>();
                    for (Iterator<JavaCompletionProposal> it = types.iterator(); it.hasNext(); ) {
                        AbstractJavaCompletionProposal p = (AbstractJavaCompletionProposal)it.next();
                        IJavaElement element = p.getJavaElement();
                        if (element instanceof IType)
                            proposed.add(((IType)element).getFullyQualifiedName());
                        relevance = Math.min(relevance, p.getRelevance());
                    }

                    // insert history types
                    List<String> history =
                            WorkerMessageHandler.get().getContentAssistHistory().getHistory(expectedType.getFullyQualifiedName())
                                                .getTypes();
                    relevance -= history.size() + 1;
                    for (Iterator<String> it = history.iterator(); it.hasNext(); ) {
                        String type = it.next();
                        if (proposed.contains(type))
                            continue;

                        JavaCompletionProposal proposal =
                                createTypeProposal(relevance, type, collector.getInvocationContext());

                        if (proposal != null)
                            types.add(proposal);
                        relevance++;
                    }
                }
            }

            List<JavaCompletionProposal> templateProposals =
                    templateCompletionProposalComputer.computeCompletionProposals(collector.getInvocationContext());
            JavaCompletionProposal[] array =
                    templateProposals.toArray(new JavaCompletionProposal[templateProposals.size()]);
            javaCompletionProposals = types.toArray(new JavaCompletionProposal[0]);
            JavaCompletionProposal[] proposals = new JavaCompletionProposal[javaCompletionProposals.length + array.length];
            System.arraycopy(javaCompletionProposals, 0, proposals, 0, javaCompletionProposals.length);
            System.arraycopy(array, 0, proposals, javaCompletionProposals.length, array.length);

            Arrays.sort(proposals, comparator);
            return proposals;
        } catch (AssertionFailedException ex) {
            //todo log errors
//            Log.error(getClass(), ex);
            throw new RuntimeException(ex);

        } catch (Exception ex) {
            //todo log errors
//            Log.error(getClass(), ex);
            throw new RuntimeException(ex);
        }
//        return new JavaCompletionProposal[0];
    }

    private JavaCompletionProposal createTypeProposal(int relevance, String fullyQualifiedType,
                                                      JavaContentAssistInvocationContext context) {
        IType type = WorkerTypeInfoStorage.get().getTypeByFqn(fullyQualifiedType);

        if (type == null)
            return null;

        org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal proposal =
                org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal.create(
                        org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal.TYPE_REF, context.getInvocationOffset());
        proposal.setCompletion(fullyQualifiedType.toCharArray());
        proposal.setDeclarationSignature(Signature.getQualifier(type.getFullyQualifiedName().toCharArray()));
        proposal.setFlags(type.getFlags());
        proposal.setRelevance(relevance);
        proposal.setReplaceRange(context.getInvocationOffset(), context.getInvocationOffset());
        proposal.setSignature(Signature.createTypeSignature(fullyQualifiedType, true).toCharArray());

        return new LazyGenericTypeProposal(proposal, context);

    }
}
