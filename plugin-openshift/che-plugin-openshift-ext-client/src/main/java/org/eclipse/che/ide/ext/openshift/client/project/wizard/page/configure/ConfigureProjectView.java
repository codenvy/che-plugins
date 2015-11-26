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
package org.eclipse.che.ide.ext.openshift.client.project.wizard.page.configure;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;

import java.util.List;

/**
 * View for {@link ConfigureProjectPresenter}.
 *
 * @author Vlad Zhukovskiy
 */
@ImplementedBy(ConfigureProjectViewImpl.class)
public interface ConfigureProjectView extends View<ConfigureProjectView.ActionDelegate> {

    /** Reset input and controls state. */
    void resetControls();

    /** @return true if new OpenShift project radio button selected. */
    boolean isNewOpenShiftProjectSelected();

    /** Sets exist OpnShift projects into view. */
    void setExistOpenShiftProjects(List<Project> projects);

    /** @return new OpenShift project name. */
    String getOpenShiftNewProjectName();

    /** Show invalid OpenShift project name error message. */
    void showOsProjectNameError(String message);

    /** Hide invalid OpenShift project name error message. */
    void hideOsProjectNameError();

    /** Show invalid Che project name error message. */
    void showCheProjectNameError(String message);

    /** Hide invalid Che project name error message. */
    void hideCheProjectNameError();

    /** @return new Che project name. */
    String getCheNewProjectName();

    /** @return open shift project description */
    String getOpenShiftProjectDescription();

    /** @return Che project description. */
    String getCheProjectDescription();

    /** @return open shift project display name */
    String getOpenShiftProjectDisplayName();

    /** @return selected existed OpenShift project or null. */
    Project getExistedSelectedProject();

    /**
     * Enables or disables all elements on the view.
     *
     * @param enabled
     *         true or false to enable or disable
     */
    void setElementsEnabled(boolean enabled);

    /** Handles operations from the view. */
    interface ActionDelegate {
        /** Process operations when openshift project name changed. */
        void onOpenShiftNewProjectNameChanged();

        /** Process operations when che project name changed. */
        void onCheNewProjectNameChanged();

        /** Process operations when openshift project description changed. */
        void onOpenShiftDescriptionChanged();

        /** Process operations when che project description changed. */
        void onCheDescriptionChanged();

        /** Process operations when openshift project display name changed. */
        void onOpenShiftDisplayNameChanged();

        /** Process operation when existed project selected. */
        void onExistProjectSelected();
    }
}
