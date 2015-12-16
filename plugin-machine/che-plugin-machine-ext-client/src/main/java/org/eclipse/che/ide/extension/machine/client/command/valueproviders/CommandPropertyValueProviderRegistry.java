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
package org.eclipse.che.ide.extension.machine.client.command.valueproviders;

import java.util.List;
import java.util.Set;

/**
 * Registry for {@link CommandPropertyValueProvider}s.
 *
 * @author Artem Zatsarynnyi
 * @see CommandPropertyValueProvider
 */
public interface CommandPropertyValueProviderRegistry {

    /** Returns keys of all registered {@link CommandPropertyValueProvider}s. */
    Set<String> getKeys();

    /** Returns {@link CommandPropertyValueProvider} by the given key. */
    CommandPropertyValueProvider getProvider(String key);

    /** Returns all registered {@link CommandPropertyValueProvider}s. */
    List<CommandPropertyValueProvider> getProviders();
}
