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

import org.eclipse.che.plugin.docker.machine.DockerInstanceProvider;
import org.eclipse.che.plugin.docker.machine.DockerNodeFactory;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.che.api.machine.server.MachineService;
import org.eclipse.che.api.machine.server.SnapshotStorage;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;

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
        bind(SnapshotStorage.class).to(DummySnapshotStorage.class);
        bind(DockerNodeFactory.class).to(LocalDockerNodeFactory.class);
        bind(MachineService.class);
        Multibinder.newSetBinder(binder(), InstanceProvider.class).addBinding().to(DockerInstanceProvider.class);

    }
}
