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
package org.eclipse.che.ide.extension.machine.client.outputspanel.console;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * View for {@link OutputConsole}.
 *
 * @author Artem Zatsarynnyi
 */
public class OutputConsoleViewImpl extends Composite implements OutputConsoleView {

    private static final OutputConsoleViewUiBinder UI_BINDER = GWT.create(OutputConsoleViewUiBinder.class);

    private static final String PRE_STYLE = "style='margin:0px;'";

    private ActionDelegate delegate;

    /** scroll events to the bottom if view is visible */
    private boolean scrollBottomRequired = false;

    @UiField
    ScrollPanel scrollPanel;
    @UiField
    FlowPanel   consoleArea;
    @UiField
    Label commandLabel;

    /** If true - next printed line should replace the previous one. */
    private boolean carriageReturn;

    @Inject
    public OutputConsoleViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void printCommandLine(String commandLine) {
        commandLabel.setText(commandLine);
    }

    @Override
    public void print(String message, boolean cr) {
        final HTML html = new HTML(buildSafeHtmlMessage(message));
        html.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);

        if (carriageReturn) {
            consoleArea.remove(consoleArea.getWidgetCount() - 1);
            carriageReturn = false;
        }

        carriageReturn = cr;
        consoleArea.add(html);
    }

    /** Return sanitized message (with all restricted HTML-tags escaped) in {@link SafeHtml}. */
    private SafeHtml buildSafeHtmlMessage(String message) {
        return new SafeHtmlBuilder()
                .appendHtmlConstant("<pre " + PRE_STYLE + ">")
                .append(SimpleHtmlSanitizer.sanitizeHtml(message.isEmpty() ? " " : message))
                .appendHtmlConstant("</pre>")
                .toSafeHtml();
    }

    @Override
    public void scrollBottom() {
        /** scroll bottom immediately if view is visible */
        if (scrollPanel.getElement().getOffsetParent() != null) {
            scrollPanel.getElement().setScrollTop(scrollPanel.getElement().getScrollHeight());
            return;
        }

        /** otherwise, check the visibility periodically and scroll the view when it's visible */
        if (!scrollBottomRequired) {
            scrollBottomRequired = true;

            Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
                @Override
                public boolean execute() {
                    if (scrollPanel.getElement().getOffsetParent() != null) {
                        scrollPanel.getElement().setScrollTop(scrollPanel.getElement().getScrollHeight());
                        scrollBottomRequired = false;
                        return false;
                    }
                    return true;
                }
            }, 500);
        }
    }

    interface OutputConsoleViewUiBinder extends UiBinder<Widget, OutputConsoleViewImpl> {
    }
}
