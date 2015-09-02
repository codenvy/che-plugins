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
package org.eclipse.che.ide.extension.machine.client;

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;

import java.util.List;

import static org.eclipse.che.api.machine.shared.MachineStatus.RUNNING;

/** @author Artem Zatsarynnyy */
@Singleton
public class MachineComponent implements Component {

    private final MachineServiceClient machineServiceClient;
    private final AppContext           appContext;
    private final MachineManager       machineManager;

    @Inject
    public MachineComponent(MachineServiceClient machineServiceClient, AppContext appContext, MachineManager machineManager) {
        this.machineServiceClient = machineServiceClient;
        this.appContext = appContext;
        this.machineManager = machineManager;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        machineServiceClient.getMachinesStates(null).then(new Operation<List<MachineStateDescriptor>>() {
            @Override
            public void apply(List<MachineStateDescriptor> arg) throws OperationException {
                if (arg.isEmpty()) {
                    callback.onSuccess(MachineComponent.this);
                } else {
                    for (MachineStateDescriptor descriptor : arg) {
                        if (descriptor.isDev() && descriptor.getStatus() == RUNNING) {
                            appContext.setDevMachineId(descriptor.getId());
                            machineManager.onMachineRunning(descriptor.getId());
                            break;
                        }
                    }
                    callback.onSuccess(MachineComponent.this);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                callback.onFailure(new Exception(arg.getMessage()));
            }
        });
    }
}
