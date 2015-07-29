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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.ide.maven.tools.Model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.ARTIFACT_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.GROUP_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.MAVEN_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.PACKAGING;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.VERSION;


/**
 * @author Evgen Vidolob
 */

public class MavenProjectResolver {
    private static final String CLASS_PATH_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                     "<classpath>\n" +
                                                     "\t<classpathentry kind=\"src\" path=\"src/main/java\"/>\n" +
                                                     "\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n" +
                                                     "\t<classpathentry kind=\"con\" path=\"org.eclipse.che.MAVEN2_CLASSPATH_CONTAINER\"/>\n" +
                                                     "</classpath>";

    public static void createClassPath(FolderEntry projectFolder) throws ServerException, ForbiddenException, ConflictException {
        VirtualFileEntry child = projectFolder.getChild(".codenvy");
        if(child != null && child.getVirtualFile().getChild("classpath") == null){
            child.getVirtualFile().createFile("classpath", null, new ByteArrayInputStream(CLASS_PATH_CONTENT.getBytes()));
        }
    }

    public static void resolve(FolderEntry projectFolder, ProjectManager projectManager)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException, IOException {
        VirtualFileEntry pom = projectFolder.getChild("pom.xml");
        if (pom != null) {
            createClassPath(projectFolder);

            Model model = Model.readFrom(pom.getVirtualFile());
            String packaging = model.getPackaging();
            if (packaging.equals("pom")) {
                createProjectsOnModules(model,
                                        projectManager.getProject(projectFolder.getWorkspace(), projectFolder.getPath()),
                                        projectFolder.getWorkspace(),
                                        projectManager);
            }
        }
    }


    private static FolderEntry getParentFolder(String module, Project parentProject) {
        FolderEntry parentFolder = parentProject.getBaseFolder();
        int level = module.split("\\.{2}/").length - 1;
        while (level != 0 && parentFolder != null) {
            parentFolder = parentFolder.getParent();
            level--;
        }
        return parentFolder;
    }

    private static ProjectConfig createProjectConfig(FolderEntry folderEntry)
            throws ServerException, ForbiddenException, IOException {

        VirtualFileEntry pom = folderEntry.getChild("pom.xml");
        Model model = Model.readFrom(pom.getVirtualFile());

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(ARTIFACT_ID, new AttributeValue(model.getArtifactId()));
        attributes.put(GROUP_ID, new AttributeValue(model.getGroupId()));
        attributes.put(VERSION, new AttributeValue(model.getVersion()));
        attributes.put(PACKAGING, new AttributeValue(model.getPackaging()));

        return new ProjectConfig("Maven", MAVEN_ID, attributes, null, null);
    }

    private static void createProjectsOnModules(Model model, Project parentProject, String ws, ProjectManager projectManager)
            throws ServerException, ForbiddenException, ConflictException, IOException, NotFoundException {
        List<String> modules = model.getModules();
        for (String module : modules) {
            FolderEntry parentFolder = getParentFolder(module, parentProject);
            module = module.replaceAll("\\.{2}/", "");
            FolderEntry moduleEntry = (FolderEntry)parentFolder.getChild(module);
            if (moduleEntry != null && moduleEntry.getVirtualFile().getChild("pom.xml") != null) {
                Project project = projectManager.getProject(ws, moduleEntry.getPath());
                ProjectConfig projectConfig = createProjectConfig(moduleEntry);
                if (project == null) {
                    project = new Project(moduleEntry, projectManager);
                }
                project.updateConfig(projectConfig);
                resolve(project.getBaseFolder(), projectManager);
            }
        }
    }


}
