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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateEvent;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateHandler;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.MachineAppliancePresenter;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * The class contains business logic to control displaying of machines on special view.
 *
 * @author Dmitry Shnurenko
 * @author Artem Zatsarynnyy
 */
@Singleton
public class MachinePanelPresenter extends BasePresenter implements MachinePanelView.ActionDelegate,
                                                                    MachineStateHandler,
                                                                    ExtServerStateHandler {

    private final MachinePanelView            view;
    private final MachineServiceClient        service;
    private final EntityFactory               entityFactory;
    private final MachineLocalizationConstant locale;
    private final MachineAppliancePresenter   appliance;
    private final MachineResources            resources;
    private final AppContext                  appContext;

    private Machine selectedMachine;
    private boolean isFirstNode;

    @Inject
    public MachinePanelPresenter(MachinePanelView view,
                                 MachineServiceClient service,
                                 EntityFactory entityFactory,
                                 MachineLocalizationConstant locale,
                                 MachineAppliancePresenter appliance,
                                 EventBus eventBus,
                                 MachineResources resources,
                                 AppContext appContext) {
        this.view = view;
        this.view.setDelegate(this);

        this.service = service;
        this.entityFactory = entityFactory;
        this.locale = locale;
        this.appliance = appliance;
        this.resources = resources;
        this.appContext = appContext;

        eventBus.addHandler(MachineStateEvent.TYPE, this);
        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
    }

    /** Gets all machines and adds them to special place on view. */
    public void showMachines() {
        String workspaceId = appContext.getWorkspace().getId();

        Promise<List<MachineDescriptor>> machinesPromise = service.getWorkspaceMachines(workspaceId);

        machinesPromise.then(new Operation<List<MachineDescriptor>>() {
            @Override
            public void apply(List<MachineDescriptor> machines) throws OperationException {
                if (machines.isEmpty()) {
                    appliance.showStub();
                    selectedMachine = null;
                }

                isFirstNode = true;

                List<MachineTreeNode> rootChildren = new ArrayList<>();

                MachineTreeNode rootNode = entityFactory.createMachineNode(null, "root", rootChildren);

                MachineTreeNode selectedNode = null;

                for (MachineDescriptor descriptor : machines) {
                    Machine machine = entityFactory.createMachine(descriptor);
                    MachineTreeNode machineNode = entityFactory.createMachineNode(rootNode, machine, null);

                    rootChildren.add(machineNode);

                    if (isFirstNode) {
                        selectedNode = machineNode;

                        isFirstNode = false;
                    }
                }

                view.setData(rootNode);

                view.selectNode(selectedNode);
            }
        });
    }

    public Machine getSelectedMachine() {
        return selectedMachine;
    }

    /** {@inheritDoc} */
    @Override
    public void onMachineSelected(@NotNull Machine selectedMachine) {
        if (this.selectedMachine != null && this.selectedMachine.equals(selectedMachine)) {
            return;
        }

        this.selectedMachine = selectedMachine;
        appliance.showAppliance(selectedMachine);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getTitle() {
        return locale.machinePanelTitle();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    @Nullable
    @Override
    public SVGResource getTitleSVGImage() {
        return resources.machinesPartIcon();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getTitleToolTip() {
        return locale.machinePanelTooltip();
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Override
    public void onMachineRunning(MachineStateEvent event) {
        showMachines();
    }

    /** {@inheritDoc} */
    @Override
    public void onMachineDestroyed(MachineStateEvent event) {
        showMachines();
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        showMachines();
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
    }
}
