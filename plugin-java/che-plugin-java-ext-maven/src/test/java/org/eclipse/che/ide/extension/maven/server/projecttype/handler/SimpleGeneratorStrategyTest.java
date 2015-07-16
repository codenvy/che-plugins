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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.DefaultProjectManager;
import org.eclipse.che.api.project.server.FileEntry;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUser;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUserContext;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/** @author Artem Zatsarynnyy */
public class SimpleGeneratorStrategyTest {
    private static final String workspace = "my_ws";

    private ProjectManager    pm;
    private GeneratorStrategy simple;

    @Before
    public void setUp() throws Exception {
        simple = new SimpleGeneratorStrategy();
    }

    @Test
    public void testGetId() throws Exception {
        Assert.assertEquals(MavenAttributes.SIMPLE_GENERATION_STRATEGY, simple.getId());
    }

    @Test
    public void testGeneratingProject() throws Exception {
        prepareProject();
        final Path pomXml = Paths.get(Thread.currentThread().getContextClassLoader().getResource("test-pom.xml").toURI());

        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(MavenAttributes.ARTIFACT_ID, new AttributeValue("my_artifact"));
        attributeValues.put(MavenAttributes.GROUP_ID, new AttributeValue("my_group"));
        attributeValues.put(MavenAttributes.PACKAGING, new AttributeValue("jar"));
        attributeValues.put(MavenAttributes.VERSION, new AttributeValue("1.0-SNAPSHOT"));
        attributeValues.put(MavenAttributes.SOURCE_FOLDER, new AttributeValue("src/main/java"));
        attributeValues.put(MavenAttributes.TEST_SOURCE_FOLDER, new AttributeValue("src/test/java"));

        FolderEntry folder = pm.getProject(workspace, "my_project").getBaseFolder();

        simple.generateProject(folder, attributeValues, null);

        VirtualFileEntry pomFile = pm.getProject(workspace, "my_project").getBaseFolder().getChild("pom.xml");
        Assert.assertTrue(pomFile.isFile());
        Assert.assertEquals(new String(((FileEntry)pomFile).contentAsBytes()), new String(Files.readAllBytes(pomXml)));

        VirtualFileEntry srcFolder = pm.getProject(workspace, "my_project").getBaseFolder().getChild("src/main/java");
        Assert.assertTrue(srcFolder.isFolder());
        VirtualFileEntry testFolder = pm.getProject(workspace, "my_project").getBaseFolder().getChild("src/test/java");
        Assert.assertTrue(testFolder.isFolder());
    }

    private void prepareProject() throws Exception {
        final String vfsUser = "dev";
        final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

        Set<ProjectType> pts = new HashSet<>();
        final ProjectType pt = new ProjectType("mytype", "mytype type", true, false) {
            {
                //addParent(parent);
                //addConstantDefinition("child_const", "Constant", "const_value");
            }
        };


        pts.add(pt);

        final ProjectTypeRegistry projectTypeRegistry = new ProjectTypeRegistry(pts);

        final EventService eventService = new EventService();
        final VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();
        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider(workspace, eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUser, vfsUserGroups);
                    }
                }, vfsRegistry);
        vfsRegistry.registerProvider(workspace, memoryFileSystemProvider);

        //ProjectGeneratorRegistry generatorRegistry = new ProjectGeneratorRegistry(new HashSet<ProjectGenerator>());
        ProjectHandlerRegistry handlerRegistry = new ProjectHandlerRegistry(new HashSet<ProjectHandler>());

        pm = new DefaultProjectManager(vfsRegistry, eventService, projectTypeRegistry, handlerRegistry);
        pm.createProject(workspace, "my_project", new ProjectConfig("", pt.getId()), null, "public");
    }
}