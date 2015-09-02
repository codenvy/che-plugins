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
package org.eclipse.che.ide.extension.machine.client.command.valueproviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateEvent;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateHandler;

import javax.annotation.Nonnull;

/**
 * Provides dev-machine's host name.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class DevMachineHostNameProvider implements CommandPropertyValueProvider, MachineStateHandler {

    private static final String KEY = "${machine.dev.hostname}";

    private final AppContext           appContext;
    private final MachineServiceClient machineServiceClient;

    private String value;

    @Inject
    public DevMachineHostNameProvider(EventBus eventBus,
                                      AppContext appContext,
                                      MachineServiceClient machineServiceClient) {
        this.appContext = appContext;
        this.machineServiceClient = machineServiceClient;
        this.value = "";

        eventBus.addHandler(MachineStateEvent.TYPE, this);
        updateValue();
    }

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void onMachineRunning(MachineStateEvent event) {
        final Machine machine = event.getMachine();
        if (machine.isDev()) {
            final String hostName = machine.getMetadata().get("config.hostname");
            if (hostName != null) {
                value = hostName;
            }
        }
    }

    @Override
    public void onMachineDestroyed(MachineStateEvent event) {
        final Machine machine = event.getMachine();
        if (machine.isDev()) {
            value = "";
        }
    }

    private void updateValue() {
        final String devMachineId = appContext.getDevMachineId();
        if (devMachineId == null) {
            return;
        }

        machineServiceClient.getMachine(devMachineId).then(new Operation<MachineDescriptor>() {
            @Override
            public void apply(MachineDescriptor arg) throws OperationException {
                final String hostName = arg.getMetadata().get("config.hostname");
                if (hostName != null) {
                    value = hostName;
                }
            }
        });
    }
}
