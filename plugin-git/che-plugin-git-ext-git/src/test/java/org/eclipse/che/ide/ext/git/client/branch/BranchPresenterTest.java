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
package org.eclipse.che.ide.ext.git.client.branch;

import com.google.web.bindery.event.shared.Event;

import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchCheckoutRequest;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_ALL;
import static org.eclipse.che.ide.ext.git.client.patcher.WindowPatcher.RETURNED_MESSAGE;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Testing {@link BranchPresenter} functionality.
 *
 * @author Andrey Plotnikov
 */
public class BranchPresenterTest extends BaseTest {

    @Captor
    private ArgumentCaptor<InputCallback>                      inputCallbackCaptor;
    @Captor
    private ArgumentCaptor<ConfirmCallback>                    confirmCallbackCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Branch>>       createBranchCallbackCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<List<Branch>>> branchListCallbackCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<String>>       asyncRequestCallbackCaptor;

    public static final String  BRANCH_NAME        = "branchName";
    public static final String  REMOTE_BRANCH_NAME = "origin/branchName";
    public static final boolean NEED_DELETING      = true;
    public static final boolean IS_REMOTE          = true;
    public static final boolean IS_ACTIVE          = true;
    @Mock
    private BranchView             view;
    @Mock
    private FileNode               file;
    @Mock
    private EditorInput            editorInput;
    @Mock
    private EditorAgent            editorAgent;
    @Mock
    private Branch                 selectedBranch;
    @Mock
    private EditorPartPresenter    partPresenter;
    @Mock
    private GitOutputPartPresenter gitConsole;
    @Mock
    private WorkspaceAgent         workspaceAgent;
    @Mock
    private DialogFactory          dialogFactory;
    @Mock
    private DtoFactory             dtoFactory;
    @Mock
    private BranchCheckoutRequest  branchCheckoutRequest;

    private BranchPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new BranchPresenter(view, eventBus, dtoFactory, editorAgent, service, constant, appContext, notificationManager,
                                        dtoUnmarshallerFactory, gitConsole, workspaceAgent, dialogFactory);

        NavigableMap<String, EditorPartPresenter> partPresenterMap = new TreeMap<>();
        partPresenterMap.put("partPresenter", partPresenter);

