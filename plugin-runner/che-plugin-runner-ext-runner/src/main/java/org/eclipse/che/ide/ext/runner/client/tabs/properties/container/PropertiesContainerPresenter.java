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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.container;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.selection.Selection;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.selection.Selection.ENVIRONMENT;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
@Singleton
public class PropertiesContainerPresenter implements PropertiesContainer,
                                                     PropertiesContainerView.ActionDelegate,
                                                     SelectionManager.SelectionChangeListener,
                                                     PropertiesPanelPresenter.RemovePanelListener {

    private final PropertiesContainerView           view;
    private final SelectionManager                  selectionManager;
    private final WidgetFactory                     widgetFactory;
    private final AppContext                        appContext;
    private final Map<Runner, PropertiesPanel>      runnerPanels;
    private final Map<Environment, PropertiesPanel> environmentsPanels;

    private PropertiesPanel currentPanel;
    private PropertiesPanel stubPanel;

    @Inject
    public PropertiesContainerPresenter(PropertiesContainerView view,
                                        WidgetFactory widgetFactory,
                                        SelectionManager selectionManager,
                                        AppContext appContext) {
        this.view = view;
        this.selectionManager = selectionManager;
        this.view.setDelegate(this);
        this.widgetFactory = widgetFactory;
        this.appContext = appContext;
        stubPanel = widgetFactory.createPropertiesPanel();

        runnerPanels = new HashMap<>();
        environmentsPanels = new HashMap<>();

        selectionManager.addListener(this);
    }

    /** {@inheritDoc} */
    @Override
    public void show(@Nullable Runner runner) {
        if (runner == null) {
            view.clear();
            return;
        }

        // we save current panel if our container isn't shown and then we will show this panel when container is shown
        currentPanel = runnerPanels.get(runner);
        if (currentPanel == null) {
            currentPanel = widgetFactory.createPropertiesPanel(runner);
            currentPanel.hideButtonsPanel();
            runnerPanels.put(runner, currentPanel);
        }

        currentPanel.update(runner);
        view.showWidget(currentPanel);
    }

    /** {@inheritDoc} */
    @Override
    public void show(@Nullable Environment environment) {
        if (environment == null || appContext.getCurrentProject() == null) {
            view.showWidget(stubPanel);
            return;
        }

        currentPanel = environmentsPanels.get(environment);

        if (currentPanel == null) {
            currentPanel = widgetFactory.createPropertiesPanel(environment);
            currentPanel.addListener(this);
            environmentsPanels.put(environment, currentPanel);
        }

        currentPanel.update(environment);
        view.showWidget(currentPanel);
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        runnerPanels.clear();
        view.clear();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        if (currentPanel != null) {
            view.showWidget(currentPanel);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectionChanged(@Nonnull Selection selection) {
        if (ENVIRONMENT.equals(selection)) {
            return;
        }

        Runner runner = selectionManager.getRunner();
        if (runner == null) {
            return;
        }

        show(runner);
    }

    /** {@inheritDoc} */
    @Override
    public void onPanelRemoved(@Nonnull Environment environment) {
        environmentsPanels.remove(environment);
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleDummyContent(boolean visible) {
        view.setVisibleDummyContent(visible);
    }
}