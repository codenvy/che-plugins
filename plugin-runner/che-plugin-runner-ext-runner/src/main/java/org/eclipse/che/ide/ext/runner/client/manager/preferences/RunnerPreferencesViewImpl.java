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
package org.eclipse.che.ide.ext.runner.client.manager.preferences;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.*;

import javax.annotation.Nonnull;

/**
 * @author Ann Shumilova
 */
public class RunnerPreferencesViewImpl implements RunnerPreferencesView {

    private static RunnerPreferencesViewImplUiBinder uiBinder = GWT.create(RunnerPreferencesViewImplUiBinder.class);
    private final FlowPanel rootElement;
    @UiField
    ListBox shutdownField;
    @UiField
    Button setButton;
    private ActionDelegate delegate;

    public RunnerPreferencesViewImpl() {
        rootElement = uiBinder.createAndBindUi(this);

        for (Enum item : Shutdown.values()) {
            shutdownField.addItem(item.toString());
        }
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return rootElement;
    }

    @UiHandler("shutdownField")
    void handleSelectionChanged(ChangeEvent event) {
        delegate.onValueChanged();
    }

    @UiHandler("setButton")
    void onSetClicked(ClickEvent event) {
        delegate.onSetShutdownClicked();
    }

    @Override
    public void selectShutdown(@Nonnull Shutdown shutdown) {
        this.shutdownField.setItemSelected(shutdown.ordinal(), true);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Shutdown getShutdown() {
        String value = shutdownField.getValue(shutdownField.getSelectedIndex());
        return Shutdown.detect(value);
    }

    @Override
    public void enableSetButton(boolean isEnabled) {
        setButton.setEnabled(isEnabled);
    }

    interface RunnerPreferencesViewImplUiBinder
            extends UiBinder<FlowPanel, RunnerPreferencesViewImpl> {
    }
}