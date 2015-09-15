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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.HasStorablePath.StorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ext.java.client.project.node.PackageNode;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import java.util.Arrays;
import java.util.List;

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

    private final NewProjectExplorerPresenter projectExplorer;
    private final NewJavaSourceFileView       view;
    private final ProjectServiceClient        projectServiceClient;
    private final DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    private final DialogFactory               dialogFactory;
    private final List<JavaSourceFileType>    sourceFileTypes;
    private final AppContext                  appContext;

    @Inject
    public NewJavaSourceFilePresenter(NewJavaSourceFileView view,
                                      NewProjectExplorerPresenter projectExplorer,
                                      ProjectServiceClient projectServiceClient,
                                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                      DialogFactory dialogFactory,
                                      AppContext appContext) {
        this.appContext = appContext;
        sourceFileTypes = Arrays.asList(CLASS, INTERFACE, ENUM, ANNOTATION);
        this.view = view;
        this.projectExplorer = projectExplorer;
        this.projectServiceClient = projectServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
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

            FolderReferenceNode parent = (FolderReferenceNode)projectExplorer.getSelection().getHeadElement();
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

    private void createClass(String name, FolderReferenceNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public class " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private void createInterface(String name, FolderReferenceNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public interface " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private void createEnum(String name, FolderReferenceNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public enum " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private void createAnnotation(String name, FolderReferenceNode parent, String packageFragment) {
        String content = getPackageQualifier(parent, packageFragment) +
                         "public @interface " + name + DEFAULT_CONTENT;

        createSourceFile(name, parent, packageFragment, content);
    }

    private String getPackageQualifier(FolderReferenceNode parent, String packageFragment) {
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

    private void createSourceFile(final String nameWithoutExtension, final FolderReferenceNode parent, String packageFragment,
                                  final String content) {
        final String parentPath = parent.getStorablePath() + (packageFragment.isEmpty() ? "" : '/' + packageFragment.replace('.', '/'));
        ensureFolderExists(parentPath, new AsyncCallback<ItemReference>() {
            @Override
            public void onSuccess(ItemReference result) {
                createAndOpenFile(nameWithoutExtension, result, parent,  content);
            }

            @Override
            public void onFailure(Throwable caught) {
                dialogFactory.createMessageDialog("", caught.getMessage(), null).show();
            }
        });
    }

    /** Creates folder by the specified path if it doesn't exists. */
    private void ensureFolderExists(String path, final AsyncCallback<ItemReference> callback) {
        projectServiceClient.createFolder(path, new AsyncRequestCallback<ItemReference>(dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class)) {
            @Override
            protected void onSuccess(ItemReference result) {
                callback.onSuccess(result);
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

    private void createAndOpenFile(String nameWithoutExtension, ItemReference parent, FolderReferenceNode node, String content) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No opened project.");
        }

        final String fileName = nameWithoutExtension + ".java";

        projectServiceClient.createFile(parent != null ? parent.getPath() : node.getStorablePath(),
                                        fileName,
                                        content,
                                        null,
                                        createCallback(node));
    }

    protected AsyncRequestCallback<ItemReference> createCallback(final ResourceBasedNode<?> parent) {
        return new AsyncRequestCallback<ItemReference>(dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class)) {
            @Override
            protected void onSuccess(final ItemReference itemReference) {
                HasStorablePath path = new StorablePath(itemReference.getPath());

                projectExplorer.getNodeByPath(path)
                               .then(selectNode())
                               .then(openNode());
            }

            @Override
            protected void onFailure(Throwable exception) {
                dialogFactory.createMessageDialog("", JsonHelper.parseJsonMessage(exception.getMessage()), null).show();
            }
        };
    }

    protected Function<Node, Node> selectNode() {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node) throws FunctionException {
                projectExplorer.select(node, false);

                return node;
            }
        };
    }

    protected Function<Node, Node> openNode() {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node) throws FunctionException {
                if (node instanceof FileReferenceNode) {
                    ((FileReferenceNode)node).actionPerformed();
                }

                return node;
            }
        };
    }
}
