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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStatusEvent;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStatusHandler;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.MachineAppliancePresenter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The class contains business logic to control displaying of machines on special view.
 *
 * @author Dmitry Shnurenko
 * @author Artem Zatsarynnyy
 */
@Singleton
public class MachinePanelPresenter extends BasePresenter implements MachinePanelView.ActionDelegate, MachineStatusHandler {

    private final MachinePanelView            view;
    private final MachineServiceClient        service;
    private final EntityFactory               entityFactory;
    private final MachineLocalizationConstant locale;
    private final MachineAppliancePresenter   appliance;

    private Machine selectedMachine;
    private boolean isFirstNode;

    @Inject
    public MachinePanelPresenter(MachinePanelView view,
                                 MachineServiceClient service,
                                 EntityFactory entityFactory,
                                 MachineLocalizationConstant locale,
                                 MachineAppliancePresenter appliance,
                                 EventBus eventBus) {
        this.view = view;
        this.view.setDelegate(this);

        this.service = service;
        this.entityFactory = entityFactory;
        this.locale = locale;
        this.appliance = appliance;

        eventBus.addHandler(MachineStatusEvent.TYPE, this);
    }

    /** Gets all machines and adds them to special place on view. */
    public void showMachines() {
        Promise<List<MachineDescriptor>> machinesPromise = service.getMachines(null);

        machinesPromise.then(new Operation<List<MachineDescriptor>>() {
            @Override
            public void apply(List<MachineDescriptor> machines) throws OperationException {
                if (machines.isEmpty()) {
                    appliance.showStub();
                }

                isFirstNode = true;

                List<MachineTreeNode> rootChildren = new ArrayList<>();

                MachineTreeNode rootNode = entityFactory.createMachineNode(null, "root", rootChildren);

                MachineTreeNode selectedNode = null;

                for (MachineDescriptor descriptor : machines) {
                    Machine machine = entityFactory.createMachine(descriptor);
                      Window.alert(descriptor.getRecipe().getScript());
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
    public void onMachineSelected(@Nonnull Machine selectedMachine) {
        if (this.selectedMachine != null && this.selectedMachine.equals(selectedMachine)) {
            return;
        }

        this.selectedMachine = selectedMachine;
        appliance.showAppliance(selectedMachine);
    }

    /** {@inheritDoc} */
    @Nonnull
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
    public void onMachineRunning(MachineStatusEvent event) {
        showMachines();
    }

    /** {@inheritDoc} */
    @Override
    public void onMachineDestroyed(MachineStatusEvent event) {
        showMachines();
    }
}
