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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * The factory that provides an ability to create instances of {@link DockerFile}. The main idea of this class is to simplify work flow of
 * using  {@link DockerFile}.
 *
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
@Singleton
public class DockerFileFactory {

    public static final String NAME = "Runner Recipe";
    public static final String PATH = "runner_recipe";
    public static final String TYPE = "text/x-dockerfile-config";

    private final ProjectServiceClient   projectServiceClient;
    private final DtoFactory             dtoFactory;
    private final AppContext             appContext;

    @Inject
    public DockerFileFactory(ProjectServiceClient projectServiceClient,
                             DtoFactory dtoFactory,
                             AppContext appContext) {
        this.projectServiceClient = projectServiceClient;
        this.dtoFactory = dtoFactory;
        this.appContext = appContext;
    }

    /**
     * Create a new instance of {@link DockerFile} for a given href.
     *
     * @param href
     *         URL where recipe file is located
     * @return an instance of {@link DockerFile}
     * @throws IllegalStateException
     *         when no project is opened
     */
    @Nonnull
    public VirtualFile newInstance(@Nonnull String href) {
        return newInstance(href, NAME, PATH);
    }

    @Nonnull
    public VirtualFile newInstance(@Nonnull String href, @Nonnull String name, @Nonnull String path) {
        CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No project is opened");
        }

        Link link = dtoFactory.createDto(Link.class)
                              .withHref(href)
                              .withRel(DockerFile.GET_CONTENT);
        List<Link> links = Arrays.asList(link);

        ItemReference recipeFileItem = dtoFactory.createDto(ItemReference.class)
                                                 .withName(name)
                                                 .withPath(path)
                                                 .withMediaType(TYPE)
                                                 .withLinks(links);

        return new DockerFile(projectServiceClient, recipeFileItem);
    }

}