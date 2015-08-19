/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.angularjs.core.client.wizard;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;
import com.google.inject.Inject;
import com.google.inject.Provider;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.plugin.angularjs.core.client.share.Const.ANGULAR_JS_ID;
import static org.eclipse.che.plugin.angularjs.core.client.share.Const.CATEGORY_JS;

/**
 * Provides information for registering AngularJS project type into project wizard.
 *
 * @author Vitalii Parfonov
 */
public class AngularJsProjectWizardRegistrar implements ProjectWizardRegistrar {
    private final List<Provider<? extends WizardPage<ImportProject>>> wizardPages;

    @Inject
    public AngularJsProjectWizardRegistrar() {
        wizardPages = new ArrayList<>();
    }

    @Nonnull
    public String getProjectTypeId() {
        return ANGULAR_JS_ID;
    }

    @Nonnull
    public String getCategory() {
        return CATEGORY_JS;
    }

    @Nonnull
    public List<Provider<? extends WizardPage<ImportProject>>> getWizardPages() {
        return wizardPages;
    }
}