        when(selectedBranch.getDisplayName()).thenReturn(BRANCH_NAME);
        when(selectedBranch.getName()).thenReturn(BRANCH_NAME);
        when(selectedBranch.isRemote()).thenReturn(IS_REMOTE);
        when(selectedBranch.isActive()).thenReturn(IS_ACTIVE);
        when(editorAgent.getOpenedEditors()).thenReturn(partPresenterMap);
        when(partPresenter.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);
    }

    @Ignore
    public void testShowDialogWhenGetBranchesRequestIsSuccessful() throws Exception {
        final List<Branch> branches = new ArrayList<>();

        presenter.showDialog();

        verify(service).branchList(eq(projectDescriptor), eq(LIST_ALL), branchListCallbackCaptor.capture());
        AsyncRequestCallback<List<Branch>> branchListCallback = branchListCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(branchListCallback, branches);

        verify(appContext).getCurrentProject();
        verify(view).setEnableCheckoutButton(eq(DISABLE_BUTTON));
        verify(view).setEnableDeleteButton(eq(DISABLE_BUTTON));
        verify(view).setEnableRenameButton(eq(DISABLE_BUTTON));
        verify(view).showDialog();
        verify(view).setBranches(eq(branches));
        verify(notificationManager, never()).showError(anyString());
        verify(constant, never()).branchesListFailed();
    }

    @Test
    public void testShowDialogWhenGetBranchesRequestIsFailed() throws Exception {
        presenter.showDialog();

        verify(service).branchList(eq(rootProjectDescriptor), eq(LIST_ALL), branchListCallbackCaptor.capture());
        AsyncRequestCallback<List<Branch>> branchListCallback = branchListCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(branchListCallback, mock(Throwable.class));

        verify(appContext).getCurrentProject();
        verify(view).setEnableCheckoutButton(eq(DISABLE_BUTTON));
        verify(view).setEnableDeleteButton(eq(DISABLE_BUTTON));
        verify(view).setEnableRenameButton(eq(DISABLE_BUTTON));
        verify(view).showDialog();
        verify(notificationManager).showError(anyString());
        verify(constant).branchesListFailed();
    }

    @Test
    public void testOnCloseClicked() throws Exception {
        presenter.onCloseClicked();

        verify(view).close();
    }

    @Test
    public void testOnRenameClickedWhenLocalBranchSelected() throws Exception {
        reset(selectedBranch);
        when(selectedBranch.getDisplayName()).thenReturn(BRANCH_NAME);
        when(selectedBranch.isRemote()).thenReturn(false);
        InputDialog inputDialog = mock(InputDialog.class);
        when(dialogFactory.createInputDialog(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyObject(), anyObject()))
                .thenReturn(inputDialog);

        selectBranch();
        presenter.onRenameClicked();

        verify(dialogFactory).createInputDialog(anyString(), anyString(), anyString(), anyInt(), anyInt(), inputCallbackCaptor.capture(),
                                                anyObject());
        InputCallback inputCallback = inputCallbackCaptor.getValue();
        inputCallback.accepted(RETURNED_MESSAGE);


        verify(service)
                .branchRename(eq(rootProjectDescriptor), eq(BRANCH_NAME), eq(RETURNED_MESSAGE), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> renameBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(renameBranchCallback, PROJECT_PATH);

        verify(selectedBranch, times(2)).getDisplayName();
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), eq(LIST_ALL), anyObject());
        verify(dialogFactory, never()).createConfirmDialog(anyString(), anyString(), anyObject(), anyObject());
        verify(notificationManager, never()).showError(anyString());
        verify(constant, never()).branchRenameFailed();
    }

    @Test
    public void testOnRenameClickedWhenRemoteBranchSelectedAndUserConfirmRename() throws Exception {
        reset(selectedBranch);
        when(selectedBranch.getDisplayName()).thenReturn(REMOTE_BRANCH_NAME);
        when(selectedBranch.isRemote()).thenReturn(true);
        InputDialog inputDialog = mock(InputDialog.class);
        when(dialogFactory.createInputDialog(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyObject(), anyObject()))
                .thenReturn(inputDialog);
        ConfirmDialog confirmDialog = mock(ConfirmDialog.class);
        when(dialogFactory.createConfirmDialog(anyString(), anyString(), anyObject(), anyObject())).thenReturn(confirmDialog);

        selectBranch();
        presenter.onRenameClicked();

        verify(dialogFactory).createConfirmDialog(anyString(), anyString(), confirmCallbackCaptor.capture(), anyObject());
        ConfirmCallback confirmCallback = confirmCallbackCaptor.getValue();
        confirmCallback.accepted();

        verify(dialogFactory).createInputDialog(anyString(), anyString(), anyString(), anyInt(), anyInt(), inputCallbackCaptor.capture(),
                                                anyObject());
        InputCallback inputCallback = inputCallbackCaptor.getValue();
        inputCallback.accepted(RETURNED_MESSAGE);


        verify(service).branchRename(eq(rootProjectDescriptor), eq(REMOTE_BRANCH_NAME), eq(RETURNED_MESSAGE),
                                     asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> renameBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(renameBranchCallback, PROJECT_PATH);

        verify(selectedBranch, times(2)).getDisplayName();
        verify(service, times(2))
                .branchList(eq(rootProjectDescriptor), eq(LIST_ALL), anyObject());
        verify(notificationManager, never()).showError(anyString());
        verify(constant, never()).branchRenameFailed();
    }

    /** Select mock branch for testing. */
    private void selectBranch() {
        presenter.showDialog();
        presenter.onBranchSelected(selectedBranch);
    }

    @Test
    public void testOnRenameClickedWhenBranchRenameRequestIsFailed() throws Exception {
        when(selectedBranch.getDisplayName()).thenReturn(BRANCH_NAME);
        when(selectedBranch.isRemote()).thenReturn(false);
        InputDialog inputDialog = mock(InputDialog.class);
        when(dialogFactory.createInputDialog(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyObject(), anyObject()))
                .thenReturn(inputDialog);

        selectBranch();
        presenter.onRenameClicked();

        verify(dialogFactory).createInputDialog(anyString(), anyString(), anyString(), anyInt(), anyInt(), inputCallbackCaptor.capture(),
                                                anyObject());
        InputCallback inputCallback = inputCallbackCaptor.getValue();
        inputCallback.accepted(RETURNED_MESSAGE);

        verify(service)
                .branchRename(eq(rootProjectDescriptor), eq(BRANCH_NAME), eq(RETURNED_MESSAGE), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> renameBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(renameBranchCallback, mock(Throwable.class));

        verify(selectedBranch, times(2)).getDisplayName();
        verify(notificationManager).showError(anyString());
        verify(constant).branchRenameFailed();
    }

    @Test
    public void testOnDeleteClickedWhenBranchDeleteRequestIsSuccessful() throws Exception {
        selectBranch();
        presenter.onDeleteClicked();

        verify(service).branchDelete(eq(rootProjectDescriptor), eq(BRANCH_NAME), eq(NEED_DELETING), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> deleteBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(deleteBranchCallback, PROJECT_PATH);

        verify(selectedBranch).getName();
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), eq(LIST_ALL), anyObject());
        verify(constant, never()).branchDeleteFailed();
        verify(notificationManager, never()).showError(anyString());
    }

    @Test
    public void testOnDeleteClickedWhenBranchDeleteRequestIsFailed() throws Exception {
        selectBranch();
        presenter.onDeleteClicked();

        verify(service).branchDelete(eq(rootProjectDescriptor), eq(BRANCH_NAME), eq(NEED_DELETING), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> deleteBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(deleteBranchCallback, mock(Throwable.class));

        verify(selectedBranch).getName();
        verify(constant).branchDeleteFailed();
        verify(notificationManager).showError(anyString());
    }

    @Test
    public void testOnCheckoutClickedWhenSelectedNotRemoteBranch() throws Exception {
        when(selectedBranch.isRemote()).thenReturn(false);
        when(dtoFactory.createDto(BranchCheckoutRequest.class)).thenReturn(branchCheckoutRequest);

        selectBranch();
        presenter.onCheckoutClicked();

        verify(branchCheckoutRequest).setName(eq(BRANCH_NAME));
        verifyNoMoreInteractions(branchCheckoutRequest);
        verify(service).branchCheckout(eq(rootProjectDescriptor),
                                       eq(branchCheckoutRequest),
                                       asyncRequestCallbackCaptor.capture());
    }

    @Test
    public void testOnCheckoutClickedWhenSelectedRemoteBranch() throws Exception {
        when(dtoFactory.createDto(BranchCheckoutRequest.class)).thenReturn(branchCheckoutRequest);

        selectBranch();
        presenter.onCheckoutClicked();

        verify(branchCheckoutRequest).setTrackBranch(eq(BRANCH_NAME));
        verifyNoMoreInteractions(branchCheckoutRequest);
        verify(service).branchCheckout(eq(rootProjectDescriptor),
                                       eq(branchCheckoutRequest),
                                       asyncRequestCallbackCaptor.capture());
    }

    @Test
    public void testOnCheckoutClickedWhenBranchCheckoutRequestAndRefreshProjectIsSuccessful() throws Exception {
        when(dtoFactory.createDto(BranchCheckoutRequest.class)).thenReturn(branchCheckoutRequest);

        selectBranch();
        presenter.onCheckoutClicked();

        verify(branchCheckoutRequest).setTrackBranch(eq(BRANCH_NAME));
        verifyNoMoreInteractions(branchCheckoutRequest);
        verify(service).branchCheckout(eq(rootProjectDescriptor),
                                       eq(branchCheckoutRequest),
                                       asyncRequestCallbackCaptor.capture());

        AsyncRequestCallback<String> checkoutBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(checkoutBranchCallback, PROJECT_PATH);

        verify(editorAgent).getOpenedEditors();
        verify(selectedBranch, times(2)).getDisplayName();
        verify(selectedBranch).isRemote();
        verify(service).branchCheckout(eq(rootProjectDescriptor),
                                       eq(branchCheckoutRequest),
                                       anyObject());
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), eq(LIST_ALL), anyObject());
        verify(appContext).getCurrentProject();
        verify(notificationManager, never()).showError(anyString());
        verify(constant, never()).branchCheckoutFailed();
        verify(eventBus).fireEvent(Matchers.<Event<OpenProjectEvent>>anyObject());
    }

    @Test
    public void testOnCheckoutClickedWhenBranchCheckoutRequestAndRefreshProjectIsSuccessfulButOpenFileIsNotExistInBranch()
            throws Exception {
        when(dtoFactory.createDto(BranchCheckoutRequest.class)).thenReturn(branchCheckoutRequest);

        selectBranch();
        presenter.onCheckoutClicked();

        verify(branchCheckoutRequest).setTrackBranch(eq(BRANCH_NAME));
        verifyNoMoreInteractions(branchCheckoutRequest);
        verify(service).branchCheckout(eq(rootProjectDescriptor),
                                       eq(branchCheckoutRequest),
                                       asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> checkoutBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(checkoutBranchCallback, PROJECT_PATH);

        verify(editorAgent).getOpenedEditors();
        verify(selectedBranch, times(2)).getDisplayName();
        verify(selectedBranch).isRemote();
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), eq(LIST_ALL), anyObject());
        verify(appContext).getCurrentProject();
        verify(eventBus).fireEvent(Matchers.<Event<OpenProjectEvent>>anyObject());
    }

    @Test
    public void testOnCheckoutClickedWhenBranchCheckoutRequestIsFailed() throws Exception {
        when(dtoFactory.createDto(BranchCheckoutRequest.class)).thenReturn(branchCheckoutRequest);
        selectBranch();
        presenter.onCheckoutClicked();

        verify(branchCheckoutRequest).setTrackBranch(eq(BRANCH_NAME));
        verifyNoMoreInteractions(branchCheckoutRequest);
        verify(service).branchCheckout(eq(rootProjectDescriptor),
                                       eq(branchCheckoutRequest),
                                       asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<String> checkoutBranchCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(checkoutBranchCallback, mock(Throwable.class));

        verify(selectedBranch, times(2)).getDisplayName();
        verify(selectedBranch).isRemote();
    }

    @Test
    public void testOnCreateClickedWhenBranchCreateRequestIsSuccessful() throws Exception {
        InputDialog inputDialog = mock(InputDialog.class);
        when(dialogFactory.createInputDialog(anyString(), anyString(), anyObject(), anyObject())).thenReturn(inputDialog);

        presenter.showDialog();
        presenter.onCreateClicked();

        verify(dialogFactory).createInputDialog(anyString(), anyString(), inputCallbackCaptor.capture(), anyObject());
        InputCallback inputCallback = inputCallbackCaptor.getValue();
        inputCallback.accepted(BRANCH_NAME);

        verify(service).branchCreate(anyObject(), anyString(), anyString(), createBranchCallbackCaptor.capture());
        AsyncRequestCallback<Branch> createBranchCallback = createBranchCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(createBranchCallback, selectedBranch);

        verify(constant).branchTypeNew();
        verify(service).branchCreate(eq(rootProjectDescriptor), anyString(), anyString(), anyObject());
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), eq(LIST_ALL), anyObject());
    }

    @Test
    public void testOnCreateClickedWhenBranchCreateRequestIsFailed() throws Exception {
        Throwable exception = mock(Exception.class);
        InputDialog inputDialog = mock(InputDialog.class);
        when(dialogFactory.createInputDialog(anyString(), anyString(), anyObject(), anyObject())).thenReturn(inputDialog);

        presenter.showDialog();
        presenter.onCreateClicked();

        verify(dialogFactory).createInputDialog(anyString(), anyString(), inputCallbackCaptor.capture(), anyObject());
        InputCallback inputCallback = inputCallbackCaptor.getValue();
        inputCallback.accepted(BRANCH_NAME);

        verify(service).branchCreate(anyObject(), anyString(), anyString(), createBranchCallbackCaptor.capture());
        AsyncRequestCallback<Branch> createBranchCallback = createBranchCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(createBranchCallback, exception);

        verify(constant).branchCreateFailed();
        verify(notificationManager).showError(anyString());
    }

    @Test
    public void checkoutButtonShouldBeEnabled() throws Exception {
        when(selectedBranch.isActive()).thenReturn(false);

        presenter.onBranchSelected(selectedBranch);

        verify(view).setEnableCheckoutButton(eq(ENABLE_BUTTON));
    }

    @Test
    public void checkoutButtonShouldBeDisabled() throws Exception {
        when(selectedBranch.isActive()).thenReturn(true);

        presenter.onBranchSelected(selectedBranch);

        verify(view).setEnableCheckoutButton(eq(DISABLE_BUTTON));
    }

    @Test
    public void renameButtonShouldBeEnabledWhenLocalBranchSelected() throws Exception {
        when(selectedBranch.isRemote()).thenReturn(false);

        presenter.onBranchSelected(selectedBranch);

        verify(view).setEnableRenameButton(eq(ENABLE_BUTTON));
    }

    @Test
    public void renameButtonShouldBeEnabledWhenRemoteBranchSelected() throws Exception {
        when(selectedBranch.isRemote()).thenReturn(true);

        presenter.onBranchSelected(selectedBranch);

        verify(view).setEnableRenameButton(eq(ENABLE_BUTTON));
    }

    @Test
    public void deleteButtonShouldBeEnabled() throws Exception {
        when(selectedBranch.isActive()).thenReturn(false);

        presenter.onBranchSelected(selectedBranch);

        verify(view).setEnableDeleteButton(eq(ENABLE_BUTTON));
    }

    @Test
    public void deleteButtonShouldBeDisabled() throws Exception {
        when(selectedBranch.isActive()).thenReturn(true);

        presenter.onBranchSelected(selectedBranch);

        verify(view).setEnableDeleteButton(eq(DISABLE_BUTTON));
    }
}