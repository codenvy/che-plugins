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
package org.eclipse.che.plugin.docker.machine.local;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.plugin.docker.machine.DockerNode;
import org.eclipse.che.plugin.docker.machine.DockerNodeFactory;
import com.google.inject.assistedinject.Assisted;


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * Factory exposing LocalDockerNode
 *
 * @author gazarenkov
 */
@Singleton
public class LocalDockerNodeFactory implements DockerNodeFactory {

    private final LocalDockerNode node;

    @Inject
    public LocalDockerNodeFactory(@Named("vfs.local.fs_root_dir") String wsDir) throws IOException, ServerException {
        try {

           this.node = new LocalDockerNode(wsDir);

        } catch (IOException e) {
            throw new IOException(e.getMessage()+" "+"Check vfs.local.fs_root_dir configuration property.");
        }
    }

    @Override
    public DockerNode createNode(@Assisted String containerId) {
        return node;
    }

}
