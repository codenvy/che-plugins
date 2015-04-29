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
 *
 * @author vzhukovskii@codenvy.com
 */
public class SubversionProjectImporterViewImpl extends Composite implements SubversionProjectImporterView {

    interface SubversionProjectImporterViewImplUiBinder
            extends UiBinder<DockLayoutPanel, SubversionProjectImporterViewImpl> {
    }

    private ActionDelegate delegate;

    @UiField(provided = true)
    Style       style;
    @UiField
    Label       labelUrlError;
    @UiField
    HTMLPanel   descriptionArea;
    @UiField
    TextBox     projectName;
    @UiField
    TextArea    projectDescription;
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
    public SubversionProjectImporterViewImpl(SubversionExtensionResources resources,
                                             SubversionProjectImporterViewImplUiBinder uiBinder) {
        style = resources.svnProjectImporterPageStyle();
        style.ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        projectName.getElement().setAttribute("maxlength", "32");
        projectDescription.getElement().setAttribute("maxlength", "256");
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectUrl(@Nonnull String url) {
        projectUrl.setText(url);
        delegate.onProjectUrlChanged();
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectUrl() {
        return this.projectUrl.getValue();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getProjectRelativePath() {
        return this.projectRelativePath.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setNameErrorVisibility(boolean visible) {
        if (visible) {
            projectName.addStyleName(style.inputError());
        } else {
            projectName.removeStyleName(style.inputError());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectDescription(@Nonnull String text) {
        projectDescription.setText(text);
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectDescription() {
        return projectDescription.getText();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getProjectName() {
        return projectName.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectName(@Nonnull String projectName) {
        this.projectName.setValue(projectName);
        delegate.onProjectNameChanged();
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectVisibility(final boolean visible) {
        projectPublic.setValue(visible);
        projectPrivate.setValue(!visible);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getProjectVisibility() {
        return projectPrivate.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setUrlTextBoxFocused() {
        projectUrl.setFocus(true);
    }

    /** {@inheritDoc} */
    @Override
    public String getUserName() {
        return username.getText();
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return password.getText();
    }

    @UiHandler("projectName")
    void onProjectNameChanged(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.onProjectNameChanged();
    }

    @UiHandler("projectUrl")
    @SuppressWarnings("unused")
    void onProjectUrlChanged(KeyUpEvent event) {
        delegate.onProjectUrlChanged();
    }

    @UiHandler("projectRelativePath")
    void onProjectRelativePathChanged(final KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.onProjectRelativePathChanged();
    }

    @UiHandler("projectDescription")
    void onProjectDescriptionChanged(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.onProjectDescriptionChanged();
    }

    @UiHandler({"projectPublic", "projectPrivate"})
    @SuppressWarnings("unused")
    void visibilityHandler(ValueChangeEvent<Boolean> event) {
        delegate.onProjectVisibilityChanged();
    }

    @UiHandler({"username", "password"})
    @SuppressWarnings("unused")
    void credentialChangeHandler(final ValueChangeEvent<String> event) {
        delegate.onCredentialsChanged();
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
