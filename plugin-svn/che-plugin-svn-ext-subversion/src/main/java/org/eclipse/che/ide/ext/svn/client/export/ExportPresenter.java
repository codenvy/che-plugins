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
package org.eclipse.che.ide.ext.svn.client.export;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.Constants;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for the {@link org.eclipse.che.ide.ext.svn.client.export.ExportView}.
 *
 * @author Vladyslav Zhukovskyi
 */
@Singleton
public class ExportPresenter extends SubversionActionPresenter implements ExportView.ActionDelegate {

    private ExportView              view;
    private SubversionClientService service;
    private NotificationManager     notificationManager;
    private DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private DialogFactory           dialogFactory;
    private SubversionExtensionLocalizationConstants constants;

    private TreeNode<?> selectedNode;

    @Inject
    public ExportPresenter(AppContext appContext,
                           EventBus eventBus,
                           RawOutputPresenter console,
                           WorkspaceAgent workspaceAgent,
                           ProjectExplorerPart projectExplorerPart,
                           ExportView view,
                           SubversionClientService service,
                           NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           DialogFactory dialogFactory,
                           SubversionExtensionLocalizationConstants constants) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);
        this.view = view;
        this.service = service;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
        this.constants = constants;
        this.view.setDelegate(this);
    }

    public void showExport(TreeNode<?> selectedNode) {
        this.selectedNode = selectedNode;

        view.onShow();
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.onClose();
    }

    private class DownloadDialogHandler implements Notification.OpenNotificationHandler {
        private       Link   downloadLink;
        private final String exportedPath;

        public DownloadDialogHandler(@Nonnull String exportedPath) {
            this.exportedPath = exportedPath;
        }

        public void setDownloadLink(@Nullable Link downloadLink) {
            this.downloadLink = downloadLink;
        }

        @Override
        public void onOpenClicked() {
            if (downloadLink == null) {
                return;
            }

            dialogFactory.createChoiceDialog(constants.downloadTitle(),
                                             constants.downloadFinished(exportedPath),
                                             constants.downloadButton(),
                                             constants.downloadButtonCanceled(),
                                             new ConfirmCallback() {
                                                 @Override
                                                 public void accepted() {
                                                     Window.open(downloadLink.getHref(), "_self", "");
                                                 }
                                             },
                                             null).show();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onExportClicked() {
        final String projectPath = getCurrentProjectPath();

        if (projectPath == null) {
            return;
        }

        final String exportPath =
                MoreObjects.firstNonNull(Strings.emptyToNull(relPath(projectPath, ((StorableNode)selectedNode).getPath())), projectPath);
        final String revision = view.isRevisionSpecified() ? view.getRevision() : null;
        final DownloadDialogHandler openHandler = new DownloadDialogHandler(exportPath);

        final Notification notification = new Notification(constants.exportStarted(exportPath), PROGRESS, openHandler);
        notificationManager.showNotification(notification);

        view.onClose();

        Unmarshallable<Hyperlinks> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(Hyperlinks.class);

        service.export(projectPath, projectPath.equals(exportPath) ? null : exportPath, revision, new AsyncRequestCallback<Hyperlinks>(unmarshaller) {
            @Override
            protected void onSuccess(final Hyperlinks links) {
                notification.setMessage(constants.exportSuccessful(exportPath));
                notification.setStatus(FINISHED);
                notification.setType(INFO);

                final Link downloadLink = links.getLink(Constants.REL_DOWNLOAD_EXPORT_PATH);
                if (downloadLink == null) {
                    return;
                }

                openHandler.setDownloadLink(downloadLink);
                MoreObjects.firstNonNull(notification.getOpenHandler(), openHandler).onOpenClicked();
            }

            @Override
            protected void onFailure(Throwable exception) {
                String errorMessage = exception.getMessage();

                notification.setMessage(errorMessage);
                notification.setStatus(FINISHED);
                notification.setType(ERROR);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void minimize() {

    }

    /** {@inheritDoc} */
    @Override
    public void activatePart() {

    }

    private String relPath(String base, String path) {
        if (!path.startsWith(base)) {
            return null;
        }

        final String temp = path.substring(base.length());

        return temp.startsWith("/") ? temp.substring(1) : temp;
    }
}
