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
package org.eclipse.che.ide.ext.git.client.url;

import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link ShowProjectGitReadOnlyUrlPresenter} functionality.
 *
 * @author Andrey Plotnikov
 * @author Oleksii Orel
 */
public class ShowProjectGitReadOnlyUrlPresenterTest extends BaseTest {

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<String>> asyncRequestCallbackGitReadOnlyUrlCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<Remote>>> asyncRequestCallbackRemoteListCaptor;

    @Mock
    private ShowProjectGitReadOnlyUrlView      view;
    private ShowProjectGitReadOnlyUrlPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new ShowProjectGitReadOnlyUrlPresenter(view, service, appContext, constant,
                                                           notificationManager, dtoUnmarshallerFactory);
    }

    @Test
    public void getGitReadOnlyUrlAsyncCallbackIsSuccess() throws Exception {
        presenter.showDialog();
        verify(service).getGitReadOnlyUrl((ProjectDescriptor)anyObject(), asyncRequestCallbackGitReadOnlyUrlCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackGitReadOnlyUrlCaptor.getValue();

        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, LOCALE_URI);

        verify(appContext).getCurrentProject();
        verify(service).getGitReadOnlyUrl(eq(rootProjectDescriptor), (AsyncRequestCallback<String>)anyObject());
        verify(view).setLocaleUrl(eq(LOCALE_URI));
    }

    @Test
    public void getGitReadOnlyUrlAsyncCallbackIsFailed() throws Exception {
        presenter.showDialog();
        verify(service).getGitReadOnlyUrl((ProjectDescriptor)anyObject(), asyncRequestCallbackGitReadOnlyUrlCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackGitReadOnlyUrlCaptor.getValue();

        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
        onSuccess.invoke(callback, mock(Throwable.class));

        verify(appContext).getCurrentProject();
        verify(service).getGitReadOnlyUrl(eq(rootProjectDescriptor), (AsyncRequestCallback<String>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).initFailed();
    }

    @Test
    public void getGitRemoteListAsyncCallbackIsSuccess() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));
        presenter.showDialog();
        verify(service)
                .remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(), asyncRequestCallbackRemoteListCaptor.capture());
        AsyncRequestCallback<Array<Remote>> callback = asyncRequestCallbackRemoteListCaptor.getValue();

        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, remotes);

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(true), (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(view).setRemotes((Array<Remote>)anyObject());
    }

    @Test
    public void getGitRemoteListAsyncCallbackIsFailed() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));
        presenter.showDialog();
        verify(service)
                .remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(), asyncRequestCallbackRemoteListCaptor.capture());
        AsyncRequestCallback<Array<Remote>> callback = asyncRequestCallbackRemoteListCaptor.getValue();

        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
        onSuccess.invoke(callback, mock(Throwable.class));

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(true), (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(view).setRemotes(null);
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testOnCloseClicked() throws Exception {
        presenter.onCloseClicked();

        verify(view).close();
    }
}