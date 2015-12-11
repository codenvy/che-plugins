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
package org.eclipse.che.plugin.docker.machine.local.node;

import org.eclipse.che.commons.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides volumes to mount on dev machine.
 *
 * @author Michail Kuznyetsov
 */
public class DevMachineVolumesProvider implements Provider<Set<String>> {

    Set<String> devMachineSystemVolumes;
    Set<String> additionalVolumes;

    @Inject
    public DevMachineVolumesProvider(@Named("machine.docker.dev_machine.machine_volumes") Set<String> devMachineSystemVolumes,
                                     @Named("machine.docker.additional_machine_volumes") @Nullable String[] additionalVolumes) {
        this.devMachineSystemVolumes = devMachineSystemVolumes;
        this.additionalVolumes = new HashSet<>(Arrays.asList(additionalVolumes));
    }

    @Override
    public Set<String> get() {
        Set<String> volumes = new HashSet<>();
        volumes.addAll(devMachineSystemVolumes);
        volumes.addAll(additionalVolumes);
        return volumes;
    }
}
