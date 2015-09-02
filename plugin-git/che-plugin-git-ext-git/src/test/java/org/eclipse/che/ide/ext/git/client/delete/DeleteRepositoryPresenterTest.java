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
package org.eclipse.che.ide.ext.git.client.delete;

import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.window.Window;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link DeleteRepositoryPresenter} functionality.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public class DeleteRepositoryPresenterTest extends BaseTest {
    private DeleteRepositoryPresenter presenter;

    @Mock
    Window.Resources resources;

    @Mock
    Window.Css css;

    @Override
    public void disarm() {
        super.disarm();
        when(resources.centerPanelCss()).thenReturn(css);
        when(css.alignBtn()).thenReturn("sdgsdf");
        when(css.glassVisible()).thenReturn("sdgsdf");
        when(css.contentVisible()).thenReturn("sdgsdf");
        when(css.animationDuration()).thenReturn(1);
        presenter = new DeleteRepositoryPresenter(service, eventBus, constant, appContext, notificationManager);
    }

    @Test
    public void testDeleteRepositoryWhenDeleteRepositoryIsSuccessful() throws Exception {
        Map attributes = mock(Map.class);
        List vcsProvider = mock(List.class);
        when(rootProjectDescriptor.getAttributes()).thenReturn(attributes);
        when(attributes.get("vcs.provider.name")).thenReturn(vcsProvider);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).deleteRepository((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Void>)anyObject());

        presenter.deleteRepository();

        verify(appContext).getCurrentProject();
        verify(service).deleteRepository(eq(rootProjectDescriptor), (AsyncRequestCallback<Void>)anyObject());
        verify(notificationManager).showInfo(eq(constant.deleteGitRepositorySuccess()));
        verify(rootProjectDescriptor).getAttributes();
        verify(attributes).get(anyString());
        verify(vcsProvider).clear();
        verify(eventBus).fireEvent(Matchers.<RefreshProjectTreeEvent>anyObject());
    }

    @Test
    public void testDeleteRepositoryWhenDeleteRepositoryIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).deleteRepository((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Void>)anyObject());

        presenter.deleteRepository();

        verify(appContext).getCurrentProject();
        verify(service).deleteRepository(eq(rootProjectDescriptor), (AsyncRequestCallback<Void>)anyObject());
        verify(notificationManager).showError(anyString());
    }
}
