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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.plugin.docker.machine.ServerConf;

import javax.inject.Singleton;

/**
 * Guice module for extension servers feature in docker machines
 *
 * @author Alexander Garagatyi
 */
// Not a DynaModule, install manually
public class DockerExtServerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DockerMachineExtServerLauncher.class).asEagerSingleton();

        Multibinder<ServerConf> machineServers = Multibinder.newSetBinder(binder(),
                                                                          ServerConf.class,
                                                                          Names.named("machine.docker.dev_machine.machine_servers"));
        machineServers.addBinding().toInstance(new ServerConf("extensions", "4401", "http"));

        Multibinder<String> volumesMultibinder = Multibinder.newSetBinder(binder(),
                                                                          String.class,
                                                                          Names.named("machine.docker.dev_machine.machine_volumes"));

        if (SystemInfo.isWindows()) {
            volumesMultibinder.addBinding().toProvider(DockerExtServerBindingProviderWinOS.class);
        } else {
            volumesMultibinder.addBinding().toProvider(DockerExtServerBindingProviderUnix.class);
        }

        volumesMultibinder.addBinding().toProvider(DockerExtConfBindingProvider.class).in(Singleton.class);
    }
}
