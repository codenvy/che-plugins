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
package org.eclipse.che.ide.ext.java.client.editor;

import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Jso;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.collections.js.JsoStringMap;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.che.ide.ext.java.messages.CAProposalsComputedMessage;
import org.eclipse.che.ide.ext.java.messages.ComputeJavadocHandle;
import org.eclipse.che.ide.ext.java.messages.FileClosedMessage;
import org.eclipse.che.ide.ext.java.messages.FormatResultMessage;
import org.eclipse.che.ide.ext.java.messages.JavadocHandleComputed;
import org.eclipse.che.ide.ext.java.messages.Problem;
import org.eclipse.che.ide.ext.java.messages.ProblemLocationMessage;
import org.eclipse.che.ide.ext.java.messages.ProblemsMessage;
import org.eclipse.che.ide.ext.java.messages.ProposalAppliedMessage;
import org.eclipse.che.ide.ext.java.messages.RoutingTypes;
import org.eclipse.che.ide.ext.java.messages.WorkerProposal;
import org.eclipse.che.ide.ext.java.messages.impl.MessagesImpls;
import org.eclipse.che.ide.ext.java.messages.impl.OutlineUpdateMessage;
import org.eclipse.che.ide.ext.java.messages.impl.WorkerCodeBlock;
import org.eclipse.che.ide.ext.java.jdt.text.edits.CopySourceEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.CopyTargetEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.CopyingRangeMarker;
import org.eclipse.che.ide.ext.java.jdt.text.edits.DeleteEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.InsertEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MoveSourceEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MoveTargetEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MultiTextEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.RangeMarker;
import org.eclipse.che.ide.ext.java.jdt.text.edits.ReplaceEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.TextEdit;
import org.eclipse.che.ide.rest.RestContext;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.webworker.client.ErrorEvent;
import com.google.gwt.webworker.client.ErrorHandler;
import com.google.gwt.webworker.client.MessageEvent;
import com.google.gwt.webworker.client.MessageHandler;
import com.google.gwt.webworker.client.Worker;
import com.google.gwt.webworker.client.messages.MessageFilter;
import com.google.gwt.webworker.client.messages.MessageImpl;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Java parser worker impl.
 *
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 */
public class JavaParserWorkerImpl implements JavaParserWorker, ProjectActionHandler, MessageFilter.MessageRecipient<ProblemsMessage> {

    private final MessageFilter                  messageFilter;
    private final String                         workspaceId;
    private       String                         javaCAPath;
    private       Worker                         worker;
    private       String                         restContext;
    private       Map<String, WorkerCallback<?>> arrayCallbacks;
    private Map<String, WorkerCallback<WorkerCodeBlock>> outlineCallbacks = new HashMap<>();
    private Map<String, Callback<?>>                     callbacks        = new HashMap<>();


    @Inject
    public JavaParserWorkerImpl(EventBus eventBus, @RestContext String restContext,
                                @Named("workspaceId") String workspaceId, @Named("javaCA") String javaCAPath) {
        this.restContext = restContext;
        this.workspaceId = workspaceId;
        this.javaCAPath = javaCAPath;
        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        messageFilter = new MessageFilter();
        arrayCallbacks = new HashMap<>();
        messageFilter.registerMessageRecipient(RoutingTypes.PROBLEMS, this);
        messageFilter.registerMessageRecipient(RoutingTypes.CA_PROPOSALS_COMPUTED,
                                               new MessageFilter.MessageRecipient<CAProposalsComputedMessage>() {
                                                   @Override
                                                   public void onMessageReceived(CAProposalsComputedMessage message) {
                                                       handleCAComputed(message);
                                                   }
                                               });

        messageFilter
                .registerMessageRecipient(RoutingTypes.CA_PROPOSAL_APPLIED, new MessageFilter.MessageRecipient<ProposalAppliedMessage>() {
                    @Override
                    public void onMessageReceived(ProposalAppliedMessage message) {
                        handleProposalApplied(message);
                    }
                });

        messageFilter
                .registerMessageRecipient(RoutingTypes.OUTLINE_CODE_BLOCKS, new MessageFilter.MessageRecipient<OutlineUpdateMessage>() {
                    @Override
                    public void onMessageReceived(OutlineUpdateMessage message) {
                        handleUpdateOutline(message);
                    }
                });
        messageFilter
                .registerMessageRecipient(RoutingTypes.FORMAT_RESULT, new MessageFilter.MessageRecipient<FormatResultMessage>() {
                    @Override
                    public void onMessageReceived(FormatResultMessage message) {
                        handleFormatApplied(message);
                    }
                });

        messageFilter.registerMessageRecipient(RoutingTypes.JAVADOC_HANDLE_COMPUTED,
                                               new MessageFilter.MessageRecipient<JavadocHandleComputed>() {
                                                   @Override
                                                   public void onMessageReceived(JavadocHandleComputed message) {
                                                       handleJavadocMessage(message);
                                                   }
                                               });
    }

