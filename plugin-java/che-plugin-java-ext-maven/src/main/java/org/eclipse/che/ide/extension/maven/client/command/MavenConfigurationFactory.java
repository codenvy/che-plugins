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
package org.eclipse.che.ide.extension.maven.client.command;

import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.ConfigurationFactory;
import org.eclipse.che.ide.util.UUID;

import javax.annotation.Nonnull;

/**
 * Factory for {@link MavenCommandConfiguration} instances.
 *
 * @author Artem Zatsarynnyy
 */
public class MavenConfigurationFactory extends ConfigurationFactory<MavenCommandConfiguration> {

    protected MavenConfigurationFactory(CommandType commandType) {
        super(commandType);
    }

    @Nonnull
    @Override
    public MavenCommandConfiguration createFromTemplate(@Nonnull String name) {
        final MavenCommandConfiguration configuration = new MavenCommandConfiguration(name, getCommandType(), UUID.uuid(3));
        configuration.setCommandLine("clean install");
        return configuration;
    }

    @Nonnull
    @Override
    public MavenCommandConfiguration createFromCommandDescriptor(@Nonnull CommandDescriptor descriptor) {
        if (!isMavenCommand(descriptor.getCommandLine())) {
            throw new IllegalArgumentException("Not a valid Maven command: " + descriptor.getCommandLine());
        }

        final MavenCommandConfiguration configuration = new MavenCommandConfiguration(descriptor.getName(),
                                                                                      getCommandType(),
                                                                                      descriptor.getId());
        configuration.setCommandLine(descriptor.getCommandLine().replaceFirst("mvn ", ""));
        return configuration;
    }

    private static boolean isMavenCommand(String commandLine) {
        return commandLine.startsWith("mvn ");
    }
}
