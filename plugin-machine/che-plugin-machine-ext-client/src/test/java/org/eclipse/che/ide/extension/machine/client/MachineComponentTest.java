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

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynny */
@RunWith(MockitoJUnitRunner.class)
public class MachineComponentTest {

    private static final String DEV_MACHINE_ID = "id";

    @Mock
    private MachineServiceClient machineServiceClient;
    @Mock
    private AppContext           appContext;
    @Mock
    private MachineManager       machineManager;

    @Mock
    private Callback<Component, Exception> componentCallback;
    @Mock
    private MachineStateDescriptor         machineStateDescriptor;

    @InjectMocks
    private MachineComponent machineComponent;

    @Mock
    private Promise<List<MachineStateDescriptor>>                   machinesPromise;
    @Captor
    private ArgumentCaptor<Operation<List<MachineStateDescriptor>>> machinesCaptor;

    @Test
    public void shouldUseRunningDevMachine() throws Exception {
        when(machineServiceClient.getMachinesStates(anyString())).thenReturn(machinesPromise);
        when(machinesPromise.then(any(Operation.class))).thenReturn(machinesPromise);
        when(machineStateDescriptor.isDev()).thenReturn(true);
        when(machineStateDescriptor.getStatus()).thenReturn(MachineStatus.RUNNING);
        when(machineStateDescriptor.getId()).thenReturn(DEV_MACHINE_ID);

        machineComponent.start(componentCallback);

        verify(machineServiceClient).getMachinesStates(anyString());
        verify(machinesPromise).then(machinesCaptor.capture());
        machinesCaptor.getValue().apply(Collections.singletonList(machineStateDescriptor));
        verify(machineStateDescriptor).isDev();
        verify(machineStateDescriptor).getStatus();
        verify(appContext).setDevMachineId(eq(DEV_MACHINE_ID));
        verify(machineManager).onMachineRunning(eq(DEV_MACHINE_ID));
        verify(componentCallback).onSuccess(eq(machineComponent));
    }
}
