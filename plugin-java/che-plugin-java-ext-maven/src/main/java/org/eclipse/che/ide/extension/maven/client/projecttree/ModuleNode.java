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
package org.eclipse.che.ide.extension.maven.client.projecttree;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.event.UpdateTreeNodeChildrenEvent;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.project.tree.generic.UpdateTreeNodeDataIterable;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.extension.maven.client.event.BeforeModuleOpenEvent;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

/**
 * Node that represents module of multi-module project.
 *
 * @author Artem Zatsarynnyy
 */
public class ModuleNode extends MavenProjectNode implements UpdateTreeNodeDataIterable {

    private final AppContext appContext;

    @Inject
    public ModuleNode(@Assisted TreeNode<?> parent,
                      @Assisted ProjectDescriptor data,
                      @Assisted MavenProjectTreeStructure treeStructure,
                      EventBus eventBus,
                      ProjectServiceClient projectServiceClient,
                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                      IconRegistry iconRegistry,
                      AppContext appContext) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
        this.appContext = appContext;
        setDisplayIcon(iconRegistry.getIcon("maven.module").getSVGImage());
    }

    @Override
    protected void getChildren(String path, AsyncCallback<Array<ItemReference>> callback) {
        if (!isOpened()) {
            eventBus.fireEvent(new BeforeModuleOpenEvent(this));
        }
        super.getChildren(path, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void rename(final String newName, final RenameCallback callback) {
        projectServiceClient.rename(getPath(), newName, null, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                final String parentPath = ((StorableNode)getParent()).getPath();
                Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);

                projectServiceClient.getProject(parentPath + "/" + newName, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
                    @Override
                    protected void onSuccess(ProjectDescriptor projectDescriptor) {
                        setData(projectDescriptor);

                        AsyncCallback<Void> asyncCallback = new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                ModuleNode.super.rename(newName, new RenameCallback() {
                                    @Override
                                    public void onRenamed() {
                                        callback.onRenamed();

                                        eventBus.fireEvent(new RefreshProjectTreeEvent(ModuleNode.this.getParent()));
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        callback.onFailure(caught);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                Log.error(getClass(), "Error update children " + throwable);
                            }
                        };

                        eventBus.fireEvent(new UpdateTreeNodeChildrenEvent(ModuleNode.this, asyncCallback));
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    /** {@inheritDoc} */
    public void updateData(final AsyncCallback<Void> asyncCallback, String updatedParentNodePath) {
        String path = ((StorableNode)getParent()).getPath() + "/" + getName();
        Log.error(getClass(), path);

        Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.getProject(path, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectDescriptor result) {
                setData(result);
                asyncCallback.onSuccess(null);
            }

            @Override
            protected void onFailure(Throwable exception) {
                asyncCallback.onFailure(exception);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final DeleteCallback callback) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No opened project.");
        }

        final String rootProjectPath = currentProject.getRootProject().getPath();
        final String moduleRelativePath = getPath().substring(rootProjectPath.length() + 1);
        projectServiceClient.deleteModule(rootProjectPath, moduleRelativePath, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                ModuleNode.super.delete(callback);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }
}
