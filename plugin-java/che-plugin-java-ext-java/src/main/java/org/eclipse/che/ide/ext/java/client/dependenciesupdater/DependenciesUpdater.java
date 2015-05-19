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
package org.eclipse.che.ide.ext.java.client.dependenciesupdater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.ExternalLibrariesNode;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.loging.Log;

import java.util.LinkedList;
import java.util.Queue;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Updates dependencies for Maven project.
 *
 * @author Artem Zatsarynnyy
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class DependenciesUpdater {
//    private final BuilderConsolePresenter          builderConsole;
    private final NotificationManager              notificationManager;
    private final EditorAgent                      editorAgent;
//    private final BuildController                  buildController;
    private final DtoUnmarshallerFactory           dtoUnmarshallerFactory;
    private final EventBus                         eventBus;
    private final AppContext                       context;
    private final JavaNameEnvironmentServiceClient nameEnvironmentServiceClient;
    private final JavaLocalizationConstant         javaLocalizationConstant;

    private Queue<Pair<ProjectDescriptor, Boolean>> projects = new LinkedList<>();
    private boolean                                 updating = false;
    private JavaTreeStructure javaTreeStructure;
    private Notification      notification;

    @Inject
    public DependenciesUpdater(JavaLocalizationConstant javaLocalizationConstant,
                               NotificationManager notificationManager,
                               EditorAgent editorAgent,
//                               BuildController buildController,
                               DtoUnmarshallerFactory dtoUnmarshallerFactory,
                               EventBus eventBus,
                               AppContext context,
                               JavaNameEnvironmentServiceClient nameEnvironmentServiceClient
                               /*BuilderConsolePresenter builderConsole*/) {
        this.javaLocalizationConstant = javaLocalizationConstant;
        this.notificationManager = notificationManager;
        this.editorAgent = editorAgent;
//        this.buildController = buildController;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.eventBus = eventBus;
        this.context = context;
        this.nameEnvironmentServiceClient = nameEnvironmentServiceClient;
//        this.builderConsole = builderConsole;
    }

    public void updateDependencies(final ProjectDescriptor project, final boolean force) {
        if (updating) {
            projects.add(new Pair<>(project, force));
            return;
        }

        final CurrentProject currentProject = context.getCurrentProject();
        if (currentProject == null) {
            return;
        }

//        builderConsole.clear();

        javaTreeStructure = null;
        if (currentProject.getCurrentTree() instanceof JavaTreeStructure) {
            javaTreeStructure = (JavaTreeStructure)currentProject.getCurrentTree();
        }
        notification = new Notification(javaLocalizationConstant.updatingDependencies(), PROGRESS);
        notificationManager.showNotification(notification);

//        buildContext.setBuilding(true);
        updating = true;

        // send a first request to launch build process and return build task descriptor
//        final Unmarshallable<BuildTaskDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(BuildTaskDescriptor.class);
        nameEnvironmentServiceClient.updateDependencies(
                project.getPath(), force, new AsyncRequestCallback<Boolean>() {
                    @Override
                    protected void onSuccess(Boolean descriptor) {
////                        buildContext.setBuildTaskDescriptor(descriptor);
//
//                        if (descriptor.getStatus() == BuildStatus.SUCCESSFUL) {
                        onUpdated();
//                            return;
//                        }
//                        buildController.showRunningBuild(descriptor, "[INFO] Updating dependencies...");
//
//                        // send a second request to be notified when dependencies update is finished
//                        updateAndWait(descriptor, project);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        Log.warn(DependenciesUpdater.class, "Failed to launch build process and get build task descriptor for " + project);
                        updating = false;
                        updateFinishedWithError(exception, notification);
                    }
                });
    }

//    private void updateAndWait(BuildTaskDescriptor descriptor, final ProjectDescriptor project) {
//        nameEnvironmentServiceClient.updateDependenciesAndWait(project.getPath(), descriptor, new AsyncRequestCallback<Void>() {
//            @Override
//            protected void onSuccess(Void result) {
//                onUpdated();
//                parserWorker.dependenciesUpdated();
//                refreshOpenedEditors();
//                refreshExtLibs(project);
//
//                if (!projects.isEmpty()) {
//                    Pair<ProjectDescriptor, Boolean> pair = projects.poll();
//                    updateDependencies(pair.first, pair.second);
//                }
//            }
//
//            @Override
//            protected void onFailure(Throwable exception) {
//                updating = false;
//                if (!projects.isEmpty()) {
//                    Pair<ProjectDescriptor, Boolean> pair = projects.poll();
//                    updateDependencies(pair.first, pair.second);
//                }
//                updateFinishedWithError(exception, notification);
//            }
//        });
//    }

    private void onUpdated() {
        updating = false;
        notification.setMessage(javaLocalizationConstant.dependenciesSuccessfullyUpdated());
        notification.setStatus(FINISHED);
//        buildContext.setBuilding(false);
    }

    private void refreshOpenedEditors() {
        editorAgent.getOpenedEditors().iterate(new StringMap.IterationCallback<EditorPartPresenter>() {
            @Override
            public void onIteration(String s, EditorPartPresenter editorPartPresenter) {
                if (editorPartPresenter instanceof EmbeddedTextEditorPresenter) {
                    final EmbeddedTextEditorPresenter<?> editor = (EmbeddedTextEditorPresenter<?>)editorPartPresenter;
                    editor.refreshEditor();
                }
            }
        });
    }

    private void refreshExtLibs(ProjectDescriptor project) {
        if (javaTreeStructure != null) {
            ExternalLibrariesNode librariesNode = javaTreeStructure.getExternalLibrariesNode(project.getPath());
            if (librariesNode != null && librariesNode.isOpened()) {
                eventBus.fireEvent(new RefreshProjectTreeEvent(librariesNode));
            }
        }
    }

    private void updateFinishedWithError(Throwable exception, Notification notification) {
//        buildContext.setBuilding(false);
        notification.setMessage(exception.getMessage());
        notification.setType(ERROR);
        notification.setStatus(FINISHED);
    }
}
