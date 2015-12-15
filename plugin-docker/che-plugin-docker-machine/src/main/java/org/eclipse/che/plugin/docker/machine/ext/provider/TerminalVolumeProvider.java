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
 * Provides volumes configuration of machine for terminal
 *
 * <p>On Windows MUST be locate in "user.home" directory in case limitation windows+docker.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class TerminalVolumeProvider implements Provider<String> {
    @Inject
    @Named("machine.server.terminal.archive")
    private String terminalArchivePath;

    @Override
    public String get() {
        if (SystemInfo.isWindows()) {
            return System.getProperty("user.home") + "\\AppData\\Local\\che\\terminal" + ":/mnt/che/terminal:ro";
        } else {
            return terminalArchivePath + ":/mnt/che/terminal:ro";
        }
    }
}
