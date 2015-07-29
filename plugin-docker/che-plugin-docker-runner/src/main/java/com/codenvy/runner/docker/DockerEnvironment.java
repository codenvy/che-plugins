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
package com.codenvy.runner.docker;

import com.codenvy.docker.DockerFileException;
import com.codenvy.docker.Dockerfile;

import java.io.IOException;

/**
 * @author andrew00x
 */
abstract class DockerEnvironment {
    final String id;

    DockerEnvironment(String id) {
        this.id = id;
    }

    String getId() {
        return id;
    }

    abstract Mapper getMapper() throws IOException;

    abstract Dockerfile getDockerfile() throws DockerFileException;
}
