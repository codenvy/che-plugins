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

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonStreamParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.che.docker.json.ProgressStatus;

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
