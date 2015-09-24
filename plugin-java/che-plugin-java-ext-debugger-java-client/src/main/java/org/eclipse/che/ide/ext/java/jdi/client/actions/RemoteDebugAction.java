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
import org.eclipse.che.api.project.shared.dto.AttributeDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.client.debug.remotedebug.RemoteDebugPresenter;
import org.eclipse.che.ide.ext.java.shared.Constants;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Action which allows connect debugger to remote server.
 *
 * @author Dmitry Shnurenko
 * @author Anatoliy Bazko
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
    public void updateProjectAction(@NotNull ActionEvent actionEvent) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        String language = getProjectLanguage(currentProject);
        boolean debuggable = isDebuggable(language);

        actionEvent.getPresentation().setEnabledAndVisible(debuggable);
    }

    private String getProjectLanguage(CurrentProject currentProject) {
        String projectType = currentProject.getProjectDescription().getType();
        ProjectTypeDefinition definition = typeRegistry.getProjectType(projectType);

        if (definition != null) {
            for (AttributeDescriptor attr : definition.getAttributeDescriptors()) {
                if (attr.getName().equals(Constants.LANGUAGE)) {
                    List<String> values = attr.getValues();
                    if (!values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
        }

        return null;
    }

    private boolean isDebuggable(String language) {
        return language != null && language.equals("java");
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(@NotNull ActionEvent actionEvent) {
        eventLogger.log(this);
        presenter.showDialog();
    }
}
