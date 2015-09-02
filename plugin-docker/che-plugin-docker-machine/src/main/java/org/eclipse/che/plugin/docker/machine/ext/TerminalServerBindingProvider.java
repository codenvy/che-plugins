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
 * Provides volumes configuration of machine for terminal
 *
 * @author Alexander Garagatyi
 */
public class TerminalServerBindingProvider implements Provider<String> {
    private final String terminalArchivePath;

    @Inject
    public TerminalServerBindingProvider(@Named("machine.server.terminal.archive") String terminalArchivePath) {
        this.terminalArchivePath = terminalArchivePath;
    }

    // :ro removed because of bug in a docker 1.6:L
    //TODO add :ro when bug is fixed or rework terminal binding mechanism to provide copy of the terminal files to each machine
    @Override
    public String get() {
        return terminalArchivePath + ":/mnt/che/terminal";
    }
}
