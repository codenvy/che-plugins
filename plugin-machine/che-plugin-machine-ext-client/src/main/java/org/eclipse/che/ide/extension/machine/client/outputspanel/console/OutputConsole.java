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
package org.eclipse.che.ide.extension.machine.client.outputspanel.console;

import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;

/**
 * Describes requirements for the console for command output.
 *
 * @author Artem Zatsarynnyi
 */
public interface OutputConsole extends Presenter {

    /** Return command configuration which output this console shows. */
    CommandConfiguration getCommand();

    /** Return title for the console. */
    String getTitle();

    /** Start listening to the output on the given WebSocket channel. */
    void listenToOutput(String wsChannel);

    /** Set ID of the process launched by the command. */
    void attachToProcess(int pid);

    /** Checks whether the console is finished outputting or not. In other words - whether the process is alive or not. */
    boolean isFinished();

    /** Called when console is going to close. */
    void onClose();
}
