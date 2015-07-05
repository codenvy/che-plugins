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
package com.codenvy.ide.git;

import org.eclipse.che.api.auth.shared.dto.Credentials;


import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;

import org.apache.commons.codec.binary.Base64;
import org.everrest.core.impl.provider.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * If user doesn't have permissions to repository, filter will deny request with 403.
 * Filter tries to access api/vfs for given project and if no access, request
 *
 * will be denied with 403 FORBIDDEN.
 *
 * @author  Max Shaposhnik
 */
@Singleton
public class VFSPermissionsFilter implements Filter {

    @Inject
    @Named("api.endpoint")
    String apiEndPoint;

    @Inject
    @Named("vfs.local.fs_root_dir")
    String vfsRoot;

    @Inject
    @Named("git.server.uri.prefix")
    String gitServerUriPrefix;

    private static final Logger LOG = LoggerFactory.getLogger(VFSPermissionsFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        int tokenPlace;
        String lastTokenBeforePath = "/" + gitServerUriPrefix + "/";
        if ((tokenPlace = req.getRequestURL().indexOf(lastTokenBeforePath)) != -1) {
            //get path to project
            String url = req.getRequestURL().substring(tokenPlace + lastTokenBeforePath.length());
            url = url.replaceFirst("/info/refs", "");
            url = url.replaceFirst("/git-upload-pack", "");
            //adaptation to fs
            url = url.replaceAll("/", File.separator);
            //search for dotVFS directory
            File projectDirectory = Paths.get(vfsRoot, url).toFile();
            String auth;
            String userName = "";
            String password = "";
            if ((auth = req.getHeader("authorization")) != null) {
                //get encoded password phrase
                String userAndPasswordEncoded = auth.substring(6);
                // decode Base64 user:password
                String userAndPasswordDecoded = new String(Base64.decodeBase64(userAndPasswordEncoded));
                //get username and password separator ':'
                int betweenUserAndPassword = userAndPasswordDecoded.indexOf(':');
                //get username - it is before first ':'
                userName = userAndPasswordDecoded.substring(0, betweenUserAndPassword);
                //get password - it is after first ':'
                password = userAndPasswordDecoded.substring(betweenUserAndPassword + 1);
            }

            // Check if user authenticated and has permissions to project, or send response code 403
            boolean needLogout = false;
            String token = null;
            User user;
            try {
                if (!userName.isEmpty()) {
                    if (password.equals("x-codenvy")) { // internal SSO
                        token = userName;
                    } else {
                        token = getToken(userName, password);
                        if (token == null) {
                            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN);
                            return;
                        }
                        needLogout = true;
                    }
                    user = getUserBySSO(token);
                    EnvironmentContext.getCurrent().setUser(user);
                }

                if (!hasAccessToItem(projectDirectory.getParentFile().getName(), projectDirectory.getName())) {
                    if (!userName.isEmpty()) {
                        // Authenticated but no access
                        ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    } else {
                        // Not authenticated, try again with credentials
                        ((HttpServletResponse)response).addHeader("Cache-Control", "private");
                        ((HttpServletResponse)response).addHeader("WWW-Authenticate", "Basic");
                        ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            } finally {
                if (needLogout) {
                    logout();
                }
                EnvironmentContext.reset();
            }
        }
        chain.doFilter(req, response);
    }

    @Override
    public void destroy() {
    }


    private User getUserBySSO(String token) throws ServletException {
        try {
            String response = HttpJsonHelper.requestString(apiEndPoint + "/internal/sso/server/"  + token,
                                         "GET", null, Pair.of("clienturl", URLEncoder.encode(apiEndPoint, "UTF-8")));
            JsonValue value = JsonHelper.parseJson(response);
                    return new UserImpl(value.getElement("name").getStringValue(),
                                               value.getElement("id").getStringValue(),
                                               value.getElement("token").getStringValue(),
                                               Collections.<String>emptySet(),
                                               value.getElement("temporary").getBooleanValue());
        } catch (ForbiddenException | UnauthorizedException | ServerException un) {
            return null;
        } catch (ConflictException | NotFoundException | IOException | JsonParseException e) {
            LOG.warn(e.getLocalizedMessage());
            throw new ServletException(e.getMessage(), e);

        }
    }

    private String getToken(String username, String password) throws ServletException {
        try {
            Token token = HttpJsonHelper.request(Token.class,
                                                 DtoFactory.getInstance().createDto(Link.class).withMethod("POST")
                                                           .withHref(apiEndPoint + "/auth/login/"),
                                                 DtoFactory.getInstance().createDto(Credentials.class)
                                                           .withUsername(username).withPassword(password));
            return token.getValue();
        } catch (ForbiddenException | UnauthorizedException un) {
            return null;
        } catch (ConflictException | ServerException | NotFoundException | IOException e) {
            LOG.warn(e.getLocalizedMessage());
            throw new ServletException(e.getMessage(), e);
        }
    }


    private boolean hasAccessToItem(String workspaceId, String projectName) throws ServletException {
        // Trying to access http://codenvy.com/api/vfs/workspacecs037e4z3mp867le/v2/itembypath/projectname
        // we dont need any entity, just to know if we have access or no.
        try {
            HttpJsonHelper.requestString(apiEndPoint + "/vfs/" + workspaceId + "/v2/itembypath/" + projectName,
                                         "GET", null);
            return true;
        } catch (ForbiddenException | UnauthorizedException un) {
            return false;
        } catch (ConflictException | ServerException | NotFoundException | IOException e) {
            LOG.warn(e.getLocalizedMessage());
            throw new ServletException(e.getMessage(), e);
        }
    }

    private void logout() {
        try {
            HttpJsonHelper.requestString(apiEndPoint + "/auth/logout/","GET", null);
        } catch (ForbiddenException | UnauthorizedException un) {
            // OK already logout
        } catch (ConflictException | ServerException | NotFoundException | IOException e) {
            LOG.warn(e.getLocalizedMessage());
        }
    }
}
