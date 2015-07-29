/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.docker;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;

/**
 * Intended to be used in case of problem parsing and interpreting the Docker configuration file.
 *
 * @author Stéphane Daviet
 */
public class DockerFileException extends ApiException {
    public DockerFileException(ServiceError serviceError) {
        super(serviceError);
    }

    public DockerFileException(String message) {
        super(message);
    }

    public DockerFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerFileException(Throwable cause) {
        super(cause);
    }
}
