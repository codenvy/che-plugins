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
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.ItemHandler;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.SourceFileNode;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class FileWatcher {

    @Inject
    private EditorAgent editorAgent;

    private Set<EmbeddedTextEditorPresenter> editor2reconcile = new HashSet<>();

    @Inject
    private void handleFileOperations(EventBus eventBus) {
        eventBus.addHandler(ItemEvent.TYPE, new ItemHandler() {
            @Override
            public void onItem(ItemEvent event) {
                if (event.getOperation() == ItemEvent.ItemOperation.DELETED) {
                    if (event.getItem() instanceof SourceFileNode) {
                        reparseAllOpenedFiles();
                    } else if (event.getItem() instanceof PackageNode) {
                        reparseAllOpenedFiles();
                    }

                }
            }
        });
        eventBus.addHandler(ActivePartChangedEvent.TYPE, new ActivePartChangedHandler() {
            @Override
            public void onActivePartChanged(ActivePartChangedEvent event) {
                if (event.getActivePart() instanceof EmbeddedTextEditorPresenter) {
                    if (editor2reconcile.contains(event.getActivePart())) {
                        reParseEditor((EmbeddedTextEditorPresenter<?>)event.getActivePart());
                    }
                }
            }
        });
    }

    private void reParseEditor(EmbeddedTextEditorPresenter<?> editor) {
        editor.refreshEditor();
        editor2reconcile.remove(editor);
    }


    public void editorOpened(final EditorPartPresenter editor) {
        final PropertyListener propertyListener = new PropertyListener() {
            @Override
            public void propertyChanged(PartPresenter source, int propId) {
                if (propId == EditorPartPresenter.PROP_DIRTY) {
                    if (!editor.isDirty()) {
                        reparseAllOpenedFiles();
                        //remove just saved editor
                        editor2reconcile.remove((EmbeddedTextEditorPresenter)editor);
                    }
                }
            }
        };
        editor.addPropertyListener(propertyListener);
    }



    private void reparseAllOpenedFiles() {
        editorAgent.getOpenedEditors().iterate(new StringMap.IterationCallback<EditorPartPresenter>() {
            @Override
            public void onIteration(final String s, final EditorPartPresenter editorPartPresenter) {
                if (editorPartPresenter instanceof EmbeddedTextEditorPresenter) {
                    final EmbeddedTextEditorPresenter< ? > editor = (EmbeddedTextEditorPresenter< ? >)editorPartPresenter;
                    editor2reconcile.add(editor);
                }
            }
        });
    }
}
