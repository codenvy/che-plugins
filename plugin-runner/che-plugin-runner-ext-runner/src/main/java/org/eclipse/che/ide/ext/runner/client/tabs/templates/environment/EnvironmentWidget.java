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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.environment;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.common.item.ItemWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.common.item.RunnerItems;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.vectomatic.dom.svg.ui.SVGImage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;

/**
 * The class contains methods which allow change view representation of runner.
 *
 * @author Dmitry Shnurenko
 */
public class EnvironmentWidget implements RunnerItems<Environment> {

    public static final String DEFAULT_DESCRIPTION = "DEFAULT";
    public static final String CUSTOM_DESCRIPTION  = "CUSTOM";

    private final ItemWidget itemWidget;
    private final SVGImage   projectScope;
    private final SVGImage   systemScope;
    private final AppContext appContext;

    private Scope       environmentScope;
    private Environment environment;

    @Inject
    public EnvironmentWidget(final ItemWidget itemWidget,
                             RunnerResources resources,
                             final SelectionManager selectionManager,
                             AppContext appContext) {
        this.itemWidget = itemWidget;
        this.appContext = appContext;

        projectScope = new SVGImage(resources.scopeProject());
        systemScope = new SVGImage(resources.scopeSystem());

        projectScope.addClassNameBaseVal(resources.runnerCss().environmentSvg());
        systemScope.addClassNameBaseVal(resources.runnerCss().environmentSvg());

        itemWidget.setDelegate(new ItemWidget.ActionDelegate() {
            @Override
            public void onWidgetClicked() {
                selectionManager.setEnvironment(environment);
            }
        });

        this.environmentScope = SYSTEM;
    }

    /**
     * Sets special environment scope.
     *
     * @param environmentScope
     *         scope which need set
     */
    public void setScope(@Nonnull Scope environmentScope) {
        this.environmentScope = environmentScope;
    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        itemWidget.select();
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        itemWidget.unSelect();
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull Environment environment) {
        this.environment = environment;

        itemWidget.setName(environment.getName());

        String description = updateDescription();
        itemWidget.setDescription(description);

        setImage();
    }
    @Nullable
    private String updateDescription() {
        String description = environment.getDescription();

        if (description == null && PROJECT.equals(environmentScope)) {
            description = CUSTOM_DESCRIPTION;
        }

        String defaultConfig = getDefaultRunner();

        return environment.getId().equals(defaultConfig) ? DEFAULT_DESCRIPTION : description;
    }

    @Nullable
    private String getDefaultRunner() {
        CurrentProject currentProject = appContext.getCurrentProject();

        return currentProject != null ? currentProject.getRunner() : null;
    }

    private void setImage() {
        switch (environmentScope) {
            case PROJECT:
                itemWidget.setImage(projectScope);
                break;
            case SYSTEM:
                itemWidget.setImage(systemScope);
                break;
            default:
        }
    }

    /** {@inheritDoc} */
    @Override
    public Widget asWidget() {
        return itemWidget.asWidget();
    }

}