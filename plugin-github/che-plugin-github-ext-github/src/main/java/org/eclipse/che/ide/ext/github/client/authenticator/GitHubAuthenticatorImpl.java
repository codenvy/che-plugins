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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.github.client.GitHubLocalizationConstant;
import org.eclipse.che.ide.ext.ssh.client.SshKeyService;
import org.eclipse.che.ide.ext.ssh.dto.KeyItem;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.security.oauth.JsOAuthWindow;
import org.eclipse.che.security.oauth.OAuthCallback;
import org.eclipse.che.security.oauth.OAuthStatus;
import org.eclipse.che.ide.util.Config;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Roman Nikitenko
 */
public class GitHubAuthenticatorImpl implements GitHubAuthenticator, OAuthCallback, GitHubAuthenticatorViewImpl.ActionDelegate {
    public static final String GITHUB_HOST = "github.com";

    AsyncCallback<OAuthStatus> callback;

    private SshKeyService              sshKeyService;
    private DialogFactory              dialogFactory;
    private DtoUnmarshallerFactory     dtoUnmarshallerFactory;
    private GitHubAuthenticatorView    view;
    private NotificationManager        notificationManager;
    private GitHubLocalizationConstant locale;
    private String                     baseUrl;
    private AppContext                 appContext;

    @Inject
    public GitHubAuthenticatorImpl(SshKeyService sshKeyService,
                                   GitHubAuthenticatorView view,
                                   DialogFactory dialogFactory,
                                   GitHubLocalizationConstant locale,
                                   @RestContext String baseUrl,
                                   DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                   NotificationManager notificationManager,
                                   AppContext appContext) {
        this.view = view;
        this.view.setDelegate(this);
        this.locale = locale;
        this.baseUrl = baseUrl;
        this.sshKeyService = sshKeyService;
        this.dialogFactory = dialogFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.appContext = appContext;
    }

    @Override
    public void authorize(@Nonnull final AsyncCallback<OAuthStatus> callback) {
        this.callback = callback;
        view.showDialog();
    }

    @Override
    public void onCancelled() {
        callback.onFailure(new Exception("Authorization request rejected by user."));
    }

    @Override
    public void onAccepted() {
        showAuthWindow();
    }

    @Override
    public void onAuthenticated(OAuthStatus authStatus) {
        if (view.isGenerateKeysSelected()) {
            generateSshKeys(authStatus);
            return;
        }
        callback.onSuccess(authStatus);
    }

    private void showAuthWindow() {
        JsOAuthWindow authWindow = new JsOAuthWindow(getAuthUrl(), "error.url", 500, 980, this);
        authWindow.loginWithOAuth();
    }

    private String getAuthUrl() {
        String userId = appContext.getCurrentUser().getProfile().getId();
        return baseUrl
               + "/oauth/authenticate?oauth_provider=github"
               + "&scope=user,repo,write:public_key&userId=" + userId
               + "&redirect_after_login="
               + Window.Location.getProtocol() + "//"
               + Window.Location.getHost() + "/ws/"
               + Config.getWorkspaceName();
    }

    private void generateSshKeys(final OAuthStatus authStatus) {
        if (sshKeyService.getSshKeyProviders().containsKey(GITHUB_HOST)) {
            String userId = appContext.getCurrentUser().getProfile().getId();
            sshKeyService.getSshKeyProviders().get(GITHUB_HOST).generateKey(userId, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    callback.onSuccess(authStatus);
                    notificationManager.showInfo(locale.authMessageKeyUploadSuccess());
                }

                @Override
                public void onFailure(Throwable exception) {
                    dialogFactory.createMessageDialog(locale.authTitle(), locale.authMessageUnableCreateSshKey(), null).show();
                    callback.onFailure(new Exception(locale.authMessageUnableCreateSshKey()));
                    getFailedKey();
                }
            });
        } else {
            dialogFactory.createMessageDialog(locale.authTitle(), locale.authMessageUnableCreateSshKey(), null).show();
            callback.onFailure(new Exception(locale.authMessageUnableCreateSshKey()));
        }
    }

    /** Need to remove failed uploaded keys from local storage if they can't be uploaded to github */
    private void getFailedKey() {
        sshKeyService.getAllKeys(new AsyncRequestCallback<List<KeyItem>>(dtoUnmarshallerFactory.newListUnmarshaller(KeyItem.class)) {
            @Override
            public void onSuccess(List<KeyItem> result) {
                for (KeyItem key : result) {
                    if (key.getHost().equals(GITHUB_HOST)) {
                        removeFailedKey(key);
                        return;
                    }
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.error(GitHubAuthenticator.class, exception);
            }
        });
    }

    /**
     * Remove failed key.
     *
     * @param key
     *         failed key
     */
    private void removeFailedKey(@Nonnull final KeyItem key) {
        sshKeyService.deleteKey(key, new AsyncRequestCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error(GitHubAuthenticator.class, caught);
            }

            @Override
            public void onSuccess(Void result) {
            }
        });
    }
}
