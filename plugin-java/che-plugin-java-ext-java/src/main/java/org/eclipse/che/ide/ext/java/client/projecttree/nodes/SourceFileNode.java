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
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

/**
 * Node that represents a Java source file (class, interface, enum, etc.).
 *
 * @author Artem Zatsarynnyy
 */
public class SourceFileNode extends FileNode {

    @Inject
    public SourceFileNode(@Assisted TreeNode<?> parent,
                          @Assisted ItemReference data,
                          @Assisted JavaTreeStructure treeStructure,
                          EventBus eventBus,
                          ProjectServiceClient projectServiceClient,
                          DtoUnmarshallerFactory dtoUnmarshallerFactory,
                          EditorAgent editorAgent) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory, editorAgent);
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        final String name = getName();
        if (getTreeStructure().getSettings().isShowExtensionForJavaFiles()) {
            return name;
        }
        return name.substring(0, name.length() - "java".length() - 1);
    }

    @Nonnull
    @Override
    public JavaTreeStructure getTreeStructure() {
        return (JavaTreeStructure)super.getTreeStructure();
    }

    @Override
    public boolean isRenamable() {
        // Do not allow to rename Java source file as simple file.
        // This type of node needs to implement rename refactoring.
        return false;
    }

    @Override
    public void delete(final DeleteCallback callback) {
        super.delete(new DeleteCallback() {
            @Override
            public void onDeleted() {
                callback.onDeleted();

                // if parent contains one package only after deleting this child node then parent should be compacted
                if (isCompacted() && getParent() instanceof PackageNode && hasOneChildPackageOnly((PackageNode)getParent())) {
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
        return getTreeStructure().getSettings().isCompactEmptyPackages();
    }

    private boolean hasOneChildPackageOnly(PackageNode pack) {
        Array<TreeNode<?>> children = pack.getChildren();
        return children.size() == 1 && children.get(0) instanceof PackageNode;
    }
}
