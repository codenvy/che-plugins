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
package org.eclipse.che.runner.docker;

import java.io.IOException;

import org.eclipse.che.docker.DockerFileException;
import org.eclipse.che.docker.Dockerfile;

/**
 * @author andrew00x
 */
abstract class DockerEnvironment {
    final String id;

    DockerEnvironment(String id) {
        this.id = id;
    }

    String getId() {
        return id;
    }

    abstract Mapper getMapper() throws IOException;

    abstract Dockerfile getDockerfile() throws DockerFileException;
}
