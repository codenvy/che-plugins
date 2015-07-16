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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.che.ide.api.text.Position;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.java.client.JavaCss;
import org.eclipse.che.ide.ext.java.client.JavaResources;
import org.eclipse.che.ide.ext.java.client.editor.ProblemAnnotation;
import org.eclipse.che.ide.ext.java.jdt.core.IProblemRequestor;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem;
import org.eclipse.che.ide.jseditor.client.annotation.AnnotationModel;
import org.eclipse.che.ide.jseditor.client.annotation.AnnotationModelImpl;
import org.eclipse.che.ide.jseditor.client.partition.DocumentPositionMap;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorResources;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorResources.EditorCss;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * An annotation model for java sources.
 */
public class JavaAnnotationModel extends AnnotationModelImpl implements AnnotationModel, IProblemRequestor {

    private List<IProblem> reportedProblems;

    private List<ProblemAnnotation> generatedAnnotations = new ArrayList<>();

    private final JavaCss javaCss;
    private final EditorCss editorCss;

    @AssistedInject
    public JavaAnnotationModel(@Assisted final DocumentPositionMap docPositionMap,
                               final JavaResources javaResources,
                               final EditorResources editorResources) {
        super(docPositionMap);
        this.javaCss = javaResources.css();
        this.editorCss = editorResources.editorCss();
    }

    protected Position createPositionFromProblem(final IProblem problem) {
        int start = problem.getSourceStart();
        int end = problem.getSourceEnd();

        if (start == -1 && end == -1) {
            return new Position(0);
        }

        if (start == -1) {
            return new Position(end);
        }

        if (end == -1) {
            return new Position(start);
        }

        int length = end - start + 1;
        if (length < 0) {
            return null;
        }
        return new Position(start, length);
    }

    @Override
    public void acceptProblem(final IProblem problem) {
        reportedProblems.add(problem);
    }

    @Override
    public void beginReporting() {
        reportedProblems = new ArrayList<>();
    }

    @Override
    public void endReporting() {
        reportProblems(reportedProblems);
    }

    private void reportProblems(final List<IProblem> problems) {
        boolean temporaryProblemsChanged = false;

        if (!generatedAnnotations.isEmpty()) {
            temporaryProblemsChanged = true;
            super.clear();
            generatedAnnotations.clear();
        }

        if (reportedProblems != null && !reportedProblems.isEmpty()) {

            for (final IProblem problem : reportedProblems) {
                final Position position = createPositionFromProblem(problem);

                if (position != null) {
                    final ProblemAnnotation annotation = new ProblemAnnotation(problem);
                    addAnnotation(annotation, position, false);
                    generatedAnnotations.add(annotation);

                    temporaryProblemsChanged = true;
                }
            }
        }

        if (temporaryProblemsChanged) {
            fireModelChanged();
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public StringMap<String> getAnnotationDecorations() {
        final StringMap<String> decorations = Collections.createStringMap();
        // TODO configure this
        decorations.put("org.eclipse.jdt.ui.error", this.editorCss.lineError());
        decorations.put("org.eclipse.jdt.ui.warning", this.editorCss.lineWarning());

        return decorations;
    }

    @Override
    public StringMap<String> getAnnotationStyle() {
        final StringMap<String> decorations = Collections.createStringMap();
        // //TODO configure this
        decorations.put("org.eclipse.jdt.ui.error", javaCss.overviewMarkError());
        decorations.put("org.eclipse.jdt.ui.warning", javaCss.overviewMarkWarning());
        decorations.put("org.eclipse.jdt.ui.info", javaCss.overviewMarkTask());
        decorations.put("org.eclipse.ui.workbench.texteditor.task", javaCss.overviewMarkTask());
        return decorations;
    }
}
