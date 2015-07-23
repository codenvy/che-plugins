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
package org.eclipse.che.plugin.docker.machine.ext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Provides set of commands that should be added to machine recipe to add terminal to it
 *
 * @author Alexander Garagatyi
 */
public class TerminalDockerfileInstructionsProvider implements Provider<String> {
    private final String terminalArchiveUrl;

    @Inject
    public TerminalDockerfileInstructionsProvider(@Named("machine.server.terminal.archive_url") String terminalArchiveUrl) {
        this.terminalArchiveUrl = terminalArchiveUrl;
    }

    @Override
    public String get() {
        return "\nLABEL che:server:4411:ref=terminal che:server:4411:protocol=http" +
               "\nEXPOSE 4411" +
               "\nRUN mkdir -p ~/che && " +
               "wget -q " + terminalArchiveUrl + " -O ~/che/terminal.zip && " +
               "unzip ~/che/terminal.zip -d ~/che/";
    }
}
