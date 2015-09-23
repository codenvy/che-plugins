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
package org.eclipse.che.ide.ext.java.client.refactoring.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateMoveRefactoring;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateRenameRefactoring;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.LinkedRenameRefactoringApply;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.MoveSettings;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringPreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RenameRefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringUnmarshaller;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.MimeType.TEXT_PLAIN;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
final class RefactoringServiceClientImpl implements RefactoringServiceClient {

    private final AsyncRequestFactory    asyncRequestFactory;
    private final DtoUnmarshallerFactory unmarshallerFactory;
    private final String                 pathToService;

    @Inject
    public RefactoringServiceClientImpl(AsyncRequestFactory asyncRequestFactory,
                                        DtoUnmarshallerFactory unmarshallerFactory,
                                        @Named("cheExtensionPath") String extPath,
                                        @Named("workspaceId") String workspaceId) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.unmarshallerFactory = unmarshallerFactory;

        this.pathToService = extPath + "/jdt/" + workspaceId + "/refactoring/";
    }

    /** {@inheritDoc} */
    @Override
    public Promise<String> createMoveRefactoring(final CreateMoveRefactoring moveRefactoring) {
        return newPromise(new AsyncPromiseHelper.RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {

                asyncRequestFactory.createPostRequest(pathToService + "move/create", moveRefactoring)
                                   .header(ACCEPT, TEXT_PLAIN)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, new StringUnmarshaller()));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RenameRefactoringSession> createRenameRefactoring(final CreateRenameRefactoring settings) {
        final String url = pathToService + "rename/create";
        return newPromise(new AsyncPromiseHelper.RequestCall<RenameRefactoringSession>() {
            @Override
            public void makeCall(AsyncCallback<RenameRefactoringSession> callback) {
                asyncRequestFactory.createPostRequest(url, settings)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, unmarshallerFactory.newUnmarshaller(RenameRefactoringSession.class)));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RefactoringStatus> applyLinkedModeRename(final LinkedRenameRefactoringApply refactoringApply) {
        final String url = pathToService + "rename/linked/apply";
        return newPromise(new AsyncPromiseHelper.RequestCall<RefactoringStatus>() {
            @Override
            public void makeCall(AsyncCallback<RefactoringStatus> callback) {
                asyncRequestFactory.createPostRequest(url, refactoringApply)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, unmarshallerFactory.newUnmarshaller(RefactoringStatus.class)));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RefactoringStatus> setDestination(final ReorgDestination destination) {
        final String url = pathToService + "set/destination";

        return newPromise(new AsyncPromiseHelper.RequestCall<RefactoringStatus>() {
            @Override
            public void makeCall(AsyncCallback<RefactoringStatus> callback) {

                asyncRequestFactory.createPostRequest(url, destination)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, unmarshallerFactory.newUnmarshaller(RefactoringStatus.class)));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Void> setMoveSettings(final MoveSettings settings) {
        final String url = pathToService + "set/move/setting";

        return newPromise(new AsyncPromiseHelper.RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {

                asyncRequestFactory.createPostRequest(url, settings)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ChangeCreationResult> createChange(final RefactoringSession session) {
        final String url = pathToService + "create/change";

        return newPromise(new AsyncPromiseHelper.RequestCall<ChangeCreationResult>() {
            @Override
            public void makeCall(AsyncCallback<ChangeCreationResult> callback) {

                asyncRequestFactory.createPostRequest(url, session)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, unmarshallerFactory.newUnmarshaller(ChangeCreationResult.class)));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RefactoringPreview> getRefactoringPreview(final RefactoringSession session) {
        final String url = pathToService + "get/preview";

        return newPromise(new AsyncPromiseHelper.RequestCall<RefactoringPreview>() {
            @Override
            public void makeCall(AsyncCallback<RefactoringPreview> callback) {

                asyncRequestFactory.createPostRequest(url, session)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, unmarshallerFactory.newUnmarshaller(RefactoringPreview.class)));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RefactoringStatus> applyRefactoring(final RefactoringSession session) {
        final String url = pathToService + "apply";

        return newPromise(new AsyncPromiseHelper.RequestCall<RefactoringStatus>() {
            @Override
            public void makeCall(AsyncCallback<RefactoringStatus> callback) {

                asyncRequestFactory.createPostRequest(url, session)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .header(CONTENT_TYPE, APPLICATION_JSON)
                                   .send(newCallback(callback, unmarshallerFactory.newUnmarshaller(RefactoringStatus.class)));
            }
        });
    }
}
