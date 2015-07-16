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
package org.eclipse.che.ide.ext.java.client.documentation;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.ext.java.client.editor.JavaParserWorker;
import org.eclipse.che.ide.ext.java.messages.JavadocHandleComputed;
import org.eclipse.che.ide.jseditor.client.position.PositionConverter;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class QuickDocPresenter implements QuickDocumentation, QuickDocView.ActionDelegate {


    private QuickDocView view;
    private AppContext   appContext;
    private String       caContext;
    private EditorAgent  editorAgent;
    private JavaParserWorker worker;

    @Inject
    public QuickDocPresenter(QuickDocView view, AppContext appContext, @Named("javaCA") String caContext, EditorAgent editorAgent,
                             JavaParserWorker worker) {
        this.view = view;
        this.appContext = appContext;
        this.caContext = caContext;
        this.editorAgent = editorAgent;
        this.worker = worker;
    }

    @Override
    public void showDocumentation() {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor == null) {
            return;
        }

        if (!(activeEditor instanceof EmbeddedTextEditorPresenter)) {
            Log.error(getClass(), "Quick Document support only EmbeddedTextEditorPresenter as editor");
            return;
        }

        EmbeddedTextEditorPresenter editor = ((EmbeddedTextEditorPresenter)activeEditor);
        int offset = editor.getCursorOffset();
        final PositionConverter.PixelCoordinates coordinates = editor.getPositionConverter().offsetToPixel(offset);

        worker.computeJavadocHandle(offset, editor.getEditorInput().getFile().getPath(), new JavaParserWorker.Callback<JavadocHandleComputed>() {
            @Override
            public void onCallback(JavadocHandleComputed result) {
                if (result != null) {
                    String key = URL.encodeQueryString(result.getKey());
                    view.show(caContext + "/javadoc/" + appContext.getWorkspace().getId() + "/find?fqn=" + key + "&projectpath=" +
                              appContext.getCurrentProject().getProjectDescription().getPath(), coordinates.getX(), coordinates.getY() + 16);
                }
            }
        });
    }

    @Override
    public void onCloseView() {

    }
}
