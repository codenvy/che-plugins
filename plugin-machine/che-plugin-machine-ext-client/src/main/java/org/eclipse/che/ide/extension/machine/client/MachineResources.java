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
package org.eclipse.che.ide.extension.machine.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * The resource interface for the Machine extension.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
public interface MachineResources extends ClientBundle {

    /** Returns the icon for clear console button. */
    @Source("images/console/clear-logs.svg")
    SVGResource clear();

    /** Returns the icon for 'Execute Selected Command' action. */
    @Source("images/execute.svg")
    SVGResource execute();

    /** Returns the icon for 'Edit Commands...' action. */
    @Source("images/recipe.svg")
    SVGResource recipe();

    /** Returns the icon for 'Arbitrary' command type. */
    @Source("command/arbitrary/arbitrary-command-type.svg")
    SVGResource arbitraryCommandType();

    @Source("images/project-perspective.svg")
    SVGResource projectPerspective();

    @Source("images/machine-perspective.svg")
    SVGResource machinePerspective();

    @Source("images/machines-part-icon.svg")
    SVGResource machinesPartIcon();

    @Source("recipe-template.txt")
    TextResource recipeTemplate();

    ImageResource close();

    /** Returns the CSS resource for the Machine extension. */
    @Source({"machine.css", "org/eclipse/che/ide/api/ui/style.css"})
    Css getCss();

    /** The CssResource interface for the Machine extension. */
    interface Css extends CssResource {

        /** Returns the CSS class name for 'Machine' console panel. */
        String machineConsole();

        /** Returns the CSS class name for 'Machine' console toolbar. */
        String consoleToolbar();

        String commandConsole();

        /** Returns the CSS class name for tab-panel in 'Outputs' console. */
        String outputsConsoleTabsPanel();

        /** Returns the CSS class name for hint label in 'Edit Commands' dialog. */
        String commandHint();

        /** Returns the CSS class name for tab button in 'Outputs' console. */
        String outputsContainerConsoleTab();

        /** Returns the CSS class name for selected tab button in 'Outputs' console. */
        String outputsContainerConsoleTabSelected();

        String outputsContainerConsoleTabPanel();

        /** Returns the CSS class name for text label of tab button in 'Outputs' console. */
        String outputsContainerConsoleTabLabel();

        /** Returns the CSS class name for close button of tab button in 'Outputs' console. */
        String outputsContainerConsoleTabCloseButton();

        String activeTab();

        String disableTab();

        String activeTabText();

        String fullSize();

        String unavailableLabel();

        String selectRecipe();

        String opacityButton();

        String unSelectRecipe();
    }
}
