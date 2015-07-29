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

import java.util.Arrays;

/**
 * @author andrew00x
 */
public class Exec {
    private final String[] command;
    private final String   id;

    Exec(String[] command, String id) {
        this.command = command;
        this.id = id;
    }

    public String[] getCommand() {
        return command;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Exec{" +
               "command=" + Arrays.toString(command) +
               ", id='" + id + '\'' +
               '}';
    }
}
