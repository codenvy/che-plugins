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
package org.eclipse.che.ide.ext.git.client.compare.changedList;

import elemental.dom.Element;
import elemental.html.TableCellElement;
import elemental.html.TableElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.dom.Elements;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The implementation of {@link ChangedListView}.
 *
 * @author Igor Vinokur
 */
@Singleton
public class ChangedListViewImpl extends Window implements ChangedListView {
    interface BranchViewImplUiBinder extends UiBinder<Widget, ChangedListViewImpl> {
    }

    private static BranchViewImplUiBinder ourUiBinder = GWT.create(BranchViewImplUiBinder.class);

    Button btnClose;
    Button btnCompare;
    @UiField
    ScrollPanel branchesPanel;
    @UiField(provided = true)
    final         GitResources            res;
    @UiField(provided = true)
    final         GitLocalizationConstant locale;
    private       SimpleList<String>      changedList;
    private       ActionDelegate          delegate;

    /** Create presenter. */
    @Inject
    protected ChangedListViewImpl(GitResources resources,
                                  GitLocalizationConstant locale,
                                  org.eclipse.che.ide.Resources coreRes) {
        this.res = resources;
        this.locale = locale;

        Widget widget = ourUiBinder.createAndBindUi(this);

        this.setTitle(locale.changeListTitle());
        this.setWidget(widget);

        TableElement changedFileElement = Elements.createTableElement();
        changedFileElement.setAttribute("style", "width: 100%");
        SimpleList.ListEventDelegate<String> listBranchesDelegate = new SimpleList.ListEventDelegate<String>() {
            @Override
            public void onListItemClicked(Element itemElement, String itemData) {
                changedList.getSelectionModel().setSelectedItem(itemData);
                delegate.onFileSelected(itemData);
            }

            @Override
            public void onListItemDoubleClicked(Element listItemBase, String itemData) {
            }
        };
        SimpleList.ListItemRenderer<String> listBranchesRenderer = new SimpleList.ListItemRenderer<String>() {
            @Override
            public void render(Element itemElement, String itemData) {
                TableCellElement label = Elements.createTDElement();
                
                String file = itemData.substring(2);
                String colour = "";
                if (itemData.startsWith("A")) {
                    colour = "\"#228B22\"";
                } else if (itemData.startsWith("D")) {
                    colour = "\"#8B0000\"";
                } else if (itemData.startsWith("M")) {
                    colour = "\"#1E90FF\"";
                }

                SafeHtmlBuilder sb = new SafeHtmlBuilder();

                sb.appendHtmlConstant("<table><tr><td>");
                sb.appendHtmlConstant("<div id=\"" + UIObject.DEBUG_ID_PREFIX + "git-changed-file" + file + "\">");
                sb.appendHtmlConstant("<font color=" + colour + ">");
                sb.appendEscaped(file);
                sb.appendHtmlConstant("</font>");
                sb.appendHtmlConstant("</td>");

                sb.appendHtmlConstant("</tr></table>");

                label.setInnerHTML(sb.toSafeHtml().asString());

                itemElement.appendChild(label);
            }

            @Override
            public Element createElement() {
                return Elements.createTRElement();
            }
        };

        changedList = SimpleList
                .create((SimpleList.View)changedFileElement, coreRes.defaultSimpleListCss(), listBranchesRenderer, listBranchesDelegate);
        this.branchesPanel.add(changedList);

        createButtons();
    }

    private void createButtons() {
        btnClose = createButton(locale.buttonClose(), "git-compare-btn-close", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCloseClicked();
            }
        });
        addButtonToFooter(btnClose);

        btnCompare = createButton(locale.buttonCompare(), "git-compare-btn-compare", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCompareClicked();
            }
        });
        addButtonToFooter(btnCompare);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void setChanges(@NotNull List<String> branches) {
        this.changedList.render(branches);
        if (this.changedList.getSelectionModel().getSelectedItem() == null) {
            delegate.onFileUnselected();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        this.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void showDialog() {
        this.show();
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableCompareButton(boolean enabled) {
        btnCompare.setEnabled(enabled);
    }
}