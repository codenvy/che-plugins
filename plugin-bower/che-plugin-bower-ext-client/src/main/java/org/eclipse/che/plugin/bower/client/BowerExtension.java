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
package org.eclipse.che.plugin.bower.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.ide.Constants;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.extension.builder.client.BuilderLocalizationConstant;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.plugin.bower.client.menu.BowerInstallAction;
import org.eclipse.che.plugin.bower.client.menu.LocalizationConstant;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_BUILD_CONTEXT_MENU;
import static org.eclipse.che.ide.api.constraints.Anchor.AFTER;


/**
 * Extension registering Bower commands
 *
 * @author Florent Benoit
 */
@Singleton
@Extension(title = "Bower")
public class BowerExtension {

    @Inject
    public BowerExtension(ActionManager actionManager,
                          BuilderLocalizationConstant builderLocalizationConstant,
                          LocalizationConstant localizationConstantBower,
                          final BowerInstallAction bowerInstallAction,
                          EventBus eventBus,
                          final ProjectServiceClient projectServiceClient,
                          final DtoUnmarshallerFactory dtoUnmarshallerFactory) {

        actionManager.registerAction(localizationConstantBower.bowerInstallId(), bowerInstallAction);

        // Get Build menu
        DefaultActionGroup buildMenuActionGroup = (DefaultActionGroup)actionManager.getAction(GROUP_BUILD_CONTEXT_MENU);

        // create constraint
        Constraints afterBuildConstraints = new Constraints(AFTER, builderLocalizationConstant.buildProjectControlId());

        // Add Bower in build menu
        buildMenuActionGroup.add(bowerInstallAction, afterBuildConstraints);

        // Install Bower dependencies when projects is being opened and that there is no app/bower_components
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectCreated(ProjectActionEvent event) {

                final ProjectDescriptor project = event.getProject();
                boolean isBowerJsProject = isBowerJsProject(project);

                String projectPath = project.getPath();
                if (!projectPath.endsWith("/")) {
                    projectPath += "/";
                }

                if (isBowerJsProject) {

                    // Check if there is bower.json file
                    projectServiceClient.getFileContent(projectPath + "bower.json", new AsyncRequestCallback<String>() {
                        @Override
                        protected void onSuccess(String result) {
                            Unmarshallable<TreeElement> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(TreeElement.class);
                            projectServiceClient.getTree(project.getPath(), 2, new AsyncRequestCallback<TreeElement>(unmarshaller) {
                                @Override
                                protected void onSuccess(TreeElement treeElement) {
                                    ItemReference appDirectory = null;
                                    ItemReference bowerComponentsDirectory = null;
                                    for (TreeElement element : treeElement.getChildren()) {
                                        if ("app".equals(element.getNode().getName()) && "folder".equals(element.getNode().getType())) {
                                            appDirectory = element.getNode();
                                            for (TreeElement e : element.getChildren()) {
                                                if ("bower_components".equals(e.getNode().getName()) && "folder".equals(
                                                        e.getNode().getType())) {
                                                    bowerComponentsDirectory = e.getNode();
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                    }

                                    if (appDirectory != null) {
                                        // Bower configured for the project but not yet initialized ?
                                        if (bowerComponentsDirectory == null) {
                                            // Install bower dependencies as the 'app/bower_components' folder doesn't exist
                                            bowerInstallAction.installDependencies();
                                        }
                                    } else {
                                        // Install bower dependencies as the 'app' folder has not been found
                                        bowerInstallAction.installDependencies();
                                    }
                                }

                                @Override
                                protected void onFailure(Throwable ignore) {
                                    // nothing to do
                                }
                            });
                        }

                        @Override
                        protected void onFailure(Throwable ignore) {
                            // nothing to do
                        }
                    });
                }
            }

            @Override
            public void onProjectDeleted(ProjectActionEvent event) {

            }
        });

    }


    private boolean isBowerJsProject(@Nonnull ProjectDescriptor projectDescriptor) {
        Map<String, List<String>> attributes = projectDescriptor.getAttributes();
        if (attributes.containsKey(Constants.FRAMEWORK)) {
            List<String> frameworks = attributes.get(Constants.FRAMEWORK);
            return frameworks.contains("AngularJS") || frameworks.contains("BasicJS");
        }
        return false;
    }
}
