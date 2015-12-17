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
package org.eclipse.che.ide.extension.machine.client.processes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.UUID;

import javax.validation.constraints.NotNull;

import java.util.Collection;

/**
 * The class describes node in process tree (it can be machine, process or terminal).
 *
 * @author Anna Shumilova
 */
public class ProcessTreeNode {

    /** The set of nodes. */
    public enum ProcessNodeType {
        ROOT_NODE,
        MACHINE_NODE,
        COMMAND_NODE,
        TERMINAL_NODE
    }

    public final static String ROOT = "root";

    private final ProcessNodeType                  type;
    private final String                           id;
    private final String                           displayName;
    private final ProcessTreeNode                  parent;
    private final Object                           data;
    private final Collection<ProcessTreeNode>      children;
    private       TreeNodeElement<ProcessTreeNode> treeNodeElement;

    @Inject
    public ProcessTreeNode(@Assisted ProcessNodeType type,
                           @Assisted ProcessTreeNode parent,
                           @Assisted("data") Object data,
                           @Assisted Collection<ProcessTreeNode> children) {
        this.type = type;
        this.parent = parent;
        this.data = data;
        this.children = children;

        switch (type) {
            case MACHINE_NODE:
                id = ((MachineDto)data).getId();
                displayName = ((MachineDto)data).getName();
                break;
            case COMMAND_NODE:
                String name = ((CommandConfiguration)data).getName();
                id = name + UUID.uuid();
                displayName = name;
                break;
            case TERMINAL_NODE:
                id = (String)data + UUID.uuid();
                displayName = (String)data;
                break;
            default:
                id = ROOT;
                displayName = ROOT;
        }
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return displayName;
    }

    @NotNull
    public ProcessNodeType getType() {
        return type;
    }

    @NotNull
    public ProcessTreeNode getParent() {
        return parent;
    }

    @NotNull
    public Object getData() {
        return data;
    }

    @Nullable
    public Collection<ProcessTreeNode> getChildren() {
        return children;
    }

    @NotNull
    public TreeNodeElement<ProcessTreeNode> getTreeNodeElement() {
        return treeNodeElement;
    }

    public void setTreeNodeElement(@NotNull TreeNodeElement<ProcessTreeNode> treeNodeElement) {
        this.treeNodeElement = treeNodeElement;
    }
}
