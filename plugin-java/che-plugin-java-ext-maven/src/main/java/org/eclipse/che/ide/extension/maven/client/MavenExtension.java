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
package org.eclipse.che.ide.extension.maven.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.type.wizard.PreSelectedProjectTypeManager;
import org.eclipse.che.ide.ext.java.client.dependenciesupdater.DependenciesUpdater;
import org.eclipse.che.ide.extension.maven.client.actions.CreateMavenModuleAction;
import org.eclipse.che.ide.extension.maven.client.actions.CustomBuildAction;
import org.eclipse.che.ide.extension.maven.client.actions.UpdateDependencyAction;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_BUILD;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_BUILD_CONTEXT_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_FILE_NEW;

/**
 * Maven extension entry point.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
@Extension(title = "Maven", version = "3.0.0")
public class MavenExtension {
    private static List<MavenArchetype> archetypes;

    @Inject
    public MavenExtension(PreSelectedProjectTypeManager preSelectedProjectManager) {

        preSelectedProjectManager.setProjectTypeIdToPreselect(MavenAttributes.MAVEN_ID, 100);

        archetypes = Arrays.asList(new MavenArchetype("org.apache.maven.archetypes", "maven-archetype-quickstart", "RELEASE", null),
                                   new MavenArchetype("org.apache.maven.archetypes", "maven-archetype-webapp", "RELEASE", null),
                                   new MavenArchetype("org.apache.openejb.maven", "tomee-webapp-archetype", "1.7.1", null),
                                   new MavenArchetype("org.apache.cxf.archetype", "cxf-jaxws-javafirst", "RELEASE", null),
                                   new MavenArchetype("org.apache.cxf.archetype", "cxf-jaxrs-service", "RELEASE", null));
    }

    public static List<MavenArchetype> getAvailableArchetypes() {
        return archetypes;
    }

    @Inject
    private void bindEvents(final EventBus eventBus,
                            final DependenciesUpdater dependenciesUpdater,
                            final ProjectServiceClient projectServiceClient,
                            final DtoUnmarshallerFactory dtoUnmarshallerFactory) {

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                ProjectDescriptor project = event.getProject();
                if (isValidForResolveDependencies(project)) {
                    dependenciesUpdater.updateDependencies(project, false);
                }
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
            }
        });
    }

    @Inject
    private void prepareActions(ActionManager actionManager,
                                CustomBuildAction customBuildAction,
                                UpdateDependencyAction updateDependencyAction,
                                MavenLocalizationConstant mavenLocalizationConstants,
                                CreateMavenModuleAction createMavenModuleAction) {
        // register actions
        actionManager.registerAction(mavenLocalizationConstants.buildProjectControlId(), customBuildAction);
        actionManager.registerAction("updateDependency", updateDependencyAction);
        actionManager.registerAction("createMavenModule", createMavenModuleAction);

        // add actions in main menu
        DefaultActionGroup buildMenuActionGroup = (DefaultActionGroup)actionManager.getAction(GROUP_BUILD);
        buildMenuActionGroup.add(customBuildAction);
        buildMenuActionGroup.add(updateDependencyAction);

        DefaultActionGroup newGroup = (DefaultActionGroup)actionManager.getAction(GROUP_FILE_NEW);
        newGroup.add(createMavenModuleAction, new Constraints(Anchor.AFTER, "newProject"));

        // add actions in context menu
        DefaultActionGroup buildContextMenuGroup = (DefaultActionGroup)actionManager.getAction(GROUP_BUILD_CONTEXT_MENU);
        buildContextMenuGroup.addSeparator();
        buildContextMenuGroup.addAction(updateDependencyAction);
    }

    @Inject
    private void registerIcons(IconRegistry iconRegistry, MavenResources mavenResources) {
        iconRegistry.registerIcon(new Icon("maven.module", mavenResources.module()));
        // icons for file names
        iconRegistry.registerIcon(new Icon("maven/pom.xml.file.small.icon", mavenResources.maven()));
    }

    private boolean isValidForResolveDependencies(ProjectDescriptor project) {
        Map<String, List<String>> attr = project.getAttributes();
        BuildersDescriptor builders = project.getBuilders();
        return builders != null && "maven".equals(builders.getDefault()) &&
               !(attr.containsKey(MavenAttributes.PACKAGING) && "pom".equals(attr.get(MavenAttributes.PACKAGING).get(0)));
    }
}
