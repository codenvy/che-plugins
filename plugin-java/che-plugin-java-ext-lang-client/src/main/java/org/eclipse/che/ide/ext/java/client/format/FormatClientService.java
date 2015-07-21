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
package org.eclipse.che.ide.ext.java.client.format;

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

/**
 * Client for Format service.
 *
 * @author Roman Nikitenko
 */
public interface FormatClientService {

    /**
     * Creates edits that describe how to format the given string.
     *
     * @param offset
     *         The given offset to start recording the edits (inclusive).
     * @param length
     *         the given length to stop recording the edits (exclusive).
     * @param content
     *         the content to format
     * @param callback
     *         the callback to use for the response
     */
    void format(int offset, int length, String content, AsyncRequestCallback<Array<Change>> callback);
}
