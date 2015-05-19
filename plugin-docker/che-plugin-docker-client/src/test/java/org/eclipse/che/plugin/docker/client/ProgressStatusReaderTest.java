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
package org.eclipse.che.plugin.docker.client;

import org.eclipse.che.plugin.docker.client.json.ProgressStatus;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Eugene Voevodin
 */
public class ProgressStatusReaderTest {

    @Test
    public void shouldParseSequenceOfProcessStatusObjects() throws IOException {
        final String src = "{\"stream\":\"Step 0 : FROM busybox\\n\"}\n" +
                           "{\"status\":\"The image you are pulling has been verified\",\"id\":\"busybox:latest\"}\n";

        final ProgressStatusReader reader = new ProgressStatusReader(new ByteArrayInputStream(src.getBytes()));

        final ProgressStatus status1 = reader.next();
        final ProgressStatus status2 = reader.next();

        assertEquals("Step 0 : FROM busybox\n", status1.getStream());
        assertEquals("The image you are pulling has been verified", status2.getStatus());
        assertEquals("busybox:latest", status2.getId());
        assertNull(reader.next());
    }

    @Test
    public void shouldReturnNullIfJsonIsIncorrect() throws IOException {
        final String src = "not json";

        final ProgressStatusReader reader = new ProgressStatusReader(new ByteArrayInputStream(src.getBytes()));

        assertNull(reader.next());
    }
}
