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
package org.eclipse.che.plugin.docker.client.connection;

import com.google.common.io.ByteStreams;

import org.eclipse.che.commons.lang.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author andrew00x
 */
public abstract class DockerConnection {
    private String method;
    private String path;
    private List<Pair<String, ?>> headers = Collections.emptyList();
    private Entity<?> entity;

    public DockerConnection method(String method) {
        this.method = method;
        return this;
    }

    public DockerConnection path(String path) {
        this.path = path;
        return this;
    }

    public DockerConnection headers(List<Pair<String, ?>> headers) {
        this.headers = headers;
        return this;
    }

    public DockerConnection entity(InputStream entity) {
        this.entity = new StreamEntity(entity);
        return this;
    }

    public DockerConnection entity(String entity) {
        this.entity = new StringEntity(entity);
        return this;
    }

    public DockerConnection entity(byte[] entity) {
        this.entity = new BytesEntity(entity);
        return this;
    }

    public DockerResponse request() throws IOException {
        return request(method, path, headers, entity);
    }

    protected abstract DockerResponse request(String method, String path, List<Pair<String, ?>> headers, Entity entity) throws IOException;

    public abstract void close();

    static abstract class Entity<T> {
        final T entity;

        Entity(T entity) {
            this.entity = entity;
        }

        abstract void writeTo(OutputStream output) throws IOException;
    }

    static class StreamEntity extends Entity<InputStream> {
        StreamEntity(InputStream entity) {
            super(entity);
        }

        @Override
        public void writeTo(OutputStream output) throws IOException {
            try {
                ByteStreams.copy(entity, output);
                output.flush();
            } finally {
                entity.close();
            }
        }
    }

    static class StringEntity extends Entity<String> {
        StringEntity(String entity) {
            super(entity);
        }

        @Override
        public void writeTo(OutputStream output) throws IOException {
            output.write(entity.getBytes());
            output.flush();
        }
    }

    static class BytesEntity extends Entity<byte[]> {
        BytesEntity(byte[] entity) {
            super(entity);
        }

        @Override
        public void writeTo(OutputStream output) throws IOException {
            output.write(entity);
            output.flush();
        }
    }
}
