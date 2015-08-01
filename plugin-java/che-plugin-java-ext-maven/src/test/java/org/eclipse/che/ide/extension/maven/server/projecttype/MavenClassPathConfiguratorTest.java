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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.DefaultProjectManager;
import org.eclipse.che.api.project.server.FileEntry;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUser;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUserContext;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * @author Roman Nikitenko
 */
public class MavenClassPathConfiguratorTest {

    private ProjectManager projectManager;

    private static final String      WORKSPACE                 = "workspace";
    private static final String      VFS_USER                  = "dev";
    private static final Set<String> VFS_USER_GROUPS           = new LinkedHashSet<>(Arrays.asList("workspace/developer"));
    private static final String      SOURCE_DIRECTORY          = "src/somePath/java";
    private static final String      DEFAULT_SOURCE_DIRECTORY  = "src/main/java";
    private static final String      CLASS_PATH_CONTENT        =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<classpath>\n" +
            "\t<classpathentry kind=\"src\" path=\"%s\"/>\n" +
            "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n" +
            "\t<classpathentry kind=\"con\" path=\"org.eclipse.che.MAVEN2_CLASSPATH_CONTAINER\"/>\n" +
            "</classpath>";
    private static final String      POM_CONTENT               =
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
    private static final String      POM_CONTENT_WITHOUT_BUILD =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>POM_CONTENT</packaging>\n" +
            "</project>";

    @Before
    public void setup() throws Exception {
        Set<ProjectType> pts = new HashSet<>();
        final ProjectType pt = new ProjectType("maven", "Maven type", true, false) {
        };
        pts.add(pt);
        final ProjectTypeRegistry projectTypeRegistry = new ProjectTypeRegistry(pts);

        VirtualFileSystemRegistry virtualFileSystemRegistry = new VirtualFileSystemRegistry();
        EventService eventService = new EventService();
        ProjectHandlerRegistry handlerRegistry = new ProjectHandlerRegistry(new HashSet<ProjectHandler>());
        projectManager =
                new DefaultProjectManager(virtualFileSystemRegistry,
                                          eventService,
                                          projectTypeRegistry, handlerRegistry);
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
                }, virtualFileSystemRegistry);
        virtualFileSystemRegistry.registerProvider(WORKSPACE, memoryFileSystemProvider);
        projectManager = injector.getInstance(ProjectManager.class);
    }

    @Test
    public void testConfigureWhenPomNotContainsSourceDirectory() throws Exception {
        String classPath = String.format(CLASS_PATH_CONTENT, DEFAULT_SOURCE_DIRECTORY);
        Project testProject = projectManager.createProject(WORKSPACE, "projectName", new ProjectConfig("maven", "maven"), null, null);
        testProject.getBaseFolder().createFile("pom.xml", POM_CONTENT_WITHOUT_BUILD.getBytes(), "text/xml");

        MavenClassPathConfigurator.configure(testProject.getBaseFolder());
        VirtualFileEntry classPathFile = projectManager.getProject(WORKSPACE, "projectName").getBaseFolder().getChild(".codenvy/classpath");

        assertNotNull(classPathFile);
        Assert.assertTrue(classPathFile.isFile());
        Assert.assertEquals(new String(((FileEntry)classPathFile).contentAsBytes()), new String(classPath.getBytes()));
    }

    @Test
    public void testConfigureWhenPomContainsDefaultSourceDirectory() throws Exception {
        Project testProject = projectManager.createProject(WORKSPACE, "projectName", new ProjectConfig("maven", "maven"), null, null);
        String pom = String.format(POM_CONTENT, DEFAULT_SOURCE_DIRECTORY);
        String classPath = String.format(CLASS_PATH_CONTENT, DEFAULT_SOURCE_DIRECTORY);
        testProject.getBaseFolder().createFile("pom.xml", pom.getBytes(), "text/xml");

        MavenClassPathConfigurator.configure(testProject.getBaseFolder());
        VirtualFileEntry classPathFile = projectManager.getProject(WORKSPACE, "projectName").getBaseFolder().getChild(".codenvy/classpath");

        assertNotNull(classPathFile);
        Assert.assertTrue(classPathFile.isFile());
        Assert.assertEquals(new String(((FileEntry)classPathFile).contentAsBytes()), new String(classPath.getBytes()));
    }

    @Test
    public void testConfigureWhenPomContainsNotDefaultSourceDirectory() throws Exception {
        Project testProject = projectManager.createProject(WORKSPACE, "projectName", new ProjectConfig("maven", "maven"), null, null);
        String pom = String.format(POM_CONTENT, SOURCE_DIRECTORY);
        String classPath = String.format(CLASS_PATH_CONTENT, SOURCE_DIRECTORY);
        testProject.getBaseFolder().createFile("pom.xml", pom.getBytes(), "text/xml");

        MavenClassPathConfigurator.configure(testProject.getBaseFolder());
        VirtualFileEntry classPathFile = projectManager.getProject(WORKSPACE, "projectName").getBaseFolder().getChild(".codenvy/classpath");

        assertNotNull(classPathFile);
        Assert.assertTrue(classPathFile.isFile());
        Assert.assertEquals(new String(((FileEntry)classPathFile).contentAsBytes()), new String(classPath.getBytes()));
    }
}
