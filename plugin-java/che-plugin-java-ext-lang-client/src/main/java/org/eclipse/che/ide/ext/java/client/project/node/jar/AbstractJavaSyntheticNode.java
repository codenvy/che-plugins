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
package org.eclipse.che.ide.ext.java.client.project.node.jar;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.project.node.SyntheticBasedNode;

import javax.validation.constraints.NotNull;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractJavaSyntheticNode<DataObject> extends SyntheticBasedNode<DataObject> {
    protected final JavaNodeManager nodeManager;

    public AbstractJavaSyntheticNode(@NotNull DataObject dataObject,
                                     @NotNull ProjectDescriptor projectDescriptor,
                                     @NotNull NodeSettings nodeSettings,
                                     @NotNull JavaNodeManager nodeManager) {
        super(dataObject, projectDescriptor, nodeSettings);
        this.nodeManager = nodeManager;
    }
}
