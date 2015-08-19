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
package org.eclipse.che.ide.extension.maven.client.wizard;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;
import com.google.inject.Inject;
import com.google.inject.Provider;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.ext.java.shared.Constants.JAVA_CATEGORY;
import static org.eclipse.che.ide.extension.maven.shared.MavenAttributes.MAVEN_ID;

/**
 * Provides information for registering Maven project type into project wizard.
 *
 * @author Artem Zatsarynnyy
 */
public class MavenProjectWizardRegistrar implements ProjectWizardRegistrar {
    private final List<Provider<? extends WizardPage<ImportProject>>> wizardPages;

    @Inject
    public MavenProjectWizardRegistrar(Provider<MavenPagePresenter> mavenPagePresenter) {
        wizardPages = new ArrayList<>();
        wizardPages.add(mavenPagePresenter);
    }

    @Nonnull
    public String getProjectTypeId() {
        return MAVEN_ID;
    }

    @Nonnull
    public String getCategory() {
        return JAVA_CATEGORY;
    }

    @Nonnull
    public List<Provider<? extends WizardPage<ImportProject>>> getWizardPages() {
        return wizardPages;
    }
}
