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
package org.eclipse.che.plugin.npm.client.menu;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.plugin.npm.client.builder.BuildFinishedCallback;
import org.eclipse.che.plugin.npm.client.builder.BuilderAgent;

import java.util.Arrays;
import java.util.List;

/**
 * Action that install NPM dependencies.
 * @author Florent Benoit
 */
public class NpmInstallAction extends CustomAction implements BuildFinishedCallback {

    private DtoFactory dtoFactory;

    private BuilderAgent builderAgent;

    private EventBus eventBus;
    private final NewProjectExplorerPresenter projectExplorer;

    private boolean buildInProgress;

    private final AnalyticsEventLogger analyticsEventLogger;

    @Inject
    public NpmInstallAction(LocalizationConstant localizationConstant,
                            DtoFactory dtoFactory, BuilderAgent builderAgent, AppContext appContext,
                            AnalyticsEventLogger analyticsEventLogger, EventBus eventBus,
                            NewProjectExplorerPresenter projectExplorer) {
        super(appContext, localizationConstant.npmInstallText(), localizationConstant.npmInstallDescription());
        this.dtoFactory = dtoFactory;
        this.builderAgent = builderAgent;
        this.analyticsEventLogger = analyticsEventLogger;
        this.eventBus = eventBus;
        this.projectExplorer = projectExplorer;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        analyticsEventLogger.log(this);
        installDependencies();
    }


    public void installDependencies() {
        buildInProgress = true;
        List<String> targets = Arrays.asList("install");
        BuildOptions buildOptions = dtoFactory.createDto(BuildOptions.class).withTargets(targets).withBuilderName("npm");
        builderAgent.build(buildOptions, "Installation of npm dependencies...", "Npm dependencies successfully downloaded",
                           "Npm dependencies install failed", "npm", this);
    }

    @Override
    public void onFinished(BuildStatus buildStatus) {
        // and refresh the tree if success
        if (buildStatus == BuildStatus.SUCCESSFUL) {
//            eventBus.fireEvent(new RefreshProjectTreeEvent());
            projectExplorer.synchronizeTree();
        }

        // build finished
        buildInProgress = false;

    }


    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!buildInProgress);
    }
}
