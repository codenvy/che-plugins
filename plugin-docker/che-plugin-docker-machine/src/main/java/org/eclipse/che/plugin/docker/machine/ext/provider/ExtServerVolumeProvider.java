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
package org.eclipse.che.plugin.docker.machine.ext.provider;

import org.eclipse.che.api.core.util.SystemInfo;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Reads path to extensions server archive to mount it to docker machine
 *
 * <p>On Windows hosts MUST be locate in "user.home" directory in case limitation windows+docker.
 *
 * @author Vitalii Parfonov
 * @author Alexander Garagatyi
 */
@Singleton
public class ExtServerVolumeProvider implements Provider<String> {
    @Inject
    @Named("machine.server.ext.archive")
    private String extServerArchivePath;

    @Override
    public String get() {
        if (SystemInfo.isWindows()) {
            String extServerArchivePath = System.getProperty("user.home") + "\\AppData\\Local\\che\\ext-server.zip";
            return extServerArchivePath + ":/mnt/che/ext-server.zip:ro";
        } else {
            return extServerArchivePath + ":/mnt/che/ext-server.zip:ro";
        }
    }
}
