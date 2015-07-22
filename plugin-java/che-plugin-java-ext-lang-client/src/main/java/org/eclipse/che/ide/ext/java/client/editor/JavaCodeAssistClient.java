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
package org.eclipse.che.ide.ext.java.client.editor;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.che.ide.ext.java.shared.dto.Problem;
import org.eclipse.che.ide.ext.java.shared.dto.ProposalApplyResult;
import org.eclipse.che.ide.ext.java.shared.dto.Proposals;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;

import java.util.List;

import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class JavaCodeAssistClient {

    private final String                 machineExtPath;
    private       DtoUnmarshallerFactory unmarshallerFactory;
    private final MachineManager         machineManager;
    private final AsyncRequestFactory    asyncRequestFactory;

    @Inject
    public JavaCodeAssistClient(@Named("cheExtensionPath") String machineExtPath,
                                DtoUnmarshallerFactory unmarshallerFactory,
                                MachineManager machineManager,
                                AsyncRequestFactory asyncRequestFactory) {
        this.machineExtPath = machineExtPath;
        this.unmarshallerFactory = unmarshallerFactory;
        this.machineManager = machineManager;
        this.asyncRequestFactory = asyncRequestFactory;
    }

    public void computeProposals(String projectPath, String fqn, int offset, String contents, AsyncRequestCallback<Proposals> callback) {
        String url =
                machineExtPath + "/" + machineManager.getDeveloperMachineId() + "/jdt/code-assist/compute/completion" + "/?projectpath=" +
                projectPath + "&fqn=" + fqn + "&offset=" + offset;
        asyncRequestFactory.createPostRequest(url, null).data(contents).send(callback);
    }

    public void computeAssistProposals(String projectPath, String fqn, int offset, List<Problem> problems,
                                       AsyncRequestCallback<Proposals> callback) {
        String url = machineExtPath + "/" + machineManager.getDeveloperMachineId() + "/jdt/code-assist/compute/assist" + "/?projectpath=" +
                     projectPath + "&fqn=" + fqn + "&offset=" + offset;
        Array<Problem> prob = Collections.createArray(problems);
        asyncRequestFactory.createPostRequest(url, prob).send(callback);
    }


    public void applyProposal(String sessionId, int index, boolean insert, final AsyncCallback<ProposalApplyResult> callback) {
        String url = machineExtPath + "/" + machineManager.getDeveloperMachineId() + "/jdt/code-assist/apply/completion/?sessionid=" +
                     sessionId + "&index=" + index + "&insert=" + insert;
        Unmarshallable<ProposalApplyResult> unmarshaller =
                unmarshallerFactory.newUnmarshaller(ProposalApplyResult.class);
        asyncRequestFactory.createGetRequest(url).send(new AsyncRequestCallback<ProposalApplyResult>(unmarshaller) {
            @Override
            protected void onSuccess(ProposalApplyResult proposalApplyResult) {
                callback.onSuccess(proposalApplyResult);
            }

            @Override
            protected void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    public String getProposalDocUrl(int id, String sessionId) {
        return machineExtPath + "/" + machineManager.getDeveloperMachineId() + "/jdt/code-assist/compute/info?sessionid=" + sessionId +
               "&index=" + id;
    }

    /**
     * Creates edits that describe how to format the given string.
     * Returns the changes required to format source.
     *
     * @param offset
     *         The given offset to start recording the edits (inclusive).
     * @param length
     *         the given length to stop recording the edits (exclusive).
     * @param content
     *         the content to format
     * @param callback
     *         the callback to use for the response
     */
    public void format(int offset, int length, String content, AsyncRequestCallback<Array<Change>> callback) {
        String url =
                machineExtPath + "/" + machineManager.getDeveloperMachineId() + "/code-formatting/format?offset=" + offset + "&length=" +
                length;
        asyncRequestFactory.createPostRequest(url, null)
                           .header(CONTENT_TYPE, MimeType.TEXT_PLAIN)
                           .data(content)
                           .send(callback);
    }
}
