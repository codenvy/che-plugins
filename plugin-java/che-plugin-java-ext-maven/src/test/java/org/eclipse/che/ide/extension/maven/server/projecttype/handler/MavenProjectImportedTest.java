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
package org.eclipse.che.ide.extension.maven.server.projecttype.handler;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.DefaultProjectManager;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.ProjectManager;
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
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vitaly Parfonov
 */
public class MavenProjectImportedTest {

    private static final String workspace = "my_ws";

    private String pomJar =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "</project>";

    private String pom =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>pom</packaging>\n" +
            "    <modules>" +
            "      <module>module1</module>" +
            "      <module>module2</module>" +
            "   </modules>" +
            "</project>";

    private String pomWithNestingModule =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>pom</packaging>\n" +
            "    <modules>" +
            "      <module>../module2</module>" +
            "      <module>../module3</module>" +
            "   </modules>" +
            "</project>";

    private MavenProjectImportedHandler mavenProjectImportedHandler;

    private static final String      vfsUser       = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager projectManager;

    @Before
    public void setUp() throws Exception {

        Set<ProjectType> pts = new HashSet<>();
        final ProjectType pt = new ProjectType("maven", "Maven type", true, false) {
        };


        pts.add(pt);
        final ProjectTypeRegistry projectTypeRegistry = new ProjectTypeRegistry(pts);


        VirtualFileSystemRegistry virtualFileSystemRegistry = new VirtualFileSystemRegistry();
        EventService eventService = new EventService();
        //ProjectGeneratorRegistry generatorRegistry = new ProjectGeneratorRegistry(new HashSet<ProjectGenerator>());
        ProjectHandlerRegistry handlerRegistry = new ProjectHandlerRegistry(new HashSet<ProjectHandler>());
        projectManager =
                new DefaultProjectManager(virtualFileSystemRegistry,
                                          eventService,
                                          projectTypeRegistry, handlerRegistry);
        MockitoAnnotations.initMocks(this);
        // Bind components
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder<ProjectHandler> projectTypeResolverMultibinder = Multibinder.newSetBinder(binder(), ProjectHandler.class);
                projectTypeResolverMultibinder.addBinding().to(MavenProjectImportedHandler.class);
                bind(ProjectManager.class).toInstance(projectManager);
            }
        });


        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider(workspace, eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUser, vfsUserGroups);
                    }
                }, virtualFileSystemRegistry);
        virtualFileSystemRegistry.registerProvider(workspace, memoryFileSystemProvider);


        mavenProjectImportedHandler = injector.getInstance(MavenProjectImportedHandler.class);
        projectManager = injector.getInstance(ProjectManager.class);
    }

    @Test
    public void shouldNotChangeParentProjectType() throws Exception {
        Project test = projectManager.createProject(workspace, "test", new ProjectConfig("maven", "maven"), null, null);
        test.getBaseFolder().createFile("pom.xml", pomJar.getBytes(), "text/xml");
        mavenProjectImportedHandler.onProjectImported(test.getBaseFolder());
        assertNotNull(projectManager.getProject(workspace, "test"));
        assertNotNull(projectManager.getProject(workspace, "test").getConfig());
        assertNotNull(projectManager.getProject(workspace, "test").getConfig().getTypeId());
        Assert.assertEquals("maven", projectManager.getProject(workspace, "test").getConfig().getTypeId());
    }


    @Test
    public void withPomXmlWithFolders() throws Exception {
        Project test = projectManager.createProject(workspace, "test", new ProjectConfig("maven", "maven"), null, null);
        test.getBaseFolder().createFile("pom.xml", pomJar.getBytes(), "text/xml");
        FolderEntry folder = test.getBaseFolder().createFolder("folder1");
        folder.createFile("pom.xml", pomJar.getBytes(), "text/xml");

        FolderEntry folder1 = test.getBaseFolder().createFolder("folder2");
        folder1.createFile("pom.xml", pomJar.getBytes(), "text/xml");

        mavenProjectImportedHandler.onProjectImported(test.getBaseFolder());
        assertNotNull(projectManager.getProject(workspace, "test"));
        assertNull(projectManager.getProject(workspace, "test/folder1"));
        assertNull(projectManager.getProject(workspace, "test/folder2"));
    }


    @Test
    public void withPomXmlMultiModule() throws Exception {
        Project test = projectManager.createProject(workspace, "test", new ProjectConfig("maven", "maven"), null, null);
        test.getBaseFolder().createFile("pom.xml", pom.getBytes(), "text/xml");

        FolderEntry module1 = test.getBaseFolder().createFolder("module1");
        module1.createFile("pom.xml", pom.getBytes(), "text/xml");

        FolderEntry module2 = test.getBaseFolder().createFolder("module2");
        module2.createFile("pom.xml", pom.getBytes(), "text/xml");

        FolderEntry moduleNotDescribedInParentPom = test.getBaseFolder().createFolder("moduleNotDescribedInParentPom");
        moduleNotDescribedInParentPom.createFile("pom.xml", pom.getBytes(), "text/xml");


        mavenProjectImportedHandler.onProjectImported(test.getBaseFolder());
        assertNotNull(projectManager.getProject(workspace, "test"));
        assertNotNull(projectManager.getProject(workspace, "test/module1"));
        assertNotNull(projectManager.getProject(workspace, "test/module2"));
        assertNull(projectManager.getProject(workspace, "test/moduleNotDescribedInParentPom"));
    }

    @Test
    public void withPomXmlMultiModuleWithNesting() throws Exception {
        //test for multi module project in which the modules are specified in format: <module>../module</module>
        FolderEntry rootProject =
                projectManager.createProject(workspace, "rootProject", new ProjectConfig("maven", "maven"), null, null).getBaseFolder();
        rootProject.createFile("pom.xml", pom.getBytes(), "text/xml");

        FolderEntry module1 = rootProject.createFolder("module1");
        module1.createFile("pom.xml", pomWithNestingModule.getBytes(), "text/xml");

        FolderEntry module2 = rootProject.createFolder("module2");
        module2.createFile("pom.xml", pom.getBytes(), "text/xml");

        FolderEntry module3 = rootProject.createFolder("module3");
        module3.createFile("pom.xml", pom.getBytes(), "text/xml");

        FolderEntry moduleNotDescribedInParentPom = rootProject.createFolder("moduleNotDescribedInParentPom");
        moduleNotDescribedInParentPom.createFile("pom.xml", pom.getBytes(), "text/xml");


        mavenProjectImportedHandler.onProjectImported(rootProject);
        assertNotNull(projectManager.getProject(workspace, "rootProject"));
        assertNotNull(projectManager.getProject(workspace, "rootProject/module1"));
        assertNotNull(projectManager.getProject(workspace, "rootProject/module2"));
        assertNotNull(projectManager.getProject(workspace, "rootProject/module3"));
        assertNull(projectManager.getProject(workspace, "rootProject/test/moduleNotDescribedInParentPom"));
    }
}
