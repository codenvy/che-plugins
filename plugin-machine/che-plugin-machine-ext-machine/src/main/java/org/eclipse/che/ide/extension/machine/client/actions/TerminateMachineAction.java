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
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;

/**
 * Action to destroy the current machine (to which current project is bound).
 *
 * @author Artem Zatsarynnyy
 */
public class TerminateMachineAction extends Action {

    private final AppContext           appContext;
    private final MachineManager       machineManager;
    private final AnalyticsEventLogger eventLogger;

    @Inject
    public TerminateMachineAction(AppContext appContext,
                                  MachineLocalizationConstant localizationConstant,
                                  MachineManager machineManager,
                                  AnalyticsEventLogger eventLogger) {
        super(localizationConstant.terminateMachineControlTitle(), localizationConstant.terminateMachineControlDescription(), null, null);
        this.appContext = appContext;
        this.machineManager = machineManager;
        this.eventLogger = eventLogger;
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        e.getPresentation().setVisible(appContext.getCurrentProject() != null);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        final String currentMachineId = machineManager.getCurrentMachineId();
        if (currentMachineId == null) {
            return;
        }

        machineManager.destroyMachine(currentMachineId);
    }
}
