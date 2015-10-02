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
package org.eclipse.che.ide.ext.java.jdi.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.debug.Breakpoint;
import org.eclipse.che.ide.debug.BreakpointManager;
import org.eclipse.che.ide.ext.java.jdi.client.debug.DebuggerPresenter;
import org.eclipse.che.ide.ext.java.jdi.client.debug.DebuggerVariable;
import org.eclipse.che.ide.ext.java.jdi.client.debug.DebuggerView;
import org.eclipse.che.ide.ext.java.jdi.client.debug.changevalue.ChangeValuePresenter;
import org.eclipse.che.ide.ext.java.jdi.client.debug.expression.EvaluateExpressionPresenter;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolver;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakPoint;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerInfo;
import org.eclipse.che.ide.ext.java.jdi.shared.Location;
import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspaceHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link DebuggerPresenter} functionality.
 *
 * @author Artem Zatsarynnyy
 * @author Valeriy Svydenko
 */
public class DebuggerTest extends BaseTest {
    private static final String DEBUG_HOST = "localhost";
    private static final int    DEBUG_PORT = 8000;
    private static final String VM_NAME    = "vm_name";
    private static final String VM_VERSION = "vm_version";
    private static final String MIME_TYPE  = "application/java";

    @Captor
    private ArgumentCaptor<ProjectActionHandler> projectActionHandlerArgumentCaptor;
    @Mock
    private DebuggerView                         view;
    @Mock
    private EvaluateExpressionPresenter          evaluateExpressionPresenter;
    @Mock
    private ChangeValuePresenter                 changeValuePresenter;
    @Mock
    private BreakpointManager                    gutterManager;
    @Mock
    private FileNode                             file;
    @Mock
    private ItemReference                        fileReference;
    @Mock
    private FqnResolverFactory                   resolverFactory;
    @Mock
    private AsyncCallback<Breakpoint>            asyncCallbackBreakpoint;
    @Mock
    private ProjectDescriptor                    project;
    @Mock
    private AsyncCallback<Void>                  asyncCallbackVoid;
    @Mock
    private AppContext                           appContext;
    @Mock
    private CurrentProject                       currentProject;
    @Mock
    private EditorAgent                          editorAgent;
    @Mock
    private MessageBusProvider                   messageBusProvider;
    @Mock
    private UsersWorkspaceDto                    workspace;

    @Captor
    private ArgumentCaptor<StartWorkspaceHandler> startWorkspaceCaptor;

    @InjectMocks
    private DebuggerPresenter presenter;

    @Before
    public void setUp() {
        super.setUp();
        when(file.getData()).thenReturn(fileReference);
        when(fileReference.getMediaType()).thenReturn(MIME_TYPE);
        when(dtoFactory.createDto(Location.class)).thenReturn(mock(Location.class));
        when(dtoFactory.createDto(BreakPoint.class)).thenReturn(mock(BreakPoint.class));
        when(resolverFactory.getResolver(anyString())).thenReturn(mock(FqnResolver.class));

        when(messageBusProvider.getMessageBus()).thenReturn(messageBus);
        verify(eventBus).addHandler(eq(StartWorkspaceEvent.TYPE), startWorkspaceCaptor.capture());
        startWorkspaceCaptor.getValue().onWorkspaceStarted(workspace);
    }

