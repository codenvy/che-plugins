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
 * @author andrew00x
 */
public class ExecStart {
    private boolean detach;
    private boolean tty;

    public boolean isDetach() {
        return detach;
    }

    public void setDetach(boolean detach) {
        this.detach = detach;
    }

    public boolean isTty() {
        return tty;
    }

    public void setTty(boolean tty) {
        this.tty = tty;
    }

    @Override
    public String toString() {
        return "ExecStart{" +
               "detach=" + detach +
               ", tty=" + tty +
               '}';
    }

    // -------------------

    public ExecStart withDetach(boolean detach) {
        this.detach = detach;
        return this;
    }

    public ExecStart withTty(boolean tty) {
        this.tty = tty;
        return this;
    }
}
