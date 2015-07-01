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

import org.eclipse.che.api.auth.shared.dto.Token;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;

import org.apache.commons.codec.binary.Base64;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Arrays;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test different situations of user access to projects with different permissions.
 * Test related to @link com.codenvy.ide.git.VFSPermissionsFilter class.
 *
 * @author Max Shaposhnik
 */

@Listeners(MockitoTestNGListener.class)
public class VFSPermissionsFilterTest {

    final static String               PASSWORD  = "password";
    final static String               WORKSPACE = "workspace";
    final static String               ENDPOINT = "http://dev.box.com/api";
    @InjectMocks
    final static VFSPermissionsFilter filter    = new VFSPermissionsFilter();
    //    final File projectDirectory;
    @Mock
    HttpServletResponse response;
    @Mock
    HttpServletRequest  request;
    @Mock
    FilterChain         filterChain;

    @Mock
    private HttpJsonHelper.HttpJsonHelperImpl httpJsonHelper;

    @BeforeMethod
    public void before() throws Exception {
        System.setProperty("organization.application.server.url", "orgPath");
        // Json helper mocking
        Field f = HttpJsonHelper.class.getDeclaredField("httpJsonHelperImpl");
        f.setAccessible(true);
        f.set(null, httpJsonHelper);
        filter.init(null);

        Field api = VFSPermissionsFilter.class.getDeclaredField("apiEndPoint");
        api.setAccessible(true);
        api.set(filter, ENDPOINT);

        Field path = VFSPermissionsFilter.class.getDeclaredField("vfsRoot");
        path.setAccessible(true);
        path.set(filter, "/");

        when((request).getRequestURL())
                .thenReturn(new StringBuffer("http://host.com/git-server/").append(WORKSPACE).append("/testProject"));
    }

    //
    @Test
    public void shouldSkipFurtherIfProjectHasPermissionsForAllAndUserIsEmpty()
            throws IOException, ServletException, UnauthorizedException, ForbiddenException, ConflictException,
                   NotFoundException, ServerException {
        //given
        when(httpJsonHelper.requestString(anyString(), eq("GET"), any())).thenReturn("123");
        //when
        filter.doFilter(request, response, filterChain);
        //then should skip further request
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldRespondUnauthorizedIfProjectHasPermissionsToSpecificUserAndUserIsEmpty()
            throws IOException, ServletException, UnauthorizedException, ForbiddenException, ConflictException,
                   NotFoundException, ServerException {
        //given
        when(httpJsonHelper.requestString(anyString(), eq("GET"), any())).thenThrow(new UnauthorizedException("NO"));
        //when
        filter.doFilter(request, response, filterChain);
        //then
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED));
    }

    @Test
    public void shouldSkipFurtherIfUsernameAndPasswordAndAccessAllowed()
            throws IOException, ServletException, ApiException {
        //given
        when(request.getHeader("authorization")).thenReturn(
                "BASIC " + (Base64.encodeBase64String(("OTHERUSER" + ":" + PASSWORD).getBytes())));

        // get token
        when(httpJsonHelper.request(eq(Token.class), anyString(), eq("POST"), any())).thenReturn(DtoFactory.getInstance().createDto(Token.class).withValue("123"));
        // get user by token
        when(httpJsonHelper.requestString(contains("internal/sso/server"), eq("GET"), isNull(), eq(Pair.of("clienturl",
                                                                                                                               URLEncoder
                                                                                                                                       .encode(ENDPOINT,
                                                                                                                                               "UTF-8")))))
                .thenReturn(JsonHelper.toJson(new UserImpl("name1", "id1", "123", Arrays.asList("role1"), false)));
        // check access
        when(httpJsonHelper.requestString(anyString(), eq("GET"), any())).thenReturn("123");
        //when
        filter.doFilter(request, response, filterChain);
        //then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldSkipFurtherIfTokenPresentAndAccessAllowed()
            throws IOException, ServletException, ApiException {
        //given
        when(request.getHeader("authorization")).thenReturn(
                "BASIC " + (Base64.encodeBase64String(("OTHERUSER" + ":" + "x-codenvy").getBytes())));

        // get user by token
        when(httpJsonHelper.requestString(contains("internal/sso/server"), eq("GET"), isNull(), eq(Pair.of("clienturl",
                                                                                                           URLEncoder
                                                                                                                   .encode(ENDPOINT,
                                                                                                                           "UTF-8")))))
                .thenReturn(JsonHelper.toJson(new UserImpl("name1", "id1", "123", Arrays.asList("role1"), false)));
        // check access
        when(httpJsonHelper.requestString(anyString(), eq("GET"), any())).thenReturn("123");
        //when
        filter.doFilter(request, response, filterChain);
        //then
        verify(filterChain).doFilter(request, response);
    }

}
