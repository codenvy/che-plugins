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
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonStreamParser;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * TODO docker 1.4 has changed output format ->  write doc for new format parsing process
 * Docker daemon sends chunked data in response. One chunk isn't always one JSON object so need to read full chunk at once to be able
 * restore JSON object. This ProgressStatusReader merges (if needs) few chunks until get full JSON object that can we parsed to {@code
 * ProgressStatus} instance.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
class ProgressStatusReader {

    private static final Gson GSON = new Gson();

    private final JsonStreamParser streamParser;

    ProgressStatusReader(InputStream source) {
        streamParser = new JsonStreamParser(new InputStreamReader(source));
    }

    ProgressStatus next() throws IOException {
        if (streamParser.hasNext()) {
            try {
                return GSON.fromJson(streamParser.next(), ProgressStatus.class);
            } catch (JsonIOException ioEx) {
                throw new IOException(ioEx);
            } catch (JsonParseException ignored) {
            }
        }
        return null;
    }
}
