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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.DefaultProjectManager;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.ValueStorageException;
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
import org.eclipse.che.ide.ext.java.server.projecttype.JavaProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.GeneratorStrategy;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectGenerator;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;
import org.eclipse.che.ide.maven.tools.Model;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** @author gazarenkov */
public class MavenProjectTypeTest {
    private static final String workspace = "my_ws";

    private ProjectManager pm;


    @Before
    public void setUp() throws Exception {

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
                }, vfsRegistry);
        vfsRegistry.registerProvider(workspace, memoryFileSystemProvider);


        Set<ProjectType> projTypes = new HashSet<>();
        projTypes.add(new JavaProjectType());
        projTypes.add(new MavenProjectType(new MavenValueProviderFactory(),
                                           new JavaProjectType()));

        ProjectTypeRegistry ptRegistry = new ProjectTypeRegistry(projTypes);

        //ProjectGeneratorRegistry generatorRegistry = new ProjectGeneratorRegistry(new HashSet<ProjectGenerator>());
        Set<ProjectHandler> handlers = new HashSet<>();
        handlers.add(new MavenProjectGenerator(Collections.<GeneratorStrategy>emptySet()));

        ProjectHandlerRegistry handlerRegistry = new ProjectHandlerRegistry(handlers);

        pm = new DefaultProjectManager(vfsRegistry, eventService,
                                       ptRegistry, handlerRegistry);

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


        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(MavenAttributes.ARTIFACT_ID, new AttributeValue("myartifact"));
        attributes.put(MavenAttributes.GROUP_ID, new AttributeValue("mygroup"));
        attributes.put(MavenAttributes.VERSION, new AttributeValue("1.0"));
        attributes.put(MavenAttributes.PACKAGING, new AttributeValue("jar"));
        //attributes.put(MavenAttributes., new AttributeValue("jar"));

        Project project = pm.createProject(workspace, "myProject",
                                           new ProjectConfig("my config", "maven", attributes, null, null),
                                           null, "public");

        ProjectConfig config = project.getConfig();

//        System.out.println(" >>>" + config.getAttributes().get(MavenAttributes.ARTIFACT_ID).getString() + " "+
//                        attributes.get(MavenAttributes.ARTIFACT_ID).getString());

        Assert.assertEquals(config.getAttributes().get(MavenAttributes.ARTIFACT_ID).getString(), "myartifact");
        Assert.assertEquals(config.getAttributes().get(MavenAttributes.VERSION).getString(), "1.0");
        Assert.assertEquals(config.getAttributes().get("language").getString(), "java");


        for (VirtualFileEntry file : project.getBaseFolder().getChildren()) {

            if (file.getName().equals("pom.xml")) {

                Model pom = Model.readFrom(file.getVirtualFile().getContent().getStream());

//                Assert.assertEquals(pom.getArtifactId(), "myartifact");
                Assert.assertEquals(pom.getVersion(), "1.0");

            }

        }

    }

    @Test
    public void testEstimation() throws Exception {


        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(MavenAttributes.ARTIFACT_ID, new AttributeValue("myartifact"));
        attributes.put(MavenAttributes.GROUP_ID, new AttributeValue("mygroup"));
        attributes.put(MavenAttributes.VERSION, new AttributeValue("1.0"));
        attributes.put(MavenAttributes.PACKAGING, new AttributeValue("jar"));

        pm.createProject(workspace, "testEstimate",
                         new ProjectConfig("my config", "maven", attributes, null, null),
                         null, "public");

        pm.createProject(workspace, "testEstimateBad",
                         new ProjectConfig("my config", "blank", null, null, null),
                         null, "public");

        Map<String, AttributeValue> out = pm.estimateProject(workspace, "testEstimate", "maven");

        Assert.assertEquals(out.get(MavenAttributes.ARTIFACT_ID).getString(), "myartifact");
        Assert.assertEquals(out.get(MavenAttributes.VERSION).getString(), "1.0");


        try {
            pm.estimateProject(workspace, "testEstimateBad", "maven");
            Assert.fail("ValueStorageException expected");
        } catch (ValueStorageException e) {

        }
    }

}