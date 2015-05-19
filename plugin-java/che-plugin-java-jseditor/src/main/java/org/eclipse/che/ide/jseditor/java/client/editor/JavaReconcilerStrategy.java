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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.text.Region;
import org.eclipse.che.ide.api.texteditor.outline.OutlineModel;
import org.eclipse.che.ide.ext.java.client.editor.JavaReconcileClient;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.JarClassNode;
import org.eclipse.che.ide.ext.java.shared.dto.Problem;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileResult;
import org.eclipse.che.ide.jseditor.client.annotation.AnnotationModel;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.reconciler.DirtyRegion;
import org.eclipse.che.ide.jseditor.client.reconciler.ReconcilingStrategy;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorResources;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.List;

public class JavaReconcilerStrategy implements ReconcilingStrategy {


    private final EmbeddedTextEditorPresenter<?> editor;
    private final OutlineModel              outlineModel;
    private final JavaCodeAssistProcessor   codeAssistProcessor;
    private final AnnotationModel           annotationModel;
    private final EditorResources.EditorCss editorCss;
    private       SemanticHighlightRenderer highlighter;
    private       JavaReconcileClient       client;
    private       VirtualFile               file;
    private       Document                  document;
    private boolean first = true;
    private boolean sourceFromClass;

    @AssistedInject
    public JavaReconcilerStrategy(@Assisted @NotNull final EmbeddedTextEditorPresenter<?> editor,
                                  @Assisted final OutlineModel outlineModel,
                                  @Assisted final JavaCodeAssistProcessor codeAssistProcessor,
                                  @Assisted final AnnotationModel annotationModel,
                                  final JavaReconcileClient client,
                                  final EditorResources editorResources,
                                  final SemanticHighlightRenderer highlighter) {
        this.editor = editor;
        this.client = client;
        this.outlineModel = outlineModel;
        this.codeAssistProcessor = codeAssistProcessor;
        this.annotationModel = annotationModel;
        this.highlighter = highlighter;
        this.editorCss = editorResources.editorCss();

    }

    @Override
    public void setDocument(final EmbeddedDocument document) {
        this.document = document;
        file = editor.getEditorInput().getFile();
        sourceFromClass = file instanceof JarClassNode;
        highlighter.init(editor.getHasTextMarkers(), document);
//        new OutlineUpdater(file.getPath(), outlineModel, worker);
    }

    @Override
    public void reconcile(final DirtyRegion dirtyRegion, final Region subRegion) {
        parse();
    }

    public void parse() {
//        if (this.buildContext.isBuilding()) {
//            return;
//        }
        if (first) {
            codeAssistProcessor.disableCodeAssistant();
            first = false;
        }


        String fqn = JavaSourceFolderUtil.getFQNForFile(file);
        client.reconcile(file.getProject().getPath(), fqn, new JavaReconcileClient.ReconcileCallback() {
            @Override
            public void onReconcile(ReconcileResult result) {
                doReconcile(result.getProblems());
                highlighter.reconcile(result.getHighlightedPositions());
            }
        });
    }


    @Override
    public void reconcile(final Region partition) {
        parse();
    }

    public VirtualFile getFile() {
        return file;
    }

    private void doReconcile(final List<Problem> problems) {
        if (!first) {
            codeAssistProcessor.enableCodeAssistant();
        }

        if (this.annotationModel == null) {
            return;
        }
        ProblemRequestor problemRequestor;
        if (this.annotationModel instanceof ProblemRequestor) {
            problemRequestor = (ProblemRequestor)this.annotationModel;
            problemRequestor.beginReporting();
        } else {
            editor.setErrorState(EditorWithErrors.EditorState.NONE);
            return;
        }
        try {
            boolean error = false;
            boolean warning = false;
            for (Problem problem : problems) {

                if (!error) {
                    error = problem.isError();
                }
                if (!warning) {
                    warning = problem.isWarning();
                }
                problemRequestor.acceptProblem(problem);
            }
            if (error) {
                editor.setErrorState(EditorWithErrors.EditorState.ERROR);
            } else if (warning) {
                editor.setErrorState(EditorWithErrors.EditorState.WARNING);
            } else {
                editor.setErrorState(EditorWithErrors.EditorState.NONE);
            }
        } catch (final Exception e) {
            Log.error(getClass(), e);
        } finally {
            problemRequestor.endReporting();
        }
    }
}
