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
import com.google.gwt.resources.client.TextResource;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Client resources.
 *
 * @author Artem Zatsarynnyy
 */
public interface MachineResources extends ClientBundle {

    @Source("images/execute.svg")
    SVGResource execute();

    @Source("images/console/clear-logs.svg")
    SVGResource clear();

    @Source("test-docker-recipe.txt")
    TextResource testDockerRecipe();

    @Source("command/arbitrary/arbitrary-command-type.svg")
    SVGResource arbitraryCommandType();

    @Source({"machine.css", "org/eclipse/che/ide/api/ui/style.css"})
    Css machine();

    interface Css extends CssResource {
        String console();

        @ClassName("console-toolbar")
        String consoleToolbar();
    }
}
