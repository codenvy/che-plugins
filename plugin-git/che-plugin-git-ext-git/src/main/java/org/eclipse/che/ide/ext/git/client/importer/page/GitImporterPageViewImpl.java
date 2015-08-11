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
package org.eclipse.che.ide.ext.git.client.importer.page;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ui.Styles;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author Roman Nikitenko
 */
public class GitImporterPageViewImpl extends Composite implements GitImporterPageView {

    @UiField(provided = true)
    Style       style;

    @UiField
    Label       labelUrlError;

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
    FlowPanel   importerPanel;

    @UiField
    CheckBox    keepDirectory;

    @UiField
    TextBox     directoryName;

    private ActionDelegate delegate;

    @Inject
    public GitImporterPageViewImpl(GitResources resources,
                                   GitImporterPageViewImplUiBinder uiBinder) {
        style = resources.gitImporterPageStyle();
        style.ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));

        projectName.getElement().setAttribute("maxlength", "32");
        projectDescription.getElement().setAttribute("maxlength", "256");
    }

    @UiHandler("projectName")
    void onProjectNameChanged(KeyUpEvent event) {
        String projectNameValue = projectName.getValue();

        if (projectNameValue != null && projectNameValue.contains(" ")) {
            projectNameValue = projectNameValue.replace(" ", "-");
            projectName.setValue(projectNameValue);
        }

        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }

        delegate.projectNameChanged(projectName.getValue());
    }

    @UiHandler("projectUrl")
    void onProjectUrlChanged(KeyUpEvent event) {
        delegate.projectUrlChanged(projectUrl.getValue());
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

    @UiHandler({"keepDirectory"})
    void keepDirectoryHandler(ValueChangeEvent<Boolean> event) {
        delegate.keepDirectorySelected(event.getValue());
    }

    @UiHandler("directoryName")
    void onDirectoryNameChanged(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }

        delegate.keepDirectoryNameChanged(directoryName.getValue());
    }

    @Override
    public void setProjectUrl(@Nonnull String url) {
        projectUrl.setText(url);
        delegate.projectUrlChanged(url);
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
    public void setProjectDescription(@Nonnull String projectDescription) {
        this.projectDescription.setValue(projectDescription);
    }

    @Override
    public void setProjectVisibility(boolean visibility) {
        projectPublic.setValue(visibility, false);
        projectPrivate.setValue(!visibility, false);
    }

    @Override
    public boolean keepDirectory() {
        return keepDirectory.getValue();
    }

    @Override
    public void setKeepDirectoryChecked(boolean checked) {
        keepDirectory.setValue(checked);
    }

    @Override
    public String getDirectoryName() {
        return directoryName.getValue();
    }

    @Override
    public void setDirectoryName(String directoryName) {
        this.directoryName.setValue(directoryName);
    }

    @Override
    public void enableDirectoryNameField(boolean enable) {
        directoryName.setEnabled(enable);
    }

    @Override
    public void highlightDirectoryNameField(boolean highlight) {
        if (highlight) {
            directoryName.addStyleName(style.inputError());
        } else {
            directoryName.removeStyleName(style.inputError());
        }
    }

    @Override
    public void focusDirectoryNameFiend() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                directoryName.setFocus(true);
                directoryName.selectAll();
            }
        });
    }

    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    interface GitImporterPageViewImplUiBinder extends UiBinder<DockLayoutPanel, GitImporterPageViewImpl> {
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
