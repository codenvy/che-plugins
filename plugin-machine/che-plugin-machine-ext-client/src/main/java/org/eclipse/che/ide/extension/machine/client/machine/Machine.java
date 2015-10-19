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

import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.ServerDescriptor;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * The class which describes machine entity.
 *
 * @author Dmitry Shnurenko
 * @author Alexander Andrienko
 */
public interface Machine {

    /** @return id of current machine */
    String getId();

    /** @return current machine's display name */
    String getDisplayName();

    /** @return state of current machine */
    MachineStatus getStatus();

    /** @return type of current machine */
    String getType();

    /** @return script of machine recipe */
    String getScript();

    /** @return special url which references on terminal content. */
    @NotNull
    String getTerminalUrl();

    /** @return special url to connect to terminal web socket. */
    @NotNull
    String getWSTerminalUrl();

    /** @return special url to connect to terminal web socket. */
    @NotNull
    String getWsServerExtensionsUrl();

    /** @return active tab name for current machine */
    String getActiveTabName();

    /**
     * Sets active tab name for current machine.
     *
     * @param activeTabName
     *         tab name which need set
     */
    void setActiveTabName(String activeTabName);

    /** @return workspace id for current machine */
    String getWorkspaceId();

    /** @return servers for current machine */
    Map<String, ServerDescriptor> getServers();

    /**
     * Returns boolean which defines bounding workspace to current machine
     *
     * @return <code>true</code> machine is bounded to workspace,<code>false</code> machine isn't bounded to workspace
     */
    boolean isDev();

    /** Returns information about machine. */
    Map<String, String> getProperties();

    /** Returns path to the projects root folder. */
    String getProjectsRoot();
}