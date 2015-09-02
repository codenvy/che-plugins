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
/*
* CODENVY CONFIDENTIAL
* __________________
*
* [2012] - [2013] Codenvy, S.A.
* All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Codenvy S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to Codenvy S.A.
* and its suppliers and may be covered by U.S. and Foreign Patents,
* patents in process, and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Codenvy S.A..
*/
package org.eclipse.che.ide.ext.git.client.merge;

import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.MergeResult;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_LOCAL;
import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_REMOTE;
import static org.eclipse.che.api.git.shared.MergeResult.MergeStatus.ALREADY_UP_TO_DATE;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link MergePresenter} functionality.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public class MergePresenterTest extends BaseTest {
    public static final String DISPLAY_NAME = "displayName";
    @Mock
    private MergeView           view;
    @Mock
    private MergeResult         mergeResult;
    @Mock
    private EditorInput         editorInput;
    @Mock
    private EditorAgent         editorAgent;
    @Mock
    private Reference           selectedReference;
    @Mock
    private EditorPartPresenter partPresenter;
    private MergePresenter      presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new MergePresenter(view, eventBus, editorAgent, service, constant, appContext, notificationManager,
                                       dtoUnmarshallerFactory);
        NavigableMap<String, EditorPartPresenter> partPresenterMap = new TreeMap<>();
        partPresenterMap.put("partPresenter", partPresenter);

        when(mergeResult.getMergeStatus()).thenReturn(ALREADY_UP_TO_DATE);
        when(selectedReference.getDisplayName()).thenReturn(DISPLAY_NAME);
        when(editorAgent.getOpenedEditors()).thenReturn(partPresenterMap);
        when(partPresenter.getEditorInput()).thenReturn(editorInput);
    }

    @Test
    public void testShowDialogWhenAllOperationsAreSuccessful() throws Exception {
        final List<Branch> branches = new ArrayList<>();
        branches.add(mock(Branch.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<List<Branch>> callback = (AsyncRequestCallback<List<Branch>>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), eq(LIST_LOCAL), (AsyncRequestCallback<List<Branch>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<List<Branch>> callback = (AsyncRequestCallback<List<Branch>>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), eq(LIST_REMOTE), (AsyncRequestCallback<List<Branch>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(view).setEnableMergeButton(eq(DISABLE_BUTTON));
        verify(view).showDialog();
        verify(service).branchList(eq(rootProjectDescriptor), eq(LIST_LOCAL), (AsyncRequestCallback<List<Branch>>)anyObject());
        verify(service).branchList(eq(rootProjectDescriptor), eq(LIST_REMOTE), (AsyncRequestCallback<List<Branch>>)anyObject());
        verify(view).setRemoteBranches((List<Reference>)anyObject());
        verify(view).setLocalBranches((List<Reference>)anyObject());
        verify(notificationManager, never()).showNotification((Notification)anyObject());
    }

    @Test
    public void testShowDialogWhenAllOperationsAreFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<List<Branch>> callback = (AsyncRequestCallback<List<Branch>>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), eq(LIST_LOCAL), (AsyncRequestCallback<List<Branch>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<List<Branch>> callback = (AsyncRequestCallback<List<Branch>>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), eq(LIST_REMOTE), (AsyncRequestCallback<List<Branch>>)anyObject());

        presenter.showDialog();

        verify(service).branchList(eq(rootProjectDescriptor), eq(LIST_LOCAL), (AsyncRequestCallback<List<Branch>>)anyObject());
        verify(service).branchList(eq(rootProjectDescriptor), eq(LIST_REMOTE), (AsyncRequestCallback<List<Branch>>)anyObject());
        verify(notificationManager, times(2)).showNotification((Notification)anyObject());
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }

    @Test
    public void testOnMergeClickedWhenMergeRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<MergeResult> callback = (AsyncRequestCallback<MergeResult>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, mergeResult);
                return callback;
            }
        }).when(service).merge((ProjectDescriptor)anyObject(), anyString(), (AsyncRequestCallback<MergeResult>)anyObject());

        presenter.onReferenceSelected(selectedReference);
        presenter.onMergeClicked();

        verify(view).close();
        verify(editorAgent).getOpenedEditors();
        verify(service).merge(eq(rootProjectDescriptor), eq(DISPLAY_NAME), (AsyncRequestCallback<MergeResult>)anyObject());
        verify(appContext).getCurrentProject();
        verify(partPresenter).getEditorInput();
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testOnMergeClickedWhenMergeRequestIsFailed() throws Exception {
        when(selectedReference.getDisplayName()).thenReturn(DISPLAY_NAME);

        presenter.onReferenceSelected(selectedReference);
        presenter.onMergeClicked();

        verify(service).merge((ProjectDescriptor)anyObject(), eq(DISPLAY_NAME), (AsyncRequestCallback<MergeResult>)anyObject());
    }

    @Test
    public void testOnReferenceSelected() throws Exception {
        when(selectedReference.getDisplayName()).thenReturn(DISPLAY_NAME);

        presenter.onReferenceSelected(selectedReference);
        presenter.onMergeClicked();

        verify(service).merge((ProjectDescriptor)anyObject(), eq(DISPLAY_NAME), (AsyncRequestCallback<MergeResult>)anyObject());
    }
}