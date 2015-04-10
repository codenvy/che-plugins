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
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;

import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;

/**
 * Presenter for the {@link org.eclipse.che.ide.ext.svn.client.export.ExportView}.
 *
 * @author Vladyslav Zhukovskyi
 */
@Singleton
public class ExportPresenter extends SubversionActionPresenter implements ExportView.ActionDelegate {

    private ExportView              view;
    private NotificationManager     notificationManager;
    private SubversionExtensionLocalizationConstants constants;
    private final String              baseHttpUrl;

    private TreeNode<?> selectedNode;

    @Inject
    public ExportPresenter(AppContext appContext,
                           EventBus eventBus,
                           RawOutputPresenter console,
                           WorkspaceAgent workspaceAgent,
                           ProjectExplorerPart projectExplorerPart,
                           ExportView view,
                           NotificationManager notificationManager,
                           SubversionExtensionLocalizationConstants constants,
                           @Named("restContext") String restContext,
                           @Named("workspaceId") String workspaceId) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);
        this.view = view;
        this.notificationManager = notificationManager;
        this.constants = constants;
        this.view.setDelegate(this);

        this.baseHttpUrl = restContext + "/svn/" + workspaceId;
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

        final Notification notification = new Notification(constants.exportStarted(exportPath), PROGRESS);
        notificationManager.showNotification(notification);

        view.onClose();

        char prefix = '?';
        StringBuilder url = new StringBuilder(baseHttpUrl + "/export" + projectPath);

        if (!Strings.isNullOrEmpty(exportPath)) {
            url.append(prefix).append("path").append('=').append(exportPath);
            prefix = '&';
        }

        if (!Strings.isNullOrEmpty(revision)) {
            url.append(prefix).append("revision").append('=').append(revision);
        }

        Window.open(url.toString(), "_self", "");
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
