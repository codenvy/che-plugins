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
package org.eclipse.che.ide.ext.runner.client.tabs.templates;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.tabs.common.item.RunnerItems;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.defaultrunnerinfo.DefaultRunnerInfo;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget.FilterWidget;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;

/**
 * The Class provides graphical implementation of runner environments.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class TemplatesViewImpl extends Composite implements TemplatesView {
    interface TemplatesViewImplUiBinder extends UiBinder<Widget, TemplatesViewImpl> {
    }

    private static final TemplatesViewImplUiBinder UI_BINDER = GWT.create(TemplatesViewImplUiBinder.class);

    private static final String GWT_POPUP_STANDARD_STYLE = "gwt-PopupPanel";

    private static final int TOP_SHIFT  = 90;
    private static final int LEFT_SHIFT = 175;

    @UiField
    FlowPanel         environmentsPanel;
    @UiField
    SimplePanel       filterPanel;
    @UiField
    SimpleLayoutPanel defaultRunner;

    @UiField(provided = true)
    final RunnerResources            resources;
    @UiField(provided = true)
    final RunnerLocalizationConstant locale;

    private final Map<Environment, EnvironmentWidget> environmentWidgets;
    private final WidgetFactory                       widgetFactory;
    private final List<EnvironmentWidget>             cacheWidgets;
    private final Label                               defaultRunnerStub;
    private final DefaultRunnerInfo                   defaultRunnerInfo;
    private final PopupPanel                          popupPanel;

    private ActionDelegate delegate;

    @Inject
    public TemplatesViewImpl(RunnerResources resources,
                             RunnerLocalizationConstant locale,
                             WidgetFactory widgetFactory,
                             DefaultRunnerInfo defaultRunnerInfo,
                             PopupPanel popupPanel) {
        this.resources = resources;
        this.locale = locale;
        this.widgetFactory = widgetFactory;
        this.defaultRunnerInfo = defaultRunnerInfo;

        this.popupPanel = popupPanel;
        this.popupPanel.removeStyleName(GWT_POPUP_STANDARD_STYLE);
        this.popupPanel.add(defaultRunnerInfo);

        initWidget(UI_BINDER.createAndBindUi(this));

        this.environmentWidgets = new HashMap<>();
        this.cacheWidgets = new ArrayList<>();

        this.defaultRunnerStub = new Label(locale.templatesDefaultRunnerStub());
        this.defaultRunnerStub.addStyleName(resources.runnerCss().fullSize());
        this.defaultRunnerStub.addStyleName(resources.runnerCss().defaultRunnerStub());
        this.defaultRunnerStub.addStyleName(resources.runnerCss().fontSizeTen());

        addDefaultRunnerInfoHandler();
    }

    private void addDefaultRunnerInfoHandler() {
        defaultRunner.addDomHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                delegate.onDefaultRunnerMouseOver();
            }
        }, MouseOverEvent.getType());

        defaultRunner.addDomHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                popupPanel.hide();
            }
        }, MouseOutEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    public void addEnvironment(@Nonnull Map<Scope, List<Environment>> environments) {
        clearEnvironmentsPanel();
        int i = 0;

        for (Environment environment : environments.get(PROJECT)) {
            addEnvironment(environment, PROJECT, i++);
        }

        List<Environment> systemEnvironments = environments.get(SYSTEM);

        for (Environment environment : systemEnvironments) {
            addEnvironment(environment, SYSTEM, i++);
        }
    }

    private void addEnvironment(@Nonnull Environment environment, @Nonnull Scope scope, @Nonnegative int index) {
        EnvironmentWidget widget = getItem(index);

        widget.setScope(scope);
        widget.update(environment);

        environmentWidgets.put(environment, widget);
        environmentsPanel.add(widget);
    }

    @Nonnull
    private EnvironmentWidget getItem(@Nonnegative int index) {
        if (cacheWidgets.size() > index) {
            EnvironmentWidget widget = cacheWidgets.get(index);
            widget.unSelect();
            return widget;
        }

        EnvironmentWidget widget = widgetFactory.createEnvironment();

        cacheWidgets.add(widget);

        return widget;
    }

    /** {@inheritDoc} */
    @Override
    public void selectEnvironment(@Nullable Environment selectedEnvironment) {
        for (RunnerItems widget : environmentWidgets.values()) {
            widget.unSelect();
        }

        EnvironmentWidget selectedWidget = environmentWidgets.get(selectedEnvironment);
        if (selectedWidget != null) {
            selectedWidget.select();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setFilterWidget(@Nonnull FilterWidget filterWidget) {
        filterPanel.setWidget(filterWidget);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultProjectWidget(@Nullable EnvironmentWidget widget) {
        defaultRunner.setWidget(widget == null ? defaultRunnerStub : widget);
    }

    /** {@inheritDoc} */
    @Override
    public void showDefaultEnvironmentInfo(@Nonnull Environment defaultEnvironment) {
        defaultRunnerInfo.update(defaultEnvironment);

        int x = defaultRunner.getAbsoluteLeft() + LEFT_SHIFT;
        int y = defaultRunner.getAbsoluteTop() - TOP_SHIFT;

        popupPanel.setPopupPosition(x, y);
        popupPanel.show();
    }

    /** {@inheritDoc} */
    @Override
    public void clearEnvironmentsPanel() {
        environmentsPanel.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }
}