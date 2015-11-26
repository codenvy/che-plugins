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
package org.eclipse.che.ide.ext.openshift.client.project.wizard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ui.window.Window;

/**
 * Implementation of view {@link CreateProjectView}.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class CreateProjectViewImpl extends Window implements CreateProjectView {

    private static CreateProjectViewUiBinder uiBinder = GWT.create(CreateProjectViewUiBinder.class);

    interface CreateProjectViewUiBinder extends UiBinder<FlowPanel, CreateProjectViewImpl> {
    }

    @UiField
    SimplePanel wizardPanel;

    Button nextBtn;

    Button prevBtn;

    Button createBtn;

    @UiField(provided = true)
    final org.eclipse.che.ide.Resources resources;

    @UiField(provided = true)
    final CoreLocalizationConstant constants;

    private ActionDelegate delegate;

    @Inject
    public CreateProjectViewImpl(org.eclipse.che.ide.Resources resources,
                                 CoreLocalizationConstant constants,
                                 OpenshiftLocalizationConstant openshiftConstant) {
        ensureDebugId("openshift-create-from-template");

        this.resources = resources;
        this.constants = constants;

        setTitle(openshiftConstant.createFromTemplateViewTitle());
        setWidget(uiBinder.createAndBindUi(this));

        createBtn = createPrimaryButton("Create", "openshift-create-from-template-create-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCreateClicked();
            }
        });
        addButtonToFooter(createBtn);
        createBtn.addStyleName(resources.Css().buttonLoader());

        nextBtn = createButton(constants.next(), "openshift-create-from-template-next-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onNextClicked();
            }
        });
        addButtonToFooter(nextBtn);

        prevBtn = createButton(constants.back(), "openshift-create-from-template-prev-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onPreviousClicked();
            }
        });
        addButtonToFooter(prevBtn);

        getWidget().getElement().getStyle().setPadding(0, Style.Unit.PX);
    }

    /** {@inheritDoc} */
    @Override
    public void showPage(Presenter presenter) {
        wizardPanel.clear();
        presenter.go(wizardPanel);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void showWizard() {
        show();
    }

    /** {@inheritDoc} */
    @Override
    public void closeWizard() {
        hide();
    }

    /** {@inheritDoc} */
    @Override
    public void setNextButtonEnabled(boolean enabled) {
        nextBtn.setEnabled(enabled);
    }

    /** {@inheritDoc} */
    @Override
    public void setPreviousButtonEnabled(boolean enabled) {
        prevBtn.setEnabled(enabled);
    }

    /** {@inheritDoc} */
    @Override
    public void setCreateButtonEnabled(boolean enabled) {
        createBtn.setEnabled(enabled);
    }

    @Override
    public void animateCreateButton(boolean animate) {
        if (animate && !createBtn.getElement().hasAttribute("animated")) {
            // save state and start animation
            createBtn.getElement().setAttribute("originText", createBtn.getText());
            createBtn.getElement().getStyle().setProperty("minWidth", createBtn.getOffsetWidth() + "px");
            createBtn.setHTML("<i></i>");
            createBtn.getElement().setAttribute("animated", "true");
        } else if (!animate && createBtn.getElement().hasAttribute("animated")) {
            // stop animation and restore state
            createBtn.setText(createBtn.getElement().getAttribute("originText"));
            createBtn.getElement().removeAttribute("originText");
            createBtn.getElement().getStyle().clearProperty("minWidth");
            createBtn.getElement().removeAttribute("animated");
        }
    }

}
