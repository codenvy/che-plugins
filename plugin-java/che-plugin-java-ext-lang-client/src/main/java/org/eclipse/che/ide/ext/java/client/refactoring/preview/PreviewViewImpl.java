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
package org.eclipse.che.ide.ext.java.client.refactoring.preview;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ui.window.Window;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
final class PreviewViewImpl extends Window implements PreviewView {
    interface PreviewViewImplUiBinder extends UiBinder<Widget, PreviewViewImpl> {
    }

    private static final PreviewViewImplUiBinder UI_BINDER = GWT.create(PreviewViewImplUiBinder.class);

    private ActionDelegate delegate;

    @UiField
    SimplePanel changesTree;

    @UiField(provided = true)
    final JavaLocalizationConstant locale;

    @Inject
    public PreviewViewImpl(JavaLocalizationConstant locale) {
        this.locale = locale;

        setWidget(UI_BINDER.createAndBindUi(this));

        Button backButton = createButton(locale.moveDialogButtonBack(), "preview-back-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onBackButtonClicked();
            }
        });

        Button cancelButton = createButton(locale.moveDialogButtonCancel(), "preview-cancel-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        Button acceptButton = createButton(locale.moveDialogButtonOk(), "preview-ok-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onAcceptButtonClicked();
            }
        });

        getFooter().add(acceptButton);
        getFooter().add(cancelButton);
        getFooter().add(backButton);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }
}