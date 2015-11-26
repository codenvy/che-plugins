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
package org.eclipse.che.ide.ext.openshift.client.project.wizard.page.configure;

import elemental.dom.Element;
import elemental.html.SpanElement;
import elemental.html.TableElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.util.dom.Elements;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of view {@link ConfigureProjectView}.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ConfigureProjectViewImpl implements ConfigureProjectView {

    private static ConfigureProjectViewImplUiBinder uiBinder = GWT.create(ConfigureProjectViewImplUiBinder.class);

    interface ConfigureProjectViewImplUiBinder extends UiBinder<DockPanel, ConfigureProjectViewImpl> {
    }

    @UiField
    Label osProjectNameErrorLabel;

    @UiField
    Label cheProjectNameErrorLabel;

    @UiField
    TextBox osProjectNameInput;

    @UiField
    TextBox osProjectDisplayNameInput;

    @UiField
    TextArea osProjectDescriptionInput;

    @UiField
    RadioButton osNewProjectButton;

    @UiField
    RadioButton osExistProjectButton;

    @UiField
    TextBox cheProjectNameInput;

    @UiField
    TextArea cheProjectDescriptionInput;

    @UiField
    ScrollPanel osExistProjectListPanel;

    @UiField
    RadioButton chePublicProject;

    @UiField
    RadioButton chePrivateProject;

    OpenshiftResources openshiftResources;

    private SimpleList<Project> projectsList;

    private ActionDelegate delegate;
    private DockPanel      widget;
    private boolean        rewriteCheProjectName;

    @Inject
    public ConfigureProjectViewImpl(Resources resources, final OpenshiftResources openshiftResources) {
        this.openshiftResources = openshiftResources;

        widget = uiBinder.createAndBindUi(this);

        TableElement breakPointsElement = Elements.createTableElement();
        breakPointsElement.setAttribute("style", "width: 100%");

        projectsList = SimpleList.create((SimpleList.View)breakPointsElement, resources.defaultSimpleListCss(),
                                         new SimpleList.ListItemRenderer<Project>() {
                                             @Override
                                             public void render(Element listItemBase, Project itemData) {
                                                 //TODO rework this method to proper display each project name
                                                 SpanElement container = Elements.createSpanElement();
                                                 container.setInnerText(itemData.getMetadata().getName());

                                                 listItemBase.appendChild(container);
                                             }
                                         },
                                         new SimpleList.ListEventDelegate<Project>() {
                                             @Override
                                             public void onListItemClicked(Element listItemBase, Project itemData) {
                                                 if (osExistProjectButton.getValue()) {
                                                     projectsList.getSelectionModel().setSelectedItem(itemData);
                                                     cheProjectNameInput.setValue(itemData.getMetadata().getName());
                                                     delegate.onExistProjectSelected();
                                                 }
                                             }

                                             @Override
                                             public void onListItemDoubleClicked(Element listItemBase, Project itemData) {

                                             }
                                         });
        osExistProjectListPanel.add(projectsList);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public boolean isNewOpenShiftProjectSelected() {
        return osNewProjectButton.getValue();
    }

    @UiHandler({"osNewProjectButton", "osExistProjectButton"})
    public void onRadioButtonClicked(ClickEvent event) {
        final boolean enabled = osNewProjectButton.getValue();

        osProjectNameInput.setEnabled(enabled);
        osProjectDisplayNameInput.setEnabled(enabled);
        osProjectDescriptionInput.setEnabled(enabled);

        osExistProjectListPanel.setVisible(!enabled);
        if (enabled) {
            delegate.onExistProjectSelected();
        }
    }

    @UiHandler({"osProjectNameInput"})
    public void onOpenShiftProjectNameChanged(KeyUpEvent event) {
        if (isRewriteCheProjectName()) {
            cheProjectNameInput.setValue(osProjectNameInput.getValue(), true);
        }
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            return;
        }
        delegate.onOpenShiftNewProjectNameChanged();
    }

    @UiHandler({"cheProjectNameInput"})
    public void onCheProjectNameChanged(KeyUpEvent event) {
        if (cheProjectNameInput.getValue().isEmpty()) {
            setRewriteCheProjectName(true);
        } else {
            setRewriteCheProjectName(false);
        }
        delegate.onCheNewProjectNameChanged();
    }

    @UiHandler({"osProjectDescriptionInput"})
    public void onOpenShiftProjectDescriptionChanged(KeyUpEvent event) {
        cheProjectDescriptionInput.setValue(osProjectDescriptionInput.getValue(), true);
        delegate.onOpenShiftDescriptionChanged();
    }

    @UiHandler({"cheProjectDescriptionInput"})
    public void onCheProjectDescriptionChanged(KeyUpEvent event) {
        delegate.onCheDescriptionChanged();
    }

    @UiHandler({"osProjectDisplayNameInput"})
    public void onOpenShiftProjectDisplayNameChanged(KeyUpEvent event) {
        delegate.onOpenShiftDisplayNameChanged();
    }

    @Override
    public Project getExistedSelectedProject() {
        return projectsList.getSelectionModel().getSelectedItem();
    }

    @Override
    public void resetControls() {
        rewriteCheProjectName = true;
        osProjectNameInput.setValue("", true);
        osProjectDisplayNameInput.setValue("", true);
        osProjectDescriptionInput.setValue("", true);
        osNewProjectButton.setValue(Boolean.TRUE, true);

        cheProjectNameInput.setValue("", true);
        cheProjectDescriptionInput.setValue("", true);

        projectsList.render(Collections.<Project>emptyList());
    }

    @Override
    public void setExistOpenShiftProjects(List<Project> projects) {
        projectsList.render(projects);
    }

    @Override
    public String getOpenShiftNewProjectName() {
        return osProjectNameInput.getValue();
    }

    @Override
    public String getCheNewProjectName() {
        return cheProjectNameInput.getValue();
    }

    @Override
    public String getOpenShiftProjectDescription() {
        return osProjectDescriptionInput.getValue();
    }

    @Override
    public String getCheProjectDescription() {
        return cheProjectDescriptionInput.getValue();
    }

    @Override
    public String getOpenShiftProjectDisplayName() {
        return osProjectDisplayNameInput.getValue();
    }

    @Override
    public void showOsProjectNameError(@NotNull String message) {
        osProjectNameInput.addStyleName(openshiftResources.css().inputError());
        osProjectNameErrorLabel.setText(message);
    }

    @Override
    public void hideOsProjectNameError() {
        osProjectNameInput.removeStyleName(openshiftResources.css().inputError());
        osProjectNameErrorLabel.setText("");
    }

    @Override
    public void showCheProjectNameError(@NotNull String message) {
        cheProjectNameInput.addStyleName(openshiftResources.css().inputError());
        cheProjectNameErrorLabel.setText(message);
    }

    @Override
    public void hideCheProjectNameError() {
        cheProjectNameInput.removeStyleName(openshiftResources.css().inputError());
        cheProjectNameErrorLabel.setText("");
    }

    private boolean isRewriteCheProjectName() {
        return rewriteCheProjectName;
    }

    private void setRewriteCheProjectName(boolean rewriteCheProjectName) {
        this.rewriteCheProjectName = rewriteCheProjectName;
    }

    @Override
    public void setElementsEnabled(boolean enabled) {
        osNewProjectButton.setEnabled(enabled);
        osProjectNameInput.setEnabled(enabled);
        osProjectDisplayNameInput.setEnabled(enabled);
        osProjectDescriptionInput.setEnabled(enabled);

        osExistProjectButton.setEnabled(enabled);

        cheProjectNameInput.setEnabled(enabled);
        cheProjectDescriptionInput.setEnabled(enabled);

        chePublicProject.setEnabled(enabled);
        chePrivateProject.setEnabled(enabled);
    }
}
