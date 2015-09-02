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

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.ServerDescriptor;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

/**
 * The class which describes machine entity. The class is wrapper of MachineDescriptor.
 *
 * @author Dmitry Shnurenko
 */
public class Machine {

    public static final String TERMINAL_REF_KEY = "terminal";

    public static final String EXTENSIONS_REF_KEY = "extensions";

    private final MachineDescriptor descriptor;

    private String activeTabName;

    @Inject
    public Machine(MachineLocalizationConstant locale, @Assisted MachineDescriptor descriptor) {
        this.descriptor = descriptor;

        this.activeTabName = locale.tabInfo();
    }

    /** @return id of current machine */
    public String getId() {
        return descriptor.getId();
    }

    /** @return current machine's display name */
    public String getDisplayName() {
        return descriptor.getDisplayName();
    }

    /** @return state of current machine */
    public MachineStatus getStatus() {
        return descriptor.getStatus();
    }

    /** @return type of current machine */
    public String getType() {
        return descriptor.getType();
    }

    /** @return script of machine recipe */
    public String getScript() {
        return descriptor.getRecipe().getScript();
    }

    /** @return special url which references on terminal content. */
    @Nonnull
    public String getTerminalUrl() {
        Map<String, ServerDescriptor> serverDescriptors = descriptor.getServers();

        for (ServerDescriptor descriptor : serverDescriptors.values()) {

            if (TERMINAL_REF_KEY.equals(descriptor.getRef())) {
                return descriptor.getUrl();
            }
        }

        return "";
    }

    /** @return special url to connect to terminal web socket. */
    @Nonnull
    public String getWSTerminalUrl() {
        String terminalUrl = getTerminalUrl();

        terminalUrl = terminalUrl.substring(terminalUrl.indexOf(':'), terminalUrl.length());

        boolean isSecureConnection = Window.Location.getProtocol().equals("https:");

        return (isSecureConnection ? "wss" : "ws") + terminalUrl + "/pty";
    }

    /** @return special url to connect to terminal web socket. */
    @Nonnull
    public String getWsServerExtensionsUrl() {
        String url = "";
        Map<String, ServerDescriptor> serverDescriptors = descriptor.getServers();
        for (ServerDescriptor descriptor : serverDescriptors.values()) {
            if (EXTENSIONS_REF_KEY.equals(descriptor.getRef())) {
                url = descriptor.getUrl();
            }
        }

        String extUrl = url.substring(url.indexOf(':'), url.length());

        boolean isSecureConnection = Window.Location.getProtocol().equals("https:");

        return (isSecureConnection ? "wss" : "ws") + extUrl + "/che/ext/ws";
    }

    /** @return active tab name for current machine */
    public String getActiveTabName() {
        return activeTabName;
    }

    /**
     * Sets active tab name for current machine.
     *
     * @param activeTabName
     *         tab name which need set
     */
    public void setActiveTabName(String activeTabName) {
        this.activeTabName = activeTabName;
    }


    /** @return workspace id for current machine */
    public String getWorkspaceId() {
        return descriptor.getWorkspaceId();
    }

    /** @return servers for current machine */
    public Map<String, ServerDescriptor> getServers() {
        return descriptor.getServers();
    }

    /**
     * Returns boolean which defines bounding workspace to current machine
     *
     * @return <code>true</code> machine is bounded to workspace,<code>false</code> machine isn't bounded to workspace
     */
    public boolean isDev() {
        return descriptor.isDev();
    }

    /** Returns information about machine. */
    public Map<String, String> getMetadata() {
        return descriptor.getMetadata();
    }

    @Override
    public boolean equals(Object machine) {
        return this == machine || !(machine == null || getClass() != machine.getClass()) && Objects.equals(descriptor.getId(),
                                                                                                           ((Machine)machine).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(descriptor.getId());
    }
}
