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
package org.eclipse.che.ide.ext.java.client.projecttree;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.JarClassNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.SourceFileNode;

import javax.validation.constraints.Null;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Vladyslav Zhukovskii
 * @author Anatoliy Bazko
 */
public class JavaSourceFolderUtil {

    /** Indicates if the specified item is a source folder. */
    public static boolean isSourceFolder(ItemReference item, ProjectNode projectNode) {
        if ("folder".equals(item.getType())) {
            String projectBuilder = getProjectBuilder(projectNode);

            if (projectBuilder != null) {
                ProjectDescriptor projectDescriptor = projectNode.getData();
                Map<String, List<String>> attributes = projectDescriptor.getAttributes();

                final String projectPath = projectDescriptor.getPath();
                final String itemPath = item.getPath();

                List<String> sourceFolders = attributes.get(projectBuilder + ".source.folder");
                boolean isSrcDir = isSourceFolder(sourceFolders, projectPath, itemPath);

                List<String> testSourceFolders = attributes.get(projectBuilder + ".test.source.folder");
                boolean isTestDir = isSourceFolder(testSourceFolders, projectPath, itemPath);

                return isSrcDir || isTestDir;
            }
        }

        return false;
    }

    private static boolean isSourceFolder(@Null List<String> sourceFolders, String projectPath, String itemPath) {
        projectPath = removeEndingPathSeparator(projectPath);

        if (sourceFolders != null) {
            for (String sourceFolder : sourceFolders) {
                if ((projectPath + addStartingPathSeparator(sourceFolder)).equals(itemPath)) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Returns source folders list of the project to which the specified node belongs.
     * Every path in the returned list starts and ends with separator char /.
     */
    public static List<String> getSourceFolders(TreeNode<?> node) {
        List<String> allSourceFolders = new LinkedList<>();

        ProjectNode project = node.getProject();
        String projectBuilder = getProjectBuilder(project);
        String projectPath = removeEndingPathSeparator(project.getPath());

        Map<String, List<String>> attributes = project.getData().getAttributes();

        List<String> sourceFolders = attributes.get(projectBuilder + ".source.folder");
        if (sourceFolders != null) {
            for (String sourceFolder : sourceFolders) {
                allSourceFolders.add(projectPath + addStartingPathSeparator(sourceFolder) + '/');
            }
        }

        List<String> testSourceFolders = attributes.get(projectBuilder + ".test.source.folder");
        if (testSourceFolders != null) {
            for (String testSourceFolder : testSourceFolders) {
                allSourceFolders.add(projectPath + addStartingPathSeparator(testSourceFolder) + '/');
            }
        }

        return allSourceFolders;
    }

    public static String getFQNForFile(VirtualFile file) {
        String packageName = "";
        if (file instanceof SourceFileNode) {
            if (((SourceFileNode)file).getParent() instanceof PackageNode) {
                packageName = ((PackageNode)((SourceFileNode)file).getParent()).getQualifiedName();
            }
            if (!packageName.isEmpty()) {
                packageName = packageName + ".";
            }
            return packageName + file.getName().substring(0, file.getName().lastIndexOf('.'));
        }

        if (file instanceof JarClassNode) {
            return file.getPath();
        }
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }

    private static String removeEndingPathSeparator(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }

    private static String addStartingPathSeparator(String path) {
        if (!path.startsWith("/")) {
            return "/" + path;
        }

        return path;
    }

    @Null
    public static String getProjectBuilder(ProjectNode node) {
        return getProjectBuilder(node.getData().getType());
    }

    @Null
    public static String getProjectBuilder(@Null String projectType) {
        if (projectType == null) {
            return null;
        }

        switch (projectType) {
            case "maven":
            case "ant":
                return projectType;
            default:
                return null;
        }
    }
}
