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
package org.eclipse.che.ide.extension.machine.client.machine.impl;

import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.eclipse.che.api.machine.shared.MachineStatus.CREATING;

/**
 * Test for class {@link MachineBlankImpl}
 *
 * @author Alexander Andrienko
 */
@RunWith(MockitoJUnitRunner.class)
public class MachineBlankImplTest {

    private static final String TEXT = "someText";
    @Mock
    private MachineStateDescriptor      machineStateDescriptor;
    @Mock
    private MachineLocalizationConstant locale;
    @Mock
    private MachineRecipe machineRecipe;
    @Mock
    private MachineStateDescriptor      machineStateDescriptor2;

    private MachineBlankImpl machineBlank;

    @Before
    public void setUp() {
        machineBlank = new MachineBlankImpl(locale, machineStateDescriptor);

        when(machineStateDescriptor.getId()).thenReturn(TEXT);
        when(machineStateDescriptor.getDisplayName()).thenReturn(TEXT);
        when(machineStateDescriptor.getStatus()).thenReturn(CREATING);
        when(machineStateDescriptor.getType()).thenReturn(TEXT);
        when(machineStateDescriptor.getRecipe()).thenReturn(machineRecipe);
        when(machineRecipe.getScript()).thenReturn(TEXT);
    }

    @Test
    public void shouldVerifyConstructor() {
        verify(locale).tabInfo();
    }

    @Test
    public void idShouldBeReturned() {
        assertThat(machineBlank.getId(), is(TEXT));
        verify(machineStateDescriptor).getId();
    }

    @Test
    public void displayNameShouldBeReturned() {
        assertThat(machineBlank.getDisplayName(), is(TEXT));
        verify(machineStateDescriptor).getDisplayName();
    }

    @Test
    public void statusShouldBeReturned() {
        assertThat(machineBlank.getStatus(), is(CREATING));
        verify(machineStateDescriptor).getStatus();
    }

    @Test
    public void typeShouldBeReturned() {
        assertThat(machineBlank.getType(), is(TEXT));
        verify(machineStateDescriptor).getType();
    }

    @Test
    public void scriptShouldBeReturned() {
        assertThat(machineBlank.getScript(), is(TEXT));

        verify(machineStateDescriptor).getRecipe();
        verify(machineRecipe).getScript();
    }

    @Test
    public void terminalUrlShouldBeEmpty() {
        assertThat(machineBlank.getTerminalUrl(), is(""));
    }

    @Test
    public void wsTerminalUrlShouldBeEmpty() {
        assertThat(machineBlank.getWSTerminalUrl(), is(""));
    }

    @Test
    public void wsServerExtensionsUrlShouldBeEmpty() {
        assertThat(machineBlank.getWsServerExtensionsUrl(), is(""));
    }

    @Test
    public void tabNameShouldBeChanged() {
        machineBlank.setActiveTabName(TEXT);
        assertThat(machineBlank.getActiveTabName(), is(TEXT));
    }

    @Test
    public void workspaceIdShouldBeReturned() {
        machineBlank.getWorkspaceId();
        verify(machineStateDescriptor).getWorkspaceId();
    }

    @Test
    public void listServerShouldBeEmpty() {
        assertThat(machineBlank.getServers().isEmpty(), is(true));
    }

    @Test
    public void shouldNotBeDevMachine() {
        assertThat(machineBlank.isDev(), is(false));
    }

    @Test
    public void propertiesShouldBeEmpty() {
        assertThat(machineBlank.getProperties().isEmpty(), is(true));
    }

    @Test
    public void projectRootShouldBeEmpty() {
        assertThat(machineBlank.getProjectsRoot(), is(""));
    }

    @Test
    public void equalsShouldReturnTrueWhenObjectIsNull() {
        assertThat(machineBlank.equals(null), is(false));
    }

    @Test
    public void equalsShouldReturnTrueWhenObjectIsSame() {
        assertThat(machineBlank.equals(machineBlank), is(true));
    }

    @Test
    public void equalsShouldReturnFalseWhenObjectHasTypeNotMachineBlankImpl() {
        assertThat(machineBlank.equals(new Object()), is(false));
    }

    @Test
    public void equalsShouldReturnFalseForRunnerDifferentMachineBlankImpl() {
        MachineBlankImpl machineBlank2 = new MachineBlankImpl(locale, machineStateDescriptor2);
        assertThat(machineBlank.equals(machineBlank2), is(false));
    }

    @Test
    public void hashCodeShouldBeEquivalentForEquivalentObjects() {
        MachineBlankImpl machineBlank = new MachineBlankImpl(locale, machineStateDescriptor);
        MachineBlankImpl machineBlank2 = new MachineBlankImpl(locale, machineStateDescriptor);

        assertThat(machineBlank.equals(machineBlank2), is(true));
        assertThat(machineBlank.hashCode(), is(machineBlank2.hashCode()));
    }
}
