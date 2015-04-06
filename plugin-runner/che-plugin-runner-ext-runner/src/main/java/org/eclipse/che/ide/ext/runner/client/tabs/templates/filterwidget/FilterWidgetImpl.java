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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;

import javax.annotation.Nonnull;

/**
 * The class provides methods which allows change view representation of filter panel.
 *
 * @author Dmitry Shnurenko
 */
public class FilterWidgetImpl extends Composite implements FilterWidget {


    interface FilterWidgetImplUiBinder extends UiBinder<Widget, FilterWidgetImpl> {
    }

    private static final FilterWidgetImplUiBinder UI_BINDER = GWT.create(FilterWidgetImplUiBinder.class);

    @UiField
    CheckBox filter;

    @UiField(provided = true)
    final RunnerResources            resources;
    @UiField(provided = true)
    final RunnerLocalizationConstant locale;

    private ActionDelegate delegate;

    @Inject
    public FilterWidgetImpl(RunnerResources resources, RunnerLocalizationConstant locale) {
        this.resources = resources;
        this.locale = locale;

        initWidget(UI_BINDER.createAndBindUi(this));
    }

    @Override
    public boolean getMatchesProjectType() {
        return filter.getValue();
    }

    @Override
    public void setMatchesProjectType(boolean matches) { filter.setValue(matches); }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler({"filter"})
    public void onValueChanged(@SuppressWarnings("UnusedParameters")ValueChangeEvent<Boolean> event) {
        delegate.onValueChanged();
    }
}