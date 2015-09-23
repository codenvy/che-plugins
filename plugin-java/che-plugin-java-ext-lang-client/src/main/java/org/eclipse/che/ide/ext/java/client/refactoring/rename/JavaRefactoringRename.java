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
package org.eclipse.che.ide.ext.java.client.refactoring.rename;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.callback.PromiseHelper;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.refactoring.service.RefactoringServiceClient;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateRenameRefactoring;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.LinkedRenameRefactoringApply;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatusEntry;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RenameRefactoringSession;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateRenameRefactoring.RenameType.JAVA_ELEMENT;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.ERROR;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.FATAL;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.INFO;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.OK;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.WARNING;

/**
 * Class for rename refactoring java classes
 * @author Alexander Andrienko
 */
@Singleton
public class JavaRefactoringRename {
    private final EditorAgent              editorAgent;
    private final JavaLocalizationConstant locale;
    private final DialogFactory            dialogFactory;
    private final RefactoringServiceClient refactoringServiceClient;
    private final ProjectServiceClient     projectServiceClient;
    private final DtoFactory               dtoFactory;
    private final AppContext               appContext;
    private final EventBus                 eventBus;
    private final DtoUnmarshallerFactory   unmarshallerFactory;
    private final NotificationManager      notificationManager;

    @Inject
    public JavaRefactoringRename(EditorAgent editorAgent,
                                 JavaLocalizationConstant locale,
                                 DialogFactory dialogFactory,
                                 RefactoringServiceClient refactoringServiceClient,
                                 ProjectServiceClient projectServiceClient,
                                 DtoFactory dtoFactory,
                                 AppContext appContext,
                                 EventBus eventBus,
                                 DtoUnmarshallerFactory unmarshallerFactory,
                                 NotificationManager notificationManager) {
        this.editorAgent = editorAgent;
        this.locale = locale;
        this.dialogFactory = dialogFactory;
        this.refactoringServiceClient = refactoringServiceClient;
        this.projectServiceClient = projectServiceClient;
        this.dtoFactory = dtoFactory;
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.unmarshallerFactory = unmarshallerFactory;
        this.notificationManager = notificationManager;
    }

    /**
     * Launch java rename refactoring process
     * @param textEditorPresenter editor where user refactors code
     */
    public void refactor(TextEditor textEditorPresenter) {
        final CreateRenameRefactoring createRenameRefactoring = createRenameRefactoringDto(textEditorPresenter, appContext);

        Promise<RenameRefactoringSession> createRenamePromise = refactoringServiceClient.createRenameRefactoring(createRenameRefactoring);
        createRenamePromise.then(new Operation<RenameRefactoringSession>() {
            @Override
            public void apply(RenameRefactoringSession session) throws OperationException {
                if (session.isMastShowWizard()) {
                    //todo should add Wizard
                } else if (session.getLinkedModeModel() != null) {
                    activateLinkedModeIntoEditor(session.getSessionId());
                }
            }
        });
    }

    private void activateLinkedModeIntoEditor(final String session) {
        //todo Warning: temporary decision, now we using dialog box instead of linked mode into editor!
        dialogFactory.createInputDialog(locale.renameDialogTitle(),
                locale.renameDialogLabel(),
                new InputCallback() {
                    @Override
                    public void accepted(final String newName) {
                        final LinkedRenameRefactoringApply dto = createLinkedRenameRefactoringApplyDto(newName, session);

                        Promise<RefactoringStatus> applyModelPromise = refactoringServiceClient.applyLinkedModeRename(dto);
                        applyModelPromise.then(new Operation<RefactoringStatus>() {
                            @Override
                            public void apply(RefactoringStatus status) throws OperationException {
                                onTargetRenamed(status, newName);
                            }
                        });
                    }
                }, null).show();
    }

    private void onTargetRenamed(RefactoringStatus status, String newName) {
        switch (status.getSeverity()) {
            case OK:
                updateAfterRefactoring(newName);
                break;
            case INFO:
                notificationManager.showInfo(getNotification(status));
                break;
            case WARNING:
                notificationManager.showWarning(getNotification(status));
                break;
            case ERROR:
                notificationManager.showError(getNotification(status));
                break;
            case FATAL:
                notificationManager.showError(getNotification(status));
        }
    }

    private String getNotification(RefactoringStatus status) {
        StringBuilder notificationMessage = new StringBuilder();
        for (RefactoringStatusEntry entry: status.getEntries()) {
            String message = JsonHelper.parseJsonMessage(entry.getMessage());
            notificationMessage.append(message);
            notificationMessage.append(". ");
        }
        return notificationMessage.toString();
    }

    private void updateAfterRefactoring(String newName) {
        final VirtualFile virtualFile = editorAgent.getActiveEditor().getEditorInput().getFile();

        final String oldPath = virtualFile.getPath();
        String parentPath = oldPath.substring(0, oldPath.lastIndexOf("/"));
        final String newPath = parentPath + "/" + newName + ".java";

        //todo Warning: temporary design
        PromiseHelper.newPromise(new AsyncPromiseHelper.RequestCall<ItemReference>() {
            @Override
            public void makeCall(AsyncCallback<ItemReference> callback) {
                projectServiceClient.getItem(newPath, newCallback(callback, unmarshallerFactory.newUnmarshaller(ItemReference.class)));
            }
        }).then(new Operation<ItemReference>() {
            @Override
            public void apply(ItemReference reference) throws OperationException {
                ((FileNode)virtualFile).setData(reference);
                editorAgent.updateEditorNode(oldPath, virtualFile);
                updateAllEditors();
                eventBus.fireEvent(new RefreshProjectTreeEvent());
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                updateAllEditors();
                eventBus.fireEvent(new RefreshProjectTreeEvent());
            }
        });
    }

    private void updateAllEditors() {
        for (EditorPartPresenter editor : editorAgent.getOpenedEditors().values()) {
            String path = editor.getEditorInput().getFile().getPath();
            eventBus.fireEvent(new FileContentUpdateEvent(path));
        }
    }

    @NotNull
    private CreateRenameRefactoring createRenameRefactoringDto(TextEditor editor, AppContext appContext) {
        CreateRenameRefactoring dto = dtoFactory.createDto(CreateRenameRefactoring.class);

        dto.setOffset(editor.getCursorOffset());
        dto.setRefactorLightweight(true);

        String fqn = JavaSourceFolderUtil.getFQNForFile(editor.getEditorInput().getFile());
        dto.setPath(fqn);

        String projectPath = appContext.getCurrentProject().getProjectDescription().getPath();
        dto.setProjectPath(projectPath);

        dto.setType(JAVA_ELEMENT);

        return dto;
    }

    @NotNull
    private LinkedRenameRefactoringApply createLinkedRenameRefactoringApplyDto(String newName, String sessionId) {
        LinkedRenameRefactoringApply dto = dtoFactory.createDto(LinkedRenameRefactoringApply.class);
        dto.setNewName(newName);
        dto.setSessionId(sessionId);
        return dto;
    }
}
