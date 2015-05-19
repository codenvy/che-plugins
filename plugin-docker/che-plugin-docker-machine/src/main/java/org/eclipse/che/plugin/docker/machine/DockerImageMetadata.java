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
package org.eclipse.che.plugin.docker.machine;

import org.eclipse.che.plugin.docker.client.json.ImageInfo;
import org.eclipse.che.api.machine.server.spi.ImageKey;
import org.eclipse.che.api.machine.server.spi.ImageMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Docker implementation of {@link ImageMetadata}
 *
 * @author andrew00x
 */
public class DockerImageMetadata implements ImageMetadata {

//    DockerImageMetadata(String repository, String tag, String id, String registry) {
//        super(repository, tag, id, registry);
//    }

    private final DockerImageKey key;
    private final ImageInfo info;

    DockerImageMetadata(DockerImageKey key, ImageInfo info) {

        this.key = key;
        this.info = info;
    }


    @Override
    public ImageKey getKey() {
        return key;
    }

//    public Map<String, String> getFields() {
//        return null;
//    }


    @Override
    public String toJson() {
        return info.toString();
    }


    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("id", info.getId());
        return properties;
    }
}
