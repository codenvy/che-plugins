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
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import org.eclipse.che.ide.ui.tree.NodeDataAdapter;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The adapter for debug variable node.
 *
 * @author Andrey Plotnikov
 * @@author Dmitry Shnurenko
 */
public class VariableNodeDataAdapter implements NodeDataAdapter<DebuggerVariable> {
    private HashMap<DebuggerVariable, TreeNodeElement<DebuggerVariable>> treeNodeElements = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public int compare(@Nonnull DebuggerVariable a, @Nonnull DebuggerVariable b) {
        List<String> pathA = a.getVariablePath().getPath();
        List<String> pathB = b.getVariablePath().getPath();

        for (int i = 0; i < pathA.size(); i++) {
            String elementA = pathA.get(i);
            String elementB = pathB.get(i);

            int compare = elementA.compareTo(elementB);
            if (compare != 0) {
                return compare;
            }
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChildren(@Nonnull DebuggerVariable data) {
        return !data.isPrimitive();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public List<DebuggerVariable> getChildren(@Nonnull DebuggerVariable data) {
        return data.getVariables();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String getNodeId(@Nonnull DebuggerVariable data) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public String getNodeName(@Nonnull DebuggerVariable data) {
        return data.getName() + ": " + data.getValue();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public DebuggerVariable getParent(@Nonnull DebuggerVariable data) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public TreeNodeElement<DebuggerVariable> getRenderedTreeNode(@Nonnull DebuggerVariable data) {
        return treeNodeElements.get(data);
    }

    /** {@inheritDoc} */
    @Override
    public void setNodeName(@Nonnull DebuggerVariable data,@Nonnull String name) {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void setRenderedTreeNode(@Nonnull DebuggerVariable data,@Nonnull TreeNodeElement<DebuggerVariable> renderedNode) {
        treeNodeElements.put(data, renderedNode);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public DebuggerVariable getDragDropTarget(@Nonnull DebuggerVariable data) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public List<String> getNodePath(@Nonnull DebuggerVariable data) {
        return new ArrayList<>(data.getVariablePath().getPath());
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public DebuggerVariable getNodeByPath(@Nonnull DebuggerVariable root,@Nonnull List<String> relativeNodePath) {
        DebuggerVariable localRoot = root;
        for (int i = 0; i < relativeNodePath.size(); i++) {
            String path = relativeNodePath.get(i);
            if (localRoot != null) {
                List<DebuggerVariable> variables = new ArrayList<>(localRoot.getVariables());
                localRoot = null;
                for (int j = 0; j < variables.size(); j++) {
                    DebuggerVariable variable = variables.get(i);
                    if (variable.getName().equals(path)) {
                        localRoot = variable;
                        break;
                    }
                }

                if (i == (relativeNodePath.size() - 1)) {
                    return localRoot;
                }
            }
        }
        return null;
    }

}