    @Test
    public void testDisconnectDebuggerRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).disconnect(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onDisconnectButtonClicked();

        verify(service).disconnect(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        verifySetEnableButtons(DISABLE_BUTTON);

        verify(gutterManager).unmarkCurrentBreakpoint();
        verify(gutterManager).removeAllBreakpoints();
        verify(view).setEnableRemoveAllBreakpointsButton(DISABLE_BUTTON);
        verify(view).setEnableDisconnectButton(DISABLE_BUTTON);
        verify(workspaceAgent).removePart(presenter);
    }

    @Test
    public void testDisconnectDebuggerRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).disconnect(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onDisconnectButtonClicked();

        verify(service).disconnect(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testResumeRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).resume(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onResumeButtonClicked();

        verifySetEnableButtons(DISABLE_BUTTON);
        verify(service).resume(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(view).setVariables(anyListOf(DebuggerVariable.class));
        verify(view).setEnableChangeValueButtonEnable(eq(DISABLE_BUTTON));
        verify(gutterManager).unmarkCurrentBreakpoint();
    }

    @Test
    public void testResumeRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).resume(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.onResumeButtonClicked();

        verify(service).resume(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testStepIntoRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).stepInto(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        when(view.resetStepIntoButton(false)).thenReturn(true);

        presenter.onStepIntoButtonClicked();

        verify(service).stepInto(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(view).setVariables(anyListOf(DebuggerVariable.class));
        verify(view).setEnableChangeValueButtonEnable(eq(DISABLE_BUTTON));
        verify(gutterManager).unmarkCurrentBreakpoint();
    }

    @Test
    public void testStepIntoRequestIfKeyUp() throws Exception {
        when(view.resetStepIntoButton(false)).thenReturn(false);

        presenter.onStepIntoButtonClicked();

        verify(service, never()).stepInto(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
    }

    @Test
    public void testStepIntoRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).stepInto(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        when(view.resetStepIntoButton(false)).thenReturn(true);

        presenter.onStepIntoButtonClicked();

        verify(service).stepInto(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testStepOverRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).stepOver(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        when(view.resetStepOverButton(false)).thenReturn(true);

        presenter.onStepOverButtonClicked();

        verify(service).stepOver(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(view).setVariables(anyListOf(DebuggerVariable.class));
        verify(view).setEnableChangeValueButtonEnable(eq(DISABLE_BUTTON));
        verify(gutterManager).unmarkCurrentBreakpoint();
    }

    @Test
    public void testStepOverRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).stepOver(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        when(view.resetStepOverButton(false)).thenReturn(true);

        presenter.onStepOverButtonClicked();

        verify(service).stepOver(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testStepOverRequestIfKeyup() throws Exception {
        when(view.resetStepOverButton(false)).thenReturn(false);

        presenter.onStepOverButtonClicked();

        verify(service, never()).stepOver(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
    }

    @Test
    public void testStepReturnRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).stepReturn(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        when(view.resetStepReturnButton(false)).thenReturn(true);

        presenter.onStepReturnButtonClicked();

        verify(service).stepReturn(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(view).setVariables(anyListOf(DebuggerVariable.class));
        verify(view).setEnableChangeValueButtonEnable(eq(DISABLE_BUTTON));
        verify(gutterManager).unmarkCurrentBreakpoint();
    }

    @Test
    public void testStepReturnRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).stepReturn(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        when(view.resetStepReturnButton(false)).thenReturn(true);

        presenter.onStepReturnButtonClicked();

        verify(service).stepReturn(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testStepReturnRequestIfKeyup() throws Exception {
        when(view.resetStepReturnButton(false)).thenReturn(false);

        presenter.onStepReturnButtonClicked();

        verify(service, never()).stepReturn(anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
    }

    @Test
    public void testAddBreakpointRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).addBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.addBreakpoint(file, anyInt(), asyncCallbackBreakpoint);

        verify(service).addBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(asyncCallbackBreakpoint).onSuccess((Breakpoint)anyObject());
    }

    @Test
    public void testAddBreakpointRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).addBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.addBreakpoint(file, anyInt(), asyncCallbackBreakpoint);

        verify(service).addBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(asyncCallbackBreakpoint).onFailure((Throwable)anyObject());
    }

    @Test
    public void testRemoveBreakpointRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).deleteBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.deleteBreakpoint(file, anyInt(), asyncCallbackVoid);

        verify(service).deleteBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(asyncCallbackVoid).onSuccess((Void)anyObject());
    }

    @Test
    public void testRemoveBreakpointRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                //noinspection unchecked
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).deleteBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());

        presenter.deleteBreakpoint(file, anyInt(), asyncCallbackVoid);

        verify(service).deleteBreakpoint(anyString(), (BreakPoint)anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(asyncCallbackVoid).onFailure((Throwable)anyObject());
    }

    @Test
    public void shouldOpenChangeVariableValueDialog() throws Exception {
        presenter.onSelectedVariableElement(mock(DebuggerVariable.class));
        presenter.onChangeValueButtonClicked();

        verify(changeValuePresenter).showDialog((DebuggerInfo)anyObject(),
                                                (Variable)anyObject(),
                                                Matchers.<AsyncCallback<String>>anyObject());
    }

    @Test
    public void shouldOpenEvaluateExpressionDialog() throws Exception {
        presenter.onEvaluateExpressionButtonClicked();

        verify(evaluateExpressionPresenter).showDialog((DebuggerInfo)anyObject());
    }

    protected void verifySetEnableButtons(boolean enabled) {
        verify(view).setEnableResumeButton(eq(enabled));
        verify(view).setEnableStepIntoButton(eq(enabled));
        verify(view).setEnableStepOverButton(eq(enabled));
        verify(view).setEnableStepReturnButton(eq(enabled));
        verify(view).setEnableEvaluateExpressionButtonEnable(eq(enabled));
    }

}