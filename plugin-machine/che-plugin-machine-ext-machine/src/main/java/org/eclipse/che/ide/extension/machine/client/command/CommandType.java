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

import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * The type of a command.
 *
 * @author Artem Zatsarynnyy
 */
public interface CommandType {

    /** Returns unique identifier for this command type. */
    @Nonnull
    String getId();

    /** Returns the display name of the command type. */
    @Nonnull
    String getDisplayName();

    /** Returns the icon used to represent the command type. */
    @Nonnull
    SVGResource getIcon();

    /** Returns the {@link ConfigurationPage}s that allow to configure specific command parameters. */
    @Nonnull
    Collection<ConfigurationPage<? extends CommandConfiguration>> getConfigurationPages();

    /** Returns factory for {@link CommandConfiguration} instances. */
    @Nonnull
    ConfigurationFactory<? extends CommandConfiguration> getConfigurationFactory();
}
