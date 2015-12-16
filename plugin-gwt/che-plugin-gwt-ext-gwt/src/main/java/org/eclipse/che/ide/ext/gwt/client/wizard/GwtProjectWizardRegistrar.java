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
package org.eclipse.che.ide.ext.gwt.client.wizard;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.extension.maven.client.wizard.MavenPagePresenter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.ext.gwt.shared.Constants.GWT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.ext.java.shared.Constants.JAVA_CATEGORY;

/**
 * Provides information for registering GWT project type into project wizard.
 *
 * @author Artem Zatsarynnyi
 */
public class GwtProjectWizardRegistrar implements ProjectWizardRegistrar {
    private final List<Provider<? extends WizardPage<ProjectConfigDto>>> wizardPages;

    @Inject
    public GwtProjectWizardRegistrar(Provider<MavenPagePresenter> mavenPagePresenter) {
        wizardPages = new ArrayList<>();
        wizardPages.add(mavenPagePresenter);
    }

    @NotNull
    public String getProjectTypeId() {
        return GWT_PROJECT_TYPE_ID;
    }

    @NotNull
    public String getCategory() {
        return JAVA_CATEGORY;
    }

    @NotNull
    public List<Provider<? extends WizardPage<ProjectConfigDto>>> getWizardPages() {
        return wizardPages;
    }
}
