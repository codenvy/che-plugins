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
package org.eclipse.che.plugin.yeoman.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.plugin.yeoman.client.panel.YeomanPartPresenter;

/**
 * Extension registering Yeoman Panel
 *
 * @author Florent Benoit
 */
@Singleton
@Extension(title = "Yeoman")
public class YeomanExtension {

    @Inject
    public YeomanExtension(final YeomanResources resources,
                           final WorkspaceAgent workspaceAgent,
                           final YeomanPartPresenter yeomanPartPresenter,
                           final EventBus eventBus) {
        // inject CSS
        resources.uiCss().ensureInjected();

        // Display Yeoman Panel with this extension
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectCreated(ProjectActionEvent event) {

                ProjectDescriptor project = event.getProject();
                final String projectTypeId = project.getType();
                boolean isJSProject = projectTypeId.endsWith("JS");
                if (isJSProject) {
                    // add Yeoman panel
                    workspaceAgent.openPart(yeomanPartPresenter, PartStackType.TOOLING);
                    workspaceAgent.hidePart(yeomanPartPresenter);
                }
            }

            @Override
            public void onProjectDeleted(ProjectActionEvent event) {
                ProjectDescriptor project = event.getProject();
                final String projectTypeId = project.getType();
                boolean isJSProject = projectTypeId.endsWith("JS");
                if (isJSProject) {
                    workspaceAgent.removePart(yeomanPartPresenter);
                }
            }
        });
    }
}
