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
package org.eclipse.che.docker;

import org.eclipse.che.docker.json.SystemInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author andrew00x
 */
public class SystemInfoDriverStatusTest {
    private SystemInfo info;

    @Before
    public void initialize() {
        info = new SystemInfo();
        String[][] driverStatus = new String[4][2];
        driverStatus[0] = new String[]{"Data Space Total", "107.4 GB"};
        driverStatus[1] = new String[]{"Data Space Used", "957.6 MB"};
        driverStatus[2] = new String[]{"Metadata Space Total", "2.147 GB"};
        driverStatus[3] = new String[]{"Metadata Space Used", "1.749 MB"};
        info.setDriverStatus(driverStatus);
    }

    @Test
    public void testGetDataSpaceTotal() {
        Assert.assertEquals((long)(107.4f * (1024 * 1024 * 1024)), info.dataSpaceTotal());
    }

    @Test
    public void testGetDataSpaceUsed() {
        Assert.assertEquals((long)(957.6f * (1024 * 1024)), info.dataSpaceUsed());
    }

    @Test
    public void testGetMetaDataSpaceTotal() {
        Assert.assertEquals((long)(2.147f * (1024 * 1024 * 1024)), info.metadataSpaceTotal());
    }

    @Test
    public void testGetMetaDataSpaceUsed() {
        Assert.assertEquals((long)(1.749f * (1024 * 1024)), info.metadataSpaceUsed());
    }
}
