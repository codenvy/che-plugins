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

import com.google.inject.name.Named;

import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;

/** @author Artem Zatsarynnyy */
public interface CommandConsoleFactory {

    /** Create the instance of {@link CommandOutputConsole} for the given {@code commandConfiguration}. */
    @Named("command")
    OutputConsole create(CommandConfiguration commandConfiguration, String machineId);

    /** Create the instance of {@link DefaultOutputConsole} for the given title. */
    @Named("default")
    OutputConsole create(String title);
}
