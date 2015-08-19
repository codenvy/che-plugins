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
package org.eclipse.che.ide.extension.maven.client.projecttree;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.JavaProjectNode;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Node that represents Maven project.
 *
 * @author Artem Zatsarynnyy
 */
public class MavenProjectNode extends JavaProjectNode {

    @Inject
    public MavenProjectNode(@Assisted TreeNode<?> parent,
                            @Assisted ProjectDescriptor data,
                            @Assisted MavenProjectTreeStructure treeStructure,
                            EventBus eventBus,
                            ProjectServiceClient projectServiceClient,
                            DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);

        List<String> packaging = data.getAttributes().get(MavenAttributes.PACKAGING);
        if (packaging != null && !packaging.isEmpty()) {
            shouldAddExternalLibrariesNode = !packaging.get(0).equals("pom");
        }
    }

    @Nonnull
    @Override
    public MavenProjectTreeStructure getTreeStructure() {
        return (MavenProjectTreeStructure)super.getTreeStructure();
    }

    /** {@inheritDoc} */
    @Override
    public void refreshChildren(final AsyncCallback<TreeNode<?>> callback) {
        getModules(getData(), new AsyncCallback<List<ProjectDescriptor>>() {
            @Override
            public void onSuccess(final List<ProjectDescriptor> modules) {
                getChildren(getData().getPath(), new AsyncCallback<List<ItemReference>>() {
                    @Override
                    public void onSuccess(List<ItemReference> childItems) {
                        setChildren(getChildNodesForItems(childItems, modules));
                        callback.onSuccess(MavenProjectNode.this);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }
                });
            }

            @Override
            public void onFailure(Throwable caught) {
                //can be if pom.xml not found
                getChildren(getData().getPath(), new AsyncCallback<List<ItemReference>>() {
                    @Override
                    public void onSuccess(List<ItemReference> childItems) {
                        callback.onSuccess(MavenProjectNode.this);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }
                });
                callback.onFailure(caught);
            }
        });
    }

    /**
     * Method helps to retrieve modules of the specified project using Codenvy Project API.
     *
     * @param project
     *         project to retrieve its modules
     * @param callback
     *         callback to return retrieved modules
     */
    protected void getModules(ProjectDescriptor project, final AsyncCallback<List<ProjectDescriptor>> callback) {
        final Unmarshallable<List<ProjectDescriptor>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.getModules(project.getPath(), new AsyncRequestCallback<List<ProjectDescriptor>>(unmarshaller) {
            @Override
            protected void onSuccess(List<ProjectDescriptor> result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    private List<TreeNode<?>> getChildNodesForItems(List<ItemReference> childItems, List<ProjectDescriptor> modules) {
        List<TreeNode<?>> oldChildren = getChildren();
        List<TreeNode<?>> newChildren =  new ArrayList<>();
        for (ItemReference item : childItems) {
            AbstractTreeNode node = createChildNode(item, modules);
            if (node != null) {
                if (oldChildren.contains(node)) {
                    final int i = oldChildren.indexOf(node);
                    newChildren.add(oldChildren.get(i));
                } else {
                    newChildren.add(node);
                }
            }
        }
        return newChildren;
    }

    /**
     * Creates node for the specified item. Method called for every child item in {@link #refreshChildren(AsyncCallback)} method.
     * <p/>
     * May be overridden in order to provide a way to create a node for the specified by.
     *
     * @param item
     *         {@link ItemReference} for which need to create a node
     * @param modules
     *         modules list to identify specified item as a project's module
     * @return new node instance or <code>null</code> if the specified item is not supported
     */
    @Nullable
    protected AbstractTreeNode<?> createChildNode(ItemReference item, List<ProjectDescriptor> modules) {
        if ("project".equals(item.getType())) {
            ProjectDescriptor module = getModule(item, modules);
            if (module != null) {
                return getTreeStructure().newModuleNode(this, module);
            }
            // if project isn't a module - show it as folder
            return getTreeStructure().newJavaFolderNode(MavenProjectNode.this, item);
        } else if (JavaSourceFolderUtil.isSourceFolder(item, getProject())) {
            return getTreeStructure().newSourceFolderNode(MavenProjectNode.this, item);
        } else if ("folder".equals(item.getType())) {
            return getTreeStructure().newJavaFolderNode(MavenProjectNode.this, item);
        } else {
            return super.createChildNode(item, modules);
        }
    }

    /**
     * Returns the module descriptor that corresponds to the specified folderItem
     * or null if the folderItem does not correspond to any module from the specified list.
     */
    @Nullable
    private ProjectDescriptor getModule(ItemReference folderItem, List<ProjectDescriptor> modules) {
        if ("project".equals(folderItem.getType())) {
            for (ProjectDescriptor module : modules) {
                if (folderItem.getName().equals(module.getName())) {
                    return module;
                }
            }
        }
        return null;
    }
}
