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
package org.eclipse.che.ide.ext.git.client.pull;

import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.Event;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.ext.git.client.BranchSearcher;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.PullResponse;
import org.eclipse.che.ide.ext.git.shared.Remote;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyBoolean;
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
 * Testing {@link PullPresenter} functionality.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public class PullPresenterTest extends BaseTest {
    public static final boolean SHOW_ALL_INFORMATION = true;
    @Mock
    private FileNode            file;
    @Mock
    private PullView            view;
    @Mock
    private Branch              branch;
    @Mock
    private EditorAgent         editorAgent;
    @Mock
    private EditorInput         editorInput;
    @Mock
    private EditorPartPresenter partPresenter;
    @Mock
    private BranchSearcher      branchSearcher;
    @Mock
    private PullResponse        pullResponse;

    @InjectMocks
    private PullPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        StringMap<EditorPartPresenter> partPresenterMap = Collections.createStringMap();
        partPresenterMap.put("partPresenter", partPresenter);

        when(view.getRepositoryName()).thenReturn(REPOSITORY_NAME);
        when(view.getRepositoryUrl()).thenReturn(REMOTE_URI);
        when(view.getLocalBranch()).thenReturn(LOCAL_BRANCH);
        when(view.getRemoteBranch()).thenReturn(REMOTE_BRANCH);
        when(branch.getName()).thenReturn(REMOTE_BRANCH);
        when(editorAgent.getOpenedEditors()).thenReturn(partPresenterMap);
        when(partPresenter.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);
    }

    @Test
    public void testShowDialogWhenBranchListRequestIsSuccessful() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));
        final Array<Branch> branches = Collections.createArray();
        branches.add(branch);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, remotes);
                return callback;
            }
        }).when(service).remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(),
                                    (AsyncRequestCallback<Array<Remote>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(view, times(2)).setEnablePullButton(eq(ENABLE_BUTTON));
        verify(view).setRepositories((Array<Remote>)anyObject());
        verify(view).showDialog();
        verify(view).setRemoteBranches((Array<String>)anyObject());
        verify(view).setLocalBranches((Array<String>)anyObject());
    }

    @Test
    public void testSelectActiveBranch() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));
        final Array<Branch> branches = Collections.createArray();
        branches.add(branch);
        when(branch.isActive()).thenReturn(ACTIVE_BRANCH);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, remotes);
                return callback;
            }
        }).when(service).remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(),
                                    (AsyncRequestCallback<Array<Remote>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());
        verify(view, times(2)).setEnablePullButton(eq(ENABLE_BUTTON));
        verify(view).setRepositories((Array<Remote>)anyObject());
        verify(view).showDialog();
        verify(view).setRemoteBranches((Array<String>)anyObject());
        verify(view).setLocalBranches((Array<String>)anyObject());
        verify(view).selectRemoteBranch(anyString());

        presenter.onRemoteBranchChanged();
        verify(view).selectLocalBranch(anyString());
    }

    @Test
    public void testShowDialogWhenBranchListRequestIsFailed() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, remotes);
                return callback;
            }
        }).when(service)
          .remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(), (AsyncRequestCallback<Array<Remote>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(constant).branchesListFailed();
        verify(notificationManager).showError(anyString());
        verify(view).setEnablePullButton(eq(DISABLE_BUTTON));
    }

    @Test
    public void testShowDialogWhenRemoteListRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(),
                                    (AsyncRequestCallback<Array<Remote>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(constant).remoteListFailed();
        verify(view).setEnablePullButton(eq(DISABLE_BUTTON));
    }

    @Test
    public void testOnPullClickedWhenPullRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, pullResponse);
                return callback;
            }
        }).when(service).pull((ProjectDescriptor)anyObject(), anyString(), anyString(), (AsyncRequestCallback<PullResponse>)anyObject());

        presenter.showDialog();
        presenter.onPullClicked();

        verify(view, times(2)).getRepositoryName();
        verify(view).getRepositoryUrl();
        verify(view).close();
        verify(editorAgent).getOpenedEditors();
        verify(service).pull(eq(rootProjectDescriptor), anyString(), eq(REPOSITORY_NAME), (AsyncRequestCallback)anyObject());
        verify(notificationManager).showInfo(anyString());
        verify(constant).pullSuccess(eq(REMOTE_URI));
        verify(appContext).getCurrentProject();
        verify(eventBus, times(2)).fireEvent(Matchers.<Event<GwtEvent>>anyObject());
        verify(partPresenter).getEditorInput();
    }

    @Test
    public void testOnPullClickedWhenPullRequestIsFailed() throws Exception {
        final Throwable exception = mock(Throwable.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, exception);
                return callback;
            }
        }).when(service).pull((ProjectDescriptor)anyObject(), anyString(), anyString(), (AsyncRequestCallback<PullResponse>)anyObject());
        when(exception.getMessage()).thenReturn("error Message");

        presenter.showDialog();
        presenter.onPullClicked();

        verify(service).pull(eq(rootProjectDescriptor), anyString(), eq(REPOSITORY_NAME), (AsyncRequestCallback<PullResponse>)anyObject());
        verify(view).close();
        verify(notificationManager).showError(anyString());
    }

    @Test
    public void testOnPullClickedWhenMergeConflictHappenedAndRefreshProjectIsSuccessful() throws Exception {
        final Throwable exception = mock(Throwable.class);
        when(exception.getMessage()).thenReturn("Merge conflict");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, exception);
                return callback;
            }
        }).when(service).pull((ProjectDescriptor)anyObject(), anyString(), anyString(), (AsyncRequestCallback<PullResponse>)anyObject());

        presenter.showDialog();
        presenter.onPullClicked();

        verify(view).close();
        verify(service).pull(eq(rootProjectDescriptor), anyString(), eq(REPOSITORY_NAME), (AsyncRequestCallback)anyObject());
        verify(notificationManager).showError(anyString());
        verify(appContext).getCurrentProject();
        verify(eventBus, times(2)).fireEvent(Matchers.<Event<GwtEvent>>anyObject());
        verify(partPresenter).getEditorInput();
    }

    @Test
    public void testOnPullClickedWhenAlreadyUpToDateHappenedAndRefreshProjectIsNotCalled() throws Exception {
        when(pullResponse.isAlreadyUpToDate()).thenReturn(true);
        when(pullResponse.getCommandOutput()).thenReturn("Already up-to-date");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, pullResponse);
                return callback;
            }
        }).when(service).pull((ProjectDescriptor)anyObject(), anyString(), anyString(), (AsyncRequestCallback<PullResponse>)anyObject());

        presenter.showDialog();
        presenter.onPullClicked();

        verify(view).close();
        verify(notificationManager).showInfo("Already up-to-date");
        //check Refresh project is not called
        verify(eventBus, never()).fireEvent(Matchers.<Event<GwtEvent>>anyObject());
    }

    @Test
    public void testOnPullClickedWhenPullHappenedAndRefreshProjectIsCalled() throws Exception {
        when(pullResponse.isAlreadyUpToDate()).thenReturn(false);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, pullResponse);
                return callback;
            }
        }).when(service).pull((ProjectDescriptor)anyObject(), anyString(), anyString(), (AsyncRequestCallback<PullResponse>)anyObject());

        presenter.showDialog();
        presenter.onPullClicked();

        verify(view).close();
        verify(notificationManager).showInfo(anyString());
        verify(constant).pullSuccess(eq(REMOTE_URI));
        verify(eventBus, times(2)).fireEvent(Matchers.<Event<GwtEvent>>anyObject());
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }
}