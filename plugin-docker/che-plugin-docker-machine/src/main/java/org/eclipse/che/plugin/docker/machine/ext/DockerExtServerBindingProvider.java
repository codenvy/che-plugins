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
package org.eclipse.che.plugin.docker.machine.ext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Reads path to extensions server archive to mount it to docker machine
 *
 * @author Alexander Garagatyi
 */
public class DockerExtServerBindingProvider implements Provider<String> {
    private final String extServerArchivePath;

    @Inject
    public DockerExtServerBindingProvider(@Named("machine.server.ext.archive") String extServerArchivePath) {
        this.extServerArchivePath = extServerArchivePath;
    }

    // :ro removed because of bug in a docker 1.6:L
    //TODO add :ro when bug is fixed or rework ext server binding mechanism to provide copy of the ext server zip to each machine
    @Override
    public String get() {
        return extServerArchivePath + ":/mnt/che/ext-server.zip";
    }
}
