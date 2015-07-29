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

import com.sun.jna.Native;

import org.eclipse.che.api.core.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author andrew00x
 */
public class CLibraryFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CLibraryFactory.class);

    private static final CLibrary C_LIBRARY;

    static {
        CLibrary tmp = null;
        if (SystemInfo.isLinux()) {
            try {
                tmp = ((CLibrary)Native.loadLibrary("c", CLibrary.class));
            } catch (Exception e) {
                LOG.error("Cannot load native library", e);
            }
        }
        C_LIBRARY = tmp;
    }

    public static CLibrary getCLibrary() {
        checkCLibrary();
        return C_LIBRARY;
    }

    private static void checkCLibrary() {
        if (C_LIBRARY == null) {
            throw new IllegalStateException("Can't load native library. Not linux system?");
        }
    }

    private CLibraryFactory() {
    }
}
