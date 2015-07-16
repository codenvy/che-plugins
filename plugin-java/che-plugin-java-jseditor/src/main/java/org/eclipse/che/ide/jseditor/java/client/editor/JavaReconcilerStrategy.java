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

import org.eclipse.che.ide.api.build.BuildContext;
import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.text.Region;
import org.eclipse.che.ide.api.texteditor.outline.OutlineModel;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.client.editor.JavaParserWorker;
import org.eclipse.che.ide.ext.java.client.editor.outline.OutlineUpdater;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.JarClassNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.SourceFileNode;
import org.eclipse.che.ide.ext.java.jdt.core.IProblemRequestor;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem;
import org.eclipse.che.ide.jseditor.client.annotation.AnnotationModel;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.reconciler.DirtyRegion;
import org.eclipse.che.ide.jseditor.client.reconciler.ReconcilingStrategy;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.util.loging.Log;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.validation.constraints.NotNull;

public class JavaReconcilerStrategy implements ReconcilingStrategy, JavaParserWorker.WorkerCallback<IProblem> {


    private final BuildContext                   buildContext;
    private final EmbeddedTextEditorPresenter<?> editor;

    private final JavaParserWorker         worker;
    private final OutlineModel             outlineModel;
    private final JavaCodeAssistProcessor  codeAssistProcessor;
    private final AnnotationModel          annotationModel;

    private VirtualFile      file;
    private Document document;
    private boolean first = true;
    private boolean sourceFromClass;

    @AssistedInject
    public JavaReconcilerStrategy(@Assisted @NotNull final EmbeddedTextEditorPresenter<?> editor,
                                  @Assisted final OutlineModel outlineModel,
                                  @Assisted final JavaCodeAssistProcessor codeAssistProcessor,
                                  @Assisted final AnnotationModel annotationModel,
                                  final BuildContext buildContext,
                                  final JavaParserWorker worker) {
        this.editor = editor;
        this.buildContext = buildContext;
        this.worker = worker;
        this.outlineModel = outlineModel;
        this.codeAssistProcessor = codeAssistProcessor;
        this.annotationModel = annotationModel;

    }

    @Override
    public void setDocument(final EmbeddedDocument document) {
        this.document = document;
        file = editor.getEditorInput().getFile();
        sourceFromClass = file instanceof JarClassNode;
        new OutlineUpdater(file.getPath(), outlineModel, worker);
    }

    @Override
    public void reconcile(final DirtyRegion dirtyRegion, final Region subRegion) {
        parse();
    }

    public void parse() {
        if (this.buildContext.isBuilding()) {
            return;
        }
        if (first) {
            codeAssistProcessor.disableCodeAssistant();
            first = false;
        }

        String packageName = "";
        if (file instanceof SourceFileNode) {
            if (((SourceFileNode)file).getParent() instanceof PackageNode) {
                packageName = ((PackageNode)((SourceFileNode)file).getParent()).getQualifiedName();
            }
        }

        worker.parse(document.getContents(), file.getName(), file.getPath(), packageName, file.getProject().getPath(), false, this);
    }

    @Override
    public void reconcile(final Region partition) {
        parse();
    }

    public VirtualFile getFile() {
        return file;
    }

    @Override
    public void onResult(final Array<IProblem> problems) {
        if (!first) {
            codeAssistProcessor.enableCodeAssistant();
        }

        if (this.annotationModel == null) {
            return;
        }
        IProblemRequestor problemRequestor;
        if (this.annotationModel instanceof IProblemRequestor) {
            problemRequestor = (IProblemRequestor)this.annotationModel;
            problemRequestor.beginReporting();
        } else {
            editor.setErrorState(EditorWithErrors.EditorState.NONE);
            return;
        }
        try {
            boolean error = false;
            boolean warning = false;
            for (IProblem problem : problems.asIterable()) {
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
