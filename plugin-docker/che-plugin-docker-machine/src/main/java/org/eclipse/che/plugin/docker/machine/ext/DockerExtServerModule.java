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

import org.eclipse.che.plugin.docker.machine.ServerConf;

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

        Multibinder<ServerConf> machineServers = Multibinder.newSetBinder(binder(), ServerConf.class);
        machineServers.addBinding().toInstance(new ServerConf("extensions", "4401", "http"));

        // :ro removed because of bug in a docker 1.6:L
        //TODO add :ro when bug is fixed or rework ext server binding mechanism to provide copy of the ext server zip to each machine
        Multibinder<String> volumesMultibinder =
                Multibinder.newSetBinder(binder(), String.class, Names.named("machine.docker.system_volumes"));
        volumesMultibinder.addBinding().toProvider(DockerExtServerBindingProvider.class);
    }
}