    @SuppressWarnings("unchecked")
    private void handleJavadocMessage(JavadocHandleComputed message) {
        if (callbacks.containsKey(message.getId())) {
            Callback<JavadocHandleComputed> callback = (Callback<JavadocHandleComputed>)callbacks.remove(message.getId());
            callback.onCallback(message);
        }

    }

    @SuppressWarnings("unchecked")
    private void handleFormatApplied(FormatResultMessage message) {
        Callback<TextEdit> callback = (Callback<TextEdit>)callbacks.remove(message.id());
        Jso textEditJso = message.textEdit();
        TextEdit textEdit = convertTextEditFromJso(textEditJso);
        callback.onCallback(textEdit);
    }

    private TextEdit convertTextEditFromJso(Jso editJso) {
        TextEdit textEdit = null;
        String text;
        int length;
        int offSet = editJso.getFieldCastedToInteger("offSet");
        String type = editJso.getStringField("type");

        switch (type) {
            case "ReplaceEdit":
                text = editJso.getStringField("text");
                length = editJso.getFieldCastedToInteger("length");
                textEdit = new ReplaceEdit(offSet, length, text);
                break;
            case "DeleteEdit":
                length = editJso.getFieldCastedToInteger("length");
                textEdit = new DeleteEdit(offSet, length);
                break;
            case "InsertEdit":
                text = editJso.getStringField("text");
                textEdit = new InsertEdit(offSet, text);
                break;
            case "CopyingRangeMarker":
                length = editJso.getFieldCastedToInteger("length");
                textEdit = new CopyingRangeMarker(offSet, length);
                break;
            case "CopySourceEdit":
                length = editJso.getFieldCastedToInteger("length");
                Jso copyTargetEditJso = (Jso)editJso.getObjectField("CopyTargetEdit");

                if (copyTargetEditJso != null) {
                    int offSetCopyTargetEdit = copyTargetEditJso.getFieldCastedToInteger("offSet");

                    CopyTargetEdit copyTargetEdit = new CopyTargetEdit(offSetCopyTargetEdit);
                    textEdit = new CopySourceEdit(offSet, length, copyTargetEdit);
                } else textEdit = new CopySourceEdit(offSet, length);
                break;
            case "MoveSourceEdit":
                length = editJso.getFieldCastedToInteger("length");
                Jso moveTargetEditJso = (Jso)editJso.getObjectField("MoveTargetEdit");

                if (moveTargetEditJso != null) {
                    int offSetMoveTargetEdit = moveTargetEditJso.getFieldCastedToInteger("offSet");
                    MoveTargetEdit moveTargetEdit = new MoveTargetEdit(offSetMoveTargetEdit);
                    textEdit = new MoveSourceEdit(offSet, length, moveTargetEdit);
                } else textEdit = new MoveSourceEdit(offSet, length);
                break;
            case "MoveTargetEdit":
                Jso moveSourceEditJso = (Jso)editJso.getObjectField("MoveSourceEdit");

                if (moveSourceEditJso != null) {
                    int offSetMoveSourceEdit = moveSourceEditJso.getFieldCastedToInteger("offSet");
                    int lengthMoveSourceEdit = moveSourceEditJso.getFieldCastedToInteger("length");
                    MoveSourceEdit moveSourceEdit = new MoveSourceEdit(offSetMoveSourceEdit, lengthMoveSourceEdit);
                    textEdit = new MoveTargetEdit(offSet, moveSourceEdit);
                } else textEdit = new MoveTargetEdit(offSet);
                break;
            case "MultiTextEdit":
                length = editJso.getFieldCastedToInteger("length");
                textEdit = new MultiTextEdit(offSet, length);
                break;
            case "RangeMarker":
                length = editJso.getFieldCastedToInteger("length");
                textEdit = new RangeMarker(offSet, length);
                break;
            case "CopyTargetEdit":
                Jso copySourceEditJso = (Jso)editJso.getObjectField("CopySourceEdit");

                if (copySourceEditJso != null) {
                    int offSetCopySourceEdit = copySourceEditJso.getFieldCastedToInteger("offSet");
                    int lengthCopySourceEdit = copySourceEditJso.getFieldCastedToInteger("length");

                    CopySourceEdit copySourceEdit = new CopySourceEdit(offSetCopySourceEdit, lengthCopySourceEdit);
                    textEdit = new CopyTargetEdit(offSet, copySourceEdit);
                } else textEdit = new CopyTargetEdit(offSet);
                break;
        }
        JsoArray children = editJso.getArrayField("children");

        if (textEdit != null && children != null) {
            textEdit.addChildren(convertChildrenTextEditFromJso(children));
        }
        return textEdit;
    }

