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
package org.eclipse.che.ide.ext.openshift.client.importapp;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ui.list.CategoriesList;
import org.eclipse.che.ide.ui.list.Category;
import org.eclipse.che.ide.ui.list.CategoryRenderer;
import org.eclipse.che.ide.ui.window.Window;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * View implementation for {@ImportApplicationView}.
 *
 * @author Anna Shumilova
 * @author Vitaliy Guliy
 */
public class ImportApplicationViewImpl extends Window implements ImportApplicationView {

    interface ImportApplicationViewUiBinder extends UiBinder<DockLayoutPanel, ImportApplicationViewImpl> {
    }

    private static ImportApplicationViewUiBinder uiBinder = GWT.create(ImportApplicationViewUiBinder.class);

    private ActionDelegate delegate;

    private final OpenshiftResources openshiftResources;

    @UiField(provided = true)
    OpenshiftLocalizationConstant locale;

    @UiField
    TextBox projectName;

    @UiField
    Label projectNameErrorLabel;

    @UiField
    TextArea projectDescription;

    @UiField
    Label branchName;

    @UiField
    Label contextDir;

    @UiField
    Label sourceUrl;

    @UiField
    AbsolutePanel categoriesPanel;

    @UiField
    Label loadingCategoriesLabel;

    @UiField
    Label nothingToShowLabel;

    @UiField
    FlowPanel branchPanel;

    @UiField
    FlowPanel contextDirPanel;

    private Button importButton;

    private Button cancelButton;

    private CategoriesList buildConfigList;

    private final Category.CategoryEventDelegate<BuildConfig> buildConfigDelegate =
            new Category.CategoryEventDelegate<BuildConfig>() {
                @Override
                public void onListItemClicked(Element listItemBase, BuildConfig itemData) {
                    delegate.onBuildConfigSelected(itemData);
                }
            };

    private final CategoryRenderer<BuildConfig> buildConfigCategoryRenderer = new CategoryRenderer<BuildConfig>() {
        @Override
        public void renderElement(Element element, BuildConfig data) {
            element.setInnerText(data.getMetadata().getName());
        }

        @Override
        public SpanElement renderCategory(Category<BuildConfig> category) {
            SpanElement element = Document.get().createSpanElement();
            element.setInnerText(category.getTitle().toUpperCase());
            return element;
        }
    };

