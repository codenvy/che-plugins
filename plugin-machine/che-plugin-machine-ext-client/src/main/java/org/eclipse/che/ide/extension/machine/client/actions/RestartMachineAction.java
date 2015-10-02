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
package org.eclipse.che.ide.extension.machine.client.actions;

import com.google.inject.Inject;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachinePanelPresenter;

import javax.validation.constraints.NotNull;
import java.util.Collections;

import static org.eclipse.che.ide.extension.machine.client.perspective.MachinePerspective.MACHINE_PERSPECTIVE_ID;

/**
 * The action contains business logic which calls special method to restart machine.
 *
 * @author Dmitry Shnurenko
 */
public class RestartMachineAction extends AbstractPerspectiveAction {

    private final MachinePanelPresenter       panelPresenter;
    private final MachineManager              machineManager;
    private final MachineLocalizationConstant locale;
    private final AnalyticsEventLogger        eventLogger;

    private Machine selectedMachine;

    @Inject
    public RestartMachineAction(MachineLocalizationConstant locale,
                                MachinePanelPresenter panelPresenter,
                                MachineManager machineManager,
                                AnalyticsEventLogger eventLogger) {
        super(Collections.singletonList(MACHINE_PERSPECTIVE_ID),
              locale.controlMachineRestartText(),
              locale.controlMachineRestartTooltip(),
              null, null);

        this.panelPresenter = panelPresenter;
        this.locale = locale;
        this.eventLogger = eventLogger;
        this.machineManager = machineManager;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        selectedMachine = panelPresenter.getSelectedMachine();
        event.getPresentation().setEnabled(selectedMachine != null && !selectedMachine.isDev());
        event.getPresentation().setText(selectedMachine != null ? locale.machineRestartTextByName(selectedMachine.getDisplayName())
                                                                : locale.controlMachineRestartText());
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        machineManager.restartMachine(selectedMachine);
    }
}
