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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for command types.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class CommandTypeRegistry {

    private final Map<String, CommandType> commandTypes;

    public CommandTypeRegistry() {
        this.commandTypes = new HashMap<>();
    }

    @Inject(optional = true)
    private void register(Set<CommandType> commandTypes) {
        for (CommandType type : commandTypes) {
            final String id = type.getId();
            if (this.commandTypes.containsKey(id)) {
                Log.warn(CommandTypeRegistry.class, "Command type with ID " + id + " already registered.");
            } else {
                this.commandTypes.put(id, type);
            }
        }
    }

    /**
     * Returns command type with the specified ID or {@code null} if none.
     *
     * @param id
     *         the ID of the command type
     * @return command type or {@code null}
     */
    @Nullable
    public CommandType getCommandTypeById(String id) {
        return commandTypes.get(id);
    }

    /** Returns all registered command types. */
    @Nonnull
    public Collection<CommandType> getCommandTypes() {
        return commandTypes.values();
    }
}
