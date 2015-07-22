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

import com.google.inject.Inject;

import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.texteditor.UndoableEditor;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.formatter.ContentFormatter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

/**
 * ContentFormatter implementation
 *
 * @author Roman Nikitenko
 */
public class JavaFormatter implements ContentFormatter {

    private JavaCodeAssistClient    service;
    private EditorAgent            editorAgent;
    private DtoUnmarshallerFactory dtoUnmarshallerFactory;

    @Inject
    public JavaFormatter(JavaCodeAssistClient service,
                         EditorAgent editorAgent,
                         DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.service = service;
        this.editorAgent = editorAgent;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    @Override
    public void format(final Document document) {
        int offset = document.getSelectedLinearRange().getStartOffset();
        int length = document.getSelectedLinearRange().getLength();

        if (length <= 0 || offset < 0) {
            offset = 0;
            length = document.getContentsCharCount();
        }
        Unmarshallable<Array<Change>> unmarshaller = dtoUnmarshallerFactory.newArrayUnmarshaller(Change.class);
        service.format(offset, length, document.getContents(), new AsyncRequestCallback<Array<Change>>(unmarshaller) {
            @Override
            protected void onSuccess(Array<Change> result) {
                applyChanges(result, document);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Log.error(getClass(), exception);
            }
        });
    }

    private void applyChanges(Array<Change> changes, Document document) {
        HandlesUndoRedo undoRedo = null;
        EditorPartPresenter editorPartPresenter = editorAgent.getActiveEditor();
        if (editorPartPresenter instanceof UndoableEditor) {
            undoRedo = ((UndoableEditor)editorPartPresenter).getUndoRedo();
        }
        try {
            if (undoRedo != null) {
                undoRedo.beginCompoundChange();
            }
            for (Change change : changes.asIterable()) {
                document.replace(change.getOffset(), change.getLength(), change.getText());
            }
        } catch (final Exception e) {
            Log.error(getClass(), e);
        } finally {
            if (undoRedo != null) {
                undoRedo.endCompoundChange();
            }
        }
    }
}
