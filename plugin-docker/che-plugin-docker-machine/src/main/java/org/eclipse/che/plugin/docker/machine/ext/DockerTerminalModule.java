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
 * Guice module for terminal feature in docker machines
 *
 * @author Alexander Garagatyi
 */
// Not a DynaModule, install manually
public class DockerTerminalModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DockerMachineTerminalLauncher.class).asEagerSingleton();

        bindConstant().annotatedWith(Names.named(DockerMachineTerminalLauncher.START_TERMINAL_COMMAND))
                      .to("mkdir -p ~/che " +
                          "&& cp /mnt/che/terminal -nR ~/che" +
                          "&& ~/che/terminal/terminal -addr :4411 -cmd /bin/sh -static ~/che/terminal/");

        Multibinder<ServerConf> machineServers = Multibinder.newSetBinder(binder(),
                                                                          ServerConf.class,
                                                                          Names.named("machine.docker.machine_servers"));
        machineServers.addBinding().toInstance(new ServerConf("terminal", "4411", "http"));

        Multibinder<String> volumesMultibinder =
                Multibinder.newSetBinder(binder(), String.class, Names.named("machine.docker.machine_volumes"));
        volumesMultibinder.addBinding().toProvider(TerminalServerBindingProvider.class);
    }
}
