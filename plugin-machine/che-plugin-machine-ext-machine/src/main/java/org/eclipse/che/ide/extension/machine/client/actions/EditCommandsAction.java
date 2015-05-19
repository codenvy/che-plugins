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
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.command.edit.EditConfigurationsPresenter;

/**
 * @author Artem Zatsarynnyy
 */
@Singleton
public class EditCommandsAction extends Action {
    private final AppContext                  appContext;
    private final EditConfigurationsPresenter presenter;
    private final AnalyticsEventLogger        eventLogger;

    @Inject
    public EditCommandsAction(EditConfigurationsPresenter presenter,
                              MachineLocalizationConstant localizationConstant,
                              AppContext appContext,
                              AnalyticsEventLogger eventLogger) {
        super(localizationConstant.editConfigurationsControlTitle(), localizationConstant.editConfigurationsControlDescription(), null);
        this.presenter = presenter;
        this.appContext = appContext;
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
        presenter.show();
    }
}
