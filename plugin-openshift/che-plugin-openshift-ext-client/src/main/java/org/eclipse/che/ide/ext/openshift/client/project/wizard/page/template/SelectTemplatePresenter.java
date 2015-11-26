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

import com.google.common.base.Predicate;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.wizard.AbstractWizardPage;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.dto.NewApplicationRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.Parameter;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;

import java.util.List;

/**
 * Presenter for select template wizard page.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class SelectTemplatePresenter extends AbstractWizardPage<NewApplicationRequest> implements SelectTemplateView.ActionDelegate {

    private final SelectTemplateView     view;
    private final OpenshiftServiceClient openshiftClient;
    private final DtoFactory             dtoFactory;

    public static final String DEF_NAMESPACE = "openshift";

    private Template template;

    private Predicate<Parameter> GIT_URI = new Predicate<Parameter>() {
        @Override
        public boolean apply(Parameter input) {
            return "GIT_URI".equals(input.getName());
        }
    };

    @Inject
    public SelectTemplatePresenter(SelectTemplateView view,
                                   OpenshiftServiceClient openshiftClient,
                                   DtoFactory dtoFactory) {
        this.view = view;
        this.openshiftClient = openshiftClient;
        this.dtoFactory = dtoFactory;

        view.setDelegate(this);
    }

    /** {@inheritDoc} */
    @Override
    public void init(NewApplicationRequest dataObject) {
        super.init(dataObject);

        view.showLoadingTemplates();

        openshiftClient.getTemplates(DEF_NAMESPACE)
                .then(new Operation<List<Template>>() {
                    @Override
                    public void apply(final List<Template> templates) throws OperationException {
                        view.setTemplates(templates, false);
                    }
                });
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompleted() {
        return template != null;
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Override
    public void onTemplateSelected(Template template) {
        this.template = template;
        dataObject.setTemplate(template);
        updateDelegate.updateControls();
    }
}
