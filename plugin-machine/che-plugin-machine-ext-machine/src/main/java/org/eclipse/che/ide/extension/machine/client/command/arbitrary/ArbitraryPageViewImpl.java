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
package org.eclipse.che.ide.extension.machine.client.command.arbitrary;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * The implementation of {@link ArbitraryPageView}.
 *
 * @author Artem Zatsarynnyy
 */
public class ArbitraryPageViewImpl implements ArbitraryPageView {

    private static final MavenPageViewImplUiBinder UI_BINDER = GWT.create(MavenPageViewImplUiBinder.class);

    private final DockLayoutPanel rootElement;

    @UiField
    TextArea commandLine;

    private ActionDelegate delegate;

    public ArbitraryPageViewImpl() {
        rootElement = UI_BINDER.createAndBindUi(this);
        commandLine.setFocus(true);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return rootElement;
    }

    @Override
    public String getCommandLine() {
        return commandLine.getText();
    }

    @Override
    public void setCommandLine(String commandLine) {
        this.commandLine.setText(commandLine);
    }

    @UiHandler({"commandLine"})
    void onKeyUp(KeyUpEvent event) {
        delegate.onCommandLineChanged(getCommandLine());
    }

    interface MavenPageViewImplUiBinder extends UiBinder<DockLayoutPanel, ArbitraryPageViewImpl> {
    }
}
