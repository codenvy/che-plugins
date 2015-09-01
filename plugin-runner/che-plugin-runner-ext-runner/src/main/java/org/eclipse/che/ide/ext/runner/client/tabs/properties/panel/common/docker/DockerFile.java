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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * The file node that represents recipe file item in the project explorer tree. It needs just for opening recipe for editing (it is a
 * problem of API of editor agent).
 *
 * @author Artem Zatsarynnyy
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
public class DockerFile implements VirtualFile {

    public static final String GET_CONTENT = "get content";
    private final ProjectServiceClient projectServiceClient;
    private final ItemReference data;

    public DockerFile(@Nonnull ProjectServiceClient projectServiceClient,
                      @Nonnull ItemReference data) {
        this.projectServiceClient = projectServiceClient;
        this.data = data;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Promise<String> getContent() {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {
                for (Link link : data.getLinks()) {
                    if (GET_CONTENT.equals(link.getRel())) {
                        sendRequest(callback, link.getHref());
                        break;
                    }
                }
            }
        });
    }

    private void sendRequest(@Nonnull final AsyncCallback<String> callback, @Nonnull String href) {
        try {
            new RequestBuilder(RequestBuilder.GET, href).sendRequest("", new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    callback.onSuccess(response.getText());
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Log.error(DockerFile.class, exception);
                }
            });
        } catch (RequestException e) {
            Log.error(DockerFile.class, e);
        }
    }

    @Nonnull
    @Override
    public String getPath() {
        return data.getPath();
    }

    @Nonnull
    @Override
    public String getName() {
        return data.getName();
    }

    @Override
    public String getDisplayName() {
        return data.getName();
    }

    @Nullable
    @Override
    public String getMediaType() {
        return data.getMediaType();
    }

    @Nullable
    @Override
    public HasProjectDescriptor getProject() {
        return null;
    }

    @Override
    public String getContentUrl() {
        List<Link> links = data.getLinks();
        Link li = null;
        for (Link link : links) {
            if (link.getRel().equals("get content")) {
                li = link;
                break;
            }
        }
        return li == null ? null : li.getHref();
    }

    @Override
    public Promise<Void> updateContent(final String content) {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<Void>() {
            @Override
            public void makeCall(final AsyncCallback<Void> callback) {
                projectServiceClient.updateFile(getPath(), content, null, new AsyncRequestCallback<Void>() {
                    @Override
                    protected void onSuccess(Void result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        });
    }
}