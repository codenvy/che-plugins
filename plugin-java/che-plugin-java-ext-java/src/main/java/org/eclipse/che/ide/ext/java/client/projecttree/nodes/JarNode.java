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

import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

/**
 * @author Evgen Vidolob
 */
public class JarNode extends AbstractTreeNode<Jar> {

    private JavaTreeStructure      treeStructure;
    private JavaNavigationService  service;
    private DtoUnmarshallerFactory factory;

    /**
     * Creates new node with the specified parent, associated data and display name.
     *  @param parent
     *         parent node
     * @param data
     *         an object this node encapsulates
     * @param javaTreeStructure
     *         {@link JavaTreeStructure} which this node belongs
     * @param eventBus
     */
    @Inject
    public JarNode(@Assisted ExternalLibrariesNode parent, @Assisted Jar data, @Assisted JavaTreeStructure javaTreeStructure,
                   EventBus eventBus, JavaNavigationService service, DtoUnmarshallerFactory factory, IconRegistry registry) {
        super(parent, data, javaTreeStructure, eventBus);
        treeStructure = javaTreeStructure;
        this.service = service;
        this.factory = factory;
        setDisplayIcon(registry.getIcon("java.jar").getSVGImage());
    }

    @Nonnull
    @Override
    public String getId() {
        return String.valueOf(getData().getId());
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void refreshChildren(final AsyncCallback<TreeNode<?>> callback) {
        Unmarshallable<Array<JarEntry>> unmarshaller = factory.newArrayUnmarshaller(JarEntry.class);
        service.getLibraryChildren(getParent().getProject().getPath(), getData().getId(),
                                   new AsyncRequestCallback<Array<JarEntry>>(unmarshaller) {
                                       @Override
                                       protected void onSuccess(Array<JarEntry> result) {
                Array<TreeNode<?>> nodes = Collections.createArray();
                for (JarEntry jarNode : result.asIterable()) {
                    nodes.add(createNode(jarNode));
                }
                setChildren(nodes);
                callback.onSuccess(JarNode.this);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    private TreeNode<?> createNode(JarEntry entry) {
        switch (entry.getType()){
            case FOLDER:
            case PACKAGE:
            return treeStructure.newJarContainerNode(this, entry, getData().getId());

            case FILE:
                return treeStructure.newJarFileNode(this, entry, getData().getId());

            case CLASS_FILE:
                return treeStructure.newJarClassNode(this, entry, getData().getId());
        }
        return null;
    }
}
