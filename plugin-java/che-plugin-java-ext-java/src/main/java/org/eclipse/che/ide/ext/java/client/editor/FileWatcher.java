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
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.java.client.project.node.PackageNode;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.project.event.ResourceNodeDeletedEvent;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;

import java.util.Map;

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

        eventBus.addHandler(ResourceNodeDeletedEvent.getType(), new ResourceNodeDeletedEvent.ResourceNodeDeletedHandler() {
            @Override
            public void onResourceEvent(ResourceNodeDeletedEvent event) {

                ResourceBasedNode node = event.getNode();
                if (node instanceof PackageNode) {
                    worker.removeFqnFromCache(((PackageNode)node).getQualifiedName());
                    reparseAllOpenedFiles();
                } else if (node instanceof FileReferenceNode) {
                    String fqn = getFQN((FileReferenceNode)node);
                    worker.removeFqnFromCache(fqn);
                    reparseAllOpenedFiles();
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
        if (file.getName().endsWith(".java")) {
            if (((FileReferenceNode)file).getParent() instanceof PackageNode) {
                FileReferenceNode fileNode = (FileReferenceNode)file;

                if (fileNode.getParent() != null && fileNode.getParent() instanceof PackageNode) {
                    packageName = ((PackageNode)fileNode.getParent()).getQualifiedName();
                }
            }
        }
        return packageName + file.getName().substring(0, file.getName().indexOf('.'));
    }

    private void reparseAllOpenedFiles() {
        Map<String, EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();
        for (EditorPartPresenter editorPartPresenter : openedEditors.values()) {
            if (editorPartPresenter instanceof EmbeddedTextEditorPresenter) {
                final EmbeddedTextEditorPresenter<?> editor = (EmbeddedTextEditorPresenter<?>)editorPartPresenter;
                editor.refreshEditor();
            }
        }
    }
}
