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

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.messages.JavadocHandleComputed;
import org.eclipse.che.ide.ext.java.shared.OpenDeclarationDescriptor;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class OpenDeclarationFinder {

    private final JavaParserWorker            worker;
    private final EditorAgent                 editorAgent;
    private final JavaNavigationService       service;
    private       DtoUnmarshallerFactory      factory;
    private       AppContext                  context;
    private final NewProjectExplorerPresenter projectExplorer;
    private final JavaNodeManager             javaNodeManager;

    @Inject
    public OpenDeclarationFinder(JavaParserWorker worker,
                                 EditorAgent editorAgent,
                                 JavaNavigationService service,
                                 DtoUnmarshallerFactory factory,
                                 AppContext context,
                                 NewProjectExplorerPresenter projectExplorer,
                                 JavaNodeManager javaNodeManager) {
        this.worker = worker;
        this.editorAgent = editorAgent;
        this.service = service;
        this.factory = factory;
        this.context = context;
        this.projectExplorer = projectExplorer;
        this.javaNodeManager = javaNodeManager;
    }

    public void openDeclaration() {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor == null) {
            return;
        }

        if (!(activeEditor instanceof EmbeddedTextEditorPresenter)) {
            Log.error(getClass(), "Open Declaration support only EmbeddedTextEditorPresenter as editor");
            return;
        }
        EmbeddedTextEditorPresenter editor = ((EmbeddedTextEditorPresenter)activeEditor);
        int offset = editor.getCursorOffset();
        final VirtualFile file = editor.getEditorInput().getFile();
        worker.computeJavadocHandle(offset, file.getPath(), new JavaParserWorker.Callback<JavadocHandleComputed>() {
            @Override
            public void onCallback(JavadocHandleComputed result) {
                if (result != null) {
                    handle(result, file);
                }
            }
        });
    }

    private void handle(JavadocHandleComputed result, VirtualFile file) {
        if (result.getOffset() != -1 && result.isSource()) {
            EditorPartPresenter editorPartPresenter = editorAgent.getActiveEditor();
            fileOpened(editorPartPresenter, result.getOffset());
        } else {
            sendRequest(result.getKey(), file.getProject());
        }
    }

    private void sendRequest(String bindingKey, HasProjectDescriptor project) {
        Unmarshallable<OpenDeclarationDescriptor> unmarshaller =
                factory.newUnmarshaller(OpenDeclarationDescriptor.class);
        service.findDeclaration(project.getProjectDescriptor().getPath(), bindingKey,
                                new AsyncRequestCallback<OpenDeclarationDescriptor>(unmarshaller) {
                                    @Override
                                    protected void onSuccess(OpenDeclarationDescriptor result) {
                                        if (result != null) {
                                            handleDescriptor(result);
                                        }
                                    }

                                    @Override
                                    protected void onFailure(Throwable exception) {
                                        Log.error(OpenDeclarationFinder.class, exception);
                                    }
                                });
    }

    private void handleDescriptor(final OpenDeclarationDescriptor descriptor) {
        Map<String, EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();
        for (String s : openedEditors.keySet()) {
            if (descriptor.getPath().equals(s)) {
                EditorPartPresenter editorPartPresenter = openedEditors.get(s);
                editorAgent.activateEditor(editorPartPresenter);
                fileOpened(editorPartPresenter, descriptor.getOffset());
                return;
            }
        }

        if (descriptor.isBinary()) {
            javaNodeManager.getClassNode(context.getCurrentProject().getProjectDescription(), descriptor.getLibId(), descriptor.getPath())
                           .then(new Operation<Node>() {
                               @Override
                               public void apply(Node node) throws OperationException {
                                   if (node instanceof VirtualFile) {
                                       openFile((VirtualFile)node, descriptor);
                                   }
                               }
                           });
        } else {
            HasStorablePath path = new HasStorablePath() {
                @Nonnull
                @Override
                public String getStorablePath() {
                    return descriptor.getPath();
                }
            };

            projectExplorer.navigate(path, true).then(new Operation<Node>() {
                @Override
                public void apply(Node node) throws OperationException {
                    if (node instanceof VirtualFile) {
                        openFile((VirtualFile)node, descriptor);
                    }
                }
            });
        }
    }

    private void openFile(VirtualFile result, final OpenDeclarationDescriptor descriptor) {
        editorAgent.openEditor(result, new EditorAgent.OpenEditorCallback() {
            @Override
            public void onEditorOpened(EditorPartPresenter editor) {
                fileOpened(editor, descriptor.getOffset());
            }
        });
    }

    private void fileOpened(EditorPartPresenter editor, int offset) {
        if (editor instanceof EmbeddedTextEditorPresenter) {
            ((EmbeddedTextEditorPresenter)editor).getDocument().setSelectedRange(
                    LinearRange.createWithStart(offset).andLength(0), true);
        }
    }
}
