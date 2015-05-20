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
package org.eclipse.che.ide.ext.runner.client.tabs.console.container;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.tabs.console.button.ConsoleButton;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;

/**
 * @author Andrey Plotnikov
 */
public class ConsoleContainerViewImpl extends Composite implements ConsoleContainerView {

    interface ConsoleContainerViewImplUiBinder extends UiBinder<Widget, ConsoleContainerViewImpl> {
    }

    private static final ConsoleContainerViewImplUiBinder UI_BINDER = GWT.create(ConsoleContainerViewImplUiBinder.class);

    @UiField
    SimplePanel mainPanel;
    @UiField
    FlowPanel   buttons;
    @UiField
    Label       noRunnerLabel;
    @UiField(provided = true)
    final RunnerResources resources;

    private final WidgetFactory  widgetFactory;
    private final ConsoleButton  btnWrapText;
    private       ActionDelegate delegate;

    @Inject
    public ConsoleContainerViewImpl(RunnerResources resources,
                                    WidgetFactory widgetFactory,
                                    RunnerLocalizationConstant locale) {
        this.resources = resources;
        this.widgetFactory = widgetFactory;

        initWidget(UI_BINDER.createAndBindUi(this));

        ConsoleButton.ActionDelegate wrapTextDelegate = new ConsoleButton.ActionDelegate() {
            @Override
            public void onButtonClicked() {
                delegate.onWrapTextClicked();
            }
        };
        btnWrapText = createButton(resources.wrapText(), locale.consoleTooltipWraptext(), wrapTextDelegate);

        ConsoleButton.ActionDelegate scrollBottomDelegate = new ConsoleButton.ActionDelegate() {
            @Override
            public void onButtonClicked() {
                delegate.onScrollBottomClicked();
            }
        };
        createButton(resources.arrowBottom(), locale.consoleTooltipScroll(), scrollBottomDelegate);

        ConsoleButton.ActionDelegate cleanDelegate = new ConsoleButton.ActionDelegate() {
            @Override
            public void onButtonClicked() {
                delegate.onCleanClicked();
            }
        };
        createButton(resources.erase(), locale.consoleTooltipClear(), cleanDelegate);
    }

    @Nonnull
    private ConsoleButton createButton(@Nonnull SVGResource icon,
                                       @Nonnull String prompt,
                                       @Nonnull ConsoleButton.ActionDelegate delegate) {
        ConsoleButton button = widgetFactory.createConsoleButton(prompt, icon);
        button.setDelegate(delegate);

        buttons.add(button);

        return button;
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void showWidget(@Nonnull IsWidget console) {
        mainPanel.setWidget(console);
    }

    /** {@inheritDoc} */
    @Override
    public void removeWidget(@Nonnull IsWidget console) {
        mainPanel.remove(console);
    }

    /** {@inheritDoc} */
    @Override
    public void selectWrapTextButton(boolean isChecked) {
        btnWrapText.setCheckedStatus(isChecked);
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleNoRunnerLabel(boolean visible) {
        noRunnerLabel.setVisible(visible);
    }
}