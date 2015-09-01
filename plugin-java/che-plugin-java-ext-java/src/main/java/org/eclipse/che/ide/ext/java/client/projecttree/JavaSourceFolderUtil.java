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

import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** @author Vladyslav Zhukovskii */
public class JavaSourceFolderUtil {
    /** Tests if the specified item is a source folder. */
    public static boolean isSourceFolder(ItemReference item, HasProjectDescriptor projectNode) {
        if ("folder".equals(item.getType())) {
            ProjectDescriptor projectDescriptor = projectNode.getProjectDescriptor();
            BuildersDescriptor builders = projectDescriptor.getBuilders();
            Map<String, List<String>> attributes = projectDescriptor.getAttributes();
            if (builders != null) {
                boolean isSrcDir = false;
                boolean isTestDir = false;

                if (attributes.containsKey(builders.getDefault() + ".source.folder")) {
                    isSrcDir = (projectDescriptor.getPath() + "/" + attributes.get(builders.getDefault() + ".source.folder").get(0)).equals(item.getPath());
                }

                if (attributes.containsKey(builders.getDefault() + ".test.source.folder")) {
                    isTestDir = (projectDescriptor.getPath() + "/" + attributes.get(builders.getDefault() + ".test.source.folder").get(0)).equals(item.getPath());
                }

                return isSrcDir || isTestDir;
            }
        }

        return false;
    }

    /**
     * Returns source folders list of the project to which the specified node belongs.
     * Every path in the returned list starts and ends with separator char /.
     */
    public static List<String> getSourceFolders(TreeNode<?> node) {
        final HasProjectDescriptor project = node.getProject();
        Map<String, List<String>> attributes = project.getProjectDescriptor().getAttributes();
        final String builderName = project.getProjectDescriptor().getBuilders().getDefault();
        List<String> mySourceFolders = new LinkedList<>();

        List<String> sourceFolders = attributes.get(builderName + ".source.folder");
        if (sourceFolders != null) {
            for (String sourceFolder : sourceFolders) {
                mySourceFolders.add(project.getProjectDescriptor().getPath() + '/' + sourceFolder + '/');
            }
        }

        List<String> testSourceFolders = attributes.get(builderName + ".test.source.folder");
        if (testSourceFolders != null) {
            for (String testSourceFolder : testSourceFolders) {
                mySourceFolders.add(project.getProjectDescriptor().getPath() + '/' + testSourceFolder + '/');
            }
        }

        return mySourceFolders;
    }
}
