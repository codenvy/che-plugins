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
package org.eclipse.che.ide.extension.machine.client.command.arbitrary;

import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.ConfigurationFactory;
import org.eclipse.che.ide.util.UUID;

import javax.annotation.Nonnull;

/**
 * Factory for {@link ArbitraryCommandConfiguration} instances.
 *
 * @author Artem Zatsarynnyy
 */
public class ArbitraryConfigurationFactory extends ConfigurationFactory<ArbitraryCommandConfiguration> {

    protected ArbitraryConfigurationFactory(CommandType commandType) {
        super(commandType);
    }

    @Nonnull
    @Override
    public ArbitraryCommandConfiguration createFromTemplate(@Nonnull String name) {
        return new ArbitraryCommandConfiguration(name, getCommandType(), UUID.uuid(3));
    }

    @Nonnull
    @Override
    public ArbitraryCommandConfiguration createFromCommandDescriptor(@Nonnull CommandDescriptor descriptor) {
        final ArbitraryCommandConfiguration configuration = new ArbitraryCommandConfiguration(descriptor.getName(),
                                                                                              getCommandType(),
                                                                                              UUID.uuid(3));
        configuration.setCommandLine(descriptor.getCommandLine());
        return configuration;
    }
}
