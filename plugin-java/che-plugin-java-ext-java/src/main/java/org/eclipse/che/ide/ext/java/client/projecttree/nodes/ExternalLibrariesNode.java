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
import org.eclipse.che.ide.api.project.tree.generic.Openable;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgen Vidolob
 */
public class ExternalLibrariesNode extends AbstractTreeNode<Object> implements Openable {
    private JavaTreeStructure      treeStructure;
    private JavaNavigationService  service;
    private DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private boolean                opened;

    /**
     * Creates new node with the specified parent, associated data and display name.
     *
     * @param parent
     *         parent node
     * @param data
     *         an object this node encapsulates
     * @param eventBus
     */
    @Inject
    ExternalLibrariesNode(@Assisted JavaProjectNode parent, @Assisted Object data, @Assisted JavaTreeStructure treeStructure,
                          EventBus eventBus, IconRegistry iconRegistry, JavaNavigationService service,
                          DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(parent, data, treeStructure, eventBus);
        this.treeStructure = treeStructure;
        this.service = service;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        setDisplayIcon(iconRegistry.getIcon("java.libraries").getSVGImage());
    }

    @Nonnull
    @Override
    public String getId() {
        return "External Libraries";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "External Libraries";
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void refreshChildren(final AsyncCallback<TreeNode<?>> callback) {
        Unmarshallable<List<Jar>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(Jar.class);
        service.getExternalLibraries(getParent().getProject().getPath(), new AsyncRequestCallback<List<Jar>>(unmarshaller) {
            @Override
            protected void onSuccess(List<Jar> result) {
                List<TreeNode<?>> array = new ArrayList<>();
                for (Jar jar : result) {
                    array.add(treeStructure.newJarNode(ExternalLibrariesNode.this, jar));
                }
                setChildren(array);
                callback.onSuccess(ExternalLibrariesNode.this);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }


    @Override
    public void close() {
        opened = false;
    }

    @Override
    public boolean isOpened() {
        return opened;
    }

    @Override
    public void open() {
        opened = true;
    }
}
