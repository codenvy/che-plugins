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
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorInitException;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.ext.runner.client.constants.TimeInterval;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileEditorInput;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.eclipse.che.ide.ext.runner.client.constants.TimeInterval.ONE_SEC;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_512;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesRunnerPanelTest {

    private static final String TEXT = "some text";

    //mocks for constructors
    @Mock
    private PropertiesPanelView view;
    @Mock
    private EditorRegistry      editorRegistry;
    @Mock
    private FileTypeRegistry    fileTypeRegistry;
    @Mock
    private DockerFileFactory   dockerFileFactory;
    @Mock
    private AppContext          appContext;
    @Mock
    private TimerFactory        timerFactory;
    @Mock
    private Runner              runner;

    @Mock
    private CurrentProject      currentProject;
    @Mock
    private Timer               timer;
    @Mock
    private ProjectDescriptor   descriptor;
    @Mock
    private EditorProvider      editorProvider;
    @Mock
    private EditorPartPresenter editor;
    @Mock
    private DockerFile          file;

    @Captor
    private ArgumentCaptor<TimerFactory.TimerCallBack> timerCaptor;

    @Before
    public void setUp() {
        when(timerFactory.newInstance(any(TimerFactory.TimerCallBack.class))).thenReturn(timer);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(editorRegistry.getEditor(isNull(FileType.class))).thenReturn(editorProvider);
        when(editorProvider.getEditor()).thenReturn(editor);
        when(dockerFileFactory.newInstance(TEXT)).thenReturn(file);

        new PropertiesRunnerPanel(view,
                                  editorRegistry,
                                  fileTypeRegistry,
                                  dockerFileFactory,
                                  appContext,
                                  timerFactory,
                                  runner);

        when(runner.getTitle()).thenReturn(TEXT);
        when(runner.getRAM()).thenReturn(MB_512.getValue());
        when(runner.getScope()).thenReturn(Scope.SYSTEM);
        when(runner.getType()).thenReturn(TEXT);
        when(runner.getTitle()).thenReturn(TEXT);
    }

    @Test
    public void prepareActionShouldBePerformedWhenDockerUrlIsNotNull() throws EditorInitException {
        when(runner.getDockerUrl()).thenReturn(TEXT);
        verify(timerFactory).newInstance(timerCaptor.capture());

        timerCaptor.getValue().onRun();

        verify(runner).getDockerUrl();
        verify(timer).schedule(TimeInterval.ONE_SEC.getValue());

        verify(timer).cancel();
        verify(dockerFileFactory).newInstance(TEXT);

        verify(fileTypeRegistry).getFileTypeByFile(file);
        verify(editorRegistry).getEditor(isNull(FileType.class));
        verify(editorProvider).getEditor();

        verify(editor).addPropertyListener(any(PropertyListener.class));
        verify(editor).init(any(DockerFileEditorInput.class));

        verify(runner, times(2)).getRAM();
        verify(view).selectMemory(MB_512);

        verify(timer).schedule(ONE_SEC.getValue());

        verify(view).setEnableNameProperty(false);
        verify(view).setEnableRamProperty(false);
        verify(view).setEnableBootProperty(false);
        verify(view).setEnableShutdownProperty(false);
        verify(view).setEnableScopeProperty(false);

        verify(view).setVisibleSaveButton(false);
        verify(view).setVisibleDeleteButton(false);
        verify(view).setVisibleCancelButton(false);
        verify(view).hideSwitcher();
    }

    @Test
    public void prepareActionShouldBePerformedWhenDockerUrlIsNull() throws EditorInitException {
        verify(timerFactory).newInstance(timerCaptor.capture());

        timerCaptor.getValue().onRun();

        verify(runner).getDockerUrl();
        verify(timer, times(2)).schedule(ONE_SEC.getValue());

        verify(view).setEnableNameProperty(false);
        verify(view).setEnableRamProperty(false);
        verify(view).setEnableBootProperty(false);
        verify(view).setEnableShutdownProperty(false);
        verify(view).setEnableScopeProperty(false);

        verify(view).setVisibleSaveButton(false);
        verify(view).setVisibleDeleteButton(false);
        verify(view).setVisibleCancelButton(false);

        verify(runner).getRAM();
        verify(view).selectMemory(any(RAM.class));
        verify(view).hideSwitcher();
    }
}