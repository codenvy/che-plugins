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

import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.ItemHandler;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.SourceFileNode;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class FileWatcher {

    @Inject
    private JavaParserWorker worker;

    @Inject
    private EditorAgent editorAgent;

    @Inject
    private void handleFileOperations(EventBus eventBus) {
        eventBus.addHandler(ItemEvent.TYPE, new ItemHandler() {
            @Override
            public void onItem(ItemEvent event) {
                if (event.getOperation() == ItemEvent.ItemOperation.DELETED) {
                    if (event.getItem() instanceof SourceFileNode) {
                        String fqn = getFQN(((SourceFileNode)event.getItem()));
                        worker.removeFqnFromCache(fqn);
                        reparseAllOpenedFiles();
                    } else if (event.getItem() instanceof PackageNode) {
                        worker.removeFqnFromCache(((PackageNode)event.getItem()).getQualifiedName());
                        reparseAllOpenedFiles();
                    }

                }
            }
        });
    }


    public void editorOpened(final EditorPartPresenter editor) {
        final PropertyListener propertyListener = new PropertyListener() {
            @Override
            public void propertyChanged(PartPresenter source, int propId) {
                if (propId == EditorPartPresenter.PROP_DIRTY) {
                    if (!editor.isDirty()) {
                        VirtualFile file = editor.getEditorInput().getFile();
                        String fqn = getFQN(file);
                        worker.removeFqnFromCache(fqn);
                        reparseAllOpenedFiles();
                    }
                }
            }
        };
        editor.addPropertyListener(propertyListener);
        editor.addCloseHandler(new EditorPartPresenter.EditorPartCloseHandler() {
            @Override
            public void onClose(EditorPartPresenter editor) {
                worker.fileClosed(editor.getEditorInput().getFile().getPath());
            }
        });
    }

    private String getFQN(VirtualFile file) {
        String packageName = "";
        if(file instanceof SourceFileNode) {
            if (((SourceFileNode)file).getParent() instanceof PackageNode) {
                packageName = ((PackageNode)((SourceFileNode)file).getParent()).getQualifiedName() + '.';
            }
        }
        return packageName + file.getName().substring(0, file.getName().indexOf('.'));
    }

    private void reparseAllOpenedFiles() {
        editorAgent.getOpenedEditors().iterate(new StringMap.IterationCallback<EditorPartPresenter>() {
            @Override
            public void onIteration(final String s, final EditorPartPresenter editorPartPresenter) {
                if (editorPartPresenter instanceof EmbeddedTextEditorPresenter) {
                    final EmbeddedTextEditorPresenter< ? > editor = (EmbeddedTextEditorPresenter< ? >)editorPartPresenter;
                    editor.refreshEditor();
                }
            }
        });
    }
}
