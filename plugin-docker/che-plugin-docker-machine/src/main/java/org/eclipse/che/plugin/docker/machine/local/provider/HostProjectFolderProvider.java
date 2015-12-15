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
package org.eclipse.che.plugin.docker.machine.local.provider;

import org.eclipse.che.api.core.util.SystemInfo;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provide path to the project folder on hosted machine
 *
 * <p>On Unix managed by vfs.local.fs_root_dir property.<br>
 * On Windows MUST be locate in "user.home" directory in case limitation windows+docker
 *
 * @author Vitalii Parfonov
 * @author Alexander Garagatyi
 */
@Singleton
public class HostProjectFolderProvider implements Provider<String> {
    @Inject
    @Named("vfs.local.fs_root_dir")
    private String projectsFolder;

    @Override
    public String get() {
        if (SystemInfo.isWindows()) {
            return System.getProperty("user.home") + "\\che\\projects";
        } else {
            return projectsFolder;
        }
    }
}
