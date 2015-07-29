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
package com.codenvy.runner.docker;

import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import com.codenvy.docker.DockerFileException;
import com.codenvy.docker.Dockerfile;
import com.codenvy.docker.DockerfileParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author andrew00x
 */
class CustomDockerEnvironment extends DockerEnvironment {
    private final RunRequest request;

    CustomDockerEnvironment(RunRequest request) {
        super(null);
        this.request = request;
    }

    @Override
    Mapper getMapper() throws IOException {
        Mapper mapper = null;
        final List<String> recipeUrls = request.getRecipeUrls();
        if (!recipeUrls.isEmpty()) {
            final String mapperFileName = "Mapper.json";
            String mapperUrl = findRecipeUrl(recipeUrls, mapperFileName);
            if (mapperUrl != null) {
                try (InputStream in = new URL(mapperUrl).openStream()) {
                    mapper = JsonHelper.fromJson(in, Mapper.class, null);
                } catch (JsonParseException e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
        if (mapper == null) {
            // default, anyway that is optional
            mapper = new Mapper();
        }
        return mapper;
    }

    @Override
    Dockerfile getDockerfile() throws DockerFileException {
        Dockerfile dockerfile = null;
        final List<String> recipeUrls = request.getRecipeUrls();
        if (!recipeUrls.isEmpty()) {
            final String dockerFileName = "Dockerfile";
            final String dockerfileUrl = findRecipeUrl(recipeUrls, dockerFileName);
            if (dockerfileUrl != null) {
                try {
                    dockerfile = DockerfileParser.parse(new URL(dockerfileUrl));
                } catch (MalformedURLException e) {
                    throw new DockerFileException("Problem parsing Docker file URL: " + e.getMessage(), e);
                }
            }
        }
        if (dockerfile == null) {
            throw new DockerFileException("No Docker configuration file can be found for the current environment.");
        }
        return dockerfile;
    }

    private String findRecipeUrl(List<String> recipeUrls, String fileName) {
        for (final String recipeUrl : recipeUrls) {
            final int queryStart = recipeUrl.indexOf('?');
            final String recipePath = queryStart > 0 ? recipeUrl.substring(0, queryStart) : recipeUrl;
            if (recipePath.endsWith(fileName)) {
                if (queryStart > 0) {
                    return recipeUrl + "&token=" + request.getUserToken();
                } else {
                    return recipeUrl + "?token=" + request.getUserToken();
                }
            }
        }
        return null;
    }
}
