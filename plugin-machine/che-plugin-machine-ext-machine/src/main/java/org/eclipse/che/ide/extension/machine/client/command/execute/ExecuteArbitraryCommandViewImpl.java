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
package org.eclipse.che.ide.extension.machine.client.command.execute;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.ui.window.Window;

import javax.annotation.Nonnull;

/**
 * The implementation of {@link ExecuteArbitraryCommandView}.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ExecuteArbitraryCommandViewImpl extends Window implements ExecuteArbitraryCommandView {

    private static final ExecuteCommandViewImplUiBinder UI_BINDER = GWT.create(ExecuteCommandViewImplUiBinder.class);

    @UiField(provided = true)
    final MachineResources            machineResources;
    @UiField(provided = true)
    final MachineLocalizationConstant locale;
    @UiField
    TextBox commandBox;

    private ActionDelegate delegate;
    private Button         executeButton;

    @Inject
    protected ExecuteArbitraryCommandViewImpl(MachineResources resources, MachineLocalizationConstant locale) {
        this.machineResources = resources;
        this.locale = locale;

        setTitle(locale.executeCommandViewTitle());
        setWidget(UI_BINDER.createAndBindUi(this));
        createButtons();
    }

    private void createButtons() {
        final Button closeButton = createButton(locale.closeButton(), "view-executeCommand-close", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        getFooter().add(closeButton);

        executeButton = createButton(locale.executeButton(), "view-executeCommand-execute", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onExecuteClicked();
            }
        });
        executeButton.addStyleName(resources.centerPanelCss().blueButton());
        getFooter().add(executeButton);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getCommand() {
        return commandBox.getText();
    }

    /** {@inheritDoc} */
    @Override
    public void setCommand(@Nonnull String command) {
        commandBox.setText(command);
    }

    @UiHandler("commandBox")
    void onKeyUp(KeyUpEvent event) {
        executeButton.setEnabled(!commandBox.getText().isEmpty());
    }

    /** {@inheritDoc} */
    @Override
    protected void onEnterClicked() {
        if (!commandBox.getText().isEmpty()) {
            delegate.onExecuteClicked();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void show() {
        executeButton.setEnabled(false);
        commandBox.setText("");
        super.show();
        new Timer() {
            @Override
            public void run() {
                commandBox.setFocus(true);
            }
        }.schedule(300);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        hide();
    }

    /** {@inheritDoc} */
    @Override
    protected void onClose() {
    }

    interface ExecuteCommandViewImplUiBinder extends UiBinder<Widget, ExecuteArbitraryCommandViewImpl> {
    }
}
