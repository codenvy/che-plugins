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
package org.eclipse.che.ide.extension.machine.client.command.edit;

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.ui.tree.NodeDataAdapter;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;

import java.util.Collection;

/**
 * Node data adapter for the commands tree.
 *
 * @author Artem Zatsarynnyy
 */
public class CommandDataAdapter implements NodeDataAdapter<CommandDataAdapter.CommandTreeNode> {

    @Override
    public int compare(CommandTreeNode a, CommandTreeNode b) {
        if (a.getData() == null) {
            return 0;
        }
        if (b.getData() == null) {
            return 1;
        }
        return a.getName().compareTo(b.getName());
    }

    @Override
    public boolean hasChildren(final CommandTreeNode node) {
        return node.getChildren() != null && !node.getChildren().isEmpty();
    }

    @Override
    public Array<CommandTreeNode> getChildren(final CommandTreeNode node) {
        Array<CommandTreeNode> children = Collections.createArray();
        for (CommandTreeNode treeNode : node.getChildren()) {
            children.add(treeNode);
        }
        return children;
    }

    @Override
    public String getNodeId(final CommandTreeNode node) {
        return node.getId();
    }

    @Override
    public String getNodeName(final CommandTreeNode node) {
        return node.getName();
    }

    @Override
    public CommandTreeNode getParent(final CommandTreeNode node) {
        return node.getParent();
    }

    @Override
    public void setNodeName(final CommandTreeNode node, final String name) {
    }

    @Override
    public TreeNodeElement<CommandTreeNode> getRenderedTreeNode(final CommandTreeNode node) {
        if (node == null) {
            return null;
        }
        return node.getTreeNodeElement();
    }

    @Override
    public void setRenderedTreeNode(final CommandTreeNode node, final TreeNodeElement<CommandTreeNode> renderedNode) {
        node.setTreeNodeElement(renderedNode);
    }

    @Override
    public CommandTreeNode getDragDropTarget(final CommandTreeNode node) {
        return null;
    }

    @Override
    public Array<String> getNodePath(CommandTreeNode node) {
        return null;
    }

    @Override
    public CommandTreeNode getNodeByPath(final CommandTreeNode root, final Array<String> relativeNodePath) {
        return null;
    }

    /** Data node fot the commands tree. */
    public static class CommandTreeNode {

        private final String                      id;
        private final String                      name;
        private final CommandTreeNode             parent;
        private final Object                      data;
        private final Collection<CommandTreeNode> children;

        /** The UI tree node. */
        private TreeNodeElement<CommandTreeNode> treeNodeElement;

        public CommandTreeNode(CommandTreeNode parent, Object data, Collection<CommandTreeNode> children) {
            this.parent = parent;
            this.data = data;
            this.children = children;

            if (data instanceof CommandType) {
                id = ((CommandType)data).getId();
                name = ((CommandType)data).getDisplayName();
            } else if (data instanceof CommandConfiguration) {
                id = ((CommandConfiguration)data).getName();
                name = ((CommandConfiguration)data).getName();
            } else {
                id = "ROOT";
                name = "ROOT";
            }
        }

        public CommandTreeNode getParent() {
            return parent;
        }

        public Object getData() {
            return data;
        }

        public Collection<CommandTreeNode> getChildren() {
            return children;
        }

        private String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public TreeNodeElement<CommandTreeNode> getTreeNodeElement() {
            return treeNodeElement;
        }

        public void setTreeNodeElement(TreeNodeElement<CommandTreeNode> treeNodeElement) {
            this.treeNodeElement = treeNodeElement;
        }
    }
}
