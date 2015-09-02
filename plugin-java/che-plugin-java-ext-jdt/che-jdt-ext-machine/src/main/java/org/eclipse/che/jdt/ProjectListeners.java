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
package org.eclipse.che.jdt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.notification.ProjectItemModifiedEvent;
import org.eclipse.che.jdt.core.resources.ResourceChangedEvent;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ide.ext.machine.ProjectEventListener;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class ProjectListeners implements ProjectEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectListeners.class);
    private final File workspace;

//    private LocalFSMountStrategy fsMountStrategy;

//    private CopyOnWriteArraySet<String> notFullyCreatedProjects = new CopyOnWriteArraySet<>();

    @Inject
    public ProjectListeners(@Named("che.workspace.path") String workspacePath) {
        workspace = new File(workspacePath);
//        this.fsMountStrategy = fsMountStrategy;
//        eventService.subscribe(new ProjectCreated());
//        eventService.subscribe(new VirtualFileEventSubscriber());

    }

    @Override
    public void onEvent(ProjectItemModifiedEvent event) {
        final String eventPath = event.getPath();
        try {
            JavaModelManager.getJavaModelManager().deltaState.resourceChanged(
                    new ResourceChangedEvent(workspace, event));
        } catch (Throwable t) {
            //catch all exceptions that may be happened
            LOG.error("Can't update java model", t);
        }
        if (event.getType() == ProjectItemModifiedEvent.EventType.UPDATED) {
            ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
            ITextFileBuffer fileBuffer = manager.getTextFileBuffer(new Path(eventPath), LocationKind.IFILE);
            if (fileBuffer != null) {
                try {
                    fileBuffer.revert(new NullProgressMonitor());
                } catch (CoreException e) {
                    LOG.error("Can't read file content: " + eventPath, e);
                }
            }
        }
    }

//    private class VirtualFileEventSubscriber implements EventSubscriber<VirtualFileEvent> {
//
//        @Override
//        public void onEvent(VirtualFileEvent event) {
//            final VirtualFileEvent.ChangeType eventType = event.getType();
//            final String eventWorkspace = event.getWorkspaceId();
//            final String eventPath = event.getPath();
//            if(event.getType() == VirtualFileEvent.ChangeType.CREATED) {
//                IPath path = new Path(eventPath);
//                if (path.segmentCount() == 1) {
//                    notFullyCreatedProjects.add(eventPath);
//                    return;
//                }
//            }
//            //ignore all changes when project creating
//            if (checkIfInCreationProject(eventPath)) {
//                return;
//            }
//            try {
//                JavaModelManager.getJavaModelManager().deltaState.resourceChanged(
//                        new ResourceChangedEvent(fsMountStrategy.getMountPath(eventWorkspace), event));
//            } catch (Throwable t) {
//                //catch all exceptions that may be happened
//                LOG.error("Can't update java model", t);
//            }
//            if(event.getType() == VirtualFileEvent.ChangeType.CONTENT_UPDATED){
//                ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
//                ITextFileBuffer fileBuffer = manager.getTextFileBuffer(new Path(eventPath), LocationKind.IFILE);
//                if(fileBuffer != null) {
//                    try {
//                        fileBuffer.revert(new NullProgressMonitor());
//                    } catch (CoreException e) {
//                        LOG.error("Can't read file content: " + eventPath, e);
//                    }
//                }
//            }
//        }
//    }
//
//    private boolean checkIfInCreationProject(String path) {
//        for (String project : notFullyCreatedProjects) {
//            if (path.startsWith(project)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private class ProjectCreated implements EventSubscriber<ProjectCreatedEvent>{
//
//        @Override
//        public void onEvent(ProjectCreatedEvent event) {
//            final String eventWorkspace = event.getWorkspaceId();
//            final String eventPath = event.getProjectPath();
//            notFullyCreatedProjects.remove(eventPath);
//            try {
//                CreateEvent createEvent = new CreateEvent(eventWorkspace, eventPath, true);
//                JavaModelManager.getJavaModelManager().deltaState.resourceChanged(
//                        new ResourceChangedEvent(fsMountStrategy.getMountPath(eventWorkspace), createEvent));
//            } catch (Throwable t) {
//                //catch all exceptions that may be happened
//                LOG.error("Can't update java model", t);
//            }
//        }
//    }
}
