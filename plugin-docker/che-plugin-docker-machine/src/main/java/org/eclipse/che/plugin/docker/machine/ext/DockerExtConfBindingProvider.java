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

import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.inject.CheBootstrap;

import javax.inject.Provider;
import java.io.File;

/**
 * Provides path to the configuration folder on hosted machine for mounting it to docker machine.
 *
 * <p/> It provides different bindings value of Unix and Window OS. <br/>
 * Also it can return empty line if binding is useless.
 *
 * @author Sergii Leschenko
 */
public class DockerExtConfBindingProvider implements Provider<String> {
    public static final String EXT_CHE_LOCAL_CONF_DIR = "/mnt/che/conf";

    @Override
    public String get() {
        if (SystemInfo.isWindows()) {
            return System.getProperty("user.home") + "\\AppData\\Local\\che\\ext-conf";
        } else {
            final String localConfDir = System.getenv(CheBootstrap.CHE_LOCAL_CONF_DIR);
            return localConfDir == null ? "" : localConfDir + File.separator + "ext:" + EXT_CHE_LOCAL_CONF_DIR + ":ro";
        }
    }
}
