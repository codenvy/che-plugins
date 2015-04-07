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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.texteditor.UndoableEditor;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.java.JsonArrayListAdapter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.TestEditor;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileEditorInput;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.eclipse.che.ide.imageviewer.ImageViewerResources;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.models.EnvironmentImpl.ROOT_FOLDER;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_512;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl.PropertiesEnvironmentPanel.ENVIRONMENT_ID_PREFIX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesEnvironmentPanelTest {

    private static final String TEXT  = "someText";
    private static final String TEXT2 = "someText2";
    private Array<ItemReference> result;

    //mocks for constructors
    @Mock
    private PropertiesPanelView                        view;
    @Mock
    private DtoFactory                                 dtoFactory;
    @Mock
    private EditorRegistry                             editorRegistry;
    @Mock
    private FileTypeRegistry                           fileTypeRegistry;
    @Mock
    private DockerFileFactory                          dockerFileFactory;
    @Mock
    private ProjectServiceClient                       projectService;
    @Mock
    private EventBus                                   eventBus;
    @Mock
    private AppContext                                 appContext;
    @Mock
    private DialogFactory                              dialogFactory;
    @Mock
    private RunnerLocalizationConstant                 locale;
    @Mock
    private GetProjectEnvironmentsAction               projectEnvironmentsAction;
    @Mock
    private NotificationManager                        notificationManager;
    @Mock
    private DtoUnmarshallerFactory                     unmarshallerFactory;
    @Mock
    private AsyncCallbackBuilder<ItemReference>        asyncCallbackBuilder;
    @Mock
    private AsyncCallbackBuilder<Array<ItemReference>> asyncArrayCallbackBuilder;
    @Mock
    private AsyncCallbackBuilder<Void>                 voidAsyncCallbackBuilder;
    @Mock
    private AsyncCallbackBuilder<ProjectDescriptor>    asyncDescriptorCallbackBuilder;
    @Mock
    private Environment                                environment;
    @Mock
    private TemplatesContainer                         templatesContainer;

    @Mock
    private Runner                                     runner;
    @Mock
    private CurrentProject                             currentProject;
    @Mock
    private RunnersDescriptor                          runnersDescriptor;
    @Mock
    private RunnerConfiguration                        runnerConfiguration;
    @Mock
    private Timer                                      timer;
    @Mock
    private Unmarshallable<Array<ItemReference>>       unmarshaller;
    @Mock
    private ProjectDescriptor                          projectDescriptor;
    @Mock
    private TreeStructure                              treeStructure;
    @Mock
    private EditorProvider                             editorProvider;
    @Mock
    private EditorPartPresenter                        editor;
    @Mock
    private Throwable                                  exception;
    @Mock
    private DockerFile                                 file;
    @Mock
    private AsyncRequestCallback<ItemReference>        asyncRequestCallback;
    @Mock
    private AsyncRequestCallback<Array<ItemReference>> arrayAsyncCallback;
    @Mock
    private AsyncRequestCallback<ProjectDescriptor>    descriptorCallback;
    @Mock
    private EditorInput                                editorInput;
    @Mock
    private ConfirmDialog                              confirmDialog;
    @Mock
    private HandlesUndoRedo                            handlesUndoRedo;
    @Mock
    private AsyncRequestCallback<Void>                 voidAsyncRequestCallback;

    @Mock
    private ImageViewerResources     resources;
    @Mock
    private CoreLocalizationConstant constant;
    @Mock
    private FileType                 fileType;

    @Mock
    private PropertiesPanel.RemovePanelListener listener1;
    @Mock
    private PropertiesPanel.RemovePanelListener listener2;

    @Mock
    private ItemReference itemReference1;
    @Mock
    private ItemReference itemReference2;

    @Captor
    private ArgumentCaptor<TimerFactory.TimerCallBack>                 timerCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<ItemReference>>> asyncRequestCallbackArgCaptor;
    @Captor
    private ArgumentCaptor<PropertyListener>                           propertyListenerArgCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<ItemReference>>             successCallbackArgCaptor;
    @Captor
    private ArgumentCaptor<AsyncCallback<String>>                      editorTextCaptor;
    @Captor
    private ArgumentCaptor<FailureCallback>                            failureCallbackArgCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>>                 requestCallbackArgCaptor;
    @Captor
    private ArgumentCaptor<AsyncCallback<EditorInput>>                 editorInputCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<Array<ItemReference>>>      successCallback;
    @Captor
    private ArgumentCaptor<FailureCallback>                            failureCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<Void>>                      voidArgumentCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<ProjectDescriptor>>         updateSuccessCaptor;

    private PropertiesEnvironmentPanel presenter;

    private Map<String, RunnerConfiguration> runnerConfigs;

    @Before
    public void setUp() throws Exception {
        result = new JsonArrayListAdapter<>(Arrays.asList(itemReference1, itemReference2));

        runnerConfigs = new HashMap<>();

        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(projectDescriptor);
        when(projectDescriptor.getRunners()).thenReturn(runnersDescriptor);
        when(runnersDescriptor.getConfigs()).thenReturn(runnerConfigs);
        when(runner.getRAM()).thenReturn(MB_512.getValue());
        when(unmarshallerFactory.newArrayUnmarshaller(ItemReference.class)).thenReturn(unmarshaller);

        when(environment.getScope()).thenReturn(SYSTEM);
        when(environment.getPath()).thenReturn(TEXT);
        when(environment.getName()).thenReturn(TEXT);
        when(environment.getType()).thenReturn(TEXT);
        when(environment.getRam()).thenReturn(MB_512.getValue());
        when(environment.getId()).thenReturn(TEXT);

        when(dtoFactory.createDto(RunnerConfiguration.class)).thenReturn(runnerConfiguration);
        when(runnerConfiguration.withRam(anyInt())).thenReturn(runnerConfiguration);
        when(runnerConfiguration.getRam()).thenReturn(MB_512.getValue());

        when(currentProject.getCurrentTree()).thenReturn(treeStructure);

        when(dockerFileFactory.newInstance(TEXT)).thenReturn(file);

        when(asyncCallbackBuilder.unmarshaller(ItemReference.class)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<ItemReference>>anyObject())).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(asyncDescriptorCallbackBuilder.success(Matchers.<SuccessCallback<ProjectDescriptor>>anyObject()))
                .thenReturn(asyncDescriptorCallbackBuilder);
        when(asyncDescriptorCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncDescriptorCallbackBuilder);

        when(asyncCallbackBuilder.build()).thenReturn(asyncRequestCallback);
        when(editorRegistry.getEditor(fileType)).thenReturn(editorProvider);
        when(editorProvider.getEditor()).thenReturn(editor);
        when(editor.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);
        when(fileTypeRegistry.getFileTypeByFile(any(FileNode.class))).thenReturn(fileType);

        when(locale.runnerTabTemplates()).thenReturn(TEXT);

        when(unmarshallerFactory.newArrayUnmarshaller(ItemReference.class)).thenReturn(unmarshaller);
        when(asyncArrayCallbackBuilder.unmarshaller(unmarshaller)).thenReturn(asyncArrayCallbackBuilder);
        when(asyncArrayCallbackBuilder.success(Matchers.<SuccessCallback<Array<ItemReference>>>anyObject()))
                .thenReturn(asyncArrayCallbackBuilder);
        when(asyncArrayCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncArrayCallbackBuilder);
        when(asyncArrayCallbackBuilder.build()).thenReturn(arrayAsyncCallback);

        when(voidAsyncCallbackBuilder.success(Matchers.<SuccessCallback<Void>>anyObject()))
                .thenReturn(voidAsyncCallbackBuilder);
        when(voidAsyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(voidAsyncCallbackBuilder);
        when(voidAsyncCallbackBuilder.build()).thenReturn(voidAsyncRequestCallback);

        presenter = new PropertiesEnvironmentPanel(view,
                                                   dtoFactory,
                                                   editorRegistry,
                                                   fileTypeRegistry,
                                                   dockerFileFactory,
                                                   projectService,
                                                   eventBus,
                                                   appContext,
                                                   dialogFactory,
                                                   locale,
                                                   projectEnvironmentsAction,
                                                   notificationManager,
                                                   unmarshallerFactory,
                                                   asyncCallbackBuilder,
                                                   asyncArrayCallbackBuilder,
                                                   voidAsyncCallbackBuilder,
                                                   asyncDescriptorCallbackBuilder,
                                                   templatesContainer,
                                                   environment);

        when(locale.removeEnvironment()).thenReturn(TEXT);
        when(locale.removeEnvironmentMessage(TEXT)).thenReturn(TEXT);
        when(dialogFactory.createConfirmDialog(eq(TEXT), eq(TEXT), any(ConfirmCallback.class), isNull(CancelCallback.class)))
                .thenReturn(confirmDialog);
        when(runner.getTitle()).thenReturn(TEXT);
    }

    @Test
    public void copyButtonShouldBeClickedAndContentFromEditorShouldBeReturnedWhenRunnerConfigExist() {
        String newName = "newName";

        runnerConfigs.put(TEXT, runnerConfiguration);
        when(itemReference2.getPath()).thenReturn("text/" + newName + "/text");
        callOnSuccessCreateFile();

        verify(view, times(2)).setEnableCancelButton(false);
        verify(view, times(2)).setEnableSaveButton(false);
        verify(view, times(2)).setEnableDeleteButton(false);

        verify(environment, times(3)).getId();
        verify(environment).getScope();
        verify(environment).setRam(MB_512.getValue());
        verify(view).selectMemory(MB_512);

        verify(dtoFactory).createDto(RunnerConfiguration.class);
        verify(runnerConfiguration).withRam(MB_512.getValue());

        verify(asyncCallbackBuilder, times(2)).failure(any(FailureCallback.class));
        verify(asyncCallbackBuilder, times(2)).build();
        verify(projectEnvironmentsAction).perform();

        verify(projectService).createFile(Matchers.eq("null" + ROOT_FOLDER),
                                          anyString(),
                                          eq(TEXT),
                                          isNull(String.class),
                                          eq(asyncRequestCallback));

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallback));

        assertThat(runnerConfigs.containsKey(ENVIRONMENT_ID_PREFIX + newName), is(true));
        assertThat(runnerConfigs.get(ENVIRONMENT_ID_PREFIX + newName), equalTo(runnerConfiguration));
    }

    private void callOnSuccessCreateFile() {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(file.isReadOnly()).thenReturn(true);

        presenter.onCopyButtonClicked();

        verify(currentProject).getProjectDescription();
        verify(projectDescriptor).getPath();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgCaptor.capture());

        successCallbackArgCaptor.getValue().onSuccess(itemReference1);

        verify(editor).getEditorInput();
        verify(editorInput).getFile();

        verify(file).getContent(editorTextCaptor.capture());
        editorTextCaptor.getValue().onSuccess(TEXT);

        verify(currentProject, times(2)).getProjectDescription();
        verify(projectDescriptor, times(2)).getPath();
        verify(asyncCallbackBuilder, times(2)).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder, times(2)).success(successCallbackArgCaptor.capture());

        successCallbackArgCaptor.getValue().onSuccess(itemReference2);
    }

    @Test
    public void copyButtonShouldBeClickedAndContentFromEditorShouldBeReturnedWhenRunnerConfigNotExist() {
        callOnSuccessCreateFile();

        verify(view, times(2)).setEnableCancelButton(false);
        verify(view, times(2)).setEnableSaveButton(false);
        verify(view, times(2)).setEnableDeleteButton(false);

        verify(view).selectMemory(MB_512);
        verify(projectEnvironmentsAction).perform();
        verify(dtoFactory, never()).createDto(RunnerConfiguration.class);
    }

    @Test
    public void copyButtonShouldBeClickedAndButFileWasCreatedFailed() {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(file.isReadOnly()).thenReturn(true);

        presenter.onCopyButtonClicked();

        verify(currentProject).getProjectDescription();
        verify(projectDescriptor).getPath();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgCaptor.capture());

        successCallbackArgCaptor.getValue().onSuccess(itemReference1);

        verify(editor).getEditorInput();
        verify(editorInput).getFile();

        verify(file).getContent(editorTextCaptor.capture());
        editorTextCaptor.getValue().onSuccess(TEXT);

        verify(currentProject, times(2)).getProjectDescription();
        verify(projectDescriptor, times(2)).getPath();
        verify(asyncCallbackBuilder, times(2)).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder, times(2)).failure(failureCallbackArgCaptor.capture());

        failureCallbackArgCaptor.getValue().onFailure(exception);

        verify(exception).getMessage();

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallback));
    }

    @Test
    public void copyButtonShouldBeClickedAndButFileContentWasReturnedFailed1() {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(file.isReadOnly()).thenReturn(true);

        presenter.onCopyButtonClicked();

        verify(currentProject).getProjectDescription();
        verify(projectDescriptor).getPath();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgCaptor.capture());

        successCallbackArgCaptor.getValue().onSuccess(itemReference1);

        verify(editor).getEditorInput();
        verify(editorInput).getFile();

        verify(file).getContent(editorTextCaptor.capture());
        editorTextCaptor.getValue().onFailure(exception);

        verify(exception).getMessage();

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallback));
    }

    @Test
    public void copyButtonShouldBeClickedAndButFileContentWasReturnedFailed2() {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(file.isReadOnly()).thenReturn(true);

        presenter.onCopyButtonClicked();

        verify(currentProject).getProjectDescription();
        verify(projectDescriptor).getPath();
        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).failure(failureCallbackArgCaptor.capture());

        failureCallbackArgCaptor.getValue().onFailure(exception);

        verify(exception).getMessage();

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallback));
    }

    @Test
    public void saveButtonShouldBeClickedWhenEditorIsNotDirty() throws Exception {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(view.getRam()).thenReturn(MB_512);
        when(view.getName()).thenReturn(TEXT);
        when(environment.getName()).thenReturn(TEXT2);
        when(projectDescriptor.getPath()).thenReturn(TEXT);

        presenter.onSaveButtonClicked();

        verify(projectDescriptor, times(2)).getPath();
        verify(environment).getRam();
        verify(currentProject).getProjectDescription();
        verify(environment).getName();
        verify(environment, times(2)).getId();

        verify(view).getName();

        verify(projectService).rename(anyString(), eq(TEXT), isNull(String.class), requestCallbackArgCaptor.capture());

        verify(voidAsyncCallbackBuilder).success(voidArgumentCaptor.capture());
        voidArgumentCaptor.getValue().onSuccess(null);

        verify(editor).isDirty();

        verify(dtoFactory).createDto(RunnerConfiguration.class);
        verify(runnerConfiguration).withRam(MB_512.getValue());

        verify(projectService, times(2)).updateProject(anyString(),
                                                       eq(projectDescriptor),
                                                       Matchers.<AsyncRequestCallback<ProjectDescriptor>>anyObject());

        verify(asyncDescriptorCallbackBuilder, times(2)).success(updateSuccessCaptor.capture());

        updateSuccessCaptor.getValue().onSuccess(projectDescriptor);

        verify(projectEnvironmentsAction).perform();

        assertThat(runnerConfigs.containsKey(ENVIRONMENT_ID_PREFIX + TEXT), CoreMatchers.is(true));
    }

    @Test
    public void saveButtonShouldBeClickedButRenameFileFailed() throws Exception {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(view.getRam()).thenReturn(MB_512);
        when(view.getName()).thenReturn(TEXT);
        when(environment.getName()).thenReturn(TEXT2);
        when(exception.getMessage()).thenReturn(TEXT);

        presenter.onSaveButtonClicked();

        verify(environment).getRam();
        verify(currentProject).getProjectDescription();
        verify(projectDescriptor, times(2)).getPath();
        verify(environment, times(2)).getId();

        verify(view).getName();

        verify(projectService).rename(anyString(), eq(TEXT), isNull(String.class), requestCallbackArgCaptor.capture());

        verify(voidAsyncCallbackBuilder).failure(failureCallbackArgCaptor.capture());
        failureCallbackArgCaptor.getValue().onFailure(exception);

        verify(exception).getMessage();
        verify(notificationManager).showError(TEXT);
    }

    @Test
    public void saveButtonShouldBeClickedWhenEditorIsDirty() throws Exception {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(view.getRam()).thenReturn(MB_512);
        when(view.getName()).thenReturn(TEXT);
        when(environment.getName()).thenReturn(TEXT2);
        when(editor.isDirty()).thenReturn(true);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                AsyncCallback callback = (AsyncCallback)args[0];
                callback.onSuccess(editorInput);
                return null;
            }
        }).when(editor).doSave(any(AsyncCallback.class));
        presenter.onSaveButtonClicked();

        verify(environment).getRam();
        verify(currentProject).getProjectDescription();
        verify(projectDescriptor, times(2)).getPath();
        verify(environment, times(2)).getId();

        verify(view).getName();

        verify(projectService).rename(anyString(), eq(TEXT), isNull(String.class), requestCallbackArgCaptor.capture());
        AsyncRequestCallback<Void> requestCallback = requestCallbackArgCaptor.getValue();

        verify(projectService).rename(anyString(), anyString(), isNull(String.class), eq(requestCallback));

        verify(voidAsyncCallbackBuilder).success(voidArgumentCaptor.capture());
        voidArgumentCaptor.getValue().onSuccess(null);

        verify(projectService, times(2)).updateProject(anyString(),
                                                       eq(projectDescriptor),
                                                       Matchers.<AsyncRequestCallback<ProjectDescriptor>>anyObject());

        verify(asyncDescriptorCallbackBuilder, times(2)).success(updateSuccessCaptor.capture());

        updateSuccessCaptor.getValue().onSuccess(projectDescriptor);

        verify(projectEnvironmentsAction).perform();

        verify(editor).isDirty();
        reset(view);

        verify(editor).doSave(editorInputCaptor.capture());
        editorInputCaptor.getValue().onSuccess(editorInput);
        verify(view).setEnableSaveButton(false);
        verify(view).setEnableCancelButton(false);

        verify(dtoFactory).createDto(RunnerConfiguration.class);
        verify(runnerConfiguration).withRam(MB_512.getValue());

        assertThat(runnerConfigs.containsKey(ENVIRONMENT_ID_PREFIX + TEXT), is(true));
    }

    @Test
    public void updateProjectShouldBeSuccessfulWhenClickOnSaveButton() throws Exception {
        reset(view);
        when(view.getRam()).thenReturn(MB_512);

        presenter.onSaveButtonClicked();

        verify(asyncDescriptorCallbackBuilder).success(updateSuccessCaptor.capture());

        updateSuccessCaptor.getValue().onSuccess(projectDescriptor);

        verify(view).setEnableSaveButton(false);
        verify(view).setEnableCancelButton(false);

        verify(projectService).updateProject(anyString(),
                                             eq(projectDescriptor),
                                             Matchers.<AsyncRequestCallback<ProjectDescriptor>>anyObject());
    }

    @Test
    public void saveButtonShouldBeClickedButSaveEditorFailed() throws Exception {
        when(editorProvider.getEditor()).thenReturn(editor);
        when(view.getRam()).thenReturn(MB_512);
        when(view.getName()).thenReturn(TEXT);
        when(environment.getName()).thenReturn(TEXT2);
        when(editor.isDirty()).thenReturn(true);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                AsyncCallback callback = (AsyncCallback)args[0];
                callback.onSuccess(editorInput);
                return null;
            }
        }).when(editor).doSave(any(AsyncCallback.class));

        presenter.onSaveButtonClicked();

        verify(environment).getRam();
        verify(currentProject).getProjectDescription();
        verify(projectDescriptor, times(2)).getPath();
        verify(environment, times(2)).getId();

        verify(view).getName();

        verify(voidAsyncCallbackBuilder).success(voidArgumentCaptor.capture());
        voidArgumentCaptor.getValue().onSuccess(null);

        verify(projectService, times(2)).updateProject(anyString(),
                                                       eq(projectDescriptor),
                                                       Matchers.<AsyncRequestCallback<ProjectDescriptor>>anyObject());

        verify(asyncDescriptorCallbackBuilder, times(2)).success(updateSuccessCaptor.capture());

        updateSuccessCaptor.getValue().onSuccess(projectDescriptor);

        verify(projectEnvironmentsAction).perform();

        verify(editor).isDirty();
        verify(projectService).rename(anyString(), eq(TEXT), isNull(String.class), eq(voidAsyncRequestCallback));

        verify(voidAsyncCallbackBuilder).failure(failureCallbackArgCaptor.capture());
        failureCallbackArgCaptor.getValue().onFailure(exception);

        verify(exception).getMessage();

        verify(editor).doSave(Matchers.<AsyncCallback<EditorInput>>anyObject());
    }

    @Test
    public void deletedButtonShouldBeClickedAndScopeIsSystem() {
        presenter.onDeleteButtonClicked();

        verifyNoMoreInteractions(dialogFactory);
    }

    @Test
    public void deletedButtonShouldBeClickedAndScopeIsProjectAndDialogShouldBeShownSuccess() throws Exception {
        ArgumentCaptor<ConfirmCallback> argumentCaptor = ArgumentCaptor.forClass(ConfirmCallback.class);
        when(environment.getScope()).thenReturn(PROJECT);
        presenter.onDeleteButtonClicked();

        verify(dialogFactory).createConfirmDialog(eq(TEXT), eq(TEXT), argumentCaptor.capture(), isNull(CancelCallback.class));
        verify(confirmDialog).show();
        argumentCaptor.getValue().accepted();

        verify(projectService).delete(eq(TEXT), requestCallbackArgCaptor.capture());

        verify(voidAsyncCallbackBuilder).success(voidArgumentCaptor.capture());
        voidArgumentCaptor.getValue().onSuccess(null);

        verifyUpdateProject();
    }

    private void buttonSaveCancelDeleteShouldBeDisable() {
        verify(view).setEnableCancelButton(false);
        verify(view).setEnableSaveButton(false);
        verify(view).setEnableDeleteButton(false);
    }

    @Test
    public void deletedButtonShouldBeClickedAndScopeIsProjectAndDialogShouldBeShownSuccessAndAddedRemoveListenersShouldBeDeleted()
            throws Exception {
        ArgumentCaptor<ConfirmCallback> argumentCaptor = ArgumentCaptor.forClass(ConfirmCallback.class);
        when(environment.getScope()).thenReturn(PROJECT);

        presenter.addListener(listener1);
        presenter.addListener(listener2);

        presenter.onDeleteButtonClicked();

        verify(dialogFactory).createConfirmDialog(eq(TEXT), eq(TEXT), argumentCaptor.capture(), isNull(CancelCallback.class));
        verify(confirmDialog).show();
        argumentCaptor.getValue().accepted();

        verify(projectService).delete(eq(TEXT), requestCallbackArgCaptor.capture());

        verify(voidAsyncCallbackBuilder).success(voidArgumentCaptor.capture());
        voidArgumentCaptor.getValue().onSuccess(null);

        verifyUpdateProject();
    }

    @Test
    public void deletedButtonShouldBeClickedAndScopeIsProjectAndDialogShouldBeShownSuccessButDeletionSelectedEnvironmentFailed()
            throws Exception {
        ArgumentCaptor<ConfirmCallback> argumentCaptor = ArgumentCaptor.forClass(ConfirmCallback.class);
        when(environment.getScope()).thenReturn(PROJECT);
        when(exception.getMessage()).thenReturn(TEXT);

        presenter.onDeleteButtonClicked();

        verify(dialogFactory).createConfirmDialog(eq(TEXT), eq(TEXT), argumentCaptor.capture(), isNull(CancelCallback.class));
        verify(confirmDialog).show();
        argumentCaptor.getValue().accepted();

        verify(voidAsyncCallbackBuilder).success(voidArgumentCaptor.capture());
        voidArgumentCaptor.getValue().onSuccess(null);

        verifyUpdateProject();

        verify(projectService).delete(TEXT, voidAsyncRequestCallback);

        verify(voidAsyncCallbackBuilder).failure(failureCallbackArgCaptor.capture());
        failureCallbackArgCaptor.getValue().onFailure(exception);

        verify(exception).getMessage();
        verify(notificationManager).showError(TEXT);
    }

    private void verifyUpdateProject() {
        verify(projectService).updateProject(anyString(),
                                             eq(projectDescriptor),
                                             Matchers.<AsyncRequestCallback<ProjectDescriptor>>anyObject());

        verify(asyncDescriptorCallbackBuilder).success(updateSuccessCaptor.capture());

        updateSuccessCaptor.getValue().onSuccess(projectDescriptor);

        verify(projectEnvironmentsAction).perform();
    }

    @Test
    public void changesShouldBeCancelWhenScopeIsSystem() {
        reset(view);
        presenter.onCancelButtonClicked();

        verify(view).setEnableSaveButton(false);
        verify(view).setEnableCancelButton(false);
        verify(view).setEnableDeleteButton(false);

        verify(environment).getId();
        verify(environment).setRam(MB_512.getValue());
        verify(view).setName(TEXT);
        verify(view).selectMemory(MB_512);
        verify(environment, times(3)).getScope();
        verify(view).selectScope(SYSTEM);
    }

    @Test
    public void changesShouldBeCancelWhenScopeIsProject() {
        when(environment.getScope()).thenReturn(PROJECT);

        reset(view);
        presenter.onCancelButtonClicked();

        verify(view).setEnableSaveButton(false);
        verify(view).setEnableCancelButton(false);
        verify(view).setEnableDeleteButton(true);

        verify(environment).getName();
        verify(environment).getId();
        verify(environment).setRam(MB_512.getValue());
        verify(view).setName(TEXT);
        verify(view).selectMemory(MB_512);
        verify(environment, times(3)).getScope();
        verify(view).selectScope(PROJECT);
    }

    @Test
    public void verifySecondConstructorWhenScopeIsProject() throws Exception {
        reset(appContext, view);
        when(environment.getScope()).thenReturn(SYSTEM);
        when(environment.getName()).thenReturn(TEXT);
        when(environment.getRam()).thenReturn(MB_512.getValue());
        when(environment.getPath()).thenReturn(TEXT);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(environment.getScope()).thenReturn(PROJECT);

        presenter = new PropertiesEnvironmentPanel(view,
                                                   dtoFactory,
                                                   editorRegistry,
                                                   fileTypeRegistry,
                                                   dockerFileFactory,
                                                   projectService,
                                                   eventBus,
                                                   appContext,
                                                   dialogFactory,
                                                   locale,
                                                   projectEnvironmentsAction,
                                                   notificationManager,
                                                   unmarshallerFactory,
                                                   asyncCallbackBuilder,
                                                   asyncArrayCallbackBuilder,
                                                   voidAsyncCallbackBuilder,
                                                   asyncDescriptorCallbackBuilder,
                                                   templatesContainer,
                                                   environment);

        verify(appContext).getCurrentProject();

        buttonSaveCancelDeleteShouldBeDisable();

        verify(environment, times(2)).getScope();

        verify(view).setEnableNameProperty(true);
        verify(view).setEnableRamProperty(true);
        verify(view).setEnableBootProperty(false);
        verify(view).setEnableShutdownProperty(false);
        verify(view).setEnableScopeProperty(false);

        view.setVisibleSaveButton(true);
        view.setVisibleDeleteButton(true);
        view.setVisibleCancelButton(true);

        verify(unmarshallerFactory).newArrayUnmarshaller(ItemReference.class);

        verify(asyncArrayCallbackBuilder).success(successCallback.capture());
        successCallback.getValue().onSuccess(result);

        verify(projectService).getChildren(eq(TEXT), asyncRequestCallbackArgCaptor.capture());

        verify(currentProject, times(2)).getProjectDescription();
        verify(currentProject, times(2)).getCurrentTree();
        verify(projectDescriptor, times(2)).getRunners();
        verify(runnersDescriptor, times(2)).getConfigs();
        verify(environment, times(2)).getName();
        verify(environment, times(2)).getId();

        verify(environment, times(2)).getPath();
        //we can't use mock for fileType that why we have null FileType
        verify(editorRegistry, times(3)).getEditor(any(FileType.class));
        verify(editorProvider, times(3)).getEditor();
        verify(editor, times(3)).addPropertyListener(any(PropertyListener.class));

        verify(editor, times(3)).init(any(DockerFileEditorInput.class));

        verify(asyncArrayCallbackBuilder).failure(failureCaptor.capture());
        failureCaptor.getValue().onFailure(exception);
        verify(exception).getMessage();
    }

    @Test
    public void verifySecondConstructorWhenScopeIsSystem() throws Exception {
        verify(appContext).getCurrentProject();

        buttonSaveCancelDeleteShouldBeDisable();

        verify(environment).getScope();

        verify(view).setEnableNameProperty(false);
        verify(view).setEnableRamProperty(false);
        verify(view).setEnableBootProperty(false);
        verify(view).setEnableShutdownProperty(false);
        verify(view).setEnableScopeProperty(false);

        verify(projectDescriptor).getRunners();
        verify(runnersDescriptor).getConfigs();
        verify(environment).getId();

        verify(environment).getPath();
        verify(dockerFileFactory).newInstance(TEXT);

        verify(fileTypeRegistry).getFileTypeByFile(file);
        //we can't use mock for fileType that why we have null FileType
        verify(editorRegistry).getEditor(fileType);
        verify(editorProvider).getEditor();
        verify(editor).addPropertyListener(any(PropertyListener.class));

        verify(editor).init(any(DockerFileEditorInput.class));
    }

    @Test
    public void changesShouldBeCancelWhenScopeIsSystemAndEnvironmentNotNullAndEditorInstanceOfUndoableEditorAndEditorIsDirty() {
        reset(editorProvider, view, environment);
        when(environment.getScope()).thenReturn(SYSTEM);
        when(environment.getName()).thenReturn(TEXT);
        when(environment.getRam()).thenReturn(MB_512.getValue());

        EditorPartPresenter editor2 = mock(TestEditor.class);
        when(editor2.getEditorInput()).thenReturn(editorInput);
        when(fileTypeRegistry.getFileTypeByFile(file)).thenReturn(fileType);
        when(editorRegistry.getEditor(fileType)).thenReturn(editorProvider);
        when(editorProvider.getEditor()).thenReturn(editor2);
        when(((UndoableEditor)editor2).getUndoRedo()).thenReturn(handlesUndoRedo);

        when(file.isReadOnly()).thenReturn(true);
        when(handlesUndoRedo.undoable()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(editor2.isDirty()).thenReturn(true);

        presenter = new PropertiesEnvironmentPanel(view,
                                                   dtoFactory,
                                                   editorRegistry,
                                                   fileTypeRegistry,
                                                   dockerFileFactory,
                                                   projectService,
                                                   eventBus,
                                                   appContext,
                                                   dialogFactory,
                                                   locale,
                                                   projectEnvironmentsAction,
                                                   notificationManager,
                                                   unmarshallerFactory,
                                                   asyncCallbackBuilder,
                                                   asyncArrayCallbackBuilder,
                                                   voidAsyncCallbackBuilder,
                                                   asyncDescriptorCallbackBuilder,
                                                   templatesContainer,
                                                   environment);

        presenter.onCancelButtonClicked();

        verify(view, times(2)).setEnableSaveButton(false);
        verify(view, times(2)).setEnableCancelButton(false);
        verify(view, times(2)).setEnableDeleteButton(false);

        verify((UndoableEditor)editor2).getUndoRedo();
        verify(editor2, times(3)).isDirty();
        verify(handlesUndoRedo, times(3)).undoable();

        verify(environment).getId();
        verify(environment).setRam(MB_512.getValue());
        verify(view).setName(TEXT);
        verify(view).selectMemory(MB_512);
        verify(environment, times(3)).getScope();
        verify(view).selectScope(SYSTEM);
    }

    @Test
    public void environmentShouldBeUpdatedWhenRunnerConfigExist() throws Exception {
        reset(view);
        when(environment.getScope()).thenReturn(SYSTEM);
        when(environment.getId()).thenReturn(TEXT);
        when(currentProject.getRunner()).thenReturn(TEXT2);
        runnerConfigs.put(TEXT, runnerConfiguration);

        presenter.update(environment);

        verifyUpdateEnvironment();
        verify(runnerConfiguration).getRam();
        verify(view).changeSwitcherState(false);
    }

    private void verifyUpdateEnvironment() {
        verify(view).setEnableCancelButton(false);
        verify(view).setEnableSaveButton(false);
        verify(view).setEnableDeleteButton(false);

        verify(environment, times(4)).getId();
        verify(environment).getName();
        verify(environment).setRam(MB_512.getValue());

        verify(view).selectMemory(MB_512);
        verify(view).setName(TEXT);
        verify(view).setType(TEXT);
        verify(view).selectScope(SYSTEM);
        verify(runnerConfiguration).getRam();
    }

    @Test
    public void environmentShouldBeUpdatedWhenRunnerConfigIsNotExist() throws Exception {
        reset(view);
        when(environment.getScope()).thenReturn(SYSTEM);
        when(environment.getId()).thenReturn(TEXT);
        when(currentProject.getRunner()).thenReturn(TEXT);

        presenter.update(environment);

        verify(view).setEnableCancelButton(false);
        verify(view).setEnableSaveButton(false);
        verify(view).setEnableDeleteButton(false);

        verify(view).setEnableCancelButton(false);
        verify(view).setEnableSaveButton(false);
        verify(view).setEnableDeleteButton(false);

        verify(environment, times(3)).getId();
        verify(environment).getName();
        verify(environment).setRam(MB_512.getValue());

        verify(view).selectMemory(MB_512);
        verify(view).setName(TEXT);
        verify(view).setType(TEXT);
        verify(view).selectScope(SYSTEM);
        verify(runnerConfiguration, never()).getRam();
        verify(environment).getRam();
        verify(view).changeSwitcherState(true);
    }

    @Test
    public void configurationShouldBeChanged() throws Exception {
        when(view.getRam()).thenReturn(MB_512);
        when(environment.getScope()).thenReturn(PROJECT);

        presenter.onConfigurationChanged();

        verify(environment).setRam(512);
        verify(environment, times(2)).getScope();
        verify(view).getRam();
        verify(view).setEnableSaveButton(true);
        verify(view).setEnableCancelButton(true);
    }

    @Test
    public void switcherShouldBeOn() throws Exception {
        when(environment.getScope()).thenReturn(SYSTEM);

        presenter.onSwitcherChanged(true);

        verify(templatesContainer).setDefaultEnvironment(environment);
    }

    @Test
    public void switcherShouldBeOff() throws Exception {
        when(environment.getScope()).thenReturn(SYSTEM);

        presenter.onSwitcherChanged(false);

        verify(templatesContainer).setDefaultEnvironment(null);
    }

    @Test
    public void switcherShouldNotBeChanged() throws Exception {
        presenter.onSwitcherChanged(true);

        verify(templatesContainer).setDefaultEnvironment(environment);
    }
}