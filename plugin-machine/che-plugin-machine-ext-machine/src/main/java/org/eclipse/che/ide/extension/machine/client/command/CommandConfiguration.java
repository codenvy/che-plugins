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
package org.eclipse.che.ide.extension.machine.client.command;

import javax.annotation.Nonnull;

/**
 * Represents configured command which can be executed in machine.
 *
 * @author Artem Zatsarynnyy
 */
public interface CommandConfiguration {

    /** Returns unique identifier for this command configuration. */
    @Nonnull
    String getId();

    /** Returns command configuration name. */
    @Nonnull
    String getName();

    /** Sets command configuration name. */
    void setName(@Nonnull String name);

    /** Returns command configuration type. */
    @Nonnull
    CommandType getType();

    /** Returns command line to execute in machine. */
    @Nonnull
    String toCommandLine();
}
