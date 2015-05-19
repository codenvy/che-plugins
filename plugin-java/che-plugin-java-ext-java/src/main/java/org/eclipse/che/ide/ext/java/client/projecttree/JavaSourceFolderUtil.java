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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** @author Vladyslav Zhukovskii */
public class JavaSourceFolderUtil {
    /** Tests if the specified item is a source folder. */
    public static boolean isSourceFolder(ItemReference item, ProjectNode projectNode) {
        if ("folder".equals(item.getType())) {
            ProjectDescriptor projectDescriptor = projectNode.getData();
            //TODO only maven now
//            BuildersDescriptor builders = projectDescriptor.getBuilders();
            Map<String, List<String>> attributes = projectDescriptor.getAttributes();
//            if (builders != null) {
                boolean isSrcDir = false;
                boolean isTestDir = false;

                if (attributes.containsKey(/*builders.getDefault() +*/ "maven.source.folder")) {
                    isSrcDir = (projectDescriptor.getPath() + "/" + attributes.get(/*builders.getDefault() + */"maven.source.folder").get(0)).equals(item.getPath());
                }

                if (attributes.containsKey(/*builders.getDefault() + */"maven.test.source.folder")) {
                    isTestDir = (projectDescriptor.getPath() + "/" + attributes.get(/*builders.getDefault() + */"maven.test.source.folder").get(0)).equals(item.getPath());
                }

                return isSrcDir || isTestDir;
//            }
        }

        return false;
    }

    /**
     * Returns source folders list of the project to which the specified node belongs.
     * Every path in the returned list starts and ends with separator char /.
     */
    public static List<String> getSourceFolders(TreeNode<?> node) {
        final ProjectNode project = node.getProject();
        Map<String, List<String>> attributes = project.getData().getAttributes();
        //TODO only maven now
//        final String builderName = project.getData().getBuilders().getDefault();
        List<String> mySourceFolders = new LinkedList<>();

        List<String> sourceFolders = attributes.get(/*builderName + */"maven.source.folder");
        if (sourceFolders != null) {
            for (String sourceFolder : sourceFolders) {
                mySourceFolders.add(project.getPath() + '/' + sourceFolder + '/');
            }
        }

        List<String> testSourceFolders = attributes.get(/*builderName + */"maven.test.source.folder");
        if (testSourceFolders != null) {
            for (String testSourceFolder : testSourceFolders) {
                mySourceFolders.add(project.getPath() + '/' + testSourceFolder + '/');
            }
        }

        return mySourceFolders;
    }

    public static String getFQNForFile(VirtualFile file){
        String packageName = "";
        if (file instanceof SourceFileNode) {
            if (((SourceFileNode)file).getParent() instanceof PackageNode) {
                packageName = ((PackageNode)((SourceFileNode)file).getParent()).getQualifiedName();
            }
            if(!packageName.isEmpty()){
                packageName = packageName + ".";
            }
           return packageName + file.getName().substring(0, file.getName().lastIndexOf('.'));
        }

        if(file instanceof JarClassNode){
            return file.getPath();
        }
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }
}
