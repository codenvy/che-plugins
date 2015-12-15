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
import org.eclipse.che.inject.CheBootstrap;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;

/**
 * Provides path to the configuration folder on hosted machine for mounting it to docker machine.
 *
 * <p/> It provides different bindings value of Unix and Window OS. <br/>
 * Also it can return null value if binding is useless.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class DockerExtConfBindingProvider implements Provider<String> {
    public static final  String EXT_CHE_LOCAL_CONF_DIR = "/mnt/che/conf";
    private static final String CONTAINER_TARGET       = ":" + EXT_CHE_LOCAL_CONF_DIR + ":ro";

    @Override
    public String get() {
        if (SystemInfo.isWindows()) {
            return System.getProperty("user.home") + "\\AppData\\Local\\che\\ext-conf" + CONTAINER_TARGET;
        }

        String localConfDir = System.getenv(CheBootstrap.CHE_LOCAL_CONF_DIR);
        if (localConfDir != null) {
            File extConfDir = new File(localConfDir, "ext");
            if (extConfDir.isDirectory()) {
                return extConfDir.getAbsolutePath() + CONTAINER_TARGET;
            }
        }
        return null;
    }
}