    private TextEdit[] convertChildrenTextEditFromJso(JsoArray childrenJso) {
        TextEdit[] edits = new TextEdit[childrenJso.size()];
        for (int i = 0; i < childrenJso.size(); i++) {
            Jso child = (Jso)childrenJso.get(i);
            edits[i] = convertTextEditFromJso(child);
        }
        return edits;
    }

    private void handleUpdateOutline(OutlineUpdateMessage message) {
        if (outlineCallbacks.containsKey(message.getFilePath())) {
            WorkerCallback<WorkerCodeBlock> callback = outlineCallbacks.get(message.getFilePath());
            callback.onResult(message.getBlocks().toList());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleProposalApplied(ProposalAppliedMessage message) {
        if (callbacks.containsKey(message.id())) {
            Callback<ProposalAppliedMessage> callback = (Callback<ProposalAppliedMessage>)callbacks.remove(message.id());
            callback.onCallback(message);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCAComputed(CAProposalsComputedMessage message) {
        if (!arrayCallbacks.containsKey(message.id())) {
            return;
        }

        WorkerCallback<WorkerProposal> callback = (WorkerCallback<WorkerProposal>)arrayCallbacks.remove(message.id());
        callback.onResult(message.proposals().toList());
    }

    @Override
    public void removeFqnFromCache(String fqn) {
        if (worker == null) {
            return;
        }

        MessagesImpls.RemoveFqnMessageImpl message = MessagesImpls.RemoveFqnMessageImpl.make();
        message.setFqn(fqn);
        worker.postMessage(message.serialize());
    }

    @Override
    public void format(int offset, int length, String content, Callback<TextEdit> callback) {
        String uuid = UUID.uuid();
        callbacks.put(uuid, callback);
        MessagesImpls.FormatMessageImpl message = MessagesImpls.FormatMessageImpl.make();
        message.setId(uuid).setOffset(offset).setLength(length).setContent(content);
        worker.postMessage(message.serialize());
    }

    @Override
    public void preferenceFormatSettings(JsoStringMap<String> settings) {
        MessagesImpls.PreferenceFormatSetMessageImpl message = MessagesImpls.PreferenceFormatSetMessageImpl.make();
        message.setSettings(settings);
        worker.postMessage(message.serialize());
    }

    @Override
    public void computeJavadocHandle(int offset, String filePath, Callback<JavadocHandleComputed> callback) {
        String uuid = UUID.uuid();
        ComputeJavadocHandle message = ComputeJavadocHandle.make();
        message.setOffset(offset).setId(uuid).setFilePath(filePath);
        callbacks.put(uuid, callback);
        worker.postMessage(message.serialize());
    }

    @Override
    public void fileClosed(String path) {
        FileClosedMessage message = FileClosedMessage.make();
        message.setFilePath(path);
        if (worker != null) {
            worker.postMessage(message.serialize());
        } else {
            Log.error(getClass(), "worker is null");
        }
    }

    @Override
    public void dependenciesUpdated() {
        MessagesImpls.DependenciesUpdatedMessageImpl message = MessagesImpls.DependenciesUpdatedMessageImpl.make();
        worker.postMessage(message.serialize());
    }

    /** {@inheritDoc} */
    @Override
    public void parse(String content, String fileName, String filePath, String packageName, String projectPath,
                      boolean ignoreMethodBody, WorkerCallback<IProblem> callback) {
        if (worker == null) {
            return;
        }

        MessagesImpls.ParseMessageImpl parseMessage = MessagesImpls.ParseMessageImpl.make();
        String uuid = UUID.uuid();
        arrayCallbacks.put(uuid, callback);
        parseMessage.setSource(content).setFileName(fileName).setFilePath(filePath).setId(uuid).setPackageName(packageName)
                    .setProjectPath(projectPath).setIgnoreMethodBodiess(ignoreMethodBody);
        worker.postMessage(parseMessage.serialize());
    }

    /** {@inheritDoc} */
    @Override
    public void computeCAProposals(String content, int offset, String fileName, String projectPath,
                                   String filePath, WorkerCallback<WorkerProposal> callback) {
        if (worker == null) {
            return;
        }

        MessagesImpls.ComputeCAProposalsMessageImpl computeMessage = MessagesImpls.ComputeCAProposalsMessageImpl.make();
        String uuid = UUID.uuid();
        arrayCallbacks.put(uuid, callback);
        computeMessage.setDocContent(content).setOffset(offset).setFileName(fileName).setId(uuid).setProjectPath(projectPath)
                      .setFilePath(filePath);
        worker.postMessage(computeMessage.serialize());
    }

    /** {@inheritDoc} */
    @Override
    public void applyCAProposal(String id, Callback<ProposalAppliedMessage> callback) {
        if (worker == null) {
            return;
        }
        MessagesImpls.ApplyProposalMessageImpl message = MessagesImpls.ApplyProposalMessageImpl.make();
        message.setId(id);
        callbacks.put(id, callback);
        worker.postMessage(message.serialize());
    }

    @Override
    public void addOutlineUpdateHandler(String filePath, WorkerCallback<WorkerCodeBlock> callback) {
        outlineCallbacks.put(filePath, callback);
    }

    @Override
    public void computeQAProposals(String content, int offset, int selectionLength, boolean updatedContent,
                                   List<ProblemLocationMessage> problems, String filePath, WorkerCallback<WorkerProposal> callback) {
        MessagesImpls.ComputeCorrMessageImpl corrMessage = MessagesImpls.ComputeCorrMessageImpl.make();

        corrMessage.setDocumentContent(content)
                   .setDocumentOffset(offset)
                   .setDocumentSelectionLength(selectionLength)
                   .setUpdatedOffset(updatedContent)
                   .setProblemLocations(JsoArray.from(problems))
                   .setFilePath(filePath);

        String uuid = UUID.uuid();
        arrayCallbacks.put(uuid, callback);
        corrMessage.setId(uuid);
        worker.postMessage(corrMessage.serialize());
    }

    @Override
    public void onProjectOpened(ProjectActionEvent event) {
        if (worker != null) {
            worker.terminate();
        }
        try {

            //TODO check project type, create worker only if project is Java
//        worker = Worker.create("http://localhost:8080/ide/_app/javaParserWorker/javaParserWorker.nocache.js");
            worker = Worker.create(GWT.getModuleBaseURL() + "javaParserWorker/javaParserWorker.nocache.js");
            worker.setOnMessage(new MessageHandler() {
                @Override
                public void onMessage(MessageEvent event) {
                    MessageImpl message = event.getDataAsJSO().cast();
                    messageFilter.dispatchMessage(message);
                }
            });
            worker.setOnError(new ErrorHandler() {
                @Override
                public void onError(ErrorEvent event) {
                    Log.error(JavaParserWorkerImpl.class, event.getMessage(), event.getFilename(), event.getLineNumber());
                }
            });

            MessagesImpls.ConfigMessageImpl config = MessagesImpls.ConfigMessageImpl.make();
            config.setRestContext(restContext);
            config.setWsId("/" + workspaceId);
            config.setCaPath(javaCAPath);
            config.setProjectName(event.getProject().getName());
            config.setJavaDocContext(""); //TODO configure doc context
            worker.postMessage(config.serialize());
        } catch (Exception e) {
            Log.error(getClass(), e);
        }
    }

    @Override
    public void onProjectClosing(ProjectActionEvent event) {
    }

    @Override
    public void onProjectClosed(ProjectActionEvent event) {
        if (worker != null) {
            worker.terminate();
            worker = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessageReceived(ProblemsMessage message) {
        if (!arrayCallbacks.containsKey(message.id())) {
            return;
        }
        Array<Problem> problems = message.problems();
        List<IProblem> iProblems = new ArrayList<>();
        for (Problem p : problems.asIterable()) {
            String[] arg = new String[p.stringArguments().size()];
            for (int i = 0; i < p.stringArguments().size(); i++) {
                arg[i] = p.stringArguments().get(i);
            }
            iProblems.add(new DefaultProblem(p.originatingFileName().toCharArray(), p.message(), p.id(), arg, p.severity(),
                                             p.startPosition(),
                                             p.endPosition(), p.line(), p.column()));
        }

        WorkerCallback<IProblem> callback = (WorkerCallback<IProblem>)arrayCallbacks.remove(message.id());
        callback.onResult(iProblems);
    }

}
