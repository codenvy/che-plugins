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
package org.eclipse.che.ide.ext.svn.server;

import org.eclipse.che.api.core.ServerException;

/**
 * Exception for wrapping Subversion related exceptions.
 */
public class SubversionException extends ServerException {

    /**
     * Constructor.
     *
     * @param message the error message
     */
    public SubversionException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param throwable the throwable to wrap
     */
    public SubversionException(final Throwable throwable) {
        super(throwable);
    }

}
