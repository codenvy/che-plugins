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
package org.eclipse.che.ide.extension.machine.client.command.edit;

import elemental.events.KeyboardEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.ui.list.CategoriesList;
import org.eclipse.che.ide.ui.list.Category;
import org.eclipse.che.ide.ui.list.CategoryRenderer;
import org.eclipse.che.ide.ui.window.Window;

import org.eclipse.che.commons.annotation.Nullable;
import org.vectomatic.dom.svg.ui.SVGImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of {@link EditCommandsView}.
 *
 * @author Artem Zatsarynnyi
 * @author Oleksii Orel
 */
@Singleton
public class EditCommandsViewImpl extends Window implements EditCommandsView {

    private static final EditConfigurationsViewImplUiBinder UI_BINDER = GWT.create(EditConfigurationsViewImplUiBinder.class);

    private final EditCommandResources        commandResources;
    private final IconRegistry                iconRegistry;
    private final FlowPanel                   hintLabel;
    private final CoreLocalizationConstant    coreLocale;


    private Map<CommandType, List<CommandConfiguration>> categories;


    @UiField
    MachineLocalizationConstant machineLocale;
    @UiField
    SimplePanel categoriesPanel;
    @UiField
    TextBox     filterInputField;
    @UiField
    TextBox     configurationName;
    @UiField
    SimplePanel contentPanel;
    @UiField
    FlowPanel   savePanel;
    @UiField
    FlowPanel   overFooter;

    private ActionDelegate delegate;
    private Button         cancelButton;
    private Button         saveButton;
    private CategoriesList list;

    private CommandConfiguration selectItem;
    private CommandType          selectType;

    private final CategoryRenderer<CommandConfiguration> projectImporterRenderer =
            new CategoryRenderer<CommandConfiguration>() {
                @Override
                public void renderElement(Element element, CommandConfiguration data) {
                    UIObject.ensureDebugId(element, "commandsManager-type-" + data.getType().getId());
                    element.addClassName(commandResources.getCss().categorySubElementHeader());
                    element.setInnerText(data.getName());
                    element.appendChild(renderSubElementButtons(data));
                }

                @Override
                public SpanElement renderCategory(Category<CommandConfiguration> category) {
                    return renderCategoryHeader(category.getTitle());
                }
            };

    private final Category.CategoryEventDelegate<CommandConfiguration> projectImporterDelegate =
            new Category.CategoryEventDelegate<CommandConfiguration>() {
                @Override
                public void onListItemClicked(Element listItemBase, CommandConfiguration itemData) {
                    selectType = itemData.getType();
                    selectCommand(itemData);
                }
            };

