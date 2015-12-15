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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ui.smartTree.NodeUniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeNodeLoader;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage;
import org.eclipse.che.ide.ui.smartTree.TreeSelectionModel;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent;
import org.eclipse.che.ide.ui.window.Window;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link ChangedListView}.
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
    private       ActionDelegate          delegate;

    private Tree tree;

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

        TreeNodeStorage nodeStorage = new TreeNodeStorage(new NodeUniqueKeyProvider() {
            @NotNull
            @Override
            public String getKey(@NotNull Node item) {
                if (item instanceof HasStorablePath) {
                    return ((HasStorablePath)item).getStorablePath();
                } else {
                    return String.valueOf(item.hashCode());
                }
            }
        });

        TreeNodeLoader nodeLoader = new TreeNodeLoader(Collections.<NodeInterceptor>emptySet());

        tree = new Tree(nodeStorage, nodeLoader);
        this.branchesPanel.add(tree);

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.Mode.SINGLE);

        tree.getSelectionModel().addSelectionChangedHandler(new SelectionChangedEvent.SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent event) {
                if (!event.getSelection().isEmpty()) {
                    delegate.onFileSelected(event.getSelection().get(0).getName());
                }
            }
        });

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
    public void setChanges(@NotNull List<String> items) {
        tree.getNodeStorage().clear();

        for (String item : items) {
            tree.getNodeStorage().add(new ChangedNode(item) {
                @Override
                public void actionPerformed() {
                    delegate.onCompareClicked();
                }
            });
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