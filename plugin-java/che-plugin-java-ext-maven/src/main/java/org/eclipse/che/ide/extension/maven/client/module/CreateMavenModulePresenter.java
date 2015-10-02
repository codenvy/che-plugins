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
package org.eclipse.che.ide.extension.maven.client.module;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.GeneratorDescription;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.maven.client.MavenArchetype;
import org.eclipse.che.ide.extension.maven.client.MavenExtension;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.NameUtils;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.ARCHETYPE_GENERATION_STRATEGY;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.ARTIFACT_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.DEFAULT_SOURCE_FOLDER;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.DEFAULT_TEST_SOURCE_FOLDER;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.GROUP_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.MAVEN_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.PACKAGING;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.PARENT_ARTIFACT_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.PARENT_GROUP_ID;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.PARENT_VERSION;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.SOURCE_FOLDER;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.TEST_SOURCE_FOLDER;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.VERSION;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class CreateMavenModulePresenter implements CreateMavenModuleView.ActionDelegate {

    private       CreateMavenModuleView    view;
    private       ProjectServiceClient     projectService;
    private       DtoFactory               dtoFactory;
    private       DialogFactory            dialogFactory;
    private final ProjectExplorerPresenter projectExplorer;

    private String moduleName;

    private String         artifactId;
    private CurrentProject parentProject;

    @Inject
    public CreateMavenModulePresenter(CreateMavenModuleView view, ProjectServiceClient projectService, DtoFactory dtoFactory,
                                      DialogFactory dialogFactory, ProjectExplorerPresenter projectExplorer) {
        this.view = view;
        this.projectService = projectService;
        this.dtoFactory = dtoFactory;
        this.dialogFactory = dialogFactory;
        this.projectExplorer = projectExplorer;
        view.setDelegate(this);
    }

    public void showDialog(@NotNull CurrentProject project) {
        parentProject = project;
        view.setParentArtifactId(project.getAttributeValue(ARTIFACT_ID));
        view.setGroupId(project.getAttributeValue(GROUP_ID));
        view.setVersion(project.getAttributeValue(VERSION));
        view.reset();
        view.show();
        updateViewState();
    }

    @Override
    public void onClose() {
    }

    @Override
    public void create() {
        NewProject newProject = dtoFactory.createDto(NewProject.class);
        newProject.setType(MAVEN_ID);
        newProject.setVisibility(parentProject.getProjectDescription().getVisibility());

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(ARTIFACT_ID, Arrays.asList(artifactId));
        attributes.put(GROUP_ID, Arrays.asList(view.getGroupId()));
        attributes.put(VERSION, Arrays.asList(view.getVersion()));
        attributes.put(PACKAGING, Arrays.asList(view.getPackaging()));
        attributes.put(PARENT_ARTIFACT_ID, Arrays.asList(parentProject.getAttributeValue(ARTIFACT_ID)));
        attributes.put(PARENT_GROUP_ID, Arrays.asList(parentProject.getAttributeValue(GROUP_ID)));
        attributes.put(PARENT_VERSION, Arrays.asList(parentProject.getAttributeValue(VERSION)));
        newProject.setAttributes(attributes);

        GeneratorDescription generatorDescription;
        if (view.isGenerateFromArchetypeSelected()) {
            generatorDescription = getGeneratorDescription(view.getArchetype());
        } else {
            generatorDescription = dtoFactory.createDto(GeneratorDescription.class);
            if (!"pom".equals(view.getPackaging())) {
                attributes.put(SOURCE_FOLDER, Arrays.asList(DEFAULT_SOURCE_FOLDER));
                attributes.put(TEST_SOURCE_FOLDER, Arrays.asList(DEFAULT_TEST_SOURCE_FOLDER));
            }
        }
        newProject.setGeneratorDescription(generatorDescription);

        view.showButtonLoader(true);

        projectService.createModule(parentProject.getProjectDescription().getPath(), moduleName, newProject,
                                    new AsyncRequestCallback<ProjectDescriptor>() {
                                        @Override
                                        protected void onSuccess(ProjectDescriptor result) {
                                            view.close();
                                            view.showButtonLoader(false);

                                            projectExplorer.reloadChildren();
                                        }

                                        @Override
                                        protected void onFailure(Throwable exception) {
                                            view.showButtonLoader(false);
                                            dialogFactory.createMessageDialog("", exception.getMessage(), null).show();
                                            Log.error(CreateMavenModulePresenter.class, exception);
                                        }
                                    });
    }

    @Override
    public void projectNameChanged(String name) {
        if (NameUtils.checkProjectName(name)) {
            moduleName = name;
        } else {
            moduleName = null;
        }
        updateViewState();
    }

    private void updateViewState() {
        if (moduleName == null) {
            view.setNameError(true);
            view.setCreateButtonEnabled(false);
        } else {
            view.setNameError(false);
            if (artifactId == null) {
                view.setArtifactIdError(true);
                view.setCreateButtonEnabled(false);
            } else {
                view.setArtifactIdError(false);
                view.setCreateButtonEnabled(true);
            }
        }
        view.enableArchetypes(view.isGenerateFromArchetypeSelected());
        view.setPackagingVisibility(!view.isGenerateFromArchetypeSelected());
    }

    @Override
    public void artifactIdChanged(String artifactId) {
        if (NameUtils.checkProjectName(artifactId)) {
            this.artifactId = artifactId;
        } else {
            this.artifactId = null;
        }
        updateViewState();
    }

    @Override
    public void generateFromArchetypeChanged(boolean isGenerateFromArchetype) {
        updateViewState();
        if (!isGenerateFromArchetype) {
            view.clearArchetypes();
        } else {
            view.setArchetypes(MavenExtension.getAvailableArchetypes());
        }
    }

    private GeneratorDescription getGeneratorDescription(MavenArchetype archetype) {
        HashMap<String, String> options = new HashMap<>();
        options.put("type", ARCHETYPE_GENERATION_STRATEGY);
        options.put("archetypeGroupId", archetype.getGroupId());
        options.put("archetypeArtifactId", archetype.getArtifactId());
        options.put("archetypeVersion", archetype.getVersion());
        if (archetype.getRepository() != null) {
            options.put("archetypeRepository", archetype.getRepository());
        }
        return dtoFactory.createDto(GeneratorDescription.class).withOptions(options);
    }
}
