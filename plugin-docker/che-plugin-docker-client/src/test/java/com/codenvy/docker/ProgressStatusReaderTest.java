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

import com.codenvy.docker.json.ProgressStatus;

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
