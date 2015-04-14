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
package org.eclipse.che.ide.ext.runner.client.actions;

import com.google.inject.Inject;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.permits.ActionDenyAccessDialog;
import org.eclipse.che.ide.api.action.permits.ActionPermit;
import org.eclipse.che.ide.api.action.permits.Run;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManager;
import org.eclipse.che.ide.ext.runner.client.models.Environment;

import javax.annotation.Nonnull;

/**
 * Action which allows run project with default runner parameters.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
public class RunAction extends AbstractRunnerActions {

    private final RunnerManager              runnerManager;
    private final DtoFactory                 dtoFactory;
    private final ChooseRunnerAction         chooseRunnerAction;
    private final NotificationManager        notificationManager;
    private final RunnerLocalizationConstant locale;
    private final AppContext                 appContext;
    private final AnalyticsEventLogger       eventLogger;
    private final ActionPermit runActionPermit;
    private final ActionDenyAccessDialog runActionDenyAccessDialog;

    @Inject
    public RunAction(RunnerManager runnerManager,
                     RunnerLocalizationConstant locale,
                     AppContext appContext,
                     NotificationManager notificationManager,
                     ChooseRunnerAction chooseRunnerAction,
                     DtoFactory dtoFactory,
                     RunnerResources resources,
                     AnalyticsEventLogger eventLogger,
                     @Run ActionPermit runActionPermit,
                     @Run ActionDenyAccessDialog runActionDenyAccessDialog) {
        super(appContext, locale.actionRun(), locale.actionRunDescription(), resources.run());

        this.runnerManager = runnerManager;
        this.dtoFactory = dtoFactory;
        this.appContext = appContext;
        this.chooseRunnerAction = chooseRunnerAction;
        this.notificationManager = notificationManager;
        this.locale = locale;
        this.eventLogger = eventLogger;
        this.runActionPermit = runActionPermit;
        this.runActionDenyAccessDialog = runActionDenyAccessDialog;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(@Nonnull ActionEvent event) {
        eventLogger.log(this);
        if (runActionPermit.isAllowed()) {
            CurrentProject currentProject = appContext.getCurrentProject();
            if (currentProject == null) {
                return;
            }

            String defaultRunner = currentProject.getRunner();
            Environment environment = chooseRunnerAction.selectEnvironment();

            if (environment == null && defaultRunner == null) {
                notificationManager.showError(locale.actionRunnerNotSpecified());
            }

            if (environment == null || (defaultRunner != null && defaultRunner.equals(environment.getId()))) {
                runnerManager.launchRunner();
            } else {
                RunOptions runOptions = dtoFactory.createDto(RunOptions.class)
                                                  .withOptions(environment.getOptions())
                                                  .withEnvironmentId(environment.getId())
                                                  .withMemorySize(environment.getRam());

                runnerManager.launchRunner(runOptions, environment.getName());
            }
        } else {
            runActionDenyAccessDialog.show();
        }
    }
}