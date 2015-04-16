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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * The enum represents a list of available states of shutdowning process of a runner.
 *
 * @author Andrey Plotnikov
 */
public enum Shutdown {
    BY_TIMEOUT_1(1, "1 hour timeout"), BY_TIMEOUT_4(4, "4 hour timeout"), ALWAYS_ON(0, "always on");

    private final String name;

    private final int timeout;

    Shutdown(@Nonnegative int timeout, @Nonnull String name) {
        this.timeout = timeout;
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

    public int getTimeout() {
        return timeout;
    }
    /**
     * Returns a value of {@link Shutdown} that is equaled to a given content.
     *
     * @param content
     *         content that needs to analyze
     * @return an instance {@link Shutdown}
     */
    public static Shutdown detect(@Nonnull String content) {
        for (Shutdown shutdown : Shutdown.values()) {
            if (content.equals(shutdown.toString())) {
                return shutdown;
            }
        }

        throw new UnsupportedOperationException(
                "You tried to detect unknown shutdown. Please, check your value. Your shutdown is " + content);
    }

    public static Shutdown detect(@Nonnull int timeout) {
        for (Shutdown shutdown : Shutdown.values()) {
            if (timeout ==  shutdown.getTimeout()) {
                return shutdown;
            }
        }
        return null;
    }
}