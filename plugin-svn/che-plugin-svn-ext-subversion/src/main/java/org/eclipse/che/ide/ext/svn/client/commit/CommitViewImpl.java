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
package org.eclipse.che.ide.ext.svn.client.commit;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;

import org.eclipse.che.ide.ui.window.Window;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class CommitViewImpl extends Window implements CommitView {

    private CommitViewDelegate delegate;

    @UiField(provided = true)
    SubversionExtensionLocalizationConstants locale;

    @UiField(provided = true)
    SubversionExtensionResources res;

    @UiField
    InputElement commitAll;

    @UiField
    InputElement commitSelection;

    @UiField
    CheckBox keepLocks;

    @UiField
    TextArea message;

    private final Button btnCancel;
    private final Button btnCommit;

    @Inject
    public CommitViewImpl(final CommitViewImplUiBinder uibinder,
                          final SubversionExtensionLocalizationConstants constants,
                          final SubversionExtensionResources resources,
                          final Window.Resources windowResources) {
        super(true);
        this.locale = constants;
        this.res = resources;
        final Widget widget = uibinder.createAndBindUi(this);

        this.setTitle(locale.commitTitle());
        this.setWidget(widget);

        btnCancel = createButton(locale.buttonCancel(), "svn-commit-cancel", new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        btnCommit = createButton(locale.buttonCommit(), "svn-commit-commit", new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                delegate.onCommitClicked();
            }
        });
        btnCommit.addStyleName(windowResources.centerPanelCss().blueButton());

        getFooter().add(btnCommit);
        getFooter().add(btnCancel);
    }

    @Override
    protected void onClose() {
    }

    @Override
    public void setDelegate(final CommitViewDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        this.hide();
    }

    @Override
    public void showDialog() {
        this.show();
    }

    @Override
    public String getMessage() {
        return this.message.getText();
    }

    @Override
    public void setMessage(@Nonnull String message) {
        this.message.setText(message);
    }

    @Override
    public boolean isCommitSelection() {
        return this.commitSelection.isChecked();
    }

    @Override
    public void setCommitSelection(final boolean includeSelection) {
        this.commitSelection.setChecked(includeSelection);
        this.commitAll.setChecked(!includeSelection);
    }

    @Override
    public void setEnableCommitButton(boolean enable) {
        this.btnCommit.setEnabled(enable);
    }

    @Override
    public void focusInMessageField() {
        this.message.setFocus(true);
    }

    @UiHandler("message")
    public void onMessageChanged(KeyUpEvent event) {
        delegate.onValueChanged();
    }

    @Override
    public void setKeepLocksState(final boolean keepLocks) {
        this.keepLocks.setValue(keepLocks);
    }

    @Override
    public boolean getKeepLocksState() {
        return this.keepLocks.getValue();
    }
}
