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
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import javax.annotation.Nonnull;
import java.util.Collections;

import static org.eclipse.che.ide.extension.machine.client.perspective.MachinePerspective.MACHINE_PERSPECTIVE_ID;

/**
 * The action contains business logic which calls special method to destroy machine.
 *
 * @author Dmitry Shnurenko
 */
public class DestroyMachineAction extends AbstractPerspectiveAction {

    private final MachineLocalizationConstant locale;
    private final MachinePanelPresenter       panelPresenter;
    private final MachineManager              machineManager;
    private final AnalyticsEventLogger        eventLogger;
    private final DialogFactory               dialogFactory;

    @Inject
    public DestroyMachineAction(MachineLocalizationConstant locale,
                                MachinePanelPresenter panelPresenter,
                                MachineManager machineManager,
                                AnalyticsEventLogger eventLogger,
                                DialogFactory dialogFactory) {
        super(Collections.singletonList(MACHINE_PERSPECTIVE_ID),
              locale.machineDestroyTitle(),
              locale.machineDestroyDescription(),
              null, null);

        this.locale = locale;
        this.panelPresenter = panelPresenter;
        this.machineManager = machineManager;
        this.eventLogger = eventLogger;
        this.dialogFactory = dialogFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@Nonnull ActionEvent event) {
        final Machine selectedMachine = panelPresenter.getSelectedMachine();
        event.getPresentation().setEnabled(selectedMachine != null);
        event.getPresentation().setText(selectedMachine != null ? locale.machineDestroyTitle(selectedMachine.getDisplayName())
                                                                : locale.machineDestroyTitle());
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(@Nonnull ActionEvent event) {
        eventLogger.log(this);

        final Machine selectedMachine = panelPresenter.getSelectedMachine();
        if (selectedMachine == null) {
            return;
        }

//        if (selectedMachine.isDev()) {
//            dialogFactory.createMessageDialog(locale.devMachineCanNotBeDestroyedTitle(), locale.devMachineCanNotBeDestroyed(), null).show();
//            return;
//        }

        machineManager.destroyMachine(selectedMachine);
    }
}
