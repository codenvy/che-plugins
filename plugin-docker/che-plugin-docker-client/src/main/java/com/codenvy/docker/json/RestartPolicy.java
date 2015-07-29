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
package com.codenvy.docker.json;

/**
 * @author Alexander Garagatyi
 */
public class RestartPolicy {
    private String name;
    private int    maximumRetryCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RestartPolicy withName(String name) {
        this.name = name;
        return this;
    }

    public int getMaximumRetryCount() {
        return maximumRetryCount;
    }

    public void setMaximumRetryCount(int maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
    }

    public RestartPolicy withMaximumRetryCount(int maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
        return this;
    }

    @Override
    public String toString() {
        return "RestartPolicy{" +
               "name='" + name + '\'' +
               ", maximumRetryCount=" + maximumRetryCount +
               '}';
    }
}
