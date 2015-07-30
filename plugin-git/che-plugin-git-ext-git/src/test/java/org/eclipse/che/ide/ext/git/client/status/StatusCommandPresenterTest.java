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
package org.eclipse.che.ide.ext.git.client.status;

import org.eclipse.che.api.git.shared.StatusFormat;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link StatusCommandPresenter} functionality.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public class StatusCommandPresenterTest extends BaseTest {
    public static final StatusFormat IS_NOT_FORMATTED = StatusFormat.LONG;
    @InjectMocks
    private StatusCommandPresenter presenter;

    @Mock
    private WorkspaceAgent workspaceAgent;

    @Mock
    private GitOutputPartPresenter gitOutput;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new StatusCommandPresenter(workspaceAgent, service, eventBus, appContext, gitOutput, constant, notificationManager);
    }

    @Test
    public void testShowStatusWhenStatusTextRequestIsSuccessful() throws Exception {
        doAnswer(new Answer<AsyncRequestCallback<String>>() {
            @Override
            public AsyncRequestCallback<String> answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, EMPTY_TEXT);
                return callback;
            }
        }).when(service).statusText(Matchers.<ProjectDescriptor>anyObject(),
                                    Matchers.<StatusFormat>anyObject(),
                                    Matchers.<AsyncRequestCallback<String>>anyObject());

        presenter.showStatus();

        verify(appContext).getCurrentProject();
        verify(service).statusText(eq(rootProjectDescriptor),
                                   eq(IS_NOT_FORMATTED),
                                   Matchers.<AsyncRequestCallback<String>>anyObject());
    }

    @Test
    public void testShowStatusWhenStatusTextRequestIsFailed() throws Exception {
        doAnswer(new Answer<AsyncRequestCallback<String>>() {
            @Override
            public AsyncRequestCallback<String> answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).statusText(Matchers.<ProjectDescriptor> anyObject(),
                                    Matchers.<StatusFormat> anyObject(),
                                    Matchers.<AsyncRequestCallback<String>> anyObject());

        presenter.showStatus();

        verify(appContext).getCurrentProject();
        verify(service).statusText(eq(rootProjectDescriptor), eq(IS_NOT_FORMATTED), Matchers.<AsyncRequestCallback<String>> anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).statusFailed();
    }

}