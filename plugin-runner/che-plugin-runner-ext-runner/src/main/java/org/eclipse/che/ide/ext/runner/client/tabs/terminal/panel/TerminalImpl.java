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
package org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.models.Runner.Status;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author Andrey Plotnikov
 */
public class TerminalImpl extends Composite implements Terminal {

    interface TerminalImplUiBinder extends UiBinder<Widget, TerminalImpl> {
    }

    private static final TerminalImplUiBinder UI_BINDER        = GWT.create(TerminalImplUiBinder.class);
    private static final Set<Status>          LAUNCHING_STATUS = EnumSet.of(Status.IN_PROGRESS, Status.IN_QUEUE);

    @UiField
    Label unavailableLabel;
    @UiField
    Frame terminal;

    @UiField(provided = true)
    final RunnerResources            res;
    @UiField(provided = true)
    final RunnerLocalizationConstant locale;

    private String url;

    @Inject
    public TerminalImpl(RunnerResources resources, RunnerLocalizationConstant locale) {
        this.res = resources;
        this.locale = locale;

        initWidget(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nullable Runner runner) {
        if (runner == null) {
            updateTerminalContent(false, null);
            return;
        }

        terminal.getElement().focus();

        String newTerminalUrl = runner.getTerminalURL();
        if (LAUNCHING_STATUS.contains(runner.getStatus()) || url != null && url.equals(newTerminalUrl)) {
            return;
        }

        url = newTerminalUrl;
        updateTerminalContent(runner.isAlive(), url);
    }

    private void updateTerminalContent(boolean isVisible, @Nullable String url) {
        unavailableLabel.setVisible(!isVisible);

        if (isVisible) {
            terminal.setUrl(url);
        } else {
            removeUrl();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        terminal.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public void setUnavailableLabelVisible(boolean isVisible) {
        unavailableLabel.setVisible(isVisible);
    }

    /** {@inheritDoc} */
    @Override
    public void setUrl(@Nonnull Runner runner) {
        String terminalUrl = runner.getTerminalURL();
        if (terminalUrl != null && terminalUrl.equals(url)) {
            return;
        }
        url = terminalUrl;
        terminal.setUrl(terminalUrl);
    }

    /** {@inheritDoc} */
    @Override
    public void removeUrl() {
        terminal.getElement().removeAttribute("src");
    }

}