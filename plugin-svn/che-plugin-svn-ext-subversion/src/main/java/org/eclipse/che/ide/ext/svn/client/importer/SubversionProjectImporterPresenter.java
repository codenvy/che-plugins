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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.svn.shared.ImportParameterKeys;

/**
 * Handler for the Subversion Project Importer.
 *
 * @author vzhukovskii@codenvy.com
 */
public class SubversionProjectImporterPresenter extends AbstractWizardPage<ImportProject>
        implements SubversionProjectImporterView.ActionDelegate {

    public static final String PUBLIC_VISIBILITY  = "public";
    public static final String PRIVATE_VISIBILITY = "private";

    private SubversionProjectImporterView view;

    @Inject
    public SubversionProjectImporterPresenter(SubversionProjectImporterView view) {
        this.view = view;
        this.view.setDelegate(this);
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        final NewProject project = dataObject.getProject();

        view.setProjectName(project.getName());
        view.setProjectDescription(project.getDescription());
        view.setProjectVisibility(PUBLIC_VISIBILITY.equals(project.getVisibility()));
        view.setProjectUrl(dataObject.getSource().getProject().getLocation());

        container.setWidget(view);

        view.setUrlTextBoxFocused();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectNameChanged() {
        dataObject.getProject().setName(view.getProjectName());
        updateDelegate.updateControls();

        view.setNameErrorVisibility(!NameUtils.checkProjectName(view.getProjectName()));
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectUrlChanged() {
        if (Strings.isNullOrEmpty(view.getProjectUrl())) {
            view.setProjectName("");
            return;
        }

        String projectName = Iterables.getLast(Splitter.on("/").omitEmptyStrings().split(view.getProjectUrl()));
        String calcUrl = getUrl(view.getProjectUrl(), view.getProjectRelativePath());

        view.setProjectName(projectName);
        dataObject.getSource().getProject().setLocation(calcUrl);
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectRelativePathChanged() {
        String calcUrl = getUrl(view.getProjectUrl(), view.getProjectRelativePath());
        dataObject.getSource().getProject().setLocation(calcUrl);
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectDescriptionChanged() {
        dataObject.getProject().setDescription(view.getProjectDescription());
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectVisibilityChanged() {
        dataObject.getProject().setVisibility(view.getProjectVisibility() ? PUBLIC_VISIBILITY : PRIVATE_VISIBILITY);
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void onCredentialsChanged() {
        dataObject.getSource().getProject().getParameters().put(ImportParameterKeys.PARAMETER_USERNAME, view.getUserName());
        dataObject.getSource().getProject().getParameters().put(ImportParameterKeys.PARAMETER_PASSWORD, view.getPassword());
    }

    private String getUrl(String url, String relPath) {
        return (url.endsWith("/") ? url.substring(0, url.length() - 1) : url) + (relPath.startsWith("/") ? relPath : relPath.concat("/"));
    }
}
