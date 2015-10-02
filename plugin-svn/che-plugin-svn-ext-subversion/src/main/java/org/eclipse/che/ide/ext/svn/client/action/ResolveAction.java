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
package org.eclipse.che.ide.ext.svn.client.action;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.event.SelectionChangedEvent;
import org.eclipse.che.ide.api.event.SelectionChangedHandler;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;
import org.eclipse.che.ide.ext.svn.client.resolve.ResolvePresenter;
import org.eclipse.che.ide.ext.svn.client.update.SubversionProjectUpdatedEvent;
import org.eclipse.che.ide.ext.svn.client.update.SubversionProjectUpdatedHandler;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

import org.eclipse.che.commons.annotation.Nullable;
import java.util.List;

/**
 * Extension of {@link SubversionAction} for implementing the "svn resolved" command.
 *
 * @author vzhukovskii@codenvy.com
 */
@Singleton
public class ResolveAction extends SubversionAction implements SelectionChangedHandler,
                                                               ProjectActionHandler,
                                                               SubversionProjectUpdatedHandler {

    private       ProjectExplorerPresenter projectExplorerPresenter;
    private final ResolvePresenter         presenter;

    private List<String> conflictsList;
    private boolean enable = false;

    @Inject
    public ResolveAction(final AnalyticsEventLogger eventLogger,
                         final AppContext appContext,
                         final SubversionExtensionLocalizationConstants constants,
                         final SubversionExtensionResources resources,
                         final ProjectExplorerPresenter projectExplorerPresenter,
                         final ResolvePresenter presenter,
                         final EventBus eventBus) {
        super(constants.resolvedTitle(), constants.resolvedDescription(), resources.resolved(), eventLogger,
              appContext, constants, resources, projectExplorerPresenter);
        this.projectExplorerPresenter = projectExplorerPresenter;
        this.presenter = presenter;

        eventBus.addHandler(SelectionChangedEvent.TYPE, this);
        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        eventBus.addHandler(SubversionProjectUpdatedEvent.TYPE, this);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        presenter.showConflictsDialog(conflictsList);
    }

    /** {@inheritDoc} */
    @Override
    public void updateProjectAction(final ActionEvent e) {
        super.updateProjectAction(e);

        e.getPresentation().setEnabled(enable);
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectionChanged(SelectionChangedEvent event) {
        enable = false;

        HasStorablePath selectedNode = getStorableNodeFromSelection(event.getSelection());

        if (selectedNode == null || conflictsList == null) {
            return;
        }

        for (String conflictPath : conflictsList) {
            final String absPath = (appContext.getCurrentProject().getProjectDescription().getPath() + "/" + conflictPath.trim());

            if (absPath.startsWith(selectedNode.getStorablePath())) {
                enable = true;
                break;
            }
        }

    }

    @Nullable
    private HasStorablePath getStorableNodeFromSelection(Selection<?> selection) {
        if (selection == null) {
            return null;
        }

        return projectExplorerPresenter.getSelection().getHeadElement() instanceof HasStorablePath ? (HasStorablePath)selection.getHeadElement() : null;
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectReady(ProjectActionEvent event) {
        fetchConflicts();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectClosing(ProjectActionEvent event) {
        //stub
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectClosed(ProjectActionEvent event) {
        conflictsList = null;
    }

    @Override
    public void onProjectOpened(ProjectActionEvent event) {

    }

    @Override
    public void onProjectUpdated(SubversionProjectUpdatedEvent event) {
        fetchConflicts();
    }

    private void fetchConflicts() {
        presenter.fetchConflictsList(false, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable caught) {
                enable = false;
            }

            @Override
            public void onSuccess(List<String> result) {
                conflictsList = result;
            }
        });
    }
}
