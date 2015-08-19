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
package org.eclipse.che.ide.ext.datasource.client.explorer;

import java.util.Collection;
import java.util.List;

import org.vectomatic.dom.svg.ui.SVGButtonBase;
import org.vectomatic.dom.svg.ui.SVGPushButton;

import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.ext.datasource.client.DatasourceUiResources;
import org.eclipse.che.ide.ext.datasource.shared.DatabaseMetadataEntityDTO;
import org.eclipse.che.ide.ext.datasource.shared.ExploreTableType;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.input.SignalEvent;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;

/**
 * The datasource explorer view component.
 */
@Singleton
public class DatasourceExplorerViewImpl extends BaseView<DatasourceExplorerView.ActionDelegate> implements DatasourceExplorerView {

    /** The binder interface for the datasource explorer view component. */
    interface DatasourceExplorerViewUiBinder extends UiBinder<Widget, DatasourceExplorerViewImpl> {
    }

    /** The explorer tree. */
    @UiField(provided = true)
    protected Tree<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> tree;

    /** The dropdown to select the datasource to explore. */
    @UiField
    protected ListBox datasourceListBox;

    /** The dropdown to select the table types to show. */
    @UiField
    protected ListBox tableTypesListBox;

    /** The panel where we show the selected element properties. */
    @UiField
    protected SimplePanel propertiesContainer;

    /** The button to refresh the datasource metadata. */
    @UiField
    protected SVGPushButton refreshButton;

    /** The CSS resource. */
    @UiField(provided = true)
    protected DatasourceUiResources datasourceUiResources;

    /** The split layout panel - needed so that we can set the splitter size */
    @UiField(provided = true)
    protected SplitLayoutPanel splitPanel;

    @Inject
    public DatasourceExplorerViewImpl(final DatabaseMetadataEntityDTORenderer.Resources resources,
                                      final DatabaseMetadataEntityDTORenderer renderer,
                                      final DatasourceExplorerViewUiBinder uiBinder,
                                      final DatasourceExplorerConstants constants,
                                      final DatasourceUiResources clientResource) {
        super(resources);

        /* initialize provided fields */
        this.tree = Tree.create(resources, new DatabaseMetadataEntityDTODataAdapter(), renderer);
        this.datasourceUiResources = clientResource;
        this.splitPanel = new SplitLayoutPanel(4);

        setContentWidget(uiBinder.createAndBindUi(this));

        datasourceListBox.ensureDebugId("datasourceListBox");

        refreshButton.addFace(SVGButtonBase.SVGFaceName.UP, new SVGButtonBase.SVGFace(new SVGButtonBase.SVGStyleChange[]{
                new SVGButtonBase.SVGStyleChange(new String[]{datasourceUiResources.datasourceUiCSS().explorerRefreshButtonUp()})}));
        refreshButton.addFace(SVGButtonBase.SVGFaceName.DOWN, new SVGButtonBase.SVGFace(new SVGButtonBase.SVGStyleChange[]{
                new SVGButtonBase.SVGStyleChange(new String[]{datasourceUiResources.datasourceUiCSS().explorerRefreshButtonDown()})}));
        refreshButton.setTitle(constants.exploreButtonTooltip());

        tree.setTreeEventHandler(new Tree.Listener<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode>() {
            @Override
            public void onNodeAction(TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node) {
                delegate.onDatabaseMetadataEntityAction(node.getData().getData());
            }

            @Override
            public void onNodeClosed(TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node) {
            }

            @Override
            public void onNodeContextMenu(int mouseX, int mouseY,
                                          TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node) {
                delegate.onContextMenu(mouseX, mouseY);
            }

            @Override
            public void onNodeDragStart(TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node, MouseEvent event) {
            }

            @Override
            public void onNodeDragDrop(TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node, MouseEvent event) {
            }

            @Override
            public void onNodeExpanded(TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node) {
            }

            @Override
            public void onNodeSelected(final TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> node, final SignalEvent event) {
                // we must force single selection and check unselection
                final List<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> selectedNodes = tree.getSelectionModel().getSelectedNodes();
                if (selectedNodes.isEmpty()) {
                    // this was a unselection
                    Log.debug(DatasourceExplorerViewImpl.class, "Unselect tree item (CTRL+click) - send null as selected item.");
                    delegate.onDatabaseMetadataEntitySelected(null);
                } else if (selectedNodes.size() == 1) {
                    // normal selection with exactly one selected element
                    Log.debug(DatasourceExplorerViewImpl.class, "Normal tree item selection.");
                    tree.getSelectionModel().clearSelections();
                    tree.getSelectionModel().selectSingleNode(node.getData());
                    delegate.onDatabaseMetadataEntitySelected(node.getData().getData());
                } else {
                    // attempt to do multiple selection with ctrl or shift
                    Log.debug(DatasourceExplorerViewImpl.class,
                              "Multiple selection triggered in datasource explorer tree - keep the last one.");
                    tree.getSelectionModel().clearSelections();
                    tree.getSelectionModel().selectSingleNode(node.getData());
                    delegate.onDatabaseMetadataEntitySelected(node.getData().getData());
                }
            }

            @Override
            public void onRootContextMenu(int mouseX, int mouseY) {
                delegate.onContextMenu(mouseX, mouseY);
            }

            @Override
            public void onRootDragDrop(MouseEvent event) {
            }

            @Override
            public void onKeyboard(KeyboardEvent event) {
            }
        });
    }

    @Override
    public void setDatasourceList(final Collection<String> datasourceIds) {
        if (datasourceIds == null || datasourceIds.isEmpty()) {
            this.datasourceListBox.clear();
            delegate.onSelectedDatasourceChanged(null);
            return;
        }

        // save the currently selected item
        int index = this.datasourceListBox.getSelectedIndex();
        String selectedValue = null;
        if (index != -1) {
            selectedValue = this.datasourceListBox.getValue(index);
        }

        this.datasourceListBox.clear();
        // add an empty item even if there is only one datasource in order to avoid preload at IDE startup
        this.datasourceListBox.addItem("", "");
        for (String datasourceId : datasourceIds) {
            this.datasourceListBox.addItem(datasourceId);
        }

        // restore selected value if needed
        if (index != -1) {
            for (int i = 0; i < this.datasourceListBox.getItemCount(); i++) {
                if (this.datasourceListBox.getItemText(i).equals(selectedValue)) {
                    this.datasourceListBox.setSelectedIndex(i);
                    break;
                }
            }
            delegate.onSelectedDatasourceChanged(getSelectedId());
        } else {
            // if there is empty item + one datasource
            if (this.datasourceListBox.getItemCount() == 2) {
                this.datasourceListBox.setSelectedIndex(0);
                delegate.onSelectedDatasourceChanged(this.datasourceListBox.getValue(0));
            }
        }
    }

    @Override
    public void setTableTypesList(final Collection<String> tableTypes) {
        this.tableTypesListBox.clear();
        for (final String tableType : tableTypes) {
            this.tableTypesListBox.addItem(tableType);
        }
    }

    @Override
    public void setTableTypes(final ExploreTableType tableType) {
        this.tableTypesListBox.setSelectedIndex(tableType.getIndex());
    }

    public String getSelectedId() {
        int index = this.datasourceListBox.getSelectedIndex();
        if (index != -1) {
            return this.datasourceListBox.getValue(index);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setItems(final DatabaseMetadataEntityDTO database) {
        if (database == null) {
            // should probably clean up the model too but we'd need a null-safe DataAdapter and Renderer
            tree.asWidget().setVisible(false);
        } else {
            tree.asWidget().setVisible(true);
            tree.getModel().setRoot(new DatabaseMetadataEntityDTODataAdapter.EntityTreeNode(null, database));
            tree.renderTree(1);
        }
    }

    @Override
    public AcceptsOneWidget getPropertiesDisplayContainer() {
        return this.propertiesContainer;
    }

    /**
     * Handler to react to clicks on the refresh button.
     *
     * @param event the event
     */
    @UiHandler("refreshButton")
    public void onRefreshClick(final ClickEvent event) {
        delegate.onClickExploreButton(datasourceListBox.getValue(datasourceListBox.getSelectedIndex()));
    }

    /**
     * Handler to react to value change in the datasource listbox.
     *
     * @param event the event
     */
    @UiHandler("datasourceListBox")
    public void onDatasourceListChanged(final ChangeEvent event) {
        delegate.onSelectedDatasourceChanged(datasourceListBox.getValue(datasourceListBox.getSelectedIndex()));
    }

    /**
     * Handler to react to value change in the table types listbox.
     *
     * @param event the event
     */
    @UiHandler("tableTypesListBox")
    public void onTableTypesListChanged(final ChangeEvent event) {
        delegate.onSelectedTableTypesChanged(this.tableTypesListBox.getSelectedIndex());
    }

}
