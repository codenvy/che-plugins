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
package org.eclipse.che.ide.ext.runner.client.tabs.console.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Runner;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.ext.runner.client.tabs.console.panel.Lines.CLEANED;
import static org.eclipse.che.ide.ext.runner.client.tabs.console.panel.Lines.MAXIMUM;
import static org.eclipse.che.ide.ext.runner.client.tabs.console.panel.MessageType.DOCKER;
import static org.eclipse.che.ide.ext.runner.client.tabs.console.panel.MessageType.ERROR;
import static org.eclipse.che.ide.ext.runner.client.tabs.console.panel.MessageType.INFO;
import static org.eclipse.che.ide.ext.runner.client.tabs.console.panel.MessageType.WARNING;

/**
 * @author Artem Zatsarynnyy
 * @author Vitaliy Guliy
 * @author Mihail Kuznyetsov
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 */
public class ConsoleImpl extends Composite implements Console {

    interface ConsoleImplUiBinder extends UiBinder<Widget, ConsoleImpl> {
    }

    private static final ConsoleImplUiBinder UI_BINDER = GWT.create(ConsoleImplUiBinder.class);

    @UiField
    ScrollPanel panel;
    @UiField
    FlowPanel   output;
    @UiField
    FlowPanel   mainPanel;
    @UiField(provided = true)
    final RunnerResources res;

    private final Provider<MessageBuilder> messageBuilderProvider;
    private final WidgetFactory            widgetFactory;
    private final Runner                   runner;

    private boolean isWrappedText;

    @Inject
    public ConsoleImpl(RunnerResources resources,
                       Provider<MessageBuilder> messageBuilderProvider,
                       WidgetFactory widgetFactory,
                       @Nonnull @Assisted Runner runner) {
        this.res = resources;
        this.messageBuilderProvider = messageBuilderProvider;
        this.widgetFactory = widgetFactory;
        this.runner = runner;

        initWidget(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void print(@Nonnull String text) {
        // nothing to display
        if (text.isEmpty()) {
            return;
        }

        //The message from server can be include a few lines of console
        // We detect type from the full message which can be multiline
        MessageType messageType = MessageType.detect(text);

        for (String message : text.split("\n")) {
            if (message.isEmpty()) {
                // don't print empty message
                continue;
            }

            MessageBuilder messageBuilder = messageBuilderProvider.get()
                                                                  .message(message)
                                                                  .type(messageType);

            if (DOCKER.equals(messageType) && message.startsWith(DOCKER.getPrefix() + ' ' + ERROR.getPrefix())) {
                messageBuilder.type(ERROR);
            }

            print(messageBuilder.build());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void printInfo(@Nonnull String line) {
        MessageBuilder messageBuilder = messageBuilderProvider.get()
                                                              .type(INFO)
                                                              .message(INFO.getPrefix() + ' ' + line);
        print(messageBuilder.build());
    }

    /** {@inheritDoc} */
    @Override
    public void printError(@Nonnull String line) {
        MessageBuilder messageBuilder = messageBuilderProvider.get()
                                                              .type(ERROR)
                                                              .message(ERROR.getPrefix() + ' ' + line);
        print(messageBuilder.build());
    }

    /** {@inheritDoc} */
    @Override
    public void printWarn(@Nonnull String line) {
        MessageBuilder messageBuilder = messageBuilderProvider.get()
                                                              .type(WARNING)
                                                              .message(WARNING.getPrefix() + ' ' + line);
        print(messageBuilder.build());
    }

    private void print(@Nonnull SafeHtml message) {
        cleanOverHeadLinesIfAny();

        HTML html = new HTML(message);
        html.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
        if (isWrappedText) {
            html.addStyleName(res.runnerCss().wrappedText());
        }

        output.add(html);

        scrollBottom();
    }

    private void cleanOverHeadLinesIfAny() {
        if (output.getWidgetCount() < MAXIMUM.getValue()) {
            return;
        }

        // remove first 10% of current lines on screen
        for (int i = 0; i < CLEANED.getValue(); i++) {
            output.remove(0);
        }

        Link logLink = runner.getLogUrl();
        if (logLink == null) {
            return;
        }

        String logUrl = logLink.getHref();
        if (logUrl == null) {
            return;
        }

        // print link to full logs in top of console
        output.insert(widgetFactory.createFullLogMessage(logUrl), 0);
    }

    /** {@inheritDoc} */
    @Override
    public void scrollBottom() {
        panel.getElement().setScrollTop(panel.getElement().getScrollHeight());
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        output.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void changeWrapTextParam() {
        isWrappedText = !isWrappedText;
        String wrappedText = res.runnerCss().wrappedText();

        for (int i = 0; i < output.getWidgetCount(); i++) {
            Widget widget = output.getWidget(i);

            if (isWrappedText) {
                widget.addStyleName(wrappedText);
            } else {
                widget.removeStyleName(wrappedText);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWrapText() {
        return isWrappedText;
    }

}
