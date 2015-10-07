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
package org.eclipse.che.ide.ext.git.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.parts.ConsolePart;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.workspace.WorkBenchPartControllerImpl;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Git output View Part.
 *
 * @author Vitaly Parfonov
 */
@Singleton
public class GitOutputPartPresenter extends BasePresenter implements GitOutputPartView.ActionDelegate, ConsolePart {
    private static final String TITLE = "Git";
    private       GitOutputPartView view;
    private final WorkspaceAgent    workspaceAgent;
    private boolean isTabPresent = false;

    /** Construct empty Part */
    @Inject
    public GitOutputPartPresenter(GitOutputPartView view,
                                  final EventBus eventBus,
                                  final WorkspaceAgent workspaceAgent) {
        this.view = view;
        this.workspaceAgent = workspaceAgent;
        this.view.setTitle(TITLE);
        this.view.setDelegate(this);

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {

            @Override
            public void onProjectDeleted(ProjectActionEvent event) {
                clear();
                workspaceAgent.hidePart(GitOutputPartPresenter.this);
                workspaceAgent.removePart(GitOutputPartPresenter.this);
                isTabPresent = false;
            }

            @Override
            public void onProjectCreated(ProjectActionEvent event) {

            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return "Displays Git output";
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    private void performPostOutputActions() {
        if (isTabPresent) {
            PartPresenter activePart = partStack.getActivePart();
            if (activePart == null || !activePart.equals(this)) {
                partStack.setActivePart(this);
            }
        } else {
            workspaceAgent.openPart(GitOutputPartPresenter.this, PartStackType.INFORMATION);
            partStack.setActivePart(this);
            isTabPresent = true;
        }
        view.scrollBottom();
    }

    /**
     * Print text on console.
     *
     * @param text
     *         text that need to be shown
     */
    public void print(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            view.print(line.isEmpty() ? " " : line);
        }
        performPostOutputActions();
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        view.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void onClearClicked() {
        clear();
    }

    /** {@inheritDoc} */
    @Override
    public void onOpen() {
        super.onOpen();
        Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
            @Override
            public boolean execute() {
                view.scrollBottom();
                return false;
            }
        }, WorkBenchPartControllerImpl.DURATION);
    }

    @Override
    public void displayException(Exception e) {
    }

    @Override
    public void printInfo(String text) {
        view.printInfo(text);
        performPostOutputActions();
    }

    @Override
    public void printWarn(String text) {
        view.printWarn(text);
        performPostOutputActions();
    }

    @Override
    public void printError(String text) {
        view.printError(text);
        performPostOutputActions();
    }

}