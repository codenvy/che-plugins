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
package org.eclipse.che.ide.extension.maven.client.command;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * The implementation of {@link MavenPageView}.
 *
 * @author Artem Zatsarynnyy
 */
public class MavenPageViewImpl implements MavenPageView {

    private static final MavenPageViewImplUiBinder UI_BINDER = GWT.create(MavenPageViewImplUiBinder.class);

    private final DockLayoutPanel rootElement;

    @UiField
    TextBox commandLine;

    private ActionDelegate delegate;

    public MavenPageViewImpl() {
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

    interface MavenPageViewImplUiBinder extends UiBinder<DockLayoutPanel, MavenPageViewImpl> {
    }
}