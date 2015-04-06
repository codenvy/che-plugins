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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.defaultrunnerinfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;

import javax.annotation.Nonnull;

/**
 * The class contains methods which allows change information about default runner.
 *
 * @author Dmitry Shnurenko
 */
public class DefaultRunnerInfoImpl extends Composite implements DefaultRunnerInfo {
    interface DefaultRunnerInfoWidgetUiBinder extends UiBinder<Widget, DefaultRunnerInfoImpl> {
    }

    private static final DefaultRunnerInfoWidgetUiBinder UI_BINDER = GWT.create(DefaultRunnerInfoWidgetUiBinder.class);

    @UiField
    Label name;
    @UiField
    Label type;
    @UiField
    Label ram;

    @UiField(provided = true)
    final RunnerResources            resources;
    @UiField(provided = true)
    final RunnerLocalizationConstant locale;

    @Inject
    public DefaultRunnerInfoImpl(RunnerResources resources, RunnerLocalizationConstant locale) {
        this.resources = resources;
        this.locale = locale;

        initWidget(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull Environment environment) {
        name.setText(environment.getName());
        type.setText(environment.getType());
        ram.setText(String.valueOf(environment.getRam()) + " MB");
    }
}