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
package org.eclipse.che.ide.ext.java.client.projecttree.nodes;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil.getSourceFolders;

/**
 * Node that represents a java package.
 *
 * @author Artem Zatsarynnyy
 */
public class PackageNode extends AbstractSourceContainerNode {

    @Inject
    public PackageNode(@Assisted TreeNode<?> parent,
                       @Assisted ItemReference data,
                       @Assisted JavaTreeStructure treeStructure,
                       EventBus eventBus,
                       ProjectServiceClient projectServiceClient,
                       DtoUnmarshallerFactory dtoUnmarshallerFactory,
                       IconRegistry iconRegistry) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
        setDisplayIcon(iconRegistry.getIcon("java.package").getSVGImage());
    }

    @Nonnull
    @Override
    public String getId() {
        return getDisplayName().replace('.', '/');
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        if (getTreeStructure().getSettings().isCompactEmptyPackages()) {
            final String parentPath = ((StorableNode)getParent()).getPath();
            return getPath().replaceFirst(parentPath + "/", "").replace('/', '.');
        }
        return super.getDisplayName();
    }

    /**
     * Returns the full-qualified name of the package.
     *
     * @return the full-qualified name, or an empty string for the default package
     */
    public String getQualifiedName() {
        for (String sourceFolder : getSourceFolders(this)) {
            if (getPath().startsWith(sourceFolder)) {
                return getPath().replaceFirst(sourceFolder, "").replace('/', '.');
            }
        }
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRenamable() {
        // Do not allow to rename package as simple folder.
        // This type of node needs to implement rename refactoring.
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final DeleteCallback callback) {
        super.delete(new DeleteCallback() {
            @Override
            public void onDeleted() {
                callback.onDeleted();

                // if parent package contains one package only then parent may be compacted after deleting this child node
                if (!isCompacted() && getParent() instanceof PackageNode && hasOneChildPackageOnly((PackageNode)getParent())) {
                    eventBus.fireEvent(new RefreshProjectTreeEvent(getParent().getParent()));
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    private boolean isCompacted() {
        return getTreeStructure().getSettings().isCompactEmptyPackages() && getDisplayName().contains(".");
    }

    private boolean hasOneChildPackageOnly(PackageNode pack) {
        Array<TreeNode<?>> children = pack.getChildren();
        return children.size() == 1 && children.get(0) instanceof PackageNode;
    }
}
