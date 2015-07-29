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
package com.codenvy.docker.connection;

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
