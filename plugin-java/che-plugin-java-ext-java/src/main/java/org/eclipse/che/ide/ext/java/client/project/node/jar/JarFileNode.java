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
package org.eclipse.che.ide.ext.java.client.project.node.jar;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;
import org.eclipse.che.ide.util.Pair;
import org.vectomatic.dom.svg.ui.SVGImage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class JarFileNode extends AbstractJarEntryNode implements VirtualFile, HasAction {

    private final IconRegistry iconRegistry;

    @Inject
    public JarFileNode(@Assisted JarEntry jarEntry,
                       @Assisted int libId,
                       @Assisted ProjectDescriptor projectDescriptor,
                       @Assisted NodeSettings nodeSettings,
                       @Nonnull JavaNodeManager nodeManager,
                       @Nonnull IconRegistry iconRegistry) {
        super(jarEntry, libId, projectDescriptor, nodeSettings, nodeManager);
        this.iconRegistry = iconRegistry;
    }

    @Nonnull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return Promises.resolve(Collections.<Node>emptyList());
    }

    @Override
    public void actionPerformed() {
        nodeManager.getEventBus().fireEvent(new FileEvent(this, FileEvent.FileOperation.OPEN));
    }

    @Override
    public void updatePresentation(@Nonnull NodePresentation presentation) {
        presentation.setPresentableText(getDisplayName());
        presentation.setPresentableIcon(isClassFile() ? nodeManager.getJavaNodesResources().javaClassIcon()
                                                      : nodeManager.getNodesResources().file());
        presentation.setInfoText("r/o");
        presentation.setInfoTextWrapper(Pair.of("(", ")"));
    }

    @Nonnull
    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Nonnull
    @Override
    public String getPath() {
        return getData().getPath();
    }

    @Override
    public String getDisplayName() {
        if (isClassFile()) {
            return getData().getName().substring(0, getData().getName().lastIndexOf(".class"));
        } else {
            return getData().getName();
        }
    }

    @Nullable
    @Override
    public String getMediaType() {
        return isClassFile() ? "application/java-class" : null;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Nullable
    @Override
    public HasProjectDescriptor getProject() {
        return this;
    }

    @Override
    public String getContentUrl() {
        return null;
    }

    @Override
    public Promise<String> getContent() {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<String>() {
            @Override
            public void makeCall(final AsyncCallback<String> callback) {
                nodeManager.getJavaService().getContent(getProjectDescriptor().getPath(), libId, getData().getPath(),
                                                        new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                                                            @Override
                                                            protected void onSuccess(String result) {
                                                                callback.onSuccess(result);
                                                            }

                                                            @Override
                                                            protected void onFailure(Throwable exception) {
                                                                callback.onFailure(exception);
                                                            }
                                                        });
            }
        });
    }

    @Override
    public Promise<Void> updateContent(String content) {
        return Promises.reject(JsPromiseError.create("Update content on class file is not supported."));
    }

    private boolean isClassFile() {
        return getData().getName().endsWith(".class");
    }

    private SVGImage getFileIcon() {
        String[] split = getData().getName().split("\\.");
        String ext = split[split.length - 1];
        return iconRegistry.getIcon(getProjectDescriptor().getType() + "/" + ext + ".file.small.icon").getSVGImage();
    }
}
