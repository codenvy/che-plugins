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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl;

import com.google.inject.Inject;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;

/**
 * This class is stub of the Properties Panel
 *
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 * @author Alexander Andrienko
 */
public class PropertiesStubPanel extends PropertiesPanelPresenter {
    @Inject
    public PropertiesStubPanel(PropertiesPanelView view, AppContext appContext) {
        super(view, appContext);

        this.view.setName("");
        this.view.setType("");

        this.view.setEnableNameProperty(false);
        this.view.setEnableRamProperty(false);
        this.view.setEnableBootProperty(false);
        this.view.setEnableShutdownProperty(false);
        this.view.setEnableScopeProperty(false);

        this.view.hideButtonsPanel();
        this.view.hideSwitcher();
    }
}