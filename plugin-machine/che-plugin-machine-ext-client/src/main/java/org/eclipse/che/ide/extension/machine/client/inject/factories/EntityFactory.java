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
package org.eclipse.che.ide.extension.machine.client.inject.factories;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.ServerDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.MachineState;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.server.Server;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachineTreeNode;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.editor.RecipeEditorPanel;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.Tab;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.container.TabContainerView.TabSelectHandler;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.content.TabPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.header.TabHeader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Special factory for creating entities.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
public interface EntityFactory {

    /**
     * Creates machine object.
     *
     * @return an instance of {@link Machine}
     */
    Machine createMachine(@Nonnull MachineDescriptor descriptor);

    /**
     * Creates machine state object.
     *
     * @return an instance of {@link MachineState}
     */
    MachineState createMachineState(@Nonnull MachineStateDescriptor descriptor);

    /**
     * Creates tab entity using special parameters.
     *
     * @param tabHeader
     *         header of tab
     * @param tabPresenter
     *         content of tab
     * @return an instance of {@link Tab}
     */
    Tab createTab(@Nonnull TabHeader tabHeader, @Nonnull TabPresenter tabPresenter, @Nullable TabSelectHandler handler);

    /**
     * Creates a properties panel widget for a given environment.
     *
     * @param recipeDescriptor
     *         descriptor that needs to be bound with a widget
     * @return an instance of {@link RecipeEditorPanel}
     */
    @Nonnull
    RecipeEditorPanel createRecipeEditorPanel(@Nullable RecipeDescriptor recipeDescriptor);

    /**
     * Creates server entity with special parameters.
     *
     * @param port
     *         server port
     * @param descriptor
     *         server descriptor which contains information about current server
     * @return an instance of {@link Server}
     */
    Server createServer(@Nonnull String port, @Nonnull ServerDescriptor descriptor);

    /**
     * Creates machine node which will be displayed in special table on view.
     *
     * @param parent
     *         parent of creating node
     * @param data
     *         data of creating node
     * @param children
     *         children of creating node
     * @return an instance of{@link MachineTreeNode}
     */
    MachineTreeNode createMachineNode(@Nullable MachineTreeNode parent,
                                      @Assisted("data") Object data,
                                      Collection<MachineTreeNode> children);
}
