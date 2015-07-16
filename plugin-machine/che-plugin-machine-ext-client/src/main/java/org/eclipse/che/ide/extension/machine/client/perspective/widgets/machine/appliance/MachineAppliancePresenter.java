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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.client.inject.factories.TabItemFactory;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.inject.factories.WidgetsFactory;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.server.ServerPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.sufficientinfo.MachineInfoPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.terminal.TerminalPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachinePanelPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.RecipePartPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.container.RecipesContainerPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.Tab;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.container.TabContainerPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.container.TabContainerView.TabSelectHandler;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.content.TabPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.header.TabHeader;
import org.eclipse.che.ide.part.PartStackPresenter;
import org.eclipse.che.ide.part.PartsComparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The class is a container for tab panels which display additional information about machine and adds ability to control machine's
 * processes. The class is a wrapper of {@link TabContainerPresenter} to use logic  of {@link TabContainerPresenter} for control tabs
 * instead {@link PartStackPresenter}
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@Singleton
public class MachineAppliancePresenter extends PartStackPresenter implements ActivePartChangedHandler {

    private final MachineApplianceView      view;
    private final TabContainerPresenter     tabContainer;
    private final TerminalPresenter         terminalPresenter;
    private final MachineInfoPresenter      infoPresenter;
    private final ServerPresenter           serverPresenter;
    private final RecipesContainerPresenter recipesContainerPresenter;
    private final WidgetsFactory            widgetsFactory;
    private final EntityFactory             entityFactory;

    private Machine selectedMachine;

    @Inject
    public MachineAppliancePresenter(EventBus eventBus,
                                     PartsComparator partsComparator,
                                     PartStackEventHandler partStackEventHandler,
                                     MachineApplianceView view,
                                     final MachineLocalizationConstant locale,
                                     WidgetsFactory widgetsFactory,
                                     EntityFactory entityFactory,
                                     TabItemFactory tabItemFactory,
                                     TerminalPresenter terminalPresenter,
                                     MachineInfoPresenter infoPresenter,
                                     RecipesContainerPresenter recipesContainerPresenter,
                                     ServerPresenter serverPresenter,
                                     TabContainerPresenter tabContainer) {
        super(eventBus, partStackEventHandler, tabItemFactory, partsComparator, view, null);

        this.view = view;
        this.tabContainer = tabContainer;
        this.terminalPresenter = terminalPresenter;
        this.recipesContainerPresenter = recipesContainerPresenter;
        this.infoPresenter = infoPresenter;
        this.serverPresenter = serverPresenter;
        this.widgetsFactory = widgetsFactory;
        this.entityFactory = entityFactory;

        final String terminalTabName = locale.tabTerminal();
        final String infoTabName = locale.tabInfo();
        final String serverTabName = locale.tabServer();

        TabSelectHandler terminalHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                selectedMachine.setActiveTabName(terminalTabName);
            }
        };
        createAndAddTab(terminalTabName, terminalPresenter, terminalHandler);

        TabSelectHandler infoHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                selectedMachine.setActiveTabName(infoTabName);
            }
        };
        createAndAddTab(infoTabName, infoPresenter, infoHandler);

        TabSelectHandler serverHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                selectedMachine.setActiveTabName(serverTabName);
            }
        };
        createAndAddTab(serverTabName, serverPresenter, serverHandler);

        this.view.showContainer(tabContainer.getView().asWidget());
        eventBus.addHandler(ActivePartChangedEvent.TYPE, this);
    }

    private void createAndAddTab(@Nonnull String tabName, @Nonnull TabPresenter content, @Nullable TabSelectHandler handler) {
        TabHeader header = widgetsFactory.createTabHeader(tabName);
        Tab tab = entityFactory.createTab(header, content, handler);

        tabContainer.addTab(tab);
    }

    /**
     * Shows all information and processes about current machine.
     *
     * @param machine
     *         machine for which need show info
     */
    public void showAppliance(@Nonnull Machine machine) {
        selectedMachine = machine;

        view.showContainer(tabContainer.getView().asWidget());

        tabContainer.showTab(machine.getActiveTabName());

        terminalPresenter.updateTerminal(machine);
        infoPresenter.update(machine);
        serverPresenter.updateInfo(machine);
    }

    /** {@inheritDoc} */
    @Override
    public void onActivePartChanged(ActivePartChangedEvent event) {
        if (event.getActivePart() instanceof RecipePartPresenter) {
            view.showContainer(recipesContainerPresenter.getView());
        } else if (event.getActivePart() instanceof MachinePanelPresenter) {
            view.showContainer(tabContainer.getView());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** Shows special stub panel when no machine exist. */
    public void showStub() {
        view.showContainer(null);
    }
}
