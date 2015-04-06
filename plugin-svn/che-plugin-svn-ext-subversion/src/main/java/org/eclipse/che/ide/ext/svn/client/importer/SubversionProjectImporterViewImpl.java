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
package org.eclipse.che.ide.ext.svn.client.importer;

import org.eclipse.che.ide.ui.Styles;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

import javax.annotation.Nonnull;

import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;

/**
 * View implementation for the Subversion project importer.
 */
public class SubversionProjectImporterViewImpl extends Composite implements SubversionProjectImporterView {

    interface SubversionProjectImporterViewImplUiBinder
            extends UiBinder<DockLayoutPanel, SubversionProjectImporterViewImpl> {}

    private ActionDelegate delegate;

    @UiField(provided = true)
    Style       style;
    @UiField
    Label labelUrlError;
    @UiField
    HTMLPanel descriptionArea;
    @UiField
    TextBox projectName;
    @UiField
    TextArea projectDescription;
    @UiField
    RadioButton projectPrivate;
    @UiField
    RadioButton projectPublic;
    @UiField
    TextBox     projectUrl;
    @UiField
    TextBox     projectRelativePath;
    @UiField
    TextBox     username;
    @UiField
    TextBox     password;

    @Inject
    public SubversionProjectImporterViewImpl(final SubversionExtensionResources resources,
                                             final SubversionProjectImporterViewImplUiBinder uiBinder) {
        style = resources.svnProjectImporterPageStyle();
        style.ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        projectName.getElement().setAttribute("maxlength", "32");
        projectDescription.getElement().setAttribute("maxlength", "256");
    }

    @UiHandler("projectName")
    void onProjectNameChanged(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.projectNameChanged(projectName.getValue());
    }

    @UiHandler("projectUrl")
    void onProjectUrlChanged(KeyUpEvent event) {
        delegate.projectUrlChanged(projectUrl.getValue());
    }

    @UiHandler("projectRelativePath")
    void onProjectRelativePathChanged(final KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.projecRelativePathChanged(this.projectRelativePath.getValue());
    }

    @UiHandler("projectDescription")
    void onProjectDescriptionChanged(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.projectDescriptionChanged(projectDescription.getValue());
    }

    @UiHandler({"projectPublic", "projectPrivate"})
    void visibilityHandler(ValueChangeEvent<Boolean> event) {
        delegate.projectVisibilityChanged(projectPublic.getValue());
    }

    @UiHandler({"username", "password"})
    void credentialChangeHandler(final ValueChangeEvent<String> event) {
        delegate.credentialsChanged(this.username.getValue(), this.password.getValue());
    }

    @Override
    public void setProjectUrl(@Nonnull String url) {
        projectUrl.setText(url);
        delegate.projectUrlChanged(url);
    }

    @Override
    public String getProjectUrl() {
        return this.projectUrl.getValue();
    }

    @Override
    public void reset() {
        projectUrl.setText("");
        projectRelativePath.setText("trunk");
        projectName.setText("");
        projectDescription.setText("");
        descriptionArea.clear();
        projectPublic.setValue(true);
        projectPrivate.setValue(false);
        hideUrlError();
        hideNameError();
    }

    @Nonnull
    @Override
    public String getProjectRelativePath() {
        return this.projectRelativePath.getValue();
    }

    @Override
    public void setProjectRelativePath(@Nonnull final String projectRelativePath) {
        this.projectRelativePath.setValue(projectRelativePath);
    }

    @Override
    public void showNameError() {
        projectName.addStyleName(style.inputError());
    }

    @Override
    public void hideNameError() {
        projectName.removeStyleName(style.inputError());
    }

    @Override
    public void showUrlError(@Nonnull String message) {
        projectUrl.addStyleName(style.inputError());
        labelUrlError.setText(message);
    }

    @Override
    public void hideUrlError() {
        projectUrl.removeStyleName(style.inputError());
        labelUrlError.setText("");
    }

    @Override
    public void setProjectDescription(@Nonnull String text) {
        descriptionArea.getElement().setInnerText(text);
    }

    @Nonnull
    @Override
    public String getProjectName() {
        return projectName.getValue();
    }

    @Override
    public void setProjectName(@Nonnull String projectName) {
        this.projectName.setValue(projectName);
        delegate.projectNameChanged(projectName);
    }

    @Override
    public void setProjectVisibility(final boolean visible) {
        projectPublic.setValue(visible, false);
        projectPrivate.setValue(!visible, false);
    }

    @Override
    public void focusInUrlInput() {
        projectUrl.setFocus(true);
    }

    @Override
    public void setInputsEnableState(boolean isEnabled) {
        projectName.setEnabled(isEnabled);
        projectDescription.setEnabled(isEnabled);
        projectUrl.setEnabled(isEnabled);

        if (isEnabled) {
            focusInUrlInput();
        }
    }

    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    public interface Style extends Styles {
        String mainPanel();

        String namePanel();

        String labelPosition();

        String marginTop();

        String alignRight();

        String alignLeft();

        String labelErrorPosition();

        String radioButtonPosition();

        String description();

        String label();

        String horizontalLine();
    }

}
