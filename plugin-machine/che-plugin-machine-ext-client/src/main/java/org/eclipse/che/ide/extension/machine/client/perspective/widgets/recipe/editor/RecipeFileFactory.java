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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;

/**
 * The factory that provides an ability to create instances of {@link RecipeFile}. The main idea of this class is to simplify work flow of
 * using  {@link RecipeFile}.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class RecipeFileFactory {

    public static final String NAME = "Machine Recipe";
    public static final String PATH = "machine_recipe";
    public static final String TYPE = "text/x-dockerfile";

    private final EventBus               eventBus;
    private final ProjectServiceClient   projectServiceClient;
    private final AppContext             appContext;
    private final DtoFactory             dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final Provider<EditorAgent>  editorAgentProvider;

    @Inject
    public RecipeFileFactory(EventBus eventBus,
                             ProjectServiceClient projectServiceClient,
                             AppContext appContext,
                             DtoFactory dtoFactory,
                             Provider<EditorAgent> editorAgentProvider,
                             DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.editorAgentProvider = editorAgentProvider;
        this.projectServiceClient = projectServiceClient;
        this.dtoFactory = dtoFactory;
    }

    /**
     * Create a new instance of {@link RecipeFile} for a given href.
     *
     * @param content
     *         script of the recipe
     * @return an instance of {@link RecipeFile}
     * @throws IllegalStateException
     *         when no project is opened
     */
    @Nonnull
    public RecipeFile newInstance(@Nonnull String content) {
        return newInstance(content, NAME, PATH);
    }

    @Nonnull
    private RecipeFile newInstance(@Nonnull String content, @Nonnull String name, @Nonnull String path) {
        CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No project is opened");
        }


        ItemReference recipeFileItem = dtoFactory.createDto(ItemReference.class)
                                                 .withName(name)
                                                 .withPath(path)
                                                 .withMediaType(TYPE);

        return new RecipeFile(content,
                              eventBus,
                              projectServiceClient,
                              dtoUnmarshallerFactory,
                              recipeFileItem,
                              currentProject.getCurrentTree(),
                              editorAgentProvider.get());
    }

}