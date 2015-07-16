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
package org.eclipse.che.ide.ext.java.client.settings.compiler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.java.client.settings.property.PropertyWidget;

import javax.annotation.Nonnull;

/**
 * The class provides special panel to store special property widgets which allow setup compiler. Also the class contains methods
 * to control this panel.
 *
 * @author Dmitry Shnurenko
 */
public class CompilerSetupViewImpl extends Composite implements CompilerSetupView {
    interface CompilerSetupViewImplUiBinder extends UiBinder<Widget, CompilerSetupViewImpl> {
    }

    private static final CompilerSetupViewImplUiBinder UI_BINDER = GWT.create(CompilerSetupViewImplUiBinder.class);

    @UiField
    FlowPanel properties;

    @Inject
    public CompilerSetupViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void addProperty(@Nonnull PropertyWidget propertyWidget) {
        properties.add(propertyWidget);
    }
}