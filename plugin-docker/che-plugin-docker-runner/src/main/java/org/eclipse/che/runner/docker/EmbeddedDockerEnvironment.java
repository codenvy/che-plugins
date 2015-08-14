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
package org.eclipse.che.runner.docker;

import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.docker.DockerFileException;
import org.eclipse.che.docker.Dockerfile;
import org.eclipse.che.docker.DockerfileParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author andrew00x
 */
class EmbeddedDockerEnvironment extends DockerEnvironment {
    private final    File baseDir;
    private volatile Misc misc;

    EmbeddedDockerEnvironment(String id, File baseDir) {
        super(id);
        this.baseDir = baseDir;
    }

    File getBaseDir() {
        return baseDir;
    }

    /** Description for embedded environment. It helps user to understand nature and possibilities of this environment. */
    String getDescription() {
        return getMisc().getDescription();
    }

    /** Display name for embedded environment. */
    String getDisplayName() {
        return getMisc().getDisplayName();
    }

    private Misc getMisc() {
        Misc myMisc = misc;
        if (myMisc == null) {
            synchronized (this) {
                myMisc = misc;
                if (myMisc == null) {
                    final File miscFile = new File(baseDir, "Misc.json");
                    if (miscFile.exists()) {
                        try (FileReader r = new FileReader(miscFile)) {
                            misc = myMisc = JsonHelper.fromJson(r, Misc.class, null);
                        } catch (JsonParseException | IOException ignored) {
                        }
                    }
                    if (myMisc == null) {
                        misc = myMisc = new Misc();
                    }
                }
            }
        }
        return myMisc;
    }

    @Override
    Mapper getMapper() throws IOException {
        // Re-read for each request since see file updates.
        // Probably that isn't best solution but it's ok for now.
        final File mapperFile = new File(baseDir, "Mapper.json");
        if (mapperFile.exists()) {
            try (FileReader r = new FileReader(mapperFile)) {
                return JsonHelper.fromJson(r, Mapper.class, null);
            } catch (JsonParseException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        // default, anyway that is optional
        return new Mapper();
    }

    @Override
    Dockerfile getDockerfile() throws DockerFileException {
        // Re-read for each request since see file updates.
        // Probably that isn't best solution but it's ok for now.
        final File dockerFile = new File(baseDir, "Dockerfile");
        if (!dockerFile.exists()) {
            throw new DockerFileException(String.format("Unable find Docker file for environment %s", id));
        }
        return DockerfileParser.parse(dockerFile);
    }
}
