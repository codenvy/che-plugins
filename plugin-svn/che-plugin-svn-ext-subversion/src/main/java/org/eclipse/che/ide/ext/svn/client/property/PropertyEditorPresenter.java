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
package org.eclipse.che.ide.ext.svn.client.property;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.shared.Depth;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for the {@link org.eclipse.che.ide.ext.svn.client.property.PropertyEditorView}.
 *
 * @author Vladyslav Zhukovskyi
 * @author Stephane Tournie
 */
@Singleton
public class PropertyEditorPresenter extends SubversionActionPresenter implements PropertyEditorView.ActionDelegate {

    private PropertyEditorView                       view;
    private SubversionClientService                  service;
    private DtoUnmarshallerFactory                   dtoUnmarshallerFactory;
    private NotificationManager                      notificationManager;
    private SubversionExtensionLocalizationConstants constants;
    private Notification                             notification;

    @Inject
    protected PropertyEditorPresenter(AppContext appContext,
                                      EventBus eventBus,
                                      RawOutputPresenter console,
                                      WorkspaceAgent workspaceAgent,
                                      ProjectExplorerPresenter projectExplorerPart,
                                      PropertyEditorView view,
                                      SubversionClientService service,
                                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                      NotificationManager notificationManager,
                                      SubversionExtensionLocalizationConstants constants) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);
        this.view = view;
        this.service = service;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.constants = constants;
        view.setDelegate(this);
    }

    public void showEditor() {
        view.onShow();
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.onClose();
    }

    /** {@inheritDoc} */
    @Override
    public void onOkClicked() {
        final String projectPath = getCurrentProjectPath();

        if (projectPath == null) {
            return;
        }

        view.onClose();

        if (view.isEditPropertySelected()) {
            editProperty(projectPath);
        } else if (view.isDeletePropertySelected()) {
            deleteProperty(projectPath);
        }
    }

    @Override
    public void onPropertyNameChanged(String propertyName) {
        final String projectPath = getCurrentProjectPath();

        if (projectPath == null) {
            return;
        }

        getAndShowPropertyValue(propertyName, projectPath);
    }

    /** {@inheritDoc} */
    @Override
    public void minimize() {
        // stub
    }

    /** {@inheritDoc} */
    @Override
    public void activatePart() {
        // stub
    }

    private void getAndShowPropertyValue(String property, String projectPath) {
        String headPath = getSelectedPaths().get(0);
        Unmarshallable<CLIOutputResponse> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class);
        service.propertyGet(projectPath, property, headPath, new AsyncRequestCallback<CLIOutputResponse>(unmarshaller) {
            @Override
            protected void onSuccess(CLIOutputResponse result) {
                List<String> values = result.getOutput();
                view.setPropertyCurrentValue(values);
            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.showError(exception.getMessage());
            }
        });
    }

    private void editProperty(String projectPath) {
        final String propertyName = view.getSelectedProperty();
        final Depth depth = view.getDepth();
        final String propertyValue = view.getPropertyValue();
        final boolean force = view.isForceSelected();

        String headPath = getSelectedPaths().get(0);

        notification = new Notification(constants.propertyModifyStart(), PROGRESS);
        notificationManager.showNotification(notification);

        Unmarshallable<CLIOutputResponse> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class);
        service.propertySet(projectPath, propertyName, propertyValue, depth, force, headPath,
                            new AsyncRequestCallback<CLIOutputResponse>(unmarshaller) {
                                @Override
                                protected void onSuccess(CLIOutputResponse result) {
                                    printResponse(result.getCommand(), result.getOutput(), result.getErrOutput());

                                    notification.setMessage(constants.propertyModifyFinished());
                                    notification.setStatus(FINISHED);
                                    notification.setType(INFO);
                                }

                                @Override
                                protected void onFailure(Throwable exception) {
                                    String errorMessage = exception.getMessage();

                                    notification.setMessage(constants.propertyModifyFailed() + errorMessage);
                                    notification.setStatus(FINISHED);
                                    notification.setType(ERROR);
                                }
                            });
    }

    private void deleteProperty(String projectPath) {
        final String propertyName = view.getSelectedProperty();
        final Depth depth = view.getDepth();
        final boolean force = view.isForceSelected();

        String headPath = getSelectedPaths().get(0);

        notification = new Notification(constants.propertyRemoveStart(), PROGRESS);
        notificationManager.showNotification(notification);

        Unmarshallable<CLIOutputResponse> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class);
        service.propertyDelete(projectPath, propertyName, depth, force, headPath,
                               new AsyncRequestCallback<CLIOutputResponse>(unmarshaller) {
                                   @Override
                                   protected void onSuccess(CLIOutputResponse result) {
                                       printResponse(result.getCommand(), result.getOutput(), result.getErrOutput());

                                       notification.setMessage(constants.propertyRemoveFinished());
                                       notification.setStatus(FINISHED);
                                       notification.setType(INFO);
                                   }

                                   @Override
                                   protected void onFailure(Throwable exception) {
                                       String errorMessage = exception.getMessage();

                                       notification.setMessage(constants.propertyRemoveFailed() + errorMessage);
                                       notification.setStatus(FINISHED);
                                       notification.setType(ERROR);
                                   }
                               });
    }

    @Override
    public void obtainExistingPropertiesForPath() {
        final String projectPath = getCurrentProjectPath();

        if (projectPath == null) {
            return;
        }

        String headPath = getSelectedPaths().get(0);

        Unmarshallable<CLIOutputResponse> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class);
        service.propertyList(projectPath, headPath, new AsyncRequestCallback<CLIOutputResponse>(unmarshaller) {
            @Override
            protected void onSuccess(CLIOutputResponse result) {
                List<String> properties = new ArrayList<String>();
                for (String property : result.getOutput()) {
                    properties.add(property.trim());
                }
                view.setExistingPropertiesForPath(properties);
            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.showError(exception.getMessage());
            }
        });
    }
}
