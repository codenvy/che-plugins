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
package org.eclipse.che.ide.ext.java.client.refactoring.move.wizard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.refactoring.RefactorInfo;
import org.eclipse.che.ide.ext.java.client.refactoring.move.MoveType;
import org.eclipse.che.ide.ext.java.client.refactoring.move.MovedItemType;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.JavaProject;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.PackageFragment;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.PackageFragmentRoot;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatusEntry;
import org.eclipse.che.ide.ui.window.Window;

import java.util.Iterator;
import java.util.List;

import static org.eclipse.che.ide.ext.java.client.refactoring.move.MoveType.REFACTOR_MENU;
import static org.eclipse.che.ide.ext.java.client.refactoring.move.MovedItemType.COMPILATION_UNIT;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
final class MoveViewImpl extends Window implements MoveView {
    interface MoveViewImplUiBinder extends UiBinder<Widget, MoveViewImpl> {
    }

    private static MoveViewImplUiBinder UI_BINDER = GWT.create(MoveViewImplUiBinder.class);

    private Button preview;
    private Button cancel;
    private Button accept;

    @UiField
    SimplePanel icon;
    @UiField
    TextBox     patternField;
    @UiField
    CheckBox    updateFullNames;
    @UiField
    Label       classNameUR;
    @UiField
    CheckBox    updateReferences;
    @UiField
    ScrollPanel treePanel;
    @UiField
    Label       className;
    @UiField
    FlowPanel   treePanelToHide;
    @UiField
    FlowPanel   patternsPanelToHide;

    @UiField
    Label       errorLabel;

    @UiField(provided = true)
    final JavaLocalizationConstant locale;

    private ActionDelegate delegate;

    @Inject
    public MoveViewImpl(JavaLocalizationConstant locale) {
        this.locale = locale;

        setTitle(locale.moveDialogTitle());

        setWidget(UI_BINDER.createAndBindUi(this));

        createButtons(locale);
    }

    private void createButtons(JavaLocalizationConstant locale) {
        preview = createButton(locale.moveDialogButtonPreview(), "move-preview-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onPreviewButtonClicked();
            }
        });

        cancel = createButton(locale.moveDialogButtonCancel(), "move-cancel-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        accept = createButton(locale.moveDialogButtonOk(), "move-accept-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onAcceptButtonClicked();
            }
        });

        getFooter().add(accept);
        getFooter().add(cancel);
        getFooter().add(preview);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void show(RefactorInfo refactorInfo) {
        MoveType moveType = refactorInfo.getMoveType();
        MovedItemType movedItemType = refactorInfo.getMovedItemType();

        treePanelToHide.setVisible(REFACTOR_MENU.equals(moveType));
        patternsPanelToHide.setVisible(COMPILATION_UNIT.equals(movedItemType));

        List<?> selectedItems = refactorInfo.getSelectedItems();

        int selectionSize = selectedItems.size();

        boolean isMultiSelection = selectionSize > 1;

        StorableNode selectedItem = (StorableNode)selectedItems.get(0);

        classNameUR.setText(isMultiSelection ? locale.multiSelectionReferences(selectionSize) : selectedItem.getDisplayName());
        className.setText(isMultiSelection ? locale.multiSelectionDestination(selectionSize) : selectedItem.getDisplayName());

        show();
    }

    @Override
    public void setTreeOfDestinations(List<JavaProject> projects) {

        final SingleSelectionModel<Object> selectionModel = new SingleSelectionModel<>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Object object = selectionModel.getSelectedObject();

                if (object instanceof JavaProject) {
                    JavaProject project = (JavaProject)object;
                    delegate.setMoveDestinationPath(project.getPath(), project.getPath());
                }
                if (object instanceof PackageFragmentRoot) {
                    PackageFragmentRoot fragmentRoot = (PackageFragmentRoot)object;
                    delegate.setMoveDestinationPath(fragmentRoot.getPath(), fragmentRoot.getProjectPath());
                }

                if (object instanceof PackageFragment) {
                    PackageFragment fragment = (PackageFragment)object;
                    delegate.setMoveDestinationPath(fragment.getPath(), fragment.getProjectPath());
                }
            }
        });
        CellTree tree = new CellTree(new ProjectsAndPackagesModel(projects, selectionModel), null);
        treePanel.clear();
        treePanel.add(tree);
        expandAll(tree.getRootTreeNode());
    }

    @Override
    public void showStatusMessage(RefactoringStatus status) {
        RefactoringStatusEntry statusEntry = getEntryMatchingSeverity(status.getSeverity(), status.getEntries());
        if(statusEntry != null) {
            errorLabel.setText(statusEntry.getMessage());
        } else {
            errorLabel.setText("");
        }
    }

    @Override
    public void clearStatusMessage() {
        errorLabel.setText("");
    }

    @Override
    public boolean isUpdateReferences() {
        return updateReferences.getValue();
    }

    @Override
    public boolean isUpdateQualifiedNames() {
        return updateFullNames.getValue();
    }

    @Override
    public String getFilePatterns() {
        return patternField.getValue();
    }

    /**
     * Returns the first entry which severity is equal or greater than the
     * given severity. If more than one entry exists that matches the
     * criteria the first one is returned. Returns <code>null</code> if no
     * entry matches.
     *
     * @param severity the severity to search for. Must be one of <code>FATAL
     *  </code>, <code>ERROR</code>, <code>WARNING</code> or <code>INFO</code>
     * @param entries
     * @return the entry that matches the search criteria
     */
    public RefactoringStatusEntry getEntryMatchingSeverity(int severity, List<RefactoringStatusEntry> entries) {


        Iterator iter= entries.iterator();
        while (iter.hasNext()) {
            RefactoringStatusEntry entry= (RefactoringStatusEntry)iter.next();
            if (entry.getSeverity() >= severity)
                return entry;
        }
        return null;
    }

    private void expandAll(TreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (!node.isChildLeaf(i)) {
                expandAll(node.setChildOpen(i, true));
            }
        }
    }
}