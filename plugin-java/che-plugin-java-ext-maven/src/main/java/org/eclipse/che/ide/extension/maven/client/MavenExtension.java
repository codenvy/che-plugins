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

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.TreeStructureProviderRegistry;
import org.eclipse.che.ide.api.project.type.wizard.PreSelectedProjectTypeManager;
import org.eclipse.che.ide.ext.java.client.dependenciesupdater.DependenciesUpdater;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateEvent;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateHandler;
import org.eclipse.che.ide.extension.maven.client.actions.CreateMavenModuleAction;
import org.eclipse.che.ide.extension.maven.client.actions.UpdateDependencyAction;
import org.eclipse.che.ide.extension.maven.client.event.BeforeModuleOpenEvent;
import org.eclipse.che.ide.extension.maven.client.event.BeforeModuleOpenHandler;
import org.eclipse.che.ide.extension.maven.client.projecttree.MavenProjectTreeStructureProvider;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private        ProjectDescriptor     project;

    @Inject
    public MavenExtension(TreeStructureProviderRegistry treeStructureProviderRegistry,
                          PreSelectedProjectTypeManager preSelectedProjectManager) {

        treeStructureProviderRegistry.associateProjectTypeToTreeProvider(MavenAttributes.MAVEN_ID, MavenProjectTreeStructureProvider.ID);

        preSelectedProjectManager.setProjectTypeIdToPreselect(MavenAttributes.MAVEN_ID, 100);

        archetypes =
                Arrays.asList(new MavenArchetype("org.apache.maven.archetypes", "maven-archetype-quickstart", "RELEASE", null),
                              new MavenArchetype("org.apache.maven.archetypes", "maven-archetype-webapp", "RELEASE", null),
                              new MavenArchetype("org.apache.openejb.maven", "tomee-webapp-archetype", "1.7.1", null));
    }

    public static List<MavenArchetype> getAvailableArchetypes() {
        return archetypes;
    }

    @Inject
    private void bindEvents(final EventBus eventBus, final DependenciesUpdater dependenciesUpdater) {
        eventBus.addHandler(BeforeModuleOpenEvent.TYPE, new BeforeModuleOpenHandler() {
            @Override
            public void onBeforeModuleOpen(BeforeModuleOpenEvent event) {
                if (isValidForResolveDependencies(event.getModule().getProject().getData())) {
                    dependenciesUpdater.updateDependencies(event.getModule().getData());
                }
            }
        });

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                project = event.getProject();
                if (isValidForResolveDependencies(project)) {
                    dependenciesUpdater.updateDependencies(project);
                }
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
            }
        });

        eventBus.addHandler(MachineStateEvent.TYPE, new MachineStateHandler() {
            @Override
            public void onMachineRunning(MachineStateEvent event) {
                if (project != null) {
                    if (isValidForResolveDependencies(project)) {
                        new Timer() {
                            @Override
                            public void run() {
                                dependenciesUpdater.updateDependencies(project);
                            }
                        }.schedule(5000);
                    }
                }
            }

            @Override
            public void onMachineDestroyed(MachineStateEvent event) {
            }
        });
    }

    @Inject
    private void prepareActions(ActionManager actionManager,
                                UpdateDependencyAction updateDependencyAction,
                                CreateMavenModuleAction createMavenModuleAction) {
        // register actions
        actionManager.registerAction("updateDependency", updateDependencyAction);
        actionManager.registerAction("createMavenModule", createMavenModuleAction);

        // add actions in main menu
        DefaultActionGroup buildMenuActionGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_CODE);
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
        return !(attr.containsKey(MavenAttributes.PACKAGING) && "pom".equals(attr.get(MavenAttributes.PACKAGING).get(0)));
    }
}
