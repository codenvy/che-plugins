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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;

/**
 * Action for launch rename refactoring of java files
 * @author Alexander Andrienko
 */
@Singleton
public class RenameRefactoringAction extends Action {

    private final EditorAgent           editorAgent;
    private final JavaRefactoringRename javaRefactoringRename;
    private final AppContext            appContext;

    @Inject
    public RenameRefactoringAction(EditorAgent editorAgent,
                                   JavaLocalizationConstant locale,
                                   JavaRefactoringRename javaRefactoringRename,
                                   AppContext appContext) {
        super(locale.renameRefactoringActionName(), "");
        this.editorAgent = editorAgent;
        this.javaRefactoringRename = javaRefactoringRename;
        this.appContext = appContext;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final EditorPartPresenter editorPart = editorAgent.getActiveEditor();
        final TextEditor textEditorPresenter;

        if (editorPart instanceof TextEditor) {
            textEditorPresenter = (TextEditor)editorPart;
        } else {
            return;
        }
        javaRefactoringRename.refactor(textEditorPresenter);
    }

    @Override
    public void update(ActionEvent event) {
        CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            event.getPresentation().setEnabled(false);
            return;
        }
        //todo Warning:we need improve this code for multi-module projects
        boolean isJavaProject = currentProject.getRootProject().getAttributes().get("language").contains("java");
        EditorPartPresenter editorPart = editorAgent.getActiveEditor();
        if (isJavaProject && editorPart != null && editorPart instanceof TextEditor && ((TextEditor)editorPart).isFocused()) {
            VirtualFile virtualFile = editorPart.getEditorInput().getFile();
            String mediaType = virtualFile.getMediaType();

            if (mediaType != null && ((mediaType.equals(MimeType.TEXT_X_JAVA) ||
                    mediaType.equals(MimeType.TEXT_X_JAVA_SOURCE) ||
                    mediaType.equals(MimeType.APPLICATION_JAVA_CLASS))))
                event.getPresentation().setEnabled(true);

        } else {
            event.getPresentation().setEnabled(false);
        }
    }
}
