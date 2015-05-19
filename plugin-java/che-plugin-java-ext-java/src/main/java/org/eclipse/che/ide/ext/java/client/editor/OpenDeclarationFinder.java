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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.ext.java.shared.OpenDeclarationDescriptor;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class OpenDeclarationFinder {

    private final EditorAgent            editorAgent;
    private final JavaNavigationService  service;
    private       DtoUnmarshallerFactory factory;
    private       JavaNavigationService  navigationService;
    private       AppContext             context;

    @Inject
    public OpenDeclarationFinder(EditorAgent editorAgent, JavaNavigationService service,
                                 DtoUnmarshallerFactory factory, JavaNavigationService navigationService, AppContext context) {
        this.editorAgent = editorAgent;
        this.service = service;
        this.factory = factory;
        this.navigationService = navigationService;
        this.context = context;
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
        Unmarshallable<OpenDeclarationDescriptor> unmarshaller =
                factory.newUnmarshaller(OpenDeclarationDescriptor.class);
        service.findDeclaration(file.getProject().getPath(), JavaSourceFolderUtil.getFQNForFile(file), offset,
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

//    private void handle(JavadocHandleComputed result, VirtualFile file) {
//        if (result.getOffset() != -1 && result.isSource()) {
//           EditorPartPresenter editorPartPresenter = editorAgent.getActiveEditor();
//           fileOpened(editorPartPresenter, result.getOffset());
//        } else {
//            sendRequest(result.getKey(), file.getProject());
//        }
//    }

//    private void sendRequest(String bindingKey, ProjectNode project) {
//        Unmarshallable<OpenDeclarationDescriptor> unmarshaller =
//                factory.newUnmarshaller(OpenDeclarationDescriptor.class);
//        service.findDeclaration(project.getPath(), bindingKey, new AsyncRequestCallback<OpenDeclarationDescriptor>(unmarshaller) {
//            @Override
//            protected void onSuccess(OpenDeclarationDescriptor result) {
//                if (result != null) {
//                    handleDescriptor(result);
//                }
//            }
//
//            @Override
//            protected void onFailure(Throwable exception) {
//                Log.error(OpenDeclarationFinder.class, exception);
//            }
//        });
//    }

    private void handleDescriptor(final OpenDeclarationDescriptor descriptor) {
        StringMap<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();
        for (String s : openedEditors.getKeys().asIterable()) {
            if (descriptor.getPath().equals(s)) {
                EditorPartPresenter editorPartPresenter = openedEditors.get(s);
                editorAgent.activateEditor(editorPartPresenter);
                fileOpened(editorPartPresenter, descriptor.getOffset());
                return;
            }
        }


        TreeStructure tree = context.getCurrentProject().getCurrentTree();
        if (descriptor.isBinary()) {
            if (tree instanceof JavaTreeStructure) {
                ((JavaTreeStructure)tree)
                        .getClassFileByPath(context.getCurrentProject().getProjectDescription().getPath(), descriptor.getLibId(),
                                            descriptor.getPath(), new AsyncCallback<TreeNode<?>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Log.error(OpenDeclarationFinder.class, caught);
                            }

                            @Override
                            public void onSuccess(TreeNode<?> result) {
                                if (result instanceof VirtualFile) {
                                    openFile((VirtualFile)result, descriptor);
                                }
                            }
                        });
            }
        } else {
            tree.getNodeByPath(descriptor.getPath(), new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.error(OpenDeclarationFinder.class, caught);
                }

                @Override
                public void onSuccess(TreeNode<?> result) {
                    if (result instanceof VirtualFile) {
                        openFile((VirtualFile)result, descriptor);
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
