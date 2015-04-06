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
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakPoint;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEventList;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerInfo;
import org.eclipse.che.ide.ext.java.jdi.shared.StackFrameDump;
import org.eclipse.che.ide.ext.java.jdi.shared.UpdateVariableRequest;
import org.eclipse.che.ide.ext.java.jdi.shared.Value;
import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.ui.loader.EmptyLoader;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.MimeType.TEXT_PLAIN;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;

/**
 * The implementation of {@link DebuggerServiceClient}.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyy
 */
@Singleton
public class DebuggerServiceClientImpl implements DebuggerServiceClient {
    private final String                          baseUrl;
    private final AsyncRequestLoader              loader;
    private final AsyncRequestFactory             asyncRequestFactory;
    private final JavaRuntimeLocalizationConstant localizationConstant;

    @Inject
    protected DebuggerServiceClientImpl(@Named("restContext") String baseUrl,
                                        @Named("workspaceId") String workspaceId,
                                        AsyncRequestLoader loader,
                                        AsyncRequestFactory asyncRequestFactory,
                                        JavaRuntimeLocalizationConstant localizationConstant) {
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        this.localizationConstant = localizationConstant;
        this.baseUrl = baseUrl + "/debug-java/" + workspaceId;
    }

    /** {@inheritDoc} */
    @Override
    public void connect(@Nonnull String host, int port, @Nonnull AsyncRequestCallback<DebuggerInfo> callback) {
        final String requestUrl = baseUrl + "/connect";
        final String params = "?host=" + host + "&port=" + port;
        asyncRequestFactory.createGetRequest(requestUrl + params).loader(loader, localizationConstant.debuggerConnecting()).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect(@Nonnull String id, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/disconnect/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader, localizationConstant.debuggerDisconnecting()).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void addBreakpoint(@Nonnull String id, @Nonnull BreakPoint breakPoint, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/breakpoints/add/" + id;
        asyncRequestFactory.createPostRequest(requestUrl, breakPoint).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getAllBreakpoints(@Nonnull String id, @Nonnull AsyncRequestCallback<String> callback) {
        final String requestUrl = baseUrl + "/breakpoints/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteBreakpoint(@Nonnull String id, @Nonnull BreakPoint breakPoint, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/breakpoints/delete/" + id;
        asyncRequestFactory.createPostRequest(requestUrl, breakPoint).loader(new EmptyLoader()).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAllBreakpoints(@Nonnull String id, @Nonnull AsyncRequestCallback<String> callback) {
        final String requestUrl = baseUrl + "/breakpoints/delete_all/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void checkEvents(@Nonnull String id, @Nonnull AsyncRequestCallback<DebuggerEventList> callback) {
        final String requestUrl = baseUrl + "/events/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(new EmptyLoader()).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getStackFrameDump(@Nonnull String id, @Nonnull AsyncRequestCallback<StackFrameDump> callback) {
        final String requestUrl = baseUrl + "/dump/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void resume(@Nonnull String id, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/resume/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getValue(@Nonnull String id, @Nonnull Variable var, @Nonnull AsyncRequestCallback<Value> callback) {
        final String requestUrl = baseUrl + "/value/get/" + id;
        asyncRequestFactory.createPostRequest(requestUrl, var.getVariablePath()).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(@Nonnull String id, @Nonnull UpdateVariableRequest request, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/value/set/" + id;
        asyncRequestFactory.createPostRequest(requestUrl, request).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void stepInto(@Nonnull String id, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/step/into/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void stepOver(@Nonnull String id, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/step/over/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void stepReturn(@Nonnull String id, @Nonnull AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseUrl + "/step/out/" + id;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void evaluateExpression(@Nonnull String id, @Nonnull String expression, @Nonnull AsyncRequestCallback<String> callback) {
        final String requestUrl = baseUrl + "/expression/" + id;
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .data(expression)
                           .header(ACCEPT, TEXT_PLAIN)
                           .header(CONTENTTYPE, TEXT_PLAIN)
                           .loader(new EmptyLoader())
                           .send(callback);
    }
}