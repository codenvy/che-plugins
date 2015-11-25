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
package org.eclipse.che.ide.ext.openshift.server;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.openshift.internal.restclient.OpenShiftAPIVersion;
import com.openshift.internal.restclient.http.UrlConnectionHttpClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.NoopSSLCertificateCallback;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import com.openshift.restclient.http.IHttpClient;

import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.ide.ext.openshift.shared.dto.OpenshiftServerInfo;
import org.eclipse.che.security.oauth.RemoteOAuthTokenProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * @author Sergii Leschenko
 */
@Singleton
public class ClientFactory {
    private final LoadingCache<String, IClient> token2clientCache;
    private final String                        openshiftApiEndpoint;
    private final RemoteOAuthTokenProvider      provider;
    /** Client instance without specified token for providing information about openshift server*/
    private final IClient                       infoClient;

    @Inject
    public ClientFactory(@Named("openshift.api.endpoint") String openshiftApiEndpoint,
                         RemoteOAuthTokenProvider provider) {
        this.openshiftApiEndpoint = openshiftApiEndpoint;
        this.provider = provider;
        this.token2clientCache = CacheBuilder.newBuilder()
                                             .maximumSize(1000)
                                             .expireAfterAccess(10, TimeUnit.MINUTES)
                                             .build(new CacheLoader<String, IClient>() {
                                                 @Override
                                                 public IClient load(String token) throws Exception {
                                                     return createClient(token);
                                                 }
                                             });
        this.infoClient = new com.openshift.restclient.ClientFactory().create(openshiftApiEndpoint, new NoopSSLCertificateCallback());
    }

    public OpenshiftServerInfo getClientInfo() {
        final String openShiftAPIVersion = infoClient.getOpenShiftAPIVersion();
        final String openshiftEndpoint = (OpenShiftAPIVersion.v1beta3.toString().equals(openShiftAPIVersion) ? "osapi" : "oapi") +
                                         "/" + openShiftAPIVersion;
        return newDto(OpenshiftServerInfo.class).withOpenshiftEndpoint(infoClient.getBaseURL().toString() + openshiftEndpoint);
    }

    /**
     * Returns instance of IClient for working with openshift server for current user
     *
     * @throws UnauthorizedException
     *         when user did not have access token to openshift server
     * @throws ServerException
     *         when some exception occurs during getting of access token
     */
    public IClient getOpenshiftClient() throws UnauthorizedException, ServerException {
        try {
            final String userId = EnvironmentContext.getCurrent().getUser().getId();
            return token2clientCache.get(getToken(userId));
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof UnauthorizedException) {
                throw ((UnauthorizedException)cause);
            } else if (cause instanceof ServerException) {
                throw ((ServerException)cause);
            }
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns instance of IHttpClient for working with openshift server for current user
     *
     * @throws UnauthorizedException
     *         when user did not have access token to openshift server
     * @throws ServerException
     *         when some exception occurs during getting of access token
     */
    public IHttpClient getHttpClient() throws UnauthorizedException, ServerException {
        return newIHttpClient(EnvironmentContext.getCurrent().getUser().getId());
    }

    private String getToken(String userId) throws ServerException, UnauthorizedException {
        final OAuthToken token;
        try {
            token = provider.getToken("openshift", userId);
        } catch (IOException e) {
            throw new ServerException("Error getting of access token to openshift", e);
        }

        if (token == null || isNullOrEmpty(token.getToken())) {
            throw new UnauthorizedException("User doesn't have access token to openshift");
        }

        return token.getToken();
    }

    private IHttpClient newIHttpClient(String userId) throws ServerException, UnauthorizedException {
        return new UrlConnectionHttpClientBuilder().setAcceptMediaType("application/json")
                                                   .setSSLCertificateCallback(new NoopSSLCertificateCallback())
                                                   .setAuthorizationStrategy(new TokenAuthorizationStrategy(getToken(userId)))
                                                   .client();
    }

    private IClient createClient(String token) throws UnauthorizedException, ServerException {
        IClient client = new com.openshift.restclient.ClientFactory().create(openshiftApiEndpoint, new NoopSSLCertificateCallback());
        client.setAuthorizationStrategy(new TokenAuthorizationStrategy(token));
        return client;
    }
}
