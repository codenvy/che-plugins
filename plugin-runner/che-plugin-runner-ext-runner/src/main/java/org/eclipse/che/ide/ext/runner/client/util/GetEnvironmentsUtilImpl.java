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
package org.eclipse.che.ide.ext.runner.client.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.ext.runner.client.inject.factories.ModelsFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
public class GetEnvironmentsUtilImpl implements GetEnvironmentsUtil {

    private final ModelsFactory       modelsFactory;
    private final ProjectTypeRegistry projectTypeRegistry;
    private final AppContext          appContext;

    @Inject
    public GetEnvironmentsUtilImpl(ModelsFactory modelsFactory, ProjectTypeRegistry projectTypeRegistry, AppContext appContext) {
        this.modelsFactory = modelsFactory;
        this.projectTypeRegistry = projectTypeRegistry;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public List<RunnerEnvironmentLeaf> getAllEnvironments(@Nonnull RunnerEnvironmentTree tree) {
        List<RunnerEnvironmentLeaf> allEnvironments = new ArrayList<>();

        getEnvironments(tree, allEnvironments);

        return allEnvironments;
    }

    private void getEnvironments(@Nonnull RunnerEnvironmentTree tree, @Nonnull List<RunnerEnvironmentLeaf> allEnvironments) {
        for (RunnerEnvironmentLeaf environmentLeaf : tree.getLeaves()) {
            allEnvironments.add(environmentLeaf);
        }

        for (RunnerEnvironmentTree environmentTree : tree.getNodes()) {
            getEnvironments(environmentTree, allEnvironments);
        }
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public List<Environment> getEnvironmentsFromNodes(@Nonnull List<RunnerEnvironmentLeaf> leaves, @Nonnull Scope scope) {
        Set<Environment> sortEnvironment = new TreeSet<>();

        for (RunnerEnvironmentLeaf environmentLeaf : leaves) {
            Environment environment = modelsFactory.createEnvironment(environmentLeaf.getEnvironment(), scope);

            sortEnvironment.add(environment);
        }

        return new ArrayList<>(sortEnvironment);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public List<Environment> getEnvironmentsByProjectType(@Nonnull RunnerEnvironmentTree tree,
                                                          @Nonnull String projectType,
                                                          @Nonnull Scope scope) {
        List<RunnerEnvironmentLeaf> leaves = new ArrayList<>();

        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return new ArrayList<>();
        }

        String defaultRunner = currentProject.getRunner();

        if (defaultRunner == null || defaultRunner.startsWith(DEFAULT_RUNNER_PROJECT_TYPE) || PROJECT.equals(scope)) {
            RunnerEnvironmentTree categoryNode = getRunnerCategoryByProjectType(tree, projectType);

            leaves = getAllEnvironments(categoryNode);
        } else {
            String category = getCorrectCategoryName(defaultRunner);

            String[] nodeNames = category.split("/");

            for (String nodeName : nodeNames) {
                tree = tree.getNode(nodeName);
            }

            leaves.addAll(getAllEnvironments(tree));
        }

        return getEnvironmentsFromNodes(leaves, scope);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public String getType() {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return "";
        }

        ProjectDescriptor descriptor = currentProject.getProjectDescription();

        String defaultRunner = currentProject.getRunner();

        if (defaultRunner == null || defaultRunner.startsWith(DEFAULT_RUNNER_PROJECT_TYPE)) {
            ProjectTypeDefinition definition = projectTypeRegistry.getProjectType(descriptor.getType());

            List<String> categories = definition.getRunnerCategories();
            if (categories == null || categories.isEmpty()) {
                return BLANK_TYPE;
            }
            return categories.get(0);
        } else {
            return getCorrectCategoryName(defaultRunner);
        }
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getCorrectCategoryName(@Nonnull String defaultRunner) {
        int index = defaultRunner.indexOf('/') + 1;
        return defaultRunner.substring(index, defaultRunner.lastIndexOf('/'));
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public RunnerEnvironmentTree getRunnerCategoryByProjectType(@Nonnull RunnerEnvironmentTree tree, @Nonnull String projectType) {
        ProjectTypeDefinition definition = projectTypeRegistry.getProjectType(projectType);

        List<String> categories = definition.getRunnerCategories();
        if (categories == null || categories.isEmpty()) {
            return tree;
        }

        String category = categories.get(0);
        tree.setDisplayName(category);

        RunnerEnvironmentTree categoryNode = tree.getNode(category.toLowerCase());
        if (categoryNode == null) {
            return tree;
        }

        return categoryNode;
    }

}