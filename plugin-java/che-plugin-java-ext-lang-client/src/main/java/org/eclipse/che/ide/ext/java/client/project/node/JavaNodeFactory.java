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
package org.eclipse.che.ide.ext.java.client.project.node;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.ext.java.client.project.node.jar.ExternalLibrariesNode;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarContainerNode;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarFileNode;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarFolderNode;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettings;
import org.eclipse.che.ide.ext.java.shared.ContentRoot;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;

import javax.validation.constraints.NotNull;

/**
 * @author Vlad Zhukovskiy
 */
public interface JavaNodeFactory {
    ExternalLibrariesNode newExternalLibrariesNode(@NotNull ProjectDescriptor projectDescriptor,
                                                   @NotNull NodeSettings nodeSettings);

    JarContainerNode newJarContainerNode(@NotNull Jar jar,
                                         @NotNull ProjectDescriptor projectDescriptor,
                                         @NotNull NodeSettings nodeSettings);

    JarFileNode newJarFileNode(@NotNull JarEntry jarEntry,
                               int libId,
                               @NotNull ProjectDescriptor projectDescriptor,
                               @NotNull NodeSettings nodeSettings);

    JarFolderNode newJarFolderNode(@NotNull JarEntry jarEntry,
                                   int libId,
                                   @NotNull ProjectDescriptor projectDescriptor,
                                   @NotNull NodeSettings nodeSettings);

    PackageNode newPackageNode(@NotNull ItemReference itemReference,
                               @NotNull ProjectDescriptor projectDescriptor,
                               @NotNull JavaNodeSettings nodeSettings);

    JavaFileNode newJavaFileNode(@NotNull ItemReference itemReference,
                                 @NotNull ProjectDescriptor projectDescriptor,
                                 @NotNull JavaNodeSettings nodeSettings);

    SourceFolderNode newSourceFolderNode(@NotNull ItemReference itemReference,
                                         @NotNull ProjectDescriptor projectDescriptor,
                                         @NotNull JavaNodeSettings nodeSettings,
                                         @NotNull ContentRoot contentRootType);
}
