/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.yeoman.client.panel;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.plugin.yeoman.client.builder.BuildFinishedCallback;
import org.eclipse.che.plugin.yeoman.client.builder.BuilderAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Presenter that will display the Yeoman panel on the right.
 *
 * @author Florent Benoit
 */
@Singleton
public class YeomanPartPresenter extends BasePresenter implements YeomanPartView.ActionDelegate,
                                                                  BuildFinishedCallback {

    /**
     * The view of the yeoman panel.
     */
    private final YeomanPartView view;

    private FoldingPanelFactory      foldingPanelFactory;
    private GeneratedItemViewFactory generatedItemViewFactory;

    /**
     * Associate a generator type to a list of names to generate.
     */
    private Map<YeomanGeneratorType, List<String>> namesByTypes;
    /**
     * Associate a generator type to a widget
     */
    private Map<YeomanGeneratorType, FoldingPanel> widgetByTypes;

    private EventBus eventBus;

    private DtoFactory   dtoFactory;
    private BuilderAgent builderAgent;
    private final NewProjectExplorerPresenter projectExplorer;

    @Inject
    public YeomanPartPresenter(YeomanPartView view, EventBus eventBus, FoldingPanelFactory foldingPanelFactory,
                               GeneratedItemViewFactory generatedItemViewFactory, DtoFactory dtoFactory,
                               BuilderAgent builderAgent, NewProjectExplorerPresenter projectExplorer) {
        this.view = view;
        this.eventBus = eventBus;
        this.foldingPanelFactory = foldingPanelFactory;
        this.generatedItemViewFactory = generatedItemViewFactory;
        this.dtoFactory = dtoFactory;
        this.builderAgent = builderAgent;
        this.projectExplorer = projectExplorer;
        this.namesByTypes = new HashMap<>();
        this.widgetByTypes = new HashMap<>();

        view.setTitle("Yeoman");
        view.setDelegate(this);
        updateGenerateButton();
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return "Yeoman";
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return "Yeoman Generator";
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /**
     * Add the given item to the view and the model
     * @param generatedName
     * @param selectedType
     */
    public void addItem(String generatedName, YeomanGeneratorType selectedType) {

        // entry name not empty?
        if (generatedName == null || "".equals(generatedName)) {
            return;
        }

        // nothing selected
        if (selectedType == null) {
            return;
        }

        // existing values ?
        List<String> generatedNames = namesByTypes.get(selectedType);
        if (generatedNames == null) {
            // needs to add also the widget
            FoldingPanel foldingPanel = foldingPanelFactory.create(selectedType.getLabelName());
            widgetByTypes.put(selectedType, foldingPanel);
            view.addFoldingPanel(foldingPanel);
            generatedNames = new ArrayList<>();
            namesByTypes.put(selectedType, generatedNames);
        }


        // check if already exists
        if (generatedNames.contains(generatedName)) {
            return;
        }

        // needs to add element
        generatedNames.add(generatedName);

        // Also create the widget
        GeneratedItemView item = generatedItemViewFactory.create(generatedName, selectedType);
        item.setAnchor(view);
        // add it in the right parent
        widgetByTypes.get(selectedType).add(item);

        updateGenerateButton();

    }

    /**
     * Callback used when build has been finished
     * @param buildStatus
     */
    @Override
    public void onFinished(BuildStatus buildStatus) {
        // refresh the tree if it is successful
        if (buildStatus == BuildStatus.SUCCESSFUL) {
//            eventBus.fireEvent(new RefreshProjectTreeEvent());
            projectExplorer.synchronizeTree();
            // remove what has been generated
            namesByTypes.clear();
            widgetByTypes.clear();
            view.clear();
            updateGenerateButton();
        }

    }

    /**
     * Generates the yeoman stuff.
     */
    public void generate() {
        List<String> targets = new ArrayList<>();

        // Now add all the generated case
        for (Map.Entry<YeomanGeneratorType, List<String>> entry : namesByTypes.entrySet()) {
            YeomanGeneratorType type = entry.getKey();
            List<String> names = entry.getValue();
            for (String name : names) {
                targets.add("angular:".concat(type.getName().toLowerCase()));
                targets.add(name);
            }
        }

        BuildOptions buildOptions = dtoFactory.createDto(BuildOptions.class).withTargets(targets).withBuilderName("yeoman");
        builderAgent.build(buildOptions, "Using Yeoman generator...", "The Yeoman generator has finished",
                           "Failed to use Yeoman generator", "yeoman", this);

        // disable the button
        view.disableGenerateButton();
        // enable progress the button
        view.enableProgressOnGenerateButton();
    }


    /**
     * Remove an item from the data
     * @param type the type of item
     * @param name the name of the item
     * @param itemView the widget component
     */
    public void removeItem(YeomanGeneratorType type, String name, GeneratedItemView itemView) {
        // get names
        List<String> existingNames = namesByTypes.get(type);

        // contains element to remove
        if (existingNames != null && existingNames.contains(name)) {
            existingNames.remove(name);
            // No more items, remove the widget
            if (existingNames.isEmpty()) {
                namesByTypes.remove(type);
                FoldingPanel previous = widgetByTypes.remove(type);
                view.removeFoldingPanel(previous);
            } else {
                // remove the entry
                FoldingPanel selectedPanel = widgetByTypes.get(type);
                selectedPanel.remove(itemView);
            }

        }
        updateGenerateButton();

    }

    /**
     * Notify the view that it needs to either enable or disable the generate button.
     */
    protected void updateGenerateButton() {
        if (namesByTypes.isEmpty()) {
            view.disableGenerateButton();
            view.disableProgressOnGenerateButton();
        } else {
            view.enableGenerateButton();
        }
    }

    /**
     * @return the widgets
     */
    public Map<YeomanGeneratorType, FoldingPanel> getWidgetByTypes() {
        return widgetByTypes;
    }

    /**
     * @return the names
     */
    public Map<YeomanGeneratorType, List<String>> getNamesByTypes() {
        return namesByTypes;
    }
}
