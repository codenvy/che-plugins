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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.container;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;

import javax.annotation.Nonnull;

/**
 * @author Andrey Plotnikov
 */
public class PropertiesContainerViewImpl extends Composite implements PropertiesContainerView {

    interface PropertiesContainerViewImplUiBinder extends UiBinder<Widget, PropertiesContainerViewImpl> {
    }

    private static final PropertiesContainerViewImplUiBinder UI_BINDER = GWT.create(PropertiesContainerViewImplUiBinder.class);

    @UiField
    SimpleLayoutPanel mainPanel;
    @UiField
    Label noRunnerLabel;
    @UiField(provided = true)
    final RunnerResources resources;

    @Inject
    public PropertiesContainerViewImpl(RunnerResources resources) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void showWidget(@Nonnull PropertiesPanel panel) {
        panel.go(mainPanel);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        mainPanel.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleNoRunnerLabel(boolean visible) {
        noRunnerLabel.setVisible(visible);
    }
}