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
package org.eclipse.che.ide.ext.runner.client.tabs.terminal.container;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.runner.client.RunnerResources;

import javax.annotation.Nonnull;

/**
 * @author Valeriy Svydenko
 */
public class TerminalContainerViewImpl extends Composite implements TerminalContainerView {

    interface TerminalContainerViewImplUiBinder extends UiBinder<Widget, TerminalContainerViewImpl> {
    }

    private static final TerminalContainerViewImplUiBinder UI_BINDER = GWT.create(TerminalContainerViewImplUiBinder.class);

    @UiField
    FlowPanel mainPanel;
    @UiField(provided = true)
    final RunnerResources resources;
    @UiField
    Label noRunnerLabel;

    @Inject
    public TerminalContainerViewImpl(RunnerResources resources) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        // elided
    }

    /** {@inheritDoc} */
    @Override
    public void addWidget(@Nonnull IsWidget terminal) {
        mainPanel.add(terminal);
    }

    /** {@inheritDoc} */
    @Override
    public void removeWidget(@Nonnull IsWidget terminal) {
        mainPanel.remove(terminal);
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleNoRunnerLabel(boolean isVisible) {
        noRunnerLabel.setVisible(isVisible);
    }

}