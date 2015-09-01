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
package org.eclipse.che.ide.ext.git.client.remove;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for removing files from index and file system.
 *
 * @author Ann Zhuleva
 */
public class RemoveFromIndexPresenter implements RemoveFromIndexView.ActionDelegate {
    private       RemoveFromIndexView         view;
    private       EventBus                    eventBus;
    private final NewProjectExplorerPresenter projectExplorer;
    private       GitServiceClient            service;
    private       GitLocalizationConstant     constant;
    private       AppContext                  appContext;
    private       CurrentProject              project;
    private       SelectionAgent              selectionAgent;
    private       NotificationManager         notificationManager;
    private       List<EditorPartPresenter>   openedEditors;
    private       EditorAgent                 editorAgent;

    /**
     * Create presenter
     *
     * @param view
     * @param service
     * @param constant
     * @param appContext
     * @param notificationManager
     */
    @Inject
    public RemoveFromIndexPresenter(RemoveFromIndexView view,
                                    EventBus eventBus,
                                    GitServiceClient service,
                                    GitLocalizationConstant constant,
                                    AppContext appContext,
                                    SelectionAgent selectionAgent,
                                    NotificationManager notificationManager,
                                    EditorAgent editorAgent,
                                    NewProjectExplorerPresenter projectExplorer) {
        this.view = view;
        this.eventBus = eventBus;
        this.projectExplorer = projectExplorer;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.appContext = appContext;
        this.selectionAgent = selectionAgent;
        this.notificationManager = notificationManager;
        this.editorAgent = editorAgent;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        String workDir = project.getRootProject().getPath();
        view.setMessage(formMessage(workDir));
        view.setRemoved(false);
        view.showDialog();
    }

    /**
     * Form the message to display for removing from index, telling the user what is gonna to be removed.
     *
     * @return {@link String} message to display
     */
    @Nonnull
    private String formMessage(@Nonnull String workDir) {
        Selection<ResourceBasedNode<?>> selection = (Selection<ResourceBasedNode<?>>)selectionAgent.getSelection();

        String path;
        if (selection == null || selection.getHeadElement() == null) {
            path = project.getRootProject().getPath();
        } else {
            path = ((HasStorablePath)selection.getHeadElement()).getStorablePath();
        }

        String pattern = path.replaceFirst(workDir, "");
        pattern = (pattern.startsWith("/")) ? pattern.replaceFirst("/", "") : pattern;

        // Root of the working tree:
        if (pattern.isEmpty() || "/".equals(pattern)) {
            return constant.removeFromIndexAll();
        }

        // Do not display path longer 40 characters
        if (pattern.length() > 40) {
            pattern = pattern.substring(0, 40) + "...";
        }

        if (selection != null && selection.getHeadElement() instanceof FolderReferenceNode) {
            return constant.removeFromIndexFolder(pattern).asString();
        } else {
            return constant.removeFromIndexFile(pattern).asString();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoveClicked() {
        openedEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().values()) {
            openedEditors.add(partPresenter);
        }

        service.remove(project.getRootProject(), getFilePatterns(), view.isRemoved(),
                       new AsyncRequestCallback<String>() {
                           @Override
                           protected void onSuccess(String result) {
                               Notification notification = new Notification(constant.removeFilesSuccessfull(), INFO);
                               notificationManager.showNotification(notification);

                               if (!view.isRemoved()) {
                                   projectExplorer.reloadChildren(getResourceBasedNode());

                                   if (projectExplorer.getSelection().getHeadElement() instanceof FileReferenceNode) {
                                       FileReferenceNode selectFile = ((FileReferenceNode)projectExplorer.getSelection().getHeadElement());
                                       for (EditorPartPresenter partPresenter : openedEditors) {
                                           VirtualFile openFile = partPresenter.getEditorInput().getFile();
                                           //to close selected file if it open
                                           if (selectFile.getPath().equals(openFile.getPath())) {
                                               eventBus.fireEvent(new FileEvent(openFile, FileEvent.FileOperation.CLOSE));
                                           }
                                       }
                                   }
                               }
                           }

                           @Override
                           protected void onFailure(Throwable exception) {
                               handleError(exception);
                           }
                       }
                      );
        view.close();
    }

    @Nullable
    protected ResourceBasedNode<?> getResourceBasedNode() {
        List<?> selection = projectExplorer.getSelection().getAllElements();
        //we should be sure that user selected single element to work with it
        if (selection != null && selection.isEmpty() || selection.size() > 1) {
            return null;
        }

        Object o = selection.get(0);

        if (o instanceof ResourceBasedNode<?>) {
            ResourceBasedNode<?> node = (ResourceBasedNode<?>)o;
            //it may be file node, so we should take parent node
            if (node.isLeaf() && isResourceAndStorableNode(node.getParent())) {
                return (ResourceBasedNode<?>)node.getParent();
            }

            return isResourceAndStorableNode(node) ? node : null;
        }

        return null;
    }

    protected boolean isResourceAndStorableNode(@Nullable Node node) {
        return node != null && node instanceof ResourceBasedNode<?> && node instanceof HasStorablePath;
    }

    /**
     * Returns pattern of the items to be removed.
     *
     * @return pattern of the items to be removed
     */
    @Nonnull
    private List<String> getFilePatterns() {
        Selection<ResourceBasedNode<?>> selection = (Selection<ResourceBasedNode<?>>)selectionAgent.getSelection();
        String path;
        if (selection == null || selection.getHeadElement() == null) {
            path = project.getRootProject().getPath();
        } else {
            path = ((HasStorablePath)selection.getHeadElement()).getStorablePath();
        }

        String pattern = path.replaceFirst(project.getRootProject().getPath(), "");
        pattern = (pattern.startsWith("/")) ? pattern.replaceFirst("/", "") : pattern;

        return (pattern.length() == 0 || "/".equals(pattern)) ? new ArrayList<>(Arrays.asList("."))
                                                              : new ArrayList<>(Arrays.asList(pattern));
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@Nonnull Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.removeFilesFailed();
        Notification notification = new Notification(errorMessage, ERROR);
        notificationManager.showNotification(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

}
