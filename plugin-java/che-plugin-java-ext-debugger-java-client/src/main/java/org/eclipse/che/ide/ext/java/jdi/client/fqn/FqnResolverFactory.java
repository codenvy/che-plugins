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
package org.eclipse.che.ide.ext.java.jdi.client.fqn;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.annotation.Nullable;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/** @author Evgen Vidolob */
@Singleton
public class FqnResolverFactory {
    private Map<String, FqnResolver> resolvers;

    /** Create factory. */
    @Inject
    protected FqnResolverFactory() {
        this.resolvers = new HashMap<>();
    }

    public void addResolver(@NotNull String mimeType, @NotNull FqnResolver resolver) {
        resolvers.put(mimeType, resolver);
    }

    @Nullable
    public FqnResolver getResolver(@NotNull String mimeType) {
        return resolvers.get(mimeType);
    }

    public boolean isResolverExist(@NotNull String mimeType) {
        return resolvers.containsKey(mimeType);
    }
}