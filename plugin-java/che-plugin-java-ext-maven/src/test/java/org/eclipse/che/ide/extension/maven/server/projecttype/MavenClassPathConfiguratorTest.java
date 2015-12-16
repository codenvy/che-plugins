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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.project.server.AttributeFilter;
import org.eclipse.che.api.project.server.DefaultProjectManager;
import org.eclipse.che.api.project.server.FileEntry;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.AbstractProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.vfs.server.SystemPathsFilter;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUser;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUserContext;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static javax.ws.rs.HttpMethod.GET;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Nikitenko
 */
public class MavenClassPathConfiguratorTest {

    private static final String      WORKSPACE                     = "workspace";
    private static final String      VFS_USER                      = "dev";
    private static final Set<String> VFS_USER_GROUPS               = new LinkedHashSet<>(Arrays.asList("workspace/developer"));
    private static final String      SOURCE_DIRECTORY              = "src/somePath/java";
    private static final String      DEFAULT_SOURCE_DIRECTORY      = "src/main/java";
    private static final String      DEFAULT_TEST_SOURCE_DIRECTORY = "src/test/java";
    private static final String      CLASS_PATH_CONTENT            =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<classpath>\n" +
            "\t<classpathentry kind=\"src\" path=\"%s\"/>\n" +
            "\t<classpathentry kind=\"src\" path=\"%s\"/>\n" +
            "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n" +
            "\t<classpathentry kind=\"con\" path=\"org.eclipse.che.MAVEN2_CLASSPATH_CONTAINER\"/>\n" +
            "</classpath>";
    private static final String      POM_CONTENT                   =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>POM_CONTENT</packaging>\n" +
            "    <build>\n" +
            "        <sourceDirectory>%s</sourceDirectory>\n" +
            "    </build>\n" +
            "</project>";
    private static final String      POM_CONTENT_WITHOUT_BUILD     =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>POM_CONTENT</packaging>\n" +
            "</project>";
    private ProjectManager projectManager;

    @Mock
    private Provider<AttributeFilter> filterProvider;
    @Mock
    private AttributeFilter           filter;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(filterProvider.get()).thenReturn(filter);
        Set<ProjectType> pts = new HashSet<>();
        final AbstractProjectType pt = new AbstractProjectType("maven", "Maven type", true, false) {
        };
        pts.add(pt);
        final ProjectTypeRegistry projectTypeRegistry = new ProjectTypeRegistry(pts);

        VirtualFileSystemRegistry virtualFileSystemRegistry = new VirtualFileSystemRegistry();
        EventService eventService = new EventService();
        ProjectHandlerRegistry handlerRegistry = new ProjectHandlerRegistry(new HashSet<>());
        projectManager = new DefaultProjectManager(virtualFileSystemRegistry,
                                                   eventService,
                                                   projectTypeRegistry, handlerRegistry, filterProvider, "");
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ProjectManager.class).toInstance(projectManager);
            }
        });

        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider(WORKSPACE, eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(VFS_USER, VFS_USER_GROUPS);
                    }
                }, virtualFileSystemRegistry, SystemPathsFilter.ANY);
        virtualFileSystemRegistry.registerProvider(WORKSPACE, memoryFileSystemProvider);
        projectManager = injector.getInstance(ProjectManager.class);

        HttpJsonHelper.HttpJsonHelperImpl httpJsonHelper = mock(HttpJsonHelper.HttpJsonHelperImpl.class);
        Field f = HttpJsonHelper.class.getDeclaredField("httpJsonHelperImpl");
        f.setAccessible(true);
        f.set(null, httpJsonHelper);

        UsersWorkspaceDto usersWorkspaceMock = mock(UsersWorkspaceDto.class);
        when(httpJsonHelper.request(any(), anyString(), eq(GET), isNull())).thenReturn(usersWorkspaceMock);
        final ProjectConfigDto projectConfigDto = DtoFactory.getInstance().createDto(ProjectConfigDto.class).withPath("/projectName");
        when(usersWorkspaceMock.getProjects()).thenReturn(Collections.singletonList(projectConfigDto));
    }

    @Test
    public void testConfigureWhenPomNotContainsSourceDirectory() throws Exception {
        String classPath = String.format(CLASS_PATH_CONTENT, DEFAULT_SOURCE_DIRECTORY, DEFAULT_TEST_SOURCE_DIRECTORY);
        Project testProject =
                projectManager.createProject(WORKSPACE, "projectName", DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                                                                 .withType("maven"), null);
        testProject.getBaseFolder().createFile("pom.xml", POM_CONTENT_WITHOUT_BUILD.getBytes());

        MavenClassPathConfigurator.configure(testProject.getBaseFolder());
        VirtualFileEntry classPathFile = projectManager.getProject(WORKSPACE, "projectName").getBaseFolder().getChild(".codenvy/classpath");

        assertNotNull(classPathFile);
        Assert.assertTrue(classPathFile.isFile());
        Assert.assertEquals(new String(((FileEntry)classPathFile).contentAsBytes()), new String(classPath.getBytes()));
    }

    @Test
    public void testConfigureWhenPomContainsDefaultSourceDirectory() throws Exception {
        Project testProject =
                projectManager.createProject(WORKSPACE, "projectName", DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                                                                 .withType("maven"), null);
        String pom = String.format(POM_CONTENT, DEFAULT_SOURCE_DIRECTORY);
        String classPath = String.format(CLASS_PATH_CONTENT, DEFAULT_SOURCE_DIRECTORY, DEFAULT_TEST_SOURCE_DIRECTORY);
        testProject.getBaseFolder().createFile("pom.xml", pom.getBytes());

        MavenClassPathConfigurator.configure(testProject.getBaseFolder());
        VirtualFileEntry classPathFile = projectManager.getProject(WORKSPACE, "projectName").getBaseFolder().getChild(".codenvy/classpath");

        assertNotNull(classPathFile);
        Assert.assertTrue(classPathFile.isFile());
        Assert.assertEquals(new String(((FileEntry)classPathFile).contentAsBytes()), new String(classPath.getBytes()));
    }

    @Test
    public void testConfigureWhenPomContainsNotDefaultSourceDirectory() throws Exception {
        Project testProject =
                projectManager.createProject(WORKSPACE, "projectName", DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                                                                 .withType("maven"), null);
        String pom = String.format(POM_CONTENT, SOURCE_DIRECTORY);
        String classPath = String.format(CLASS_PATH_CONTENT, SOURCE_DIRECTORY, DEFAULT_TEST_SOURCE_DIRECTORY);
        testProject.getBaseFolder().createFile("pom.xml", pom.getBytes());

        MavenClassPathConfigurator.configure(testProject.getBaseFolder());
        VirtualFileEntry classPathFile = projectManager.getProject(WORKSPACE, "projectName").getBaseFolder().getChild(".codenvy/classpath");

        assertNotNull(classPathFile);
        Assert.assertTrue(classPathFile.isFile());
        Assert.assertEquals(new String(((FileEntry)classPathFile).contentAsBytes()), new String(classPath.getBytes()));
    }
}
