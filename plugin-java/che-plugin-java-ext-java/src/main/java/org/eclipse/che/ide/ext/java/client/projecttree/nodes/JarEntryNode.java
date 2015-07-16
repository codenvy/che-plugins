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

import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

/**
 * @author Evgen Vidolob
 */
public abstract class JarEntryNode extends AbstractTreeNode<JarEntry> {

    protected final JavaNavigationService  service;
    protected final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final   String                 displayName;
    protected       int                    libId;

    /**
     * Creates new node with the specified parent, associated data and display name.
     *
     * @param parent
     *         parent node
     * @param data
     *         an object this node encapsulates
     * @param javaTreeStructure
     *         {@link org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure} which this node belongs
     * @param eventBus
     */
    public JarEntryNode(TreeNode<?> parent, JarEntry data, JavaTreeStructure javaTreeStructure,
                        EventBus eventBus, int libId, JavaNavigationService service,
                        DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(parent, data, javaTreeStructure, eventBus);
        this.libId = libId;
        this.service = service;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        if (data.getName().endsWith(".class")) {
            displayName = data.getName().substring(0, data.getName().lastIndexOf(".class"));
        } else {
            displayName = data.getName();
        }
    }

    @Nonnull
    @Override
    public String getId() {
        return getData().getName();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }
}
