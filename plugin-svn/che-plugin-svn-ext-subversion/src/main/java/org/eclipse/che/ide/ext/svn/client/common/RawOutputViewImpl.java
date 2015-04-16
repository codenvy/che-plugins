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
package org.eclipse.che.ide.ext.svn.client.common;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.parts.base.ToolButton;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vectomatic.dom.svg.ui.SVGImage;

/**
 * Implementation of {@link RawOutputView}.
 */
@Singleton
public class RawOutputViewImpl extends BaseView<RawOutputView.ActionDelegate> implements RawOutputView {

    interface RawOutputViewImplUiBinder extends UiBinder<Widget, RawOutputViewImpl> { }

    private static RawOutputViewImplUiBinder uiBinder = GWT.create(RawOutputViewImplUiBinder.class);

    private static final String INFO_COLOR = "lightgreen";

    @UiField
    ScrollPanel scrollPanel;

    @UiField
    FlowPanel outputArea;

    /**
     * Constructor.
     */
    @Inject
    public RawOutputViewImpl(final PartStackUIResources resources, final Resources coreResources) {
        super(resources);

        setContentWidget(uiBinder.createAndBindUi(this));

        final ToolButton clearButton = new ToolButton(new SVGImage(coreResources.clear()));

        clearButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onClearClicked();
            }
        });
        clearButton.ensureDebugId("console-clear");

        minimizeButton.ensureDebugId("console-minimizeBut");

        FlowPanel f = new FlowPanel();
        f.setWidth("100%");
        f.setHeight("18px");
        f.getElement().getStyle().setBackgroundColor(org.eclipse.che.ide.api.theme.Style.getPartBackground());

        toolBar.addSouth(f, 18);
        setToolbarHeight(22+18);

        f.getElement().getStyle().setProperty("borderTop", "#333 1px solid");
        //org.eclipse.che.ide.api.theme.Style.getOutputFontColor();
        f.add(clearButton);
    }

    @Override
    public void print(String text) {
        final String preStyle = " style='margin:0px; font-size: 11px;' ";
        final HTML html = new HTML();

        html.setHTML("<pre" + preStyle + "><span>" + text + "</span></pre>");
        html.getElement().setAttribute("style", "padding-left: 2px;");

        outputArea.add(html);
    }

    @Override
    public void print(String text, String color) {
        String preStyle = " style='margin:0px; font-size: 11px;' ";

        HTML html = new HTML();
        html.setHTML("<pre" + preStyle + "><span style='color:" + SimpleHtmlSanitizer.sanitizeHtml(color).asString() +
                ";'>" + SimpleHtmlSanitizer.sanitizeHtml(text).asString() + "</span></pre>");

        html.getElement().setAttribute("style", "padding-left: 2px;");
        outputArea.add(html);
    }

    @Override
    public void printInfo(String text) {
        print(text, INFO_COLOR);
    }

    @Override
    public void clear() {
        outputArea.clear();
    }

    @Override
    public void scrollBottom() {
        scrollPanel.getElement().setScrollTop(scrollPanel.getElement().getScrollHeight());
    }

}
