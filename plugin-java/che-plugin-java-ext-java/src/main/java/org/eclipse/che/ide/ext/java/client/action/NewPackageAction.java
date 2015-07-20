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
package org.eclipse.che.ide.ext.java.client.action;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.generic.ItemNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.JavaResources;
import org.eclipse.che.ide.ext.java.client.JavaUtils;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.AbstractSourceContainerNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.newresource.AbstractNewResourceAction;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nullable;

import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.CREATED;

/**
 * Action to create new Java package.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class NewPackageAction extends AbstractNewResourceAction {
    private final InputValidator nameValidator = new NameValidator();

    @Inject
    public NewPackageAction(JavaResources javaResources, JavaLocalizationConstant localizationConstant) {
        super(localizationConstant.actionNewPackageTitle(),
              localizationConstant.actionNewPackageDescription(),
              javaResources.packageIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        InputDialog inputDialog = dialogFactory.createInputDialog("New " + title, "Name:", new InputCallback() {
            @Override
            public void accepted(String value) {
                onAccepted(value);
            }
        }, null).withValidator(nameValidator);
        inputDialog.show();
    }

    private void onAccepted(String value) {
        final StorableNode parent = getNewResourceParent();
        if (parent == null) {
            throw new IllegalStateException("No selected parent.");
        }

        createPackage(parent, value, new AsyncCallback<ItemReference>() {
            @Override
            public void onSuccess(ItemReference result) {
                final AbstractTreeNode nodeToRefresh = getNodeToRefresh((AbstractSourceContainerNode)parent);
                eventBus.fireEvent(new RefreshProjectTreeEvent(nodeToRefresh));

                fireNodeCreated(result.getPath());
            }

            @Override
            public void onFailure(Throwable caught) {
                if (caught instanceof ServerException) {
                    String message = JsonHelper.parseJsonMessage(caught.getMessage());
                    dialogFactory.createMessageDialog("", message, null).show();
                } else {
                    dialogFactory.createMessageDialog("", caught.getMessage(), null).show();
                }
            }
        });
    }

    private void fireNodeCreated(String path) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No opened project.");
        }

        currentProject.getCurrentTree().getNodeByPath(path, new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                eventBus.fireEvent(new ItemEvent((ItemNode)result, CREATED));
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(NewPackageAction.class, caught);
            }
        });
    }

    private AbstractTreeNode getNodeToRefresh(AbstractSourceContainerNode parent) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No opened project.");
        }

        final TreeStructure currentTree = currentProject.getCurrentTree();
        if (currentTree instanceof JavaTreeStructure && ((JavaTreeStructure)currentTree).getSettings().isCompactEmptyPackages()
            && parent instanceof PackageNode && parent.getChildren().isEmpty()) {
            return (AbstractTreeNode)parent.getParent();
        }
        return parent;
    }

    @Override
    public void updateProjectAction(ActionEvent e) {
        boolean enabled = false;
        Selection<?> selection = selectionAgent.getSelection();
        if (selection != null) {
            enabled = selection.getFirstElement() instanceof AbstractSourceContainerNode;
        }
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    private void createPackage(StorableNode parent, String name, final AsyncCallback<ItemReference> callback) {
        final String path = parent.getPath() + '/' + name.replace('.', '/');
        final Unmarshallable<ItemReference> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class);
        projectServiceClient.createFolder(path, new AsyncRequestCallback<ItemReference>(unmarshaller) {
            @Override
            protected void onSuccess(ItemReference result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    private class NameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            try {
                JavaUtils.checkPackageName(value);
            } catch (final IllegalStateException e) {
                return new Violation() {
                    @Nullable
                    @Override
                    public String getMessage() {
                        return e.getMessage();
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }
}
