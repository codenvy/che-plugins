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
package org.eclipse.che.ide.ext.ssh.dto;

import org.eclipse.che.dto.shared.DTO;


/**
 * Interface describe a request for generate a SSH-key.
 *
 * @author Artem Zatsarynnyi
 */
@DTO
public interface GenKeyRequest {
    /**
     * Returns remote host name for which generate key.
     *
     * @return host name
     */
    String getHost();

    void setHost(String host);

    GenKeyRequest withHost(String host);

    /**
     * Returns comment for public key.
     *
     * @return comment
     */
    String getComment();

    void setComment(String comment);

    GenKeyRequest withComment(String comment);

    /**
     * Returns passphrase for private key.
     *
     * @return passphrase
     */
    String getPassphrase();

    void setPassphrase(String passphrase);

    GenKeyRequest withPassphrase(String passPhrase);
}