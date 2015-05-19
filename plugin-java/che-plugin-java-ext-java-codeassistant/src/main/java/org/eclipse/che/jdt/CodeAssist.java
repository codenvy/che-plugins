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

package org.eclipse.che.jdt;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Singleton;

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.che.ide.ext.java.shared.dto.Problem;
import org.eclipse.che.ide.ext.java.shared.dto.ProposalApplyResult;
import org.eclipse.che.ide.ext.java.shared.dto.ProposalPresentation;
import org.eclipse.che.ide.ext.java.shared.dto.Proposals;
import org.eclipse.che.ide.ext.java.shared.dto.Region;
import org.eclipse.che.jdt.javaeditor.TextViewer;
import org.eclipse.che.jdt.ui.CheActionAcces;
import org.eclipse.che.jface.text.contentassist.ICompletionProposal;
import org.eclipse.che.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.che.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.che.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.core.DocumentAdapter;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.RelevanceSorter;
import org.eclipse.jdt.internal.ui.text.java.TemplateCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class CodeAssist {
    private static final Logger LOG = LoggerFactory.getLogger(CodeAssist.class);
    private final Cache<String, CodeAssistContext> cache;

    public CodeAssist() {
        //todo configure expire time
        cache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).removalListener(
                new RemovalListener<String, CodeAssistContext>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, CodeAssistContext> notification) {
                        if (notification.getValue() != null) {
                            notification.getValue().clean();
                        }
                    }
                }).build();
    }

    public Proposals computeProposals(IJavaProject project, String fqn, int offset, final String content) throws JavaModelException {

        WorkingCopyOwner copyOwner = new WorkingCopyOwner() {
            @Override
            public IBuffer createBuffer(ICompilationUnit workingCopy) {
                return new org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter(workingCopy, workingCopy.getPath(), content);
            }
        };
        ICompilationUnit compilationUnit = null;

        IType type = project.findType(fqn);
        if (type == null) {
            return null;
        }
        if (type.isBinary()) {
            compilationUnit = type.getClassFile().getWorkingCopy(copyOwner, null);
        } else {
            compilationUnit = type.getCompilationUnit().getWorkingCopy(copyOwner, null);
        }

        IBuffer buffer = compilationUnit.getBuffer();
        IDocument document;
        if (buffer instanceof org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter) {
            document = ((org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter)buffer).getDocument();
        } else {
            document = new DocumentAdapter(buffer);
        }
        TextViewer viewer = new TextViewer(document, new Point(offset, 0));
        JavaContentAssistInvocationContext context =
                new JavaContentAssistInvocationContext(viewer, offset, compilationUnit);

        List<ICompletionProposal> proposals = new ArrayList<>();
        proposals.addAll(new JavaAllCompletionProposalComputer().computeCompletionProposals(context, null));
        proposals.addAll(new TemplateCompletionProposalComputer().computeCompletionProposals(context, null));

        Collections.sort(proposals, new RelevanceSorter());
        Proposals result = convertProposals(offset, compilationUnit, viewer, proposals);

        return result;
    }

    private Proposals convertProposals(int offset, ICompilationUnit compilationUnit, TextViewer viewer,
                                       List<ICompletionProposal> proposals) {
        Proposals result = DtoFactory.getInstance().createDto(Proposals.class);
        String sessionId = UUID.randomUUID().toString();
        result.setSessionId(sessionId);

        ArrayList<ProposalPresentation> presentations = new ArrayList<>();
        for (int i = 0; i < proposals.size(); i++) {
            ProposalPresentation presentation = DtoFactory.getInstance().createDto(ProposalPresentation.class);
            ICompletionProposal proposal = proposals.get(i);
            presentation.setIndex(i);
            presentation.setDisplayString(proposal.getDisplayString());
            String image = proposal.getImage() == null ? null : proposal.getImage().getImg();
            presentation.setImage(image);
            if (proposal instanceof ICompletionProposalExtension4) {
                presentation.setAutoInsertable(((ICompletionProposalExtension4)proposal).isAutoInsertable());
            }
            if(proposal instanceof CheActionAcces) {
                String actionId = ((CheActionAcces)proposal).getActionId();
                if(actionId != null){
                    presentation.setActionId(actionId);
                }
            }
            presentations.add(presentation);
        }
        result.setProposals(presentations);
        cache.put(sessionId, new CodeAssistContext(viewer, offset, proposals, compilationUnit));
        return result;
    }

    public ProposalApplyResult applyCompletion(String sessionId, int index, boolean insert) {
        CodeAssistContext context = cache.getIfPresent(sessionId);
        if (context != null) {
            try {
                return context.apply(index, insert);
            } finally {
                cache.invalidate(sessionId);
            }
        } else {
            throw new IllegalArgumentException("CodeAssist context doesn't exist or time of completion was expired");
        }
    }

    @SuppressWarnings("unchecked")
    public Proposals computeAssistProposals(IJavaProject project, String fqn, int offset, List<Problem> problems) throws CoreException {
        ICompilationUnit compilationUnit = null;

        IType type = project.findType(fqn);
        if (type == null) {
            return null;
        }
        if (type.isBinary()) {
            throw new JavaModelException(
                    new JavaModelStatus(IJavaModelStatusConstants.CORE_EXCEPTION, "Can't calculate Quick Assist on binary file"));
        } else {
            compilationUnit = type.getCompilationUnit();
        }

        IBuffer buffer = compilationUnit.getBuffer();

        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        bufferManager.connect(compilationUnit.getPath(), LocationKind.IFILE, new NullProgressMonitor());
        ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(compilationUnit.getPath(), LocationKind.IFILE);
        IDocument document =
                textFileBuffer.getDocument();
//        if (buffer instanceof org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter) {
//            document = ((org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter)buffer).getDocument();
//        } else {
//            document = new DocumentAdapter(buffer);
//        }
        TextViewer viewer = new TextViewer(document, new Point(offset, 0));
        AssistContext context = new AssistContext(compilationUnit, offset, 0);
        ArrayList proposals = new ArrayList<>();
        JavaCorrectionProcessor.collectProposals(context, problems, true, true, proposals);
        return convertProposals(offset, compilationUnit, viewer, proposals);
    }

    private class CodeAssistContext {
        private TextViewer                viewer;
        private int                       offset;
        private List<ICompletionProposal> proposals;
        private ICompilationUnit          cUnit;

        public CodeAssistContext(TextViewer viewer, int offset,
                                 List<ICompletionProposal> proposals, ICompilationUnit cUnit) {
            this.viewer = viewer;
            this.offset = offset;
            this.proposals = proposals;
            this.cUnit = cUnit;
        }

        public void clean() {
            if (cUnit != null) {
                try {
                    cUnit.discardWorkingCopy();
                } catch (JavaModelException e) {
                    //ignore
                }
                try {
                    FileBuffers.getTextFileBufferManager().disconnect(cUnit.getPath(), LocationKind.IFILE, new NullProgressMonitor());
                } catch (CoreException e) {
                    LOG.error("Can't disconnect from file buffer: " + cUnit.getPath(), e);
                }
            }
        }

        public ProposalApplyResult apply(int index, boolean insert) {
            IDocument document = viewer.getDocument();
            final List<Change> changes = new ArrayList<>();
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            document.addDocumentListener(new IDocumentListener() {
                @Override
                public void documentAboutToBeChanged(DocumentEvent event) {
                }

                @Override
                public void documentChanged(DocumentEvent event) {
                    Change dto = dtoFactory.createDto(Change.class);
                    dto.setLength(event.getLength());
                    dto.setOffset(event.getOffset());
                    dto.setText(event.getText());
                    changes.add(dto);
                }
            });
            try {
                char trigger = (char)0;
                int stateMask = insert ? 0 : SWT.CTRL;
                ICompletionProposal p = proposals.get(index);
                if (p instanceof ICompletionProposalExtension2) {
                    ICompletionProposalExtension2 e = (ICompletionProposalExtension2)p;
                    e.apply(viewer, trigger, stateMask, offset);
                } else if (p instanceof ICompletionProposalExtension) {
                    ICompletionProposalExtension e = (ICompletionProposalExtension)p;
                    e.apply(document, trigger, offset);
                } else {
                    p.apply(document);
                }

                ProposalApplyResult result = dtoFactory.createDto(ProposalApplyResult.class);
                result.setChanges(changes);
                Point selection = p.getSelection(document);
                if (selection != null) {
                    Region region = dtoFactory.createDto(Region.class);
                    region.setOffset(selection.x);
                    region.setLength(selection.y);
                    result.setSelection(region);
                }
                return result;


            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Can't find completion: " + index);
            }
        }
    }
}
