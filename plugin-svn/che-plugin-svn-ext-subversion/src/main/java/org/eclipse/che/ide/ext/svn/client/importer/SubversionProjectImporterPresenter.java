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
package org.eclipse.che.ide.ext.svn.client.importer;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.ide.api.wizard.AbstractWizardPage;
import org.eclipse.che.ide.util.NameUtils;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.che.ide.ext.svn.shared.ImportParameterKeys;

/**
 * Handler for the Subversion Project Importer.
 */
public class SubversionProjectImporterPresenter extends AbstractWizardPage<ImportProject>
        implements SubversionProjectImporterView.ActionDelegate {

    public static final String PUBLIC_VISIBILITY  = "public";
    public static final String PRIVATE_VISIBILITY = "private";

    private final SubversionProjectImporterView view;

    @Inject
    public SubversionProjectImporterPresenter(final SubversionProjectImporterView view) {
        this.view = view;
        this.view.setDelegate(this);
    }

    @Override
    public void projectNameChanged(final String name) {
        dataObject.getProject().setName(name);
        updateDelegate.updateControls();

        validateProjectName(this.view);
    }

    private boolean validateProjectName(final SubversionProjectImporterView view) {
        if (NameUtils.checkProjectName(view.getProjectName())) {
            view.hideNameError();
            return true;
        } else {
            view.showNameError();
            return false;
        }
    }

    @Override
    public void projectUrlChanged(final String url) {
        final String[] parts = url.trim().split("/");
        final String projectName = parts[parts.length - 1];

        this.view.setProjectName(projectName);

        projectNameChanged(projectName);

        final String fullUrl = buildFullUrl(url, this.view.getProjectRelativePath());
        dataObject.getSource().getProject().setLocation(fullUrl);

        updateDelegate.updateControls();
    }

    @Override
    public void projecRelativePathChanged(final String value) {
        final String fullUrl = buildFullUrl(this.view.getProjectUrl(), value);
        dataObject.getSource().getProject().setLocation(fullUrl);
    }

    private String buildFullUrl(final String base, final String complement) {
        String result = base;
        if (complement != null && !complement.isEmpty()) {
            if (!result.endsWith("/")) {
                result = result + "/";
            }
            result = result + complement;
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }

    @Override
    public void projectDescriptionChanged(final String projectDescription) {
        dataObject.getProject().setDescription(projectDescription);
        updateDelegate.updateControls();
    }

    @Override
    public void projectVisibilityChanged(boolean visible) {
        dataObject.getProject().setVisibility(visible ? PUBLIC_VISIBILITY : PRIVATE_VISIBILITY);
        updateDelegate.updateControls();
    }

    @Override
    public void go(final AcceptsOneWidget container) {
        final NewProject project = dataObject.getProject();

        view.setProjectName(project.getName());
        view.setProjectDescription(project.getDescription());
        view.setProjectVisibility(PUBLIC_VISIBILITY.equals(project.getVisibility()));
        view.setProjectUrl(dataObject.getSource().getProject().getLocation());
//        view.setProjectRelativePath("trunk");

        container.setWidget(view);

        view.setInputsEnableState(true);
        view.focusInUrlInput();
    }

    @Override
    public void credentialsChanged(final String username, final String password) {
        Map<String, String> parameters = this.dataObject.getSource().getProject().getParameters();
        if (parameters == null) {
            parameters = new HashMap<String, String>();
            this.dataObject.getSource().getProject().setParameters(parameters);
        }
        parameters.put(ImportParameterKeys.PARAMETER_USERNAME, username);
        parameters.put(ImportParameterKeys.PARAMETER_PASSWORD, password);
    }

}
