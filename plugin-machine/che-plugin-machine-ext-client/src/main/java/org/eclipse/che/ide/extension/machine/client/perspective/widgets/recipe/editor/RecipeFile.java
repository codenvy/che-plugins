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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.editor;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;

/**
 * The file node that represents recipe file item in the project explorer tree. It needs just for opening recipe for editing (it is a
 * problem of API of editor agent).
 *
 * @author Valeriy Svydenko
 */
public class RecipeFile extends FileNode {

    private final String content;

    public RecipeFile(@Nonnull String content,
                      @Nonnull EventBus eventBus,
                      @Nonnull ProjectServiceClient projectServiceClient,
                      @Nonnull DtoUnmarshallerFactory dtoUnmarshallerFactory,
                      @Nonnull ItemReference data,
                      @Nonnull TreeStructure treeStructure,
                      @Nonnull EditorAgent editorAgent) {
        super(null, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory, editorAgent);

        this.content = content;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void getContent(AsyncCallback<String> callback) {
        callback.onSuccess(content);
    }

}