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
package org.eclipse.che.ide.extension.maven.server.projecttype;

import com.google.inject.Provider;

import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.project.server.AttributeFilter;
import org.eclipse.che.api.project.server.DefaultProjectManager;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.ValueStorageException;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.vfs.server.SystemPathsFilter;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUser;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUserContext;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.server.projecttype.JavaProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.GeneratorStrategy;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectGenerator;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;
import org.eclipse.che.ide.maven.tools.Model;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.HttpMethod.GET;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** @author gazarenkov */
public class MavenProjectTypeTest {
    private static final String workspace = "my_ws";

    private ProjectManager                    pm;
    private HttpJsonHelper.HttpJsonHelperImpl httpJsonHelper;

    @Mock
    private Provider<AttributeFilter> filterProvider;
    @Mock
    private AttributeFilter           filter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(filterProvider.get()).thenReturn(filter);
        final String vfsUser = "dev";
        final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

        final EventService eventService = new EventService();

        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();
        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider(workspace, eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUser, vfsUserGroups);
                    }
                }, vfsRegistry, SystemPathsFilter.ANY);
        vfsRegistry.registerProvider(workspace, memoryFileSystemProvider);

        Set<ProjectType> projTypes = new HashSet<>();
        projTypes.add(new JavaProjectType());
        projTypes.add(new MavenProjectType(new MavenValueProviderFactory(), new JavaProjectType()));

        ProjectTypeRegistry ptRegistry = new ProjectTypeRegistry(projTypes);

        Set<ProjectHandler> handlers = new HashSet<>();
        handlers.add(new MavenProjectGenerator(Collections.<GeneratorStrategy>emptySet()));

        ProjectHandlerRegistry handlerRegistry = new ProjectHandlerRegistry(handlers);

        pm = new DefaultProjectManager(vfsRegistry, eventService, ptRegistry, handlerRegistry, filterProvider, "");

        httpJsonHelper = mock(HttpJsonHelper.HttpJsonHelperImpl.class);
        Field f = HttpJsonHelper.class.getDeclaredField("httpJsonHelperImpl");
        f.setAccessible(true);
        f.set(null, httpJsonHelper);
    }

    @Test
    public void testGetProjectType() throws Exception {
        ProjectType pt = pm.getProjectTypeRegistry().getProjectType("maven");

        Assert.assertNotNull(pt);
        Assert.assertTrue(pt.getAttributes().size() > 0);
        Assert.assertTrue(pt.isTypeOf("java"));
    }

    @Test
    public void testMavenProject() throws Exception {
        UsersWorkspaceDto usersWorkspaceMock = mock(UsersWorkspaceDto.class);
        when(httpJsonHelper.request(any(), anyString(), eq(GET), isNull())).thenReturn(usersWorkspaceMock);
        final ProjectConfigDto projectConfig = DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                                         .withName("project")
                                                         .withPath("/myProject")
                                                         .withType(MavenAttributes.MAVEN_ID);
        when(usersWorkspaceMock.getProjects()).thenReturn(Collections.singletonList(projectConfig));

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(MavenAttributes.ARTIFACT_ID, Collections.singletonList("myartifact"));
        attributes.put(MavenAttributes.GROUP_ID, Collections.singletonList("mygroup"));
        attributes.put(MavenAttributes.VERSION, Collections.singletonList("1.0"));
        attributes.put(MavenAttributes.PACKAGING, Collections.singletonList("jar"));

        Project project = pm.createProject(workspace, "myProject",
                                           DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                                     .withType("maven").withAttributes(attributes),
                                           null);

        for (VirtualFileEntry file : project.getBaseFolder().getChildren()) {
            if (file.getName().equals("pom.xml")) {
                Model pom = Model.readFrom(file.getVirtualFile().getContent().getStream());
                Assert.assertEquals(pom.getVersion(), "1.0");
            }
        }
    }

    @Test
    public void testEstimation() throws Exception {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(MavenAttributes.ARTIFACT_ID, Collections.singletonList("myartifact"));
        attributes.put(MavenAttributes.GROUP_ID, Collections.singletonList("mygroup"));
        attributes.put(MavenAttributes.VERSION, Collections.singletonList("1.0"));
        attributes.put(MavenAttributes.PACKAGING, Collections.singletonList("jar"));

        pm.createProject(workspace, "testEstimate",
                         DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                   .withType("maven").withAttributes(attributes),
                         null);

        pm.createProject(workspace, "testEstimateBad",
                         DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                   .withType("blank"),
                         null);

        Map<String, AttributeValue> out = pm.estimateProject(workspace, "testEstimate", "maven");

        Assert.assertEquals(out.get(MavenAttributes.ARTIFACT_ID).getString(), "myartifact");
        Assert.assertEquals(out.get(MavenAttributes.VERSION).getString(), "1.0");

        try {
            pm.estimateProject(workspace, "testEstimateBad", "maven");
            Assert.fail("ValueStorageException expected");
        } catch (ValueStorageException ignored) {
        }
    }
}
