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
package org.eclipse.che.ide.ext.git.client.reset.commit;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.ResetRequest;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.api.git.shared.ResetRequest.ResetType.HARD;
import static org.eclipse.che.api.git.shared.ResetRequest.ResetType.MIXED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ResetToCommitPresenter} functionality.
 *
 * @author Andrey Plotnikov
 * @author Alexander Andrienko
 */
public class ResetToCommitPresenterTest extends BaseTest {
    public static final boolean IS_TEXT_FORMATTED = true;
    public static final boolean IS_MIXED          = true;
    public static final String  FILE_PATH         = "/src/testClass.java";

    @Mock
    private ResetToCommitView      view;
    @Mock
    private FileNode               file;
    @Mock
    private EditorInput            editorInput;
    @Mock
    private EditorAgent            editorAgent;
    @Mock
    private ProjectServiceClient   projectServiceClient;
    @Mock
    private EventBus               eventBus;
    @Mock
    private EditorPartPresenter    partPresenter;
    @Mock
    private Revision               selectedRevision;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ProjectDescriptor>> argumentCaptor;

    @InjectMocks
    private ResetToCommitPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new ResetToCommitPresenter(view,
                                               service,
                                               constant,
                                               editorAgent,
                                               appContext,
                                               notificationManager,
                                               dtoUnmarshallerFactory,
                                               projectExplorer,
                                               eventBus,
                                               projectServiceClient);

        NavigableMap<String, EditorPartPresenter> partPresenterMap = new TreeMap<>();
        partPresenterMap.put("partPresenter", partPresenter);

        when(view.isMixMode()).thenReturn(IS_MIXED);
        when(selectedRevision.getId()).thenReturn(PROJECT_PATH);
        when(editorAgent.getOpenedEditors()).thenReturn(partPresenterMap);
        when(partPresenter.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);
        when(file.getPath()).thenReturn(FILE_PATH);
    }

    @Test
    public void testShowDialogWhenLogRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                GwtReflectionUtils.callOnSuccess(callback, mock(LogResponse.class));
                return callback;

            }
        }).when(service).log(any(ProjectDescriptor.class), anyBoolean(), Matchers.<AsyncRequestCallback<LogResponse>>anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).log(eq(rootProjectDescriptor), eq(!IS_TEXT_FORMATTED), Matchers.<AsyncRequestCallback<LogResponse>>anyObject());
        verify(view).setRevisions(Matchers.<ArrayList<Revision>>anyObject());
        verify(view).setMixMode(eq(IS_MIXED));
        verify(view).setEnableResetButton(eq(DISABLE_BUTTON));
        verify(view).showDialog();
    }

    @Test
    public void testShowDialogWhenLogRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));
                return callback;

            }
        }).when(service).log(any(ProjectDescriptor.class), anyBoolean(), Matchers.<AsyncRequestCallback<LogResponse>>anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).log(eq(rootProjectDescriptor), eq(!IS_TEXT_FORMATTED), Matchers.<AsyncRequestCallback<LogResponse>>anyObject());
        verify(constant).logFailed();
        verify(notificationManager).showNotification(any(Notification.class));
    }

    @Test
    public void testOnResetClickedWhenFileIsNotExistInCommitToReset()
            throws Exception {
        // Only in the cases of <code>ResetRequest.ResetType.HARD</code>  or <code>ResetRequest.ResetType.MERGE</code>
        // must change the workdir
        when(view.isMixMode()).thenReturn(false);
        when(view.isHardMode()).thenReturn(true);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                GwtReflectionUtils.callOnSuccess(callback, (Void)null);
                return callback;
            }
        }).when(service)
          .reset(any(ProjectDescriptor.class), anyString(), any(ResetRequest.ResetType.class), Matchers.<List<String>>anyObject(),
                 Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onRevisionSelected(selectedRevision);
        presenter.onResetClicked();

        verify(view).close();
        verify(selectedRevision).getId();
        verify(appContext).getCurrentProject();
        verify(currentProject).getRootProject();
        verify(service).reset(any(ProjectDescriptor.class),
                              eq(PROJECT_PATH),
                              eq(HARD),
                              Matchers.<List<String>>anyObject(),
                              Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification(any(Notification.class));
        verify(projectServiceClient).getProject(eq(PROJECT_PATH), argumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(argumentCaptor.getValue(), projectDescriptor);
        verify(projectDescriptor).getProblems();
        verify(projectExplorer).reloadChildren();
        verify(editorAgent).getOpenedEditors();
        verify(partPresenter).getEditorInput();
        verify(editorInput).getFile();
        verify(eventBus).fireEvent(Matchers.<FileContentUpdateEvent>anyObject());
    }

    @Test
    public void testOnResetClickedWhenFileIsChangedInCommitToReset()
            throws Exception {
        // Only in the cases of <code>ResetRequest.ResetType.HARD</code>  or <code>ResetRequest.ResetType.MERGE</code>
        // must change the workdir
        when(view.isMixMode()).thenReturn(false);
        when(view.isHardMode()).thenReturn(true);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                GwtReflectionUtils.callOnSuccess(callback, (Void)null);
                return callback;
            }
        }).when(service)
          .reset(any(ProjectDescriptor.class), anyString(), any(ResetRequest.ResetType.class),
                 Matchers.<List<String>>anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onRevisionSelected(selectedRevision);
        presenter.onResetClicked();

        verify(view).close();
        verify(selectedRevision).getId();
        verify(appContext).getCurrentProject();
        verify(service).reset(any(ProjectDescriptor.class),
                              eq(PROJECT_PATH),
                              eq(HARD),
                              Matchers.<List<String>>anyObject(),
                              Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification(any(Notification.class));
    }

    @Test
    public void testOnResetClickedWhenResetRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).reset(any(ProjectDescriptor.class), anyString(), any(ResetRequest.ResetType.class),
                               Matchers.<List<String>>anyObject(),
                               Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onRevisionSelected(selectedRevision);
        presenter.onResetClicked();

        verify(view).close();
        verify(selectedRevision).getId();
        verify(appContext).getCurrentProject();
        verify(service).reset(any(ProjectDescriptor.class),
                              eq(PROJECT_PATH),
                              eq(MIXED),
                              Matchers.<List<String>>anyObject(),
                              Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification(any(Notification.class));
        verify(eventBus, never()).fireEvent((Event<?>)anyObject());
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }

    @Test
    public void testOnRevisionSelected() throws Exception {
        presenter.onRevisionSelected(selectedRevision);

        verify(view).setEnableResetButton(eq(ENABLE_BUTTON));
    }
}