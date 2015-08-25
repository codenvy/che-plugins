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
package org.eclipse.che.ide.ext.git.client.checkout;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link CheckoutReferencePresenter} functionality.
 *
 * @author Roman Nikitenko
 */
public class CheckoutReferenceTest extends BaseTest {
    private static final String CORRECT_REFERENCE   = "someTag";
    private static final String INCORRECT_REFERENCE = "";

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<String>> asyncCallbackCaptor;

    @Mock
    private CheckoutReferenceView      view;
    @InjectMocks
    private CheckoutReferencePresenter presenter;

    @Override
    public void disarm() {
        super.disarm();
    }

    @Test
    public void testOnReferenceValueChangedWhenValueIsIncorrect() throws Exception {

        presenter.referenceValueChanged(INCORRECT_REFERENCE);

        view.setCheckoutButEnableState(eq(false));
    }

    @Test
    public void testOnReferenceValueChangedWhenValueIsCorrect() throws Exception {

        presenter.referenceValueChanged(CORRECT_REFERENCE);

        view.setCheckoutButEnableState(eq(true));
    }

    @Test
    public void testShowDialog() throws Exception {

        presenter.showDialog();

        verify(view).setCheckoutButEnableState(eq(false));
        verify(view).showDialog();
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }


    @Test
    public void onEnterClickedWhenValueIsIncorrect() throws Exception {
        reset(service);
        when(view.getReference()).thenReturn(INCORRECT_REFERENCE);

        presenter.onEnterClicked();

        verify(view, never()).close();
        verify(service, never()).branchCheckout((ProjectDescriptor)anyObject(), anyString(), anyString(), anyBoolean(),
                                                (AsyncRequestCallback<String>)anyObject());
    }

    @Test
    public void onEnterClickedWhenValueIsCorrect() throws Exception {
        reset(service);
        when(view.getReference()).thenReturn(CORRECT_REFERENCE);

        presenter.onEnterClicked();

        verify(view).close();
        verify(service).branchCheckout((ProjectDescriptor)anyObject(), anyString(), anyString(), anyBoolean(),
                                       (AsyncRequestCallback<String>)anyObject());
    }

    @Test
    public void testOnCheckoutClickedWhenCheckoutIsSuccessful() throws Exception {
        reset(service);
        when(view.getReference()).thenReturn(CORRECT_REFERENCE);
        when(rootProjectDescriptor.getPath()).thenReturn(PROJECT_PATH);

        presenter.onEnterClicked();

        verify(service).branchCheckout((ProjectDescriptor)anyObject(), anyString(), anyString(), anyBoolean(),
                                       asyncCallbackCaptor.capture());
        AsyncRequestCallback<String> callback = asyncCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(callback, "");

        verify(view).close();
        verify(eventBus).fireEvent(Matchers.<ProjectActionEvent>anyObject());
    }

    @Test
    public void testOnCheckoutClickedWhenCheckoutIsFailed() throws Exception {
        reset(service);
        when(view.getReference()).thenReturn(CORRECT_REFERENCE);
        when(rootProjectDescriptor.getPath()).thenReturn(PROJECT_PATH);

        presenter.onEnterClicked();

        verify(service).branchCheckout((ProjectDescriptor)anyObject(), anyString(), anyString(), anyBoolean(),
                                       asyncCallbackCaptor.capture());
        AsyncRequestCallback<String> callback = asyncCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));

        verify(view).close();
        verify(eventBus, never()).fireEvent(Matchers.<ProjectActionEvent>anyObject());
        verify(notificationManager).showError(anyString());
    }
}