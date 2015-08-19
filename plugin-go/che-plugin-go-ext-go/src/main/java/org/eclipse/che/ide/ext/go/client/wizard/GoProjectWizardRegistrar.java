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
package org.eclipse.che.ide.ext.go.client.wizard;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;
import com.google.inject.Inject;
import com.google.inject.Provider;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.ext.go.shared.ProjectAttributes.GO_CATEGORY;
import static org.eclipse.che.ide.ext.go.shared.ProjectAttributes.GO_ID;

/**
 * Provides information for registering Go project type into project wizard.
 *
 * @author Artem Zatsarynnyy
 */
public class GoProjectWizardRegistrar implements ProjectWizardRegistrar {
    private final List<Provider<? extends WizardPage<ImportProject>>> wizardPages;

    @Inject
    public GoProjectWizardRegistrar() {
        wizardPages = new ArrayList<>();
    }

    @Nonnull
    public String getProjectTypeId() {
        return GO_ID;
    }

    @Nonnull
    public String getCategory() {
        return GO_CATEGORY;
    }

    @Nonnull
    public List<Provider<? extends WizardPage<ImportProject>>> getWizardPages() {
        return wizardPages;
    }
}
