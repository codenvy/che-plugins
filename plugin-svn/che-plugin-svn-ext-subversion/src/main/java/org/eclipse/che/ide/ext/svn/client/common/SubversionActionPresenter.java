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
package org.eclipse.che.ide.ext.svn.client.common;

import org.eclipse.che.ide.api.project.tree.TreeNode;
import static org.eclipse.che.ide.ext.svn.client.common.PathTypeFilter.ALL;
import static org.eclipse.che.ide.ext.svn.client.common.PathTypeFilter.PROJECT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.ext.svn.client.action.SubversionAction;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;

import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;

/**
 * Presenter to be extended by all {@link SubversionAction} presenters.
 */
public class SubversionActionPresenter {

    static final String[][] STATUS_COLORS = {
            {"M", "rgb(247, 47, 47)"},
            {"!", "grey"},
            {"?", "lightskyblue"},
            {"A", "chartreuse"},
            {"X", "yellow"},
            {"C", "yellow"},
            {"D", "rgb(247, 47, 47)"},
            {"+", "chartreuse"},
            {"-", "rgb(247, 47, 47)"},
            {"@", "cyan"},
            {"U", "chartreuse"},
            {"G", "chartreuse"}
    };

    protected final AppContext       appContext;
    protected final EventBus         eventBus;
    protected final WorkspaceAgent   workspaceAgent;
    private final RawOutputPresenter console;
    private boolean                  isViewClosed;

    private final ProjectExplorerPart projectExplorerPart;

    /**
     * Constructor.
     */
    protected SubversionActionPresenter(final AppContext appContext,
                                        final EventBus eventBus,
                                        final RawOutputPresenter console,
                                        final WorkspaceAgent workspaceAgent,
                                        final ProjectExplorerPart projectExplorerPart) {
        this.appContext = appContext;
        this.workspaceAgent = workspaceAgent;
        this.console = console;

        isViewClosed = true;

        this.eventBus = eventBus;
        this.projectExplorerPart = projectExplorerPart;

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectCreated(final ProjectActionEvent event) {
            }

            @Override
            public void onProjectDeleted(ProjectActionEvent event) {
                isViewClosed = true;
                console.clear();
                workspaceAgent.hidePart(console);
            }
        });
    }

    /**
     * @return the current project path
     */
    protected String getCurrentProjectPath() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        final ProjectDescriptor project;
        String projectPath = null;

        if (currentProject != null) {
            project = currentProject.getProjectDescription();

            if (project != null) {
                projectPath = project.getPath();
            }
        }

        return projectPath;
    }

    /**
     * Returns currently selected project item.
     * @return
     */
    protected TreeNode<?> getSelectedNode() {
        Object selectedNode = projectExplorerPart.getSelection().getHeadElement();
        return selectedNode != null && selectedNode instanceof StorableNode ? (StorableNode)selectedNode : null;
    }

    /**
     * @return the selected paths or an empty list of there is no selection
     */
    @NotNull
    protected List<String> getSelectedPaths(final Collection<PathTypeFilter> filters) {
        final Selection<?> selection = projectExplorerPart.getSelection();
        final List<String> paths = new ArrayList<>();

        if (selection != null && !selection.isEmpty()) {
            for (final Object item : selection.getAllElements()) {
                if (matchesFilter(item, filters)) {
                    final String path = relativePath((StorableNode)item);
                    if (!path.isEmpty()) {
                        paths.add(path);
                    } else {
                        paths.add("."); //it may be root path for our project
                    }
                }
            }
        }

        return paths;
    }

    /**
     * Returns relative node path in the project.
     *
     * @param node node
     * @return relative node path
     */
    protected String relativePath(final StorableNode node) {
        String path = node.getPath().replaceFirst(node.getProject().getPath(), ""); // TODO: Move to method

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    protected List<String> getSelectedPaths() {
        return getSelectedPaths(Collections.singleton(ALL));
    }

    protected boolean matchesFilter(final Object node, final Collection<PathTypeFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (final PathTypeFilter filter : filters) {
            if (filter == ALL && node instanceof StorableNode
                || filter == PathTypeFilter.FILE && node instanceof FileNode
                || filter == PathTypeFilter.FOLDER && node instanceof FolderNode
                || filter == PathTypeFilter.PROJECT && (node instanceof ProjectNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensures view is opened.
     */
    protected void ensureViewOpened() {
        if (isViewClosed) {
            workspaceAgent.openPart(console, PartStackType.INFORMATION);
            isViewClosed = false;
        }
    }

    /**
     * Print the update output.
     *
     * @param lines text to be printed
     */
    protected void print(final List<String> lines) {
        ensureViewOpened();

        for (final String line : lines) {
            console.print(line);
        }
    }

    /**
     * Prints command line.
     *
     * @param command command line
     */
    protected void printCommand(String command) {
        ensureViewOpened();

        if (command.startsWith("'") || command.startsWith("\"")) {
            command += command.substring(1, command.length() - 1);
        }

        String line = "<span style=\"font-weight: bold; font-style: italic;\">$ " + command + "</span>";

        console.print(line);
    }

    protected void printErrors(final List<String> errors) {
        ensureViewOpened();

        for (final String line : errors) {
            console.print("<span style=\"color:red;\">" + SafeHtmlUtils.htmlEscape(line) + "</span>");
        }
    }

    /**
     * Colorizes and prints response to the output.
     *
     * @param command
     * @param output
     * @param errors
     */
    protected void printResponse(final String command, final List<String> output, final List<String> errors) {
        ensureViewOpened();

        if (command != null) {
            printCommand(command);
        }

        if (output != null) {
            for (final String line : output) {
                boolean found = false;

                if (!line.trim().isEmpty()) {
                    String prefix = line.trim().substring(0, 1);

                    for (String[] stcol : STATUS_COLORS) {
                        if (stcol[0].equals(prefix)) {
                            // TODO: Turn the file paths into links (where appropriate)
                            console.print("<span style=\"color:" + stcol[1] + ";\">" + SafeHtmlUtils.htmlEscape(line) + "</span>");
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    console.print(SafeHtmlUtils.htmlEscape(line));
                }
            }
        }

        if (errors != null) {
            for (final String line : errors) {
                console.print("<span style=\"color:red;\">" + SafeHtmlUtils.htmlEscape(line) + "</span>");
            }
        }

        console.print("");
    }

    /**
     * Print the update output & a blank line after.
     *
     * @param output text to be printed
     */
    protected void printAndSpace(final List<String> output) {
        print(output);
        console.print("");
    }

    /**
     * Notify Project explorer to update.
     */
    protected void updateProjectExplorer() {
        final Selection<?> selection = projectExplorerPart.getSelection();

        final Collection<PathTypeFilter> filters = Collections.singleton(ALL);

        if (selection != null && !selection.isEmpty()) {
            for (final Object item : selection.getAll()) {
                if (matchesFilter(item, filters)) {
                    final StorableNode node = (StorableNode)item;
                    if (node instanceof FileNode) {
                        eventBus.fireEvent(new RefreshProjectTreeEvent(node.getParent(), true));
                    } else {
                        eventBus.fireEvent(new RefreshProjectTreeEvent(node, true));
                    }
                }
            }
        }

    }

}
