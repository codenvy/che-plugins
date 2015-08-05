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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.vfs.server.observation.VirtualFileEvent;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.eclipse.che.jdt.core.resources.ResourceChangedEvent;
import org.eclipse.che.jdt.internal.core.JavaProject;
import org.eclipse.che.vfs.impl.fs.LocalFSMountStrategy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.codeassist.impl.AssistOptions;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Maintenance and create JavaProjects
 *
 * @author Evgen Vidolob
 */
@Singleton
public class JavaProjectService {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(JavaProjectService.class);

    private Cache<String, JavaProject> cache;
    private ConcurrentHashMap<String, CopyOnWriteArraySet<String>> projectInWs = new ConcurrentHashMap<>();
    private LocalFSMountStrategy fsMountStrategy;
    private String               tempDir;
    private Map<String, String> options = new HashMap<>();

    @Inject
    public JavaProjectService(EventService eventService,
                              LocalFSMountStrategy fsMountStrategy,
                              @Named("che.java.codeassistant.index.dir") String temp) {
        eventService.subscribe(new VirtualFileEventSubscriber());
        this.fsMountStrategy = fsMountStrategy;
        tempDir = temp;
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.CORE_ENCODING, "UTF-8");
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        options.put(CompilerOptions.OPTION_TargetPlatform, JavaCore.VERSION_1_7);
        options.put(AssistOptions.OPTION_PerformVisibilityCheck, AssistOptions.ENABLED);
        options.put(CompilerOptions.OPTION_ReportUnusedLocal, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_TaskTags, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_ReportUnusedPrivateMember, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_SuppressWarnings, CompilerOptions.DISABLED);
        options.put(JavaCore.COMPILER_TASK_TAGS, "TODO,FIXME,XXX");
        options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, JavaCore.ENABLED);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
        options.put(CompilerOptions.OPTION_Process_Annotations, JavaCore.ENABLED);
        options.put(CompilerOptions.OPTION_GenerateClassFiles, JavaCore.ENABLED);
        cache = CacheBuilder.newBuilder().expireAfterAccess(4, TimeUnit.HOURS).removalListener(
                new RemovalListener<String, JavaProject>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, JavaProject> notification) {
                        JavaProject value = notification.getValue();
                        if (value != null) {
                            closeProject(value);
                            deleteDependencyDirectory(value.getWsId(), value.getProjectPath());
                        }
                    }
                }).build();
    }

    public JavaProject getOrCreateJavaProject(String wsId, String projectPath) {
        String key = wsId + projectPath;
        JavaProject project = cache.getIfPresent(key);
        if (project != null) {
            return project;
        }
        File mountPath;
        try {
            mountPath = fsMountStrategy.getMountPath(wsId);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        }
        JavaProject javaProject = new JavaProject(mountPath, projectPath, tempDir, wsId, new HashMap<>(options));
        cache.put(key, javaProject);
        if (!projectInWs.containsKey(wsId)) {
            projectInWs.put(wsId, new CopyOnWriteArraySet<String>());
        }
        projectInWs.get(wsId).add(projectPath);
        return javaProject;
    }

    public boolean isProjectDependencyExist(String wsId, String projectPath) {
        if (cache.asMap().containsKey(wsId + projectPath)) {
            return true;
        }
        File projectDepDir = new File(tempDir, wsId + projectPath);
        return projectDepDir.exists();
    }

    public void removeProject(String wsId, String projectPath) {
        JavaProject javaProject = cache.getIfPresent(wsId + projectPath);
        if (projectInWs.containsKey(wsId)) {
            projectInWs.get(wsId).remove(projectPath);
        }
        if (javaProject != null) {
            cache.invalidate(wsId + projectPath);
            closeProject(javaProject);
        }
        deleteDependencyDirectory(wsId, projectPath);
    }

    private void closeProject(JavaProject javaProject) {
        try {
            javaProject.close();
        } catch (JavaModelException e) {
            LOG.error("Error when trying close project.", e);
        }
    }

    public Map<String, String> getOptions() {
        return options;
    }

    private void deleteDependencyDirectory(String wsId, String projectPath) {
        File projectDepDir = new File(tempDir, wsId + projectPath);
        if (projectDepDir.exists()) {
            IoUtil.deleteRecursive(projectDepDir);
            File wsDepDir = new File(tempDir, wsId);
            if (wsDepDir.exists()) {
                String[] files = wsDepDir.list();
                if (files == null || files.length == 0) {
                    wsDepDir.delete();
                }
            }
        }
    }

    private class VirtualFileEventSubscriber implements EventSubscriber<VirtualFileEvent> {

        @Override
        public void onEvent(VirtualFileEvent event) {
            final VirtualFileEvent.ChangeType eventType = event.getType();
            final String eventWorkspace = event.getWorkspaceId();
            final String eventPath = event.getPath();
            try {
                if (eventType == VirtualFileEvent.ChangeType.DELETED) {
                    JavaProject javaProject = cache.getIfPresent(eventWorkspace + eventPath);
                    if (javaProject != null) {
                        removeProject(eventWorkspace, eventPath);
                        return;
                    } else if (event.isFolder()) {
                        if (isProjectDependencyExist(eventWorkspace, eventPath)) {
                            deleteDependencyDirectory(eventWorkspace, eventPath);
                            return;
                        }
                    }
                }
                if (projectInWs.containsKey(eventWorkspace)) {
                    for (String path : projectInWs.get(eventWorkspace)) {
                        if (eventPath.startsWith(path)) {
                            JavaProject javaProject = cache.getIfPresent(eventWorkspace + path);
                            if (javaProject != null) {
                                try {
                                    javaProject.getJavaModelManager().deltaState.resourceChanged(
                                            new ResourceChangedEvent(fsMountStrategy.getMountPath(eventWorkspace), event));
                                    javaProject.creteNewNameEnvironment();
                                } catch (ServerException e) {
                                    LOG.error("Can't find workspace mount path", e);
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                //catch all exceptions that may be happened
                LOG.error("Can't update java model", t);
            }
        }
    }

    /**
     * Periodically cleanup cache, to avoid memory leak.
     */
    @ScheduleRate(initialDelay = 1, period = 1, unit = TimeUnit.HOURS)
    void cacheClenup() {
        cache.cleanUp();
    }
}
