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

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.refactoring.service.RefactoringServiceClient;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateRenameRefactoring;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.LinkedRenameRefactoringApply;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatusEntry;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RenameRefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.LinkedModeModel;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateRenameRefactoring.RenameType.JAVA_ELEMENT;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.ERROR;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.FATAL;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.INFO;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.OK;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.WARNING;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrinko
 */
@RunWith(GwtMockitoTestRunner.class)
public class JavaRefactoringRenameTest {

    private static final String TEXT                 = "some text for test";
    private static final String PATH                 = "to/be/or/not/to/be";
    private static final String JAVA_CLASS__NAME     = "JavaTest";
    private static final String JAVA_CLASS_FULL_NAME = JAVA_CLASS__NAME + ".java";
    private static final String NEW_JAVA_CLASS_NAME  = "NewJavaTest.java";
    private static final String SESSION_ID           = "some session id";
    private static final int    CURSOR_OFFSET        = 10;


    //variables for constructor
    @Mock
    private EditorAgent              editorAgent;
    @Mock
    private JavaLocalizationConstant locale;
    @Mock
    private DialogFactory            dialogFactory;
    @Mock
    private RefactoringServiceClient refactoringServiceClient;
    @Mock
    private ProjectServiceClient     projectServiceClient;
    @Mock
    private DtoFactory               dtoFactory;
    @Mock
    private AppContext               appContext;
    @Mock
    private EventBus                 eventBus;
    @Mock
    private DtoUnmarshallerFactory   unmarshallerFactory;
    @Mock
    private NotificationManager      notificationManager;

    @Mock
    private CreateRenameRefactoring           createRenameRefactoringDto;
    @Mock
    private LinkedRenameRefactoringApply      linkedRenameRefactoringApplyDto;
    @Mock
    private TextEditor                        textEditor;
    @Mock
    private EditorInput                       editorInput;
    @Mock
    private VirtualFile                       virtualFile;
    @Mock
    private CurrentProject                    currentProject;
    @Mock
    private ProjectDescriptor                 projectDescriptor;
    @Mock
    private Promise<RenameRefactoringSession> createRenamePromise;
    @Mock
    private RenameRefactoringSession          session;
    @Mock
    private LinkedModeModel                   linkedModel;
    @Mock
    private InputDialog                       dialog;
    @Mock
    private Promise<RefactoringStatus>        applyModelPromise;
    @Mock
    private RefactoringStatus                 status;
    @Mock
    private EditorPartPresenter               activeEditor;
    @Mock
    private RefactoringStatusEntry            entry;

    @Captor
    private ArgumentCaptor<Operation<RenameRefactoringSession>> renameRefCaptor;
    @Captor
    private ArgumentCaptor<InputCallback>                       inputArgumentCaptor;
    @Captor
    private ArgumentCaptor<Operation<RefactoringStatus>>        refactoringStatusCaptor;

    @InjectMocks
    private JavaRefactoringRename refactoringRename;

    @Before
    public void setUp() {
        when(dtoFactory.createDto(CreateRenameRefactoring.class)).thenReturn(createRenameRefactoringDto);
        when(dtoFactory.createDto(LinkedRenameRefactoringApply.class)).thenReturn(linkedRenameRefactoringApplyDto);
        when(textEditor.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(virtualFile);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getPath()).thenReturn(TEXT);
        when(virtualFile.getName()).thenReturn(JAVA_CLASS_FULL_NAME);
        when(refactoringServiceClient.createRenameRefactoring(createRenameRefactoringDto)).thenReturn(createRenamePromise);
        when(session.getLinkedModeModel()).thenReturn(linkedModel);
        when(session.getSessionId()).thenReturn(SESSION_ID);
        when(locale.renameDialogTitle()).thenReturn(TEXT);
        when(locale.renameDialogLabel()).thenReturn(TEXT);
        when(dialogFactory.createInputDialog(eq(TEXT), eq(TEXT), any(InputCallback.class), isNull(CancelCallback.class)))
                .thenReturn(dialog);
        when(refactoringServiceClient.applyLinkedModeRename(linkedRenameRefactoringApplyDto)).thenReturn(applyModelPromise);
        when(editorAgent.getActiveEditor()).thenReturn(activeEditor);
        when(activeEditor.getEditorInput()).thenReturn(editorInput);
        when(virtualFile.getPath()).thenReturn(PATH);
        when(textEditor.getCursorOffset()).thenReturn(CURSOR_OFFSET);

        when(status.getEntries()).thenReturn(Collections.singletonList(entry));
    }

