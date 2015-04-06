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
package org.eclipse.che.ide.ext.java.jdi.client.actions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.client.debug.remotedebug.RemoteDebugPresenter;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.api.project.type.RunnerCategory.JAVA;

/**
 * Action which allows connect debugger to remote server.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class RemoteDebugAction extends ProjectAction {

    private final AppContext           appContext;
    private final RemoteDebugPresenter presenter;
    private final AnalyticsEventLogger eventLogger;
    private final ProjectTypeRegistry  typeRegistry;

    @Inject
    public RemoteDebugAction(AppContext appContext,
                             RemoteDebugPresenter presenter,
                             JavaRuntimeLocalizationConstant locale,
                             ProjectTypeRegistry typeRegistry,
                             AnalyticsEventLogger eventLogger) {
        super(locale.connectToRemote(), locale.connectToRemoteDescription());

        this.appContext = appContext;
        this.typeRegistry = typeRegistry;
        this.eventLogger = eventLogger;
        this.presenter = presenter;
    }

    /** {@inheritDoc} */
    @Override
    public void updateProjectAction(@Nonnull ActionEvent actionEvent) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        String projectType = currentProject.getProjectDescription().getType();
        ProjectTypeDefinition definition = typeRegistry.getProjectType(projectType);

        String category = definition.getRunnerCategories().get(0);

        actionEvent.getPresentation().setEnabledAndVisible(JAVA.toString().equals(category));
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(@Nonnull ActionEvent actionEvent) {
        eventLogger.log(this);

        presenter.showDialog();
    }
}
