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
package org.eclipse.che.plugin.docker.machine.local;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.machine.server.MachineService;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.plugin.docker.machine.DockerInstance;
import org.eclipse.che.plugin.docker.machine.DockerInstanceProvider;
import org.eclipse.che.plugin.docker.machine.DockerMachineFactory;
import org.eclipse.che.plugin.docker.machine.local.node.DevMachineVolumesProvider;
import org.eclipse.che.plugin.docker.machine.node.DockerNode;
import org.eclipse.che.plugin.docker.machine.DockerProcess;

import java.util.Set;

/**
 * The Module for Local Docker components
 * Note that LocalDockerNodeFactory requires machine.docker.local.project parameter pointed to
 * directory containing workspace projects tree
 *
 * @author gazarenkov
 */
public class LocalDockerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MachineService.class);

        Multibinder<String> exposedPortsMultibinder =
                Multibinder.newSetBinder(binder(), String.class, Names.named("machine.docker.system_exposed_ports"));

        Multibinder<String> volumesMultibinder =
                Multibinder.newSetBinder(binder(), String.class, Names.named("machine.docker.system_volumes"));

        install(new FactoryModuleBuilder()
                        .implement(Instance.class, DockerInstance.class)
                        .implement(InstanceProcess.class, DockerProcess.class)
                        .implement(DockerNode.class, LocalDockerNode.class)
                        .build(DockerMachineFactory.class));

        Multibinder.newSetBinder(binder(), InstanceProvider.class).addBinding().to(DockerInstanceProvider.class);

        if (SystemInfo.isWindows()) {
            bind(String.class).annotatedWith(Names.named("host.projects.root"))
                              .toProvider(org.eclipse.che.plugin.docker.machine.ext.HostProjectsFolderProviderWinOS.class);
        } else {
            bind(String.class).annotatedWith(Names.named("host.projects.root"))
                              .toProvider(org.eclipse.che.plugin.docker.machine.ext.HostProjectsFolderProviderUnix.class);
        }

        bind(new TypeLiteral<Set<String>>() {}).annotatedWith(Names.named("machine.docker.dev_machine.machine_volumes_all"))
                                               .toProvider(DevMachineVolumesProvider.class);

        bind(org.eclipse.che.plugin.docker.machine.node.WorkspaceFolderPathProvider.class)
                .to(org.eclipse.che.plugin.docker.machine.local.node.LocalWorkspaceFolderPathProvider.class);

        bind(org.eclipse.che.plugin.docker.client.DockerRegistryChecker.class).asEagerSingleton();
    }
}
