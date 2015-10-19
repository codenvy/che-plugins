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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.ServerDescriptor;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * The class which describes machine entity. The class is wrapper of MachineStateDescriptor.
 *
 * @author Alexander Andrienko
 */
public class MachineBlankImpl implements Machine {

    private MachineStateDescriptor machineState;
    private String                 activeTabName;

    @Inject
    public MachineBlankImpl(MachineLocalizationConstant locale, @Assisted MachineStateDescriptor machineState) {
        this.machineState = machineState;
        this.activeTabName = locale.tabInfo();
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return machineState.getId();
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return machineState.getDisplayName();
    }

    /** {@inheritDoc} */
    @Override
    public MachineStatus getStatus() {
        return machineState.getStatus();
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return machineState.getType();
    }

    /** {@inheritDoc} */
    @Override
    public String getScript() {
        return machineState.getRecipe().getScript();
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getTerminalUrl() {
        return "";
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getWSTerminalUrl() {
        return "";
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getWsServerExtensionsUrl() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public String getActiveTabName() {
        return activeTabName;
    }

    /** {@inheritDoc} */
    @Override
    public void setActiveTabName(String activeTabName) {
        this.activeTabName = activeTabName;
    }

    /** {@inheritDoc} */
    @Override
    public String getWorkspaceId() {
        return machineState.getWorkspaceId();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, ServerDescriptor> getServers() {
        return Collections.emptyMap();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDev() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectsRoot() {
        return "";
    }

    @Override
    public boolean equals(Object machine) {
        return this == machine || !(machine == null || getClass() != machine.getClass())
                && Objects.equals(machineState.getId(), ((Machine)machine).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(machineState.getId());
    }
}
