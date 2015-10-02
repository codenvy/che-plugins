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

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.VirtualFile;

/**
 * The file node that represents recipe file item in the project explorer tree. It needs just for opening recipe for editing (it is a
 * problem of API of editor agent).
 *
 * @author Valeriy Svydenko
 */
public class RecipeFile implements VirtualFile {

    private final String content;
    private final ItemReference recipe;

    public RecipeFile(String content, ItemReference recipe) {
        this.content = content;
        this.recipe = recipe;
    }

    @Override
    public String getPath() {
        return recipe.getPath();
    }

    @Override
    public String getName() {
        return recipe.getName();
    }

    @Override
    public String getDisplayName() {
        return recipe.getName();
    }

    @Override
    public String getMediaType() {
        return recipe.getMediaType();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public HasProjectDescriptor getProject() {
        return null;
    }

    @Override
    public String getContentUrl() {
        return null;
    }

    @Override
    public Promise<String> getContent() {
        return Promises.resolve(content);
    }

    @Override
    public Promise<Void> updateContent(String content) {
        return Promises.resolve(null);
    }
}