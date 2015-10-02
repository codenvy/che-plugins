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
package org.eclipse.che.ide.ext.java.client.refactoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;

/**
 * Utility class for the refactoring operations.
 * It is needed for refreshing the project tree, updating content of the opening editors.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class RefactoringUpdater {
    private final EditorAgent              editorAgent;
    private final SelectionAgent           selectionAgent;
    private final EventBus                 eventBus;
    private final NotificationManager      notificationManager;
    private final AppContext               appContext;
    private final ProjectExplorerPresenter projectExplorer;

    private String pathToMove;

    @Inject
    public RefactoringUpdater(EditorAgent editorAgent,
                              SelectionAgent selectionAgent,
                              EventBus eventBus,
                              NotificationManager notificationManager,
                              AppContext appContext,
                              ProjectExplorerPresenter projectExplorer) {
        this.editorAgent = editorAgent;
        this.selectionAgent = selectionAgent;
        this.eventBus = eventBus;
        this.notificationManager = notificationManager;
        this.appContext = appContext;
        this.projectExplorer = projectExplorer;
    }

    /** Refreshes all open editors. */
    public void refreshOpenEditors() {
        Promise<Void> promise = Promises.resolve(null);

        for (final EditorPartPresenter editor : editorAgent.getOpenedEditors().values()) {
            final VirtualFile file = editor.getEditorInput().getFile();

            promise.thenPromise(getContent(file))
                   .thenPromise(updateEditor(editor))
                   .catchErrorPromise(onGetContentFailed(file));

//            String path = file.getPath();
//            file.getContent(new AsyncCallback<String>() {
//                @Override
//                public void onFailure(Throwable caught) {
//                    reopenMovedFile(file);
//                }
//
//                @Override
//                public void onSuccess(String result) {
//                    Document document = ((EmbeddedTextEditorPresenter)editor).getDocument();
//                    document.replace(0, document.getContents().length(), result);
//                }
//            });
        }
    }

    private Function<Void, Promise<String>> getContent(final VirtualFile file) {
        return new Function<Void, Promise<String>>() {
            @Override
            public Promise<String> apply(Void arg) throws FunctionException {
                return file.getContent();
            }
        };
    }

    private Function<String, Promise<Object>> updateEditor(final EditorPartPresenter editor) {
        return new Function<String, Promise<Object>>() {
            @Override
            public Promise<Object> apply(String content) throws FunctionException {
                Document document = ((EmbeddedTextEditorPresenter)editor).getDocument();
                document.replace(0, document.getContents().length(), content);

                return Promises.resolve(null);
            }
        };
    }

    private Function<PromiseError, Promise<Object>> onGetContentFailed(final VirtualFile file) {
        return new Function<PromiseError, Promise<Object>>() {
            @Override
            public Promise<Object> apply(PromiseError arg) throws FunctionException {
                reopenMovedFile(file);
                return Promises.resolve(null);
            }
        };
    }

    private Function<Node, Promise<Object>> openFile() {
        return new Function<Node, Promise<Object>>() {
            @Override
            public Promise<Object> apply(Node node) throws FunctionException {
                if (node instanceof FileReferenceNode) {
                    eventBus.fireEvent(new FileEvent((FileReferenceNode)node, FileEvent.FileOperation.OPEN));
                }
                return Promises.resolve(null);
            }
        };
    }

    /** Sets destinations for moving resources. */
    public void setPathToMove(String pathToMove) {
        this.pathToMove = pathToMove;
    }

    /** Refreshes project tree. */
    public void refreshProjectTree() {
        projectExplorer.reloadChildren();
//        List<?> selectionItems = selectionAgent.getSelection().getAllElements();
//        for (Object selectionItem : selectionItems) {
//
//            eventBus.fireEvent(new RefreshProjectTreeEvent(((StorableNode)selectionItem).getParent()));
//        }
//
//        appContext.getCurrentProject().getCurrentTree().getNodeByPath(pathToMove, new AsyncCallback<TreeNode<?>>() {
//            @Override
//            public void onFailure(Throwable caught) {
//                notificationManager.showError("Can not find: " + pathToMove);
//            }
//
//            @Override
//            public void onSuccess(TreeNode<?> result) {
//                eventBus.fireEvent(new RefreshProjectTreeEvent(result.getParent()));
//            }
//        });
    }

    private Promise<Object> reopenMovedFile(final VirtualFile file) {
        eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));

        final String newPathToFile = pathToMove + file.getPath().substring(file.getPath().lastIndexOf('/'));

        return projectExplorer.getNodeByPath(new HasStorablePath.StorablePath(newPathToFile)).thenPromise(openFile());

//        appContext.getCurrentProject().getCurrentTree().getNodeByPath(newPathToFile, new AsyncCallback<TreeNode<?>>() {
//            @Override
//            public void onFailure(Throwable caught) {
//                notificationManager.showError("Can not find a file: " + newPathToFile);
//            }
//
//            @Override
//            public void onSuccess(TreeNode<?> result) {
//                if (result instanceof FileNode) {
//                    eventBus.fireEvent(new FileEvent((FileNode)result, FileEvent.FileOperation.OPEN));
//                }
//            }
//        });
    }
}
