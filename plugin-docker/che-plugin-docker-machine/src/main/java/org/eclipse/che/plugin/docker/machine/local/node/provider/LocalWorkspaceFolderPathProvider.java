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

package org.eclipse.che.plugin.docker.machine.local.node.provider;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.plugin.docker.machine.node.WorkspaceFolderPathProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides path to workspace folder in CHE.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class LocalWorkspaceFolderPathProvider implements WorkspaceFolderPathProvider {
    private final String projectsFolderPath;

    @Inject
    public LocalWorkspaceFolderPathProvider(@Named("host.projects.root") String projectsFolder) throws IOException {
        Path folder = Paths.get(projectsFolder);
        if (Files.notExists(folder)) {
            Files.createDirectory(folder);
        }
        if (!Files.isDirectory(folder)) {
            throw new IOException("Projects folder " +
                                  folder.toAbsolutePath() +
                                  " is invalid. Check vfs.local.fs_root_dir configuration property.");

        }
        projectsFolderPath = folder.toAbsolutePath().toString();
    }

    @Override
    public String getPath(@Assisted("workspace") String workspaceId) {
        return projectsFolderPath;
    }
}