    @Inject
    public ImportApplicationViewImpl(OpenshiftLocalizationConstant locale,
                                     CoreLocalizationConstant constants,
                                     org.eclipse.che.ide.Resources ideResources,
                                     OpenshiftResources openshiftResources) {
        this.locale = locale;
        this.openshiftResources = openshiftResources;

        ensureDebugId("importApplication");
        setTitle(locale.importApplicationViewTitle());

        setWidget(uiBinder.createAndBindUi(this));
        getWidget().getElement().getStyle().setPadding(0, Style.Unit.PX);

        importButton = createPrimaryButton(locale.importApplicationImportButton(),
                                           "importApplication-import-button", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        delegate.onImportApplicationClicked();
                    }
                });
        addButtonToFooter(importButton);
        importButton.addStyleName(ideResources.Css().buttonLoader());

        cancelButton = createButton(constants.cancel(), "importApplication-cancel-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        addButtonToFooter(cancelButton);

        buildConfigList = new CategoriesList(ideResources);
        buildConfigList.setVisible(false);
        categoriesPanel.add(buildConfigList);
    }

    @Override
    public void showView() {
        show();
    }

    @Override
    public void closeView() {
        hide();
    }

    @Override
    public void setBlocked(boolean blocked) {
        super.setBlocked(blocked);
    }

    @Override
    public void showLoadingBuildConfigs(String message) {
        nothingToShowLabel.setVisible(false);

        if (message == null || message.isEmpty()) {
            loadingCategoriesLabel.setVisible(false);
            buildConfigList.setVisible(true);
            return;
        }

        buildConfigList.setVisible(false);
        loadingCategoriesLabel.setText(message);
        loadingCategoriesLabel.setVisible(true);
    }

    @Override
    public void setBuildConfigs(Map<String, List<BuildConfig>> buildConfigs) {
        loadingCategoriesLabel.setVisible(false);

        buildConfigList.clear();
        List<Category<?>> categoriesList = new ArrayList<>();
        for (String categoryTitle : buildConfigs.keySet()) {
            Category<BuildConfig> category = new Category<BuildConfig>(categoryTitle,
                                                                       buildConfigCategoryRenderer,
                                                                       buildConfigs.get(categoryTitle),
                                                                       buildConfigDelegate);
            categoriesList.add(category);
        }
        buildConfigList.render(categoriesList);

        buildConfigList.setVisible(!buildConfigs.isEmpty());
        nothingToShowLabel.setVisible(buildConfigs.isEmpty());
    }

    @Override
    public void enableBuildConfigs(boolean enable) {
        buildConfigList.setEnabled(enable);
    }

    @Override
    public void enableImportButton(boolean enable) {
        importButton.setEnabled(enable);
    }

    @Override
    public void animateImportButton(boolean animate) {
        if (animate && !importButton.getElement().hasAttribute("animated")) {
            // save state and start animation
            importButton.getElement().setAttribute("originText", importButton.getText());
            importButton.getElement().getStyle().setProperty("minWidth", importButton.getOffsetWidth() + "px");
            importButton.setHTML("<i></i>");
            importButton.getElement().setAttribute("animated", "true");
        } else if (!animate && importButton.getElement().hasAttribute("animated")) {
            // stop animation and restore state
            importButton.setText(importButton.getElement().getAttribute("originText"));
            importButton.getElement().removeAttribute("originText");
            importButton.getElement().getStyle().clearProperty("minWidth");
            importButton.getElement().removeAttribute("animated");
        }
    }

    @Override
    public void enableCancelButton(boolean enable) {
        cancelButton.setEnabled(enable);
    }

    @Override
    public void setProjectName(String name) {
        projectName.setValue(name);
    }

    @Override
    public void setProjectDescription(String description) {
        projectDescription.setValue(description);
    }

    @Override
    public void setApplicationInfo(BuildConfig buildConfig) {
        if (buildConfig == null) {
            sourceUrl.setText("");
            contextDir.setText("");
            branchName.setText("");
            contextDirPanel.setVisible(false);
            branchPanel.setVisible(false);
            return;
        }

        sourceUrl.setText(buildConfig.getSpec().getSource().getGit().getUri());
        contextDir.setText(buildConfig.getSpec().getSource().getContextDir());
        branchName.setText(buildConfig.getSpec().getSource().getGit().getRef());

        contextDirPanel.setVisible(
                buildConfig.getSpec().getSource().getContextDir() != null && !buildConfig.getSpec().getSource().getContextDir().isEmpty());
        branchPanel.setVisible(buildConfig.getSpec().getSource().getGit().getRef() != null &&
                               !buildConfig.getSpec().getSource().getGit().getRef().isEmpty());
    }

    @Override
    public String getProjectName() {
        return projectName.getValue();
    }

    @Override
    public String getProjectDescription() {
        return projectDescription.getValue();
    }

    @Override
    public void setErrorMessage(String message) {
        //TODO
    }

    @Override
    public void showCheProjectNameError(@NotNull String message) {
        projectName.addStyleName(openshiftResources.css().inputError());
        projectNameErrorLabel.setText(message);
    }

    @Override
    public void hideCheProjectNameError() {
        projectName.removeStyleName(openshiftResources.css().inputError());
        projectNameErrorLabel.setText("");
    }

    @Override
    public void enableNameField(boolean enable) {
        projectName.setEnabled(enable);
    }

    @Override
    public void enableDescriptionField(boolean enable) {
        projectDescription.setEnabled(enable);
    }

    @UiHandler({"projectName"})
    public void onProjectNameChanged(KeyUpEvent event) {
        delegate.onProjectNameChanged(projectName.getValue());
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }
}
