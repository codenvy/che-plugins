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
package org.eclipse.che.ide.ext.java.client.newsourcefile;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ItemNode;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.OPEN;
import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.CREATED;
import static org.eclipse.che.ide.ext.java.client.JavaUtils.checkCompilationUnitName;
import static org.eclipse.che.ide.ext.java.client.JavaUtils.checkPackageName;
import static org.eclipse.che.ide.ext.java.client.JavaUtils.isValidCompilationUnitName;
import static org.eclipse.che.ide.ext.java.client.JavaUtils.isValidPackageName;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.ANNOTATION;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.CLASS;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.ENUM;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.INTERFACE;

/**
 * Presenter for creating Java source file.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class NewJavaSourceFilePresenter implements NewJavaSourceFileView.ActionDelegate {
    private static final String DEFAULT_CONTENT = " {\n}\n";

    private final NewJavaSourceFileView     view;
    private final SelectionAgent            selectionAgent;
    private final ProjectServiceClient      projectServiceClient;
    private final DtoUnmarshallerFactory    dtoUnmarshallerFactory;
    private final EventBus                  eventBus;
    private final DialogFactory             dialogFactory;
    private final Array<JavaSourceFileType> sourceFileTypes;
    private final AppContext                appContext;

    @Inject
    public NewJavaSourceFilePresenter(NewJavaSourceFileView view, SelectionAgent selectionAgent, ProjectServiceClient projectServiceClient,
                                      DtoUnmarshallerFactory dtoUnmarshallerFactory, EventBus eventBus, DialogFactory dialogFactory,
                                      AppContext appContext) {
        this.appContext = appContext;
        sourceFileTypes = Collections.createArray(CLASS, INTERFACE, ENUM, ANNOTATION);
        this.view = view;
        this.selectionAgent = selectionAgent;
        this.projectServiceClient = projectServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.eventBus = eventBus;
        this.dialogFactory = dialogFactory;
        this.view.setDelegate(this);
    }

    public void showDialog() {
        view.setTypes(sourceFileTypes);
        view.showDialog();
    }

    @Override
    public void onCancelClicked() {
        view.close();
    }

    @Override
    public void onNameChanged() {
        try {
            final String fileNameWithExtension = getFileNameWithExtension(view.getName());
            if (!fileNameWithExtension.trim().isEmpty()) {
                checkCompilationUnitName(fileNameWithExtension);
            }
            final String packageName = getPackageFragment(view.getName());
            if (!packageName.trim().isEmpty()) {
                checkPackageName(packageName);
            }
            view.hideErrorHint();
        } catch (IllegalStateException e) {
            view.showErrorHint(e.getMessage());
        }
    }

    @Override
    public void onOkClicked() {
        final String fileNameWithExtension = getFileNameWithExtension(view.getName());
        final String fileNameWithoutExtension = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf(".java"));
        final String packageFragment = getPackageFragment(view.getName());

        if (!packageFragment.isEmpty() && !isValidPackageName(packageFragment)) {
            return;
        }
        if (isValidCompilationUnitName(fileNameWithExtension)) {
            view.close();
            final FolderNode parent = (FolderNode)selectionAgent.getSelection().getFirstElement();
            switch (view.getSelectedType()) {
                case CLASS:
                    createClass(fileNameWithoutExtension, parent, packageFragment);
                    break;
                case INTERFACE:
                    createInterface(fileNameWithoutExtension, parent, packageFragment);
                    break;
                case ENUM:
                    createEnum(fileNameWithoutExtension, parent, packageFragment);
                    break;
                case ANNOTATION:
                    createAnnotation(fileNameWithoutExtension, parent, packageFragment);
                    break;
            }
        }
    }

    private String getFileNameWithExtension(String name) {
        if (name.endsWith(".java")) {
            name = name.substring(0, name.lastIndexOf(".java"));
        }
        final int lastDotPos = name.lastIndexOf('.');
        name = name.substring(lastDotPos + 1);
        return name + ".java";
    }

    private String getPackageFragment(String name) {
        if (name.endsWith(".java")) {
            name = name.substring(0, name.lastIndexOf(".java"));
        }
        final int lastDotPos = name.lastIndexOf('.');
        if (lastDotPos >= 0) {
            return name.substring(0, lastDotPos);
        }
        return "";
    }

    private void createClass(String name, FolderNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public class " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private void createInterface(String name, FolderNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public interface " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private void createEnum(String name, FolderNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public enum " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private void createAnnotation(String name, FolderNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public @interface " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private String getPackageQualifier(FolderNode parent, String packageFragment) {
        String packageFQN = "";
        if (parent instanceof PackageNode) {
            packageFQN = ((PackageNode)parent).getQualifiedName();
        }
        if (!packageFragment.isEmpty()) {
            packageFQN = packageFQN.isEmpty() ? packageFragment : packageFQN + '.' + packageFragment;
        }
        if (!packageFQN.isEmpty()) {
            return "package " + packageFQN + ";\n\n";
        }
        return "\n";
    }

    private void createSourceFile(final String nameWithoutExtension, FolderNode parent, String packageFragment, final String content) {
        final String parentPath = parent.getPath() + (packageFragment.isEmpty() ? "" : '/' + packageFragment.replace('.', '/'));
        ensureFolderExists(parentPath, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                createAndOpenFile(nameWithoutExtension, parentPath, content);
            }

            @Override
            public void onFailure(Throwable caught) {
                dialogFactory.createMessageDialog("", caught.getMessage(), null).show();
            }
        });
    }

    /** Creates folder by the specified path if it doesn't exists. */
    private void ensureFolderExists(String path, final AsyncCallback<Void> callback) {
        projectServiceClient.createFolder(path, new AsyncRequestCallback<ItemReference>() {
            @Override
            protected void onSuccess(ItemReference result) {
                callback.onSuccess(null);
            }

            @Override
            protected void onFailure(Throwable exception) {
                if (exception.getMessage().contains("already exists")) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(exception);
                }
            }
        });
    }

    private void createAndOpenFile(String nameWithoutExtension, String parentPath, String content) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No opened project.");
        }

        final String fileName = nameWithoutExtension + ".java";
        final Unmarshallable<ItemReference> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class);
        projectServiceClient.createFile(parentPath, fileName, content, null, new AsyncRequestCallback<ItemReference>(unmarshaller) {
            @Override
            protected void onSuccess(ItemReference result) {
                currentProject.getCurrentTree().getNodeByPath(result.getPath(), new AsyncCallback<TreeNode<?>>() {
                    @Override
                    public void onSuccess(TreeNode<?> result) {
                        if (result != null) {
                            eventBus.fireEvent(new ItemEvent((ItemNode)result, CREATED));
                            eventBus.fireEvent(new FileEvent((VirtualFile)result, OPEN));
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        dialogFactory.createMessageDialog("", caught.getMessage(), null).show();
                    }
                });
            }

            @Override
            protected void onFailure(Throwable exception) {
                if (exception instanceof ServerException) {
                    String message = JsonHelper.parseJsonMessage(exception.getMessage());
                    dialogFactory.createMessageDialog("", message, null).show();
                } else {
                    dialogFactory.createMessageDialog("", exception.getMessage(), null).show();
                }
            }
        });
    }
}
