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
package org.eclipse.che.ide.ext.runner.client.tabs.templates;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget.FilterWidget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Provides methods which allow display runner environments on special widget.
 *
 * @author Dmitry Shnurenko
 */
@ImplementedBy(TemplatesViewImpl.class)
public interface TemplatesView extends View<TemplatesView.ActionDelegate> {

    /**
     * Adds environment on templates panel and.
     *
     * @param environments
     *         runner which was added
     */
    void addEnvironment(@Nonnull Map<Scope, List<Environment>> environments);

    /**
     * Sets visibility state to panel.
     *
     * @param isVisible
     *         <code>true</code> panel is visible, <code>false</code> panel is un visible
     */
    void setVisible(boolean isVisible);

    /** Clears panel with environments */
    void clearEnvironmentsPanel();

    /**
     * Selects environment widget using current environment.
     *
     * @param selectedEnvironment
     *         environment which was selected
     */
    void selectEnvironment(@Nullable Environment selectedEnvironment);

    /**
     * Sets filter widget {@link FilterWidget}to special place on templates panel.
     *
     * @param filterWidget
     *         panel which need set
     */
    void setFilterWidget(@Nonnull FilterWidget filterWidget);

    /**
     * Sets default project widget to special place on view.
     *
     * @param widget
     *         widget which need set
     */
    void setDefaultProjectWidget(@Nullable EnvironmentWidget widget);

    /**
     * Shows special popup which contains information about default environment.
     *
     * @param defaultEnvironment
     *         environment for which need display info
     */
    void showDefaultEnvironmentInfo(@Nonnull Environment defaultEnvironment);

    public interface ActionDelegate {

        /** Performs some actions when user over mouse on default runner. */
        void onDefaultRunnerMouseOver();
    }
}