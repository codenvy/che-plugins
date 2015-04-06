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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel;

import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorInitException;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.texteditor.HasReadOnlyProperty;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileEditorInput;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.api.editor.EditorPartPresenter.PROP_DIRTY;
import static org.eclipse.che.ide.api.editor.EditorPartPresenter.PROP_INPUT;

/**
 * The class that manages Properties panel widget.
 *
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
public abstract class PropertiesPanelPresenter implements PropertiesPanelView.ActionDelegate, PropertiesPanel {

    private static final String UNSUPPORTED_METHOD = "This is operation is unsupported";

    protected final PropertiesPanelView view;

    protected CurrentProject      currentProject;
    protected EditorPartPresenter editor;
    protected int                 undoOperations;

    public PropertiesPanelPresenter(@Nonnull PropertiesPanelView view, @Nonnull AppContext appContext) {
        this.view = view;
        this.view.setDelegate(this);

        currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return;
        }

        setEnableSaveCancelDeleteBtn(false);
    }

    protected void setEnableSaveCancelDeleteBtn(boolean enable) {
        view.setEnableCancelButton(enable);
        view.setEnableSaveButton(enable);
        view.setEnableDeleteButton(enable);
    }

    protected void initializeEditor(@Nonnull final FileNode file,
                                    @Nonnull EditorRegistry editorRegistry,
                                    @Nonnull FileTypeRegistry fileTypeRegistry) {
        FileType fileType = fileTypeRegistry.getFileTypeByFile(file);
        editor = editorRegistry.getEditor(fileType).getEditor();

        // wait when editor is initialized
        editor.addPropertyListener(new PropertyListener() {
            @Override
            public void propertyChanged(PartPresenter source, int propId) {
                switch (propId) {
                    case PROP_INPUT:
                        setReadOnlyProperty(file);
                        view.showEditor(editor);
                        break;
                    case PROP_DIRTY:
                        if (validateUndoOperation()) {
                            enableSaveAndCancelButtons();
                        }
                        break;
                    default:
                }
            }
        });

        try {
            editor.init(new DockerFileEditorInput(fileType, file));
        } catch (EditorInitException e) {
            Log.error(getClass(), e);
        }
    }

    private void setReadOnlyProperty(FileNode file) {
        if (editor instanceof HasReadOnlyProperty) {
            ((HasReadOnlyProperty)editor).setReadOnly(file.isReadOnly());
        }
    }

    private void enableSaveAndCancelButtons() {
        view.setEnableSaveButton(true);
        view.setEnableCancelButton(true);
    }

    private boolean validateUndoOperation() {
        // this code needs for right behaviour when someone is clicking on 'Cancel' button. We need to make disable some buttons.
        if (undoOperations == 0) {
            return true;
        }

        undoOperations--;
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigurationChanged() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);

        if (editor == null) {
            return;
        }

        editor.activate();
        editor.onOpen();
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull Runner runner) {
        view.setName(runner.getTitle());
        view.setType(runner.getType());
        view.selectMemory(RAM.detect(runner.getRAM()));
        view.selectScope(runner.getScope());
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull Environment environment) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(@Nonnull RemovePanelListener listener) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void hideButtonsPanel() {
        view.hideButtonsPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void onCopyButtonClicked() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void onSaveButtonClicked() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteButtonClicked() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelButtonClicked() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /** {@inheritDoc} */
    @Override
    public void onSwitcherChanged(boolean isOn) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }
}