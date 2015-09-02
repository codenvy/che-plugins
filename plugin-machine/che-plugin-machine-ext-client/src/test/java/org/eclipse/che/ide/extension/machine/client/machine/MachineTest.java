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
package org.eclipse.che.ide.extension.machine.client.machine;

import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.ServerDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.ide.extension.machine.client.machine.Machine.TERMINAL_REF_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class MachineTest {

    private final static String SOME_TEXT = "someText";

    @Mock
    private MachineDescriptor           descriptor;
    @Mock
    private ServerDescriptor            serverDescriptor;
    @Mock
    private MachineLocalizationConstant locale;

    private Machine machine;

    @Before
    public void setUp() {
        Map<String, ServerDescriptor> servers = new HashMap<>();
        servers.put(SOME_TEXT, serverDescriptor);

        machine = new Machine(locale, descriptor);

        when(serverDescriptor.getAddress()).thenReturn(SOME_TEXT);
        when(descriptor.getServers()).thenReturn(servers);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(locale).tabInfo();
    }

    @Test
    public void defaultActiveTabShouldBeReturned() {
        when(locale.tabInfo()).thenReturn(SOME_TEXT);
        machine = new Machine(locale, descriptor);

        String tabName = machine.getActiveTabName();

        assertThat(tabName, equalTo(SOME_TEXT));
    }

    @Test
    public void activeTabNameShouldBeSet() {
        machine.setActiveTabName(SOME_TEXT);

        String tabName = machine.getActiveTabName();

        assertThat(tabName, equalTo(SOME_TEXT));
    }

    @Test
    public void displayNameShouldBeReturned() {
        machine.getDisplayName();

        verify(descriptor).getDisplayName();
    }

    @Test
    public void idShouldBeReturned() {
        machine.getId();

        verify(descriptor).getId();
    }

    @Test
    public void stateShouldBeReturned() {
        machine.getStatus();

        verify(descriptor).getStatus();
    }

    @Test
    public void typeShouldBeReturned() {
        machine.getType();

        verify(descriptor).getType();
    }

    @Test
    public void scriptShouldBeReturned() {
        MachineRecipe machineRecipe = mock(MachineRecipe.class);
        when(descriptor.getRecipe()).thenReturn(machineRecipe);

        machine.getScript();

        verify(descriptor).getRecipe();
        verify(machineRecipe).getScript();
    }

    @Test
    public void terminalUrlShouldBeReturned() {
        when(serverDescriptor.getRef()).thenReturn(TERMINAL_REF_KEY);
        when(serverDescriptor.getUrl()).thenReturn(SOME_TEXT);

        String url = machine.getTerminalUrl();

        verify(descriptor).getServers();
        verify(serverDescriptor).getRef();
        verify(serverDescriptor).getUrl();

        assertThat(url, equalTo(SOME_TEXT));
    }

    @Test
    public void nullShouldBeReturnedWhenTerminalRefIsNull() {
        when(serverDescriptor.getRef()).thenReturn(null);

        String url = machine.getTerminalUrl();

        verify(serverDescriptor, never()).getUrl();

        assertThat(url, equalTo(""));
    }

    @Test
    public void boundedStateShouldBeReturned() {
        machine.isDev();

        verify(descriptor).isDev();
    }
}