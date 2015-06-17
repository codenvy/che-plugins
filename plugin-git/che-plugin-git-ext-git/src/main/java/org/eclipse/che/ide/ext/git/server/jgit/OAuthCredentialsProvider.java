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
package org.eclipse.che.ide.ext.git.server.jgit;

import java.io.IOException;

import org.eclipse.che.api.auth.oauth.OAuthTokenProvider;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides credentials for jGit if uri is WSO2.
 *
 * @author <a href="maito:evoevodin@codenvy.com">Eugene Voevodin</a>
 */
public class OAuthCredentialsProvider extends CredentialsProvider { //implements Startable {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthCredentialsProvider.class);
    private final OAuthTokenProvider oauthTokenProvider;

    public OAuthCredentialsProvider(OAuthTokenProvider provider) {
        this.oauthTokenProvider = provider;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
//        if (CommonFactoryUrlFormat.WSO_2_URL_PATTERN.matcher(uri.toString()).matches()) {
//            try {
//                Token token = oauthTokenProvider.getToken("wso2", ConversationState.getCurrent().getIdentity().getUserId());
//                if (token != null) {
//                    for (CredentialItem i : items) {
//                        if (i instanceof CredentialItem.Username) {
//                            ((CredentialItem.Username) i).setValue(token.getValue());
//                            continue;
//                        }
//                        if (i instanceof CredentialItem.Password) {
//                            ((CredentialItem.Password) i).setValue("x-oauth-basic".toCharArray());
//                            continue;
//                        }
//                        LOG.error("Unexpected item " + i.getClass().getName());
//                        throw new UnsupportedCredentialItem(uri, i.getClass().getName());
//                    }
//                } else {
//                    throw new JGitInternalException("not authorized");
//                }
//            } catch (IOException e) {
//                LOG.error(e.getMessage());
//            }
//        }
        return true;
    }

//    @Override
//    public void start() {
//        CredentialsProvider.setDefault(this);
//    }
//
//    @Override
//    public void stop() {
//        //nothing to do
//    }
}
