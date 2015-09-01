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
package org.eclipse.che.plugin.bower.client.menu;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.plugin.bower.client.BowerResources;
import org.eclipse.che.plugin.bower.client.builder.BuildFinishedCallback;
import org.eclipse.che.plugin.bower.client.builder.BuilderAgent;

import java.util.Arrays;
import java.util.List;

/**
 * Action that install bower dependencies.
 * @author Florent Benoit
 */
public class BowerInstallAction extends CustomAction implements BuildFinishedCallback {

    private DtoFactory dtoFactory;

    private BuilderAgent builderAgent;

    private EventBus eventBus;
    private final NewProjectExplorerPresenter projectExplorer;

    private boolean buildInProgress;

    private final AnalyticsEventLogger analyticsEventLogger;
    private       AppContext           appContext;

    @Inject
    public BowerInstallAction(LocalizationConstant localizationConstant,
                              DtoFactory dtoFactory,
                              BuilderAgent builderAgent,
                              AppContext appContext,
                              EventBus eventBus,
                              BowerResources bowerResources,
                              AnalyticsEventLogger analyticsEventLogger,
                              NewProjectExplorerPresenter projectExplorer) {
        super(appContext, localizationConstant.bowerInstallText(), localizationConstant.bowerInstallDescription(),
              bowerResources.buildIcon());
        this.dtoFactory = dtoFactory;
        this.builderAgent = builderAgent;
        this.appContext = appContext;
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
        BuildOptions buildOptions = dtoFactory.createDto(BuildOptions.class).withTargets(targets).withBuilderName("bower");
        builderAgent.build(buildOptions, "Installation of Bower dependencies...", "Bower dependencies successfully downloaded",
                           "Bower install failed", "bower", this);
    }

    @Override
    public void onFinished(BuildStatus buildStatus) {
        // and refresh the tree if success
        if (buildStatus == BuildStatus.SUCCESSFUL) {
            projectExplorer.synchronizeTree();
        }
        buildInProgress = false;
        appContext.getCurrentProject().setIsRunningEnabled(true);
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!buildInProgress);
    }

}
