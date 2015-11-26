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
package org.eclipse.che.ide.ext.openshift.client.project.wizard.page.template;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

import java.util.List;

/**
 * View for {@link SelectTemplatePresenter}.
 *
 * @author Vlad Zhukovskiy
 */
@ImplementedBy(SelectTemplateViewImpl.class)
public interface SelectTemplateView extends View<SelectTemplateView.ActionDelegate> {

    /**
     * Hides template list and shows template loader.
     */
    void showLoadingTemplates();

    /**
     * Sets available template list.
     */
    void setTemplates(List<Template> templates, boolean keepExisting);

    /**
     * Handles operations from the view.
     */
    interface ActionDelegate {

        /**
         * Process operations when user selects template.
         */
        void onTemplateSelected(Template template);
    }
}
