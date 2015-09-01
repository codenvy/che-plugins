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
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public interface JavaNodeFactory {
    ExternalLibrariesNode newExternalLibrariesNode(@Nonnull ProjectDescriptor projectDescriptor,
                                                   @Nonnull NodeSettings nodeSettings);

    JarContainerNode newJarContainerNode(@Nonnull Jar jar,
                                         @Nonnull ProjectDescriptor projectDescriptor,
                                         @Nonnull NodeSettings nodeSettings);

    JarFileNode newJarFileNode(@Nonnull JarEntry jarEntry,
                               int libId,
                               @Nonnull ProjectDescriptor projectDescriptor,
                               @Nonnull NodeSettings nodeSettings);

    JarFolderNode newJarFolderNode(@Nonnull JarEntry jarEntry,
                                   int libId,
                                   @Nonnull ProjectDescriptor projectDescriptor,
                                   @Nonnull NodeSettings nodeSettings);

    PackageNode newPackageNode(@Nonnull ItemReference itemReference,
                               @Nonnull ProjectDescriptor projectDescriptor,
                               @Nonnull JavaNodeSettings nodeSettings);

    JavaFileNode newJavaFileNode(@Nonnull ItemReference itemReference,
                                 @Nonnull ProjectDescriptor projectDescriptor,
                                 @Nonnull JavaNodeSettings nodeSettings);
}
