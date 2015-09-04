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
 * Provides volumes configuration of machine for terminal
 *
 * @author Alexander Garagatyi
 */
public class LocalStorageDockerVolumePathProvider implements Provider<String> {
    private final String localStoragePath;

    @Inject
    public LocalStorageDockerVolumePathProvider(@Named("local.storage.path") String localStoragePath) {
        this.localStoragePath = localStoragePath;
    }

    @Override
    public String get() {
        return localStoragePath + ":/local-storage";
    }
}