    @Inject
    protected EditCommandsViewImpl(org.eclipse.che.ide.Resources resources,
                                   EditCommandResources commandResources,
                                   MachineLocalizationConstant machineLocale,
                                   CoreLocalizationConstant coreLocale,
                                   IconRegistry iconRegistry) {
        this.commandResources = commandResources;
        this.machineLocale = machineLocale;
        this.coreLocale = coreLocale;
        this.iconRegistry = iconRegistry;

        selectItem = null;
        categories = new HashMap<>();
        hintLabel = new FlowPanel();

        commandResources.getCss().ensureInjected();
        setWidget(UI_BINDER.createAndBindUi(this));
        setTitle(machineLocale.editCommandsViewTitle());
        getWidget().getElement().setId("commandsManagerView");

        filterInputField.getElement().setAttribute("placeholder", machineLocale.editCommandsViewPlaceholder());
        filterInputField.getElement().addClassName(commandResources.getCss().filterPlaceholder());

        list = new CategoriesList(resources);
        list.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                switch (event.getNativeKeyCode()) {
                    case KeyboardEvent.KeyCode.INSERT:
                        delegate.onAddClicked();
                        break;
                    case KeyboardEvent.KeyCode.DELETE:
                        delegate.onRemoveClicked();
                        break;
                    default:
                        break;
                }
            }
        }, KeyDownEvent.getType());
        categoriesPanel.add(list);

        contentPanel.clear();
        savePanel.setVisible(false);

        createFooterButtons();

        getWidget().getElement().getStyle().setPadding(0, Style.Unit.PX);
    }

    private CommandType getTypeById(String commandId) {
        for (CommandType type : categories.keySet()) {
            if (type.getId().equals(commandId)) {
                return type;
            }
        }
        return null;
    }

    private SpanElement renderSubElementButtons(CommandConfiguration commandConfiguration) {
        SpanElement categorySubElement = Document.get().createSpanElement();
        categorySubElement.setClassName(commandResources.getCss().buttonArea());

        SpanElement removeCommandButtonElement = Document.get().createSpanElement();
        categorySubElement.appendChild(removeCommandButtonElement);
        removeCommandButtonElement.appendChild(this.commandResources.removeCommandButton().getSvg().getElement());
        Event.sinkEvents(removeCommandButtonElement, Event.ONCLICK);
        Event.setEventListener(removeCommandButtonElement, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (Event.ONCLICK == event.getTypeInt()) {
                    event.stopPropagation();
                    final List<CommandConfiguration> configurations = categories.get(selectItem.getType());

                    delegate.onRemoveClicked();

                    int selectPosition = configurations.indexOf(selectItem);
                    if (selectPosition == -1 && configurations.size() == 1) {
                        selectItem = null;
                        return;
                    }
                    if (selectPosition > 0) {
                        selectPosition--;
                    } else {
                        selectPosition++;
                    }
                    selectCommand(configurations.get(selectPosition));
                }
            }
        });

        SpanElement duplicateCommandButton = Document.get().createSpanElement();
        categorySubElement.appendChild(duplicateCommandButton);
        duplicateCommandButton.appendChild(this.commandResources.duplicateCommandButton().getSvg().getElement());
        Event.sinkEvents(duplicateCommandButton, Event.ONCLICK);
        Event.setEventListener(duplicateCommandButton, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (Event.ONCLICK == event.getTypeInt()) {
                    event.stopPropagation();
                    if (delegate.isViewModified()) {
                        delegate.onConfigurationSelected(selectItem);
                    } else {
                        delegate.onDuplicateClicked();
                    }
                }
            }
        });

        return categorySubElement;
    }

    private SpanElement renderCategoryHeader(final String commandId) {
        SpanElement categoryHeaderElement = Document.get().createSpanElement();
        categoryHeaderElement.setClassName(commandResources.getCss().categoryHeader());

        SpanElement iconElement = Document.get().createSpanElement();
        categoryHeaderElement.appendChild(iconElement);

        SpanElement textElement = Document.get().createSpanElement();
        categoryHeaderElement.appendChild(textElement);
        CommandType currentCommandType = getTypeById(commandId);
        textElement.setInnerText(currentCommandType != null ? currentCommandType.getDisplayName() : commandId);

        SpanElement buttonElement = Document.get().createSpanElement();
        buttonElement.appendChild(commandResources.addCommandButton().getSvg().getElement());
        categoryHeaderElement.appendChild(buttonElement);

        Event.sinkEvents(buttonElement, Event.ONCLICK);
        Event.setEventListener(buttonElement, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (Event.ONCLICK == event.getTypeInt()) {
                    event.stopPropagation();
                    savePanel.setVisible(true);
                    selectType = getTypeById(commandId);
                    delegate.onAddClicked();
                    filterInputField.setText("");//reset input field for filter
                }
            }
        });

        Icon icon = iconRegistry.getIconIfExist(commandId + ".commands.category.icon");
        if (icon != null) {
            final SVGImage iconSVG = icon.getSVGImage();
            if (iconSVG != null) {
                iconElement.appendChild(iconSVG.getElement());
                return categoryHeaderElement;
            }
        }

        return categoryHeaderElement;
    }

    private void renderCategoriesList(Map<CommandType, List<CommandConfiguration>> categories) {
        if (categories == null) {
            return;
        }

        final List<Category<?>> categoriesList = new ArrayList<>();
        final String filterTextValue = filterInputField.getText();

        for (CommandType type : categories.keySet()) {
            List<CommandConfiguration> configurations = new ArrayList<>();
            if (filterTextValue.isEmpty()) {
                configurations = categories.get(type);
            } else {  // filtering List
                for (CommandConfiguration configuration : categories.get(type)) {
                    if (configuration.getName().contains(filterTextValue)) {
                        configurations.add(configuration);
                    }
                }
            }

            Category<CommandConfiguration> category =
                    new Category<>(type.getId(), projectImporterRenderer, configurations, projectImporterDelegate);
            categoriesList.add(category);
        }

        list.clear();
        list.render(categoriesList);
        if (selectItem != null) {
            list.selectElement(selectItem);
            if (filterTextValue.isEmpty()) {
                selectElementText(configurationName.getElement());
            }
        } else {
            savePanel.setVisible(false);
            contentPanel.clear();
            if (selectType == null || categories.get(selectType).isEmpty()) {
                hintLabel.getElement().setInnerText(machineLocale.editCommandsViewHint());
                contentPanel.add(hintLabel);
            }
        }
    }

    @Override
    public void setData(Map<CommandType, List<CommandConfiguration>> categories) {
        this.categories = categories;
        renderCategoriesList(categories);
    }

    private void createFooterButtons() {
        final Button closeButton = createButton(coreLocale.close(), "window-edit-configurations-close",
                                                new ClickHandler() {
                                                    @Override
                                                    public void onClick(ClickEvent event) {
                                                        delegate.onCloseClicked();
                                                    }
                                                });

        saveButton = createButton(coreLocale.save(), "window-edit-configurations-save", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onSaveClicked();
            }
        });
        saveButton.addStyleName(resources.windowCss().primaryButton());

        cancelButton = createButton(coreLocale.cancel(), "window-edit-configurations-cancel", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });

        overFooter.add(saveButton);
        overFooter.add(cancelButton);

        addButtonToFooter(closeButton);
    }

    /**
     * Select text.
     */
    private native void selectElementText(Element element) /*-{
        element.focus();
        element.setSelectionRange(0, element.value.length);
    }-*/;

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void show() {
        super.show();
        configurationName.setText("");
    }

    @Override
    public void close() {
        this.hide();
    }

    @Override
    public AcceptsOneWidget getCommandConfigurationsDisplayContainer() {
        return contentPanel;
    }

    @Override
    public void clearCommandConfigurationsDisplayContainer() {
        contentPanel.clear();
    }

    @Override
    public String getConfigurationName() {
        return configurationName.getText();
    }

    @Override
    public void setConfigurationName(String name) {
        configurationName.setText(name);
    }

    @Override
    public void setCancelButtonState(boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    @Override
    public void setApplyButtonState(boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    @Override
    public void setFilterState(boolean enabled) {
        filterInputField.setEnabled(enabled);
    }

    @Nullable
    @Override
    public CommandType getSelectedCommandType() {
        return selectType;
    }

    @Override
    public void selectCommand(CommandConfiguration config) {
        selectItem = config;
        savePanel.setVisible(true);
        delegate.onConfigurationSelected(config);
    }

    @Nullable
    @Override
    public CommandConfiguration getSelectedConfiguration() {
        return selectItem;
    }

    @UiHandler("configurationName")
    public void onNameKeyUp(KeyUpEvent event) {
        delegate.onNameChanged();
    }

    @UiHandler("filterInputField")
    public void onFilterKeyUp(KeyUpEvent event) {
        renderCategoriesList(categories);
    }

    interface EditConfigurationsViewImplUiBinder extends UiBinder<Widget, EditCommandsViewImpl> {
    }
}