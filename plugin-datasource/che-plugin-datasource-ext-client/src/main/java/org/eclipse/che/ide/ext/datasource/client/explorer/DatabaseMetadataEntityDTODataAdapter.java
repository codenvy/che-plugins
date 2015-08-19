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
package org.eclipse.che.ide.ext.datasource.client.explorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.che.ide.ext.datasource.client.explorer.DatabaseMetadataEntityDTODataAdapter.EntityTreeNode;
import org.eclipse.che.ide.ext.datasource.shared.ColumnDTO;
import org.eclipse.che.ide.ext.datasource.shared.DatabaseDTO;
import org.eclipse.che.ide.ext.datasource.shared.DatabaseMetadataEntityDTO;
import org.eclipse.che.ide.ext.datasource.shared.SchemaDTO;
import org.eclipse.che.ide.ext.datasource.shared.TableDTO;
import org.eclipse.che.ide.ui.tree.NodeDataAdapter;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;

public class DatabaseMetadataEntityDTODataAdapter implements NodeDataAdapter<EntityTreeNode> {

    @Override
    public int compare(EntityTreeNode a, EntityTreeNode b) {
        if (a.getData() == null) {
            return 0;
        }
        if (b.getData() == null) {
            return 1;
        }
        return a.getData().getName().compareTo(b.getData().getName());
    }

    @Override
    public boolean hasChildren(final EntityTreeNode node) {
        if (node.getData() instanceof ColumnDTO) {
            return false;
        }
        return true;
    }

    @Override
    public List<EntityTreeNode> getChildren(final EntityTreeNode node) {
        final DatabaseMetadataEntityDTO data = node.getData();
        if (data instanceof ColumnDTO) {
            return null;
        }

        List<EntityTreeNode> children = new ArrayList<>();
        if (data instanceof DatabaseDTO) {
            Collection<SchemaDTO> schemas = ((DatabaseDTO)data).getSchemas()
                                                               .values();
            for (SchemaDTO schemaDTO : schemas) {
                EntityTreeNode newChild = new EntityTreeNode(node, schemaDTO);
                children.add(newChild);
            }
        }
        if (data instanceof SchemaDTO) {
            Collection<TableDTO> tables = ((SchemaDTO)data).getTables()
                                                           .values();
            for (TableDTO tableDTO : tables) {
                EntityTreeNode newChild = new EntityTreeNode(node, tableDTO);
                children.add(newChild);
            }
        }
        if (data instanceof TableDTO) {
            Collection<ColumnDTO> columns = ((TableDTO)data).getColumns()
                                                            .values();
            for (ColumnDTO columnDTO : columns) {
                EntityTreeNode newChild = new EntityTreeNode(node, columnDTO);
                children.add(newChild);
            }
        }

        return children;
    }

    @Override
    public String getNodeId(final EntityTreeNode node) {
        return node.getData().getLookupKey();
    }

    @Override
    public String getNodeName(final EntityTreeNode node) {
        return node.getData().getName();
    }

    @Override
    public EntityTreeNode getParent(final EntityTreeNode node) {
        return node.getParent();
    }

    @Override
    public void setNodeName(final EntityTreeNode node, final String name) {
        // correct implementation : do nothing
    }

    @Override
    public TreeNodeElement<EntityTreeNode> getRenderedTreeNode(final EntityTreeNode node) {
        if (node == null) {
            return null;
        }
        return node.getTreeNodeElement();
    }

    @Override
    public void setRenderedTreeNode(final EntityTreeNode node,
                                    final TreeNodeElement<EntityTreeNode> renderedNode) {
        node.setTreeNodeElement(renderedNode);
    }

    @Override
    public EntityTreeNode getDragDropTarget(final EntityTreeNode node) {
        return null;
    }

    @Override
    public List<String> getNodePath(EntityTreeNode node) {
        return null;
    }

    @Override
    public EntityTreeNode getNodeByPath(final EntityTreeNode root, final List<String> relativeNodePath) {
        return null;
    }

    /**
     * Data node fot the database entity tree.
     * 
     * @author "Mickaël Leduque"
     */
    public static class EntityTreeNode {

        /** The parent of this node (null for root). */
        private final EntityTreeNode            parent;

        /** The entity data for this node (schema, table, column DTO). */
        private final DatabaseMetadataEntityDTO data;

        /** The UI tree node. */
        private TreeNodeElement<EntityTreeNode> treeNodeElement = null;

        public EntityTreeNode(EntityTreeNode parent, DatabaseMetadataEntityDTO data) {
            super();
            this.parent = parent;
            this.data = data;
        }

        public EntityTreeNode getParent() {
            return parent;
        }

        public DatabaseMetadataEntityDTO getData() {
            return data;
        }

        public TreeNodeElement<EntityTreeNode> getTreeNodeElement() {
            return treeNodeElement;
        }

        public void setTreeNodeElement(TreeNodeElement<EntityTreeNode> treeNodeElement) {
            this.treeNodeElement = treeNodeElement;
        }
    }
}
