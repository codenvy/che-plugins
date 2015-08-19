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
package org.eclipse.che.ide.ext.github.client.authenticator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentUser;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.github.client.GitHubLocalizationConstant;
import org.eclipse.che.ide.ext.ssh.client.SshKeyProvider;
import org.eclipse.che.ide.ext.ssh.client.SshKeyService;
import org.eclipse.che.ide.ext.ssh.dto.KeyItem;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;
import org.eclipse.che.security.oauth.OAuthStatus;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testing {@link GitHubAuthenticatorImpl} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class GitHubAuthenticatorImplTest {
    public static final String GITHUB_HOST = "github.com";

    @Captor
    private ArgumentCaptor<AsyncCallback<OAuthStatus>> asyncCallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncCallback<Void>> generateKeyCallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<List<KeyItem>>> getAllKeysCallbackCaptor;

    @Mock
    private GitHubAuthenticatorView    view;
    @Mock
    private SshKeyService              sshKeyService;
    @Mock
    private DialogFactory              dialogFactory;
    @Mock
    private DtoUnmarshallerFactory     dtoUnmarshallerFactory;
    @Mock
    private NotificationManager        notificationManager;
    @Mock
    private GitHubLocalizationConstant locale;
    @Mock
    private AppContext                 appContext;
    @InjectMocks
    private GitHubAuthenticatorImpl    gitHubAuthenticator;

    @Test
    public void delegateShouldBeSet() throws Exception {
        verify(view).setDelegate(gitHubAuthenticator);
    }

    @Test
    public void dialogShouldBeShow() throws Exception {
        AsyncCallback<OAuthStatus> callback = getCallBack();
        gitHubAuthenticator.authorize(callback);

        verify(view).showDialog();
        assertThat(gitHubAuthenticator.callback, is(callback));
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsSelected() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = mock(OAuthStatus.class);

        SshKeyProvider keyProvider = mock(SshKeyProvider.class);
        Map<String, SshKeyProvider> providers = new HashMap<>();
        providers.put(GITHUB_HOST, keyProvider);

        CurrentUser user = mock(CurrentUser.class);
        ProfileDescriptor profile = mock(ProfileDescriptor.class);
        when(view.isGenerateKeysSelected()).thenReturn(true);
        when(sshKeyService.getSshKeyProviders()).thenReturn(providers);

        when(appContext.getCurrentUser()).thenReturn(user);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(userId);

        gitHubAuthenticator.onAuthenticated(authStatus);

        verify(view).isGenerateKeysSelected();
        verify(sshKeyService, times(2)).getSshKeyProviders();
        verify(appContext).getCurrentUser();
        verify(keyProvider).generateKey(eq(userId), Matchers.<AsyncCallback<Void>>anyObject());
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsNotSelected() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = mock(OAuthStatus.class);
        Map<String, SshKeyProvider> providers = new HashMap<>();
        SshKeyProvider keyProvider = mock(SshKeyProvider.class);
        providers.put(GITHUB_HOST, keyProvider);

        CurrentUser user = mock(CurrentUser.class);
        ProfileDescriptor profile = mock(ProfileDescriptor.class);
        when(view.isGenerateKeysSelected()).thenReturn(false);
        when(appContext.getCurrentUser()).thenReturn(user);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(userId);

        gitHubAuthenticator.authorize(getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        verify(view).isGenerateKeysSelected();
        verify(sshKeyService, never()).getSshKeyProviders();
        verify(keyProvider, never()).generateKey(anyString(), (AsyncCallback<Void>)anyObject());
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsSuccess() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = mock(OAuthStatus.class);
        SshKeyProvider keyProvider = mock(SshKeyProvider.class);
        Map<String, SshKeyProvider> providers = new HashMap<>();
        providers.put(GITHUB_HOST, keyProvider);

        CurrentUser user = mock(CurrentUser.class);
        ProfileDescriptor profile = mock(ProfileDescriptor.class);
        when(view.isGenerateKeysSelected()).thenReturn(true);
        when(sshKeyService.getSshKeyProviders()).thenReturn(providers);

        when(appContext.getCurrentUser()).thenReturn(user);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(userId);

        gitHubAuthenticator.authorize(getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        verify(keyProvider).generateKey(eq(userId), generateKeyCallbackCaptor.capture());
        AsyncCallback<Void> generateKeyCallback = generateKeyCallbackCaptor.getValue();
        generateKeyCallback.onSuccess(null);

        verify(view).isGenerateKeysSelected();
        verify(sshKeyService, times(2)).getSshKeyProviders();
        verify(appContext).getCurrentUser();
        verify(notificationManager).showInfo(anyString());
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsFailure() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = mock(OAuthStatus.class);

        SshKeyProvider keyProvider = mock(SshKeyProvider.class);
        Map<String, SshKeyProvider> providers = new HashMap<>();
        providers.put(GITHUB_HOST, keyProvider);

        CurrentUser user = mock(CurrentUser.class);
        ProfileDescriptor profile = mock(ProfileDescriptor.class);
        MessageDialog messageDialog = mock(MessageDialog.class);
        when(view.isGenerateKeysSelected()).thenReturn(true);
        when(sshKeyService.getSshKeyProviders()).thenReturn(providers);

        when(appContext.getCurrentUser()).thenReturn(user);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(userId);
        when(dialogFactory.createMessageDialog(anyString(), anyString(), Matchers.<ConfirmCallback>anyObject())).thenReturn(messageDialog);

        gitHubAuthenticator.authorize(getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        verify(keyProvider).generateKey(eq(userId), generateKeyCallbackCaptor.capture());
        AsyncCallback<Void> generateKeyCallback = generateKeyCallbackCaptor.getValue();
        generateKeyCallback.onFailure(new Exception(""));

        verify(view).isGenerateKeysSelected();
        verify(sshKeyService, times(2)).getSshKeyProviders();
        verify(appContext).getCurrentUser();
        verify(dialogFactory).createMessageDialog(anyString(), anyString(), Matchers.<ConfirmCallback>anyObject());
        verify(messageDialog).show();
        verify(sshKeyService).getAllKeys(Matchers.<AsyncRequestCallback<List<KeyItem>>> anyObject());
    }

    @Test
    public void onAuthenticatedWhenGetFailedKeyIsSuccess() throws Exception {
        String userId = "userId";
        KeyItem key = mock(KeyItem.class);
        List<KeyItem> keys = new ArrayList<>();
        keys.add(key);
        OAuthStatus authStatus = mock(OAuthStatus.class);
        SshKeyProvider keyProvider = mock(SshKeyProvider.class);
        Map<String, SshKeyProvider> providers = new HashMap<>();
        providers.put(GITHUB_HOST, keyProvider);

        CurrentUser user = mock(CurrentUser.class);
        ProfileDescriptor profile = mock(ProfileDescriptor.class);
        MessageDialog messageDialog = mock(MessageDialog.class);
        when(view.isGenerateKeysSelected()).thenReturn(true);
        when(sshKeyService.getSshKeyProviders()).thenReturn(providers);

        when(appContext.getCurrentUser()).thenReturn(user);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(userId);
        when(dialogFactory.createMessageDialog(anyString(), anyString(), Matchers.<ConfirmCallback>anyObject())).thenReturn(messageDialog);
        when(key.getHost()).thenReturn(GITHUB_HOST);

        gitHubAuthenticator.authorize(getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        verify(keyProvider).generateKey(eq(userId), generateKeyCallbackCaptor.capture());
        AsyncCallback<Void> generateKeyCallback = generateKeyCallbackCaptor.getValue();
        generateKeyCallback.onFailure(new Exception(""));

        verify(sshKeyService).getAllKeys(getAllKeysCallbackCaptor.capture());
        AsyncRequestCallback<List<KeyItem>> getAllKeysCallback = getAllKeysCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(getAllKeysCallback, keys);

        verify(view).isGenerateKeysSelected();
        verify(sshKeyService, times(2)).getSshKeyProviders();
        verify(appContext).getCurrentUser();
        verify(dialogFactory).createMessageDialog(anyString(), anyString(), Matchers.<ConfirmCallback>anyObject());
        verify(messageDialog).show();
        verify(sshKeyService).getAllKeys(Matchers.<AsyncRequestCallback<List<KeyItem>>>anyObject());
        verify(sshKeyService).deleteKey(Matchers.<KeyItem>anyObject(), Matchers.<AsyncRequestCallback<Void>>anyObject());
    }

    private AsyncCallback<OAuthStatus> getCallBack() {
        return new AsyncCallback<OAuthStatus>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(OAuthStatus result) {

            }
        };
    }
}
