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
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.texteditor.HasReadOnlyProperty;
import org.eclipse.che.ide.ext.runner.client.TestEditor;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.eclipse.che.ide.api.editor.EditorPartPresenter.PROP_DIRTY;
import static org.eclipse.che.ide.api.editor.EditorPartPresenter.PROP_INPUT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_512;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesPanelPresenterTest {

    private static final String TEXT = "some text";

    //mocks for constructors
    @Mock
    private PropertiesPanelView view;
    @Mock
    private AppContext          appContext;

    @Mock
    private EditorRegistry                      editorRegistry;
    @Mock
    private FileTypeRegistry                    fileTypeRegistry;
    @Mock
    private Environment                         environment;
    @Mock
    private CurrentProject                      currentProject;
    @Mock
    private ProjectDescriptor                   descriptor;
    @Mock
    private TreeStructure                       treeStructure;
    @Mock
    private EditorProvider                      editorProvider;
    @Mock
    private EditorPartPresenter                 editor;
    @Mock
    private Throwable                           exception;
    @Mock
    private DockerFile                          file;
    @Mock
    private AsyncRequestCallback<ItemReference> asyncRequestCallback;
    @Mock
    private EditorInput                         editorInput;
    @Mock
    private FileType                            fileType;
    @Mock
    private ConfirmDialog                       confirmDialog;

    @Captor
    private ArgumentCaptor<PropertyListener> propertyListenerArgCaptor;

    private PropertiesPanelPresenter presenter;

    @Before
    public void setUp() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        when(environment.getScope()).thenReturn(SYSTEM);
        when(environment.getPath()).thenReturn(TEXT);
        when(environment.getName()).thenReturn(TEXT);
        when(environment.getType()).thenReturn(TEXT);
        when(environment.getRam()).thenReturn(MB_512.getValue());

        when(currentProject.getCurrentTree()).thenReturn(treeStructure);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);

        when(fileTypeRegistry.getFileTypeByFile(file)).thenReturn(fileType);
        when(editorRegistry.getEditor(fileType)).thenReturn(editorProvider);
        when(editorProvider.getEditor()).thenReturn(editor);

        when(editor.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);

        presenter = new DummyPanelProperties(view, appContext);
    }

    @Test
    public void verifyFirstConstructorWhenCurrentProjectNotNull() {
        reset(view, appContext);
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter = new DummyPanelProperties(view, appContext);

        verify(appContext).getCurrentProject();

        verify(view, never()).setEnableCancelButton(false);
        verify(view, never()).setEnableSaveButton(false);
        verify(view, never()).setEnableDeleteButton(false);
    }

    @Test
    public void verifyFirstConstructorWhenCurrentProjectIsNull() {
        verify(appContext).getCurrentProject();

        buttonSaveCancelDeleteShouldBeDisable();
    }

    private void buttonSaveCancelDeleteShouldBeDisable() {
        verify(view).setEnableCancelButton(false);
        verify(view).setEnableSaveButton(false);
        verify(view).setEnableDeleteButton(false);
    }

    @Test
    public void propertiesShouldBeChangedWithPropIdPropInputAndEditorIsNotInstanceOfHasReadOnlyProperty() {
        PartPresenter partPresenter = mock(PartPresenter.class);

        presenter.initializeEditor(file, editorRegistry, fileTypeRegistry);

        verify(fileTypeRegistry).getFileTypeByFile(file);
        verify(editorRegistry).getEditor(fileType);
        verify(editorProvider).getEditor();

        verify(editor).addPropertyListener(propertyListenerArgCaptor.capture());
        propertyListenerArgCaptor.getValue().propertyChanged(partPresenter, PROP_INPUT);

        verify(view).showEditor(editor);
    }

    @Test
    public void propertiesShouldBeChangedWithPropIdPropInputAndEditorIsInstanceOfHasReadOnlyProperty() {
        PartPresenter partPresenter = mock(PartPresenter.class);
        EditorPartPresenter editor2 = mock(TestEditor.class);
        when(editorProvider.getEditor()).thenReturn(editor2);
        when(file.isReadOnly()).thenReturn(true);

        presenter.initializeEditor(file, editorRegistry, fileTypeRegistry);

        verify(fileTypeRegistry).getFileTypeByFile(file);
        verify(editorRegistry).getEditor(fileType);
        verify(editorProvider).getEditor();

        verify(editor2).addPropertyListener(propertyListenerArgCaptor.capture());

        propertyListenerArgCaptor.getValue().propertyChanged(partPresenter, PROP_INPUT);
        verify(file).isReadOnly();
        verify((HasReadOnlyProperty)editor2).setReadOnly(true);

        verify(view).showEditor(editor2);
    }

    @Test
    public void propertiesShouldBeChangedWithPropIdDropDirtyWhenValidateUndoOperationIsTrue() {
        PartPresenter partPresenter = mock(PartPresenter.class);
        EditorPartPresenter editor2 = mock(TestEditor.class);
        when(editorProvider.getEditor()).thenReturn(editor2);
        when(file.isReadOnly()).thenReturn(true);

        presenter.initializeEditor(file, editorRegistry, fileTypeRegistry);

        verify(fileTypeRegistry).getFileTypeByFile(file);
        verify(editorRegistry).getEditor(fileType);
        verify(editorProvider).getEditor();

        verify(editor2).addPropertyListener(propertyListenerArgCaptor.capture());
        propertyListenerArgCaptor.getValue().propertyChanged(partPresenter, PROP_DIRTY);

        verify(view).setEnableSaveButton(true);
        verify(view).setEnableCancelButton(true);
    }

    @Test
    public void propertiesShouldBeChangedWithPropIdDropDirtyWhenValidateUndoOperationIsFalse() {
        PartPresenter partPresenter = mock(PartPresenter.class);
        EditorPartPresenter editor2 = mock(TestEditor.class);
        when(editorProvider.getEditor()).thenReturn(editor2);
        when(file.isReadOnly()).thenReturn(true);

        presenter.initializeEditor(file, editorRegistry, fileTypeRegistry);

        verify(editor2).addPropertyListener(propertyListenerArgCaptor.capture());
        propertyListenerArgCaptor.getValue().propertyChanged(partPresenter, PROP_DIRTY);

        verify(view).setEnableSaveButton(true);
        verify(view).setEnableCancelButton(true);
    }

    @Test
    public void presenterShouldGoneContainer() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(view);
    }

    @Test
    public void runnerShouldBeUpdated() {
        Runner runner1 = mock(Runner.class);
        when(runner1.getRAM()).thenReturn(MB_512.getValue());

        reset(view);
        presenter.update(runner1);

        verify(view).setName(runner1.getTitle());
        verify(view).setType(runner1.getType());
        verify(runner1).getRAM();
        verify(view).selectMemory(MB_512);
        verify(view).selectScope(runner1.getScope());
    }

    @Test
    public void buttonsPanelShouldBeHide() throws Exception {
        presenter.hideButtonsPanel();

        verify(view).hideButtonsPanel();
    }

    @Test
    public void presenterShouldGoneContainerWhenEditorNotNull() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.initializeEditor(file, editorRegistry, fileTypeRegistry);
        presenter.go(container);

        verify(container).setWidget(view);
        verify(editor).activate();
        verify(editor).onOpen();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addingListenerShouldBeUnsupportedOperation() {
        PropertiesPanel.RemovePanelListener listener = mock(PropertiesPanel.RemovePanelListener.class);
        presenter.addListener(listener);
    }

    @Test
    public void presenterShouldGoneContainerWhenEditorIsNull() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(view);
        verify(editor, never()).activate();
        verify(editor, never()).onOpen();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clickingToButtonCopyShouldBeUnsupportedOperation() {
        presenter.onCopyButtonClicked();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clickingToButtonSaveShouldBeUnsupportedOperation() {
        presenter.onSaveButtonClicked();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clickingToButtonDeleteShouldBeUnsupportedOperation() {
        presenter.onDeleteButtonClicked();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clickingToButtonCancelShouldBeUnsupportedOperation() {
        presenter.onCancelButtonClicked();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedOperationExceptionShouldBeThrownWhenCallOnConfigChange() {
        presenter.onConfigurationChanged();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedOperationExceptionShouldBeThrownWhenCallUpdateEnvironment() {
        presenter.update(environment);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedOperationExceptionShouldBeThrownWhenTryChangeEnvironment() {
        presenter.onSwitcherChanged(true);
    }

    private class DummyPanelProperties extends PropertiesPanelPresenter {

        public DummyPanelProperties(PropertiesPanelView view,
                                    AppContext appContext) {
            super(view, appContext);
        }
    }

}