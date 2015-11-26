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
package org.eclipse.che.ide.ext.openshift.server.rest;

import com.google.common.collect.ImmutableMap;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.http.IHttpClient;
import com.openshift.restclient.model.IImageStream;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.openshift.server.ClientFactory;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStreamTag;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.che.ide.ext.openshift.server.DtoConverter.toDto;
import static org.eclipse.che.ide.ext.openshift.server.DtoConverter.toOpenshiftResource;

/**
 * @author Sergii Leschenko
 */
@Path("/openshift/{ws-id}/namespace/{namespace}/imagestream")
public class ImageStreamService {
    private final ClientFactory clientFactory;
    private final String        getTagUrlTemplate;

    @Inject
    public ImageStreamService(ClientFactory clientFactory) {
        this.getTagUrlTemplate = clientFactory.getClientInfo().getOpenshiftEndpoint()
                                 + "/namespaces/{namespace}/imagestreamtags/{imageStream}:{tag}";
        this.clientFactory = clientFactory;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImageStream createImageStream(@PathParam("namespace") String namespace,
                                         ImageStream imageStream) throws BadRequestException, UnauthorizedException, ServerException {
        if (imageStream.getKind() == null) {
            imageStream.setKind(ResourceKind.IMAGE_STREAM);
        }
        if (!ResourceKind.IMAGE_STREAM.equals(imageStream.getKind())) {
            throw new BadRequestException(imageStream.getKind() + " cannot be handled as a " + ResourceKind.IMAGE_STREAM);
        }

        final IClient client = clientFactory.getOpenshiftClient();
        final IImageStream openshiftImageStream = toOpenshiftResource(client, imageStream);
        return toDto(ImageStream.class, client.create(openshiftImageStream, namespace));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ImageStream> getImageStreams(@PathParam("namespace") String namespace,
                                             @QueryParam("application") String application) throws UnauthorizedException, ServerException {
        Map<String, String> labels = new HashMap<>();
        if (application != null) {
            labels.put("application", application);
        }

        List<IImageStream> imageStreams = clientFactory.getOpenshiftClient().list(ResourceKind.IMAGE_STREAM, namespace, labels);
        return imageStreams.stream()
                           .map(imageStream -> toDto(ImageStream.class, imageStream))
                           .collect(Collectors.toList());
    }

    @GET
    @Path("/{imageStream}")
    @Produces(MediaType.APPLICATION_JSON)
    public ImageStream getImageStream(@PathParam("namespace") String namespace,
                                      @PathParam("imageStream") String imageStream) throws UnauthorizedException, ServerException {

        return toDto(ImageStream.class, clientFactory.getOpenshiftClient().get(ResourceKind.IMAGE_STREAM, imageStream, namespace));
    }

    @PUT
    @Path("/{imageStream}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImageStream updateImageStream(@PathParam("namespace") String namespace,
                                         @PathParam("imageStream") String imageStreamName,
                                         ImageStream imageStream) throws ForbiddenException, UnauthorizedException, ServerException {
        if (!imageStreamName.equals(imageStream.getMetadata().getName())) {
            throw new ForbiddenException("Name of resources can read only access mode");
        }
        final IClient client = clientFactory.getOpenshiftClient();
        return toDto(ImageStream.class, client.update(toOpenshiftResource(client, imageStream)));
    }

    @GET
    @Path("/{imageStream}/tag/{imageStreamTag}")
    @Produces(MediaType.APPLICATION_JSON)
    public ImageStreamTag getImageStreamTag(@PathParam("namespace") String namespace,
                                            @PathParam("imageStream") String imageStream,
                                            @PathParam("imageStreamTag") String imageStreamTag)
            throws UnauthorizedException, ServerException {
        URL url;
        try {
            url = UriBuilder.fromPath(getTagUrlTemplate).buildFromMap(ImmutableMap.of("namespace", namespace,
                                                                                      "imageStream", imageStream,
                                                                                      "tag", imageStreamTag))
                            .toURL();
        } catch (MalformedURLException e) {
            throw new ServerException("Unable to get image stream tag. " + e.getMessage(), e);
        }

        try {
            final String response = clientFactory.getHttpClient().get(url, IHttpClient.DEFAULT_READ_TIMEOUT);
            return DtoFactory.getInstance().createDtoFromJson(response, ImageStreamTag.class);
        } catch (SocketTimeoutException e) {
            throw new ServerException("Unable to get image stream tag. " + e.getMessage(), e);
        }
    }
}