    @Test
    public void renameRefactoringShouldBeAppliedSuccess() throws OperationException {
        when(status.getSeverity()).thenReturn(OK);

        refactoringRename.refactor(textEditor);

        mainCheckRenameRefactoring();

        verify(editorAgent).getActiveEditor();
        verify(activeEditor).getEditorInput();
        verify(virtualFile).getPath();
    }

    @Test
    public void renameRefactoringShouldBeFailedByFatalError() throws OperationException {
        when(status.getSeverity()).thenReturn(FATAL);

        refactoringRename.refactor(textEditor);

        mainCheckRenameRefactoring();
        verify(entry).getMessage();
        verify(notificationManager).showError(anyString());
    }

    @Test
    public void renameRefactoringShouldBeFailedByError() throws OperationException {
        when(status.getSeverity()).thenReturn(ERROR);

        refactoringRename.refactor(textEditor);

        mainCheckRenameRefactoring();
        verify(entry).getMessage();
        verify(notificationManager).showError(anyString());
    }

    @Test
    public void renameRefactoringShouldBeWithWarning() throws OperationException {
        when(status.getSeverity()).thenReturn(WARNING);

        refactoringRename.refactor(textEditor);

        mainCheckRenameRefactoring();
        verify(entry).getMessage();
        verify(notificationManager).showWarning(anyString());
    }

    @Test
    public void renameRefactoringShouldBeWithINFO() throws OperationException {
        when(status.getSeverity()).thenReturn(INFO);

        refactoringRename.refactor(textEditor);

        mainCheckRenameRefactoring();
        verify(entry).getMessage();
        verify(notificationManager).showInfo(anyString());
    }

    private void mainCheckRenameRefactoring() throws OperationException {
        verify(dtoFactory).createDto(CreateRenameRefactoring.class);
        verify(textEditor).getCursorOffset();
        verify(createRenameRefactoringDto).setOffset(CURSOR_OFFSET);
        verify(createRenameRefactoringDto).setRefactorLightweight(true);
        verify(textEditor).getEditorInput();
        verify(editorInput).getFile();
        verify(createRenameRefactoringDto).setPath(JAVA_CLASS__NAME);
        verify(createRenameRefactoringDto).setProjectPath(TEXT);
        verify(appContext).getCurrentProject();
        verify(currentProject).getProjectDescription();
        verify(projectDescriptor).getPath();
        verify(createRenameRefactoringDto).setProjectPath(TEXT);
        verify(createRenameRefactoringDto).setType(JAVA_ELEMENT);

        verify(refactoringServiceClient).createRenameRefactoring(createRenameRefactoringDto);
        verify(createRenamePromise).then(renameRefCaptor.capture());
        renameRefCaptor.getValue().apply(session);
        verify(session).isMastShowWizard();
        verify(session).getLinkedModeModel();
        verify(session).getSessionId();

        verify(dialogFactory).createInputDialog(eq(TEXT), eq(TEXT), inputArgumentCaptor.capture(), isNull(CancelCallback.class));
        inputArgumentCaptor.getValue().accepted(NEW_JAVA_CLASS_NAME);
        verify(dtoFactory).createDto(LinkedRenameRefactoringApply.class);
        linkedRenameRefactoringApplyDto.setNewName(NEW_JAVA_CLASS_NAME);
        linkedRenameRefactoringApplyDto.setSessionId(SESSION_ID);

        verify(refactoringServiceClient).applyLinkedModeRename(linkedRenameRefactoringApplyDto);

        verify(applyModelPromise).then(refactoringStatusCaptor.capture());
        refactoringStatusCaptor.getValue().apply(status);

        verify(status).getSeverity();
    }
}
