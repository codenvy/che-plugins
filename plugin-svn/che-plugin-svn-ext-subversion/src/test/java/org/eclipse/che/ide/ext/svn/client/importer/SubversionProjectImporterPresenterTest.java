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
package org.eclipse.che.ide.ext.svn.client.importer;

import org.eclipse.che.api.project.shared.dto.*;
import org.eclipse.che.ide.api.wizard.Wizard;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.importer.SubversionProjectImporterPresenter;
import org.eclipse.che.ide.ext.svn.client.importer.SubversionProjectImporterView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SubversionProjectImporterPresenter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SubversionProjectImporterPresenterTest {
    @Mock
    private ImportProject                            dataObject;
    @Mock
    private ImportSourceDescriptor                   importSourceDescriptor;
    @Mock
    private NewProject                               newProject;
    @Mock
    private Map<String, String>                      parameters;
    @Mock
    private SubversionProjectImporterView            view;
    @Mock
    private SubversionExtensionLocalizationConstants constants;
    @InjectMocks
    private SubversionProjectImporterPresenter       presenter;
    @Mock
    private Wizard.UpdateDelegate                    updateDelegate;

    /**
     * Setup the tests.
     *
     * @throws Exception if anything goes wrong
     */
    @Before
    public void setUp() throws Exception {
        Source source = mock(Source.class);
        when(importSourceDescriptor.getParameters()).thenReturn(parameters);
        when(source.getProject()).thenReturn(importSourceDescriptor);
        when(dataObject.getSource()).thenReturn(source);
        when(dataObject.getProject()).thenReturn(newProject);
        when(view.getProjectRelativePath()).thenReturn("");

        presenter.setUpdateDelegate(updateDelegate);
        presenter.init(dataObject);
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectNameChanged(String)} with an empty name.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEmptyProjectName() throws Exception {
        final String projectName = "";

        presenter.projectNameChanged(projectName);

        verify(dataObject.getProject()).setName(eq(projectName));
        verify(updateDelegate).updateControls();
        verify(view).showNameError();
        verify(view, never()).hideNameError();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectNameChanged(String)} with an invalid name.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testInvalidProjectName() throws Exception {
        final String projectName = "+subversion+";

        presenter.projectNameChanged(projectName);

        verify(dataObject.getProject()).setName(eq(projectName));
        verify(updateDelegate).updateControls();
        verify(view).showNameError();
        verify(view, never()).hideNameError();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectNameChanged(String)} with a valid name.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testValidProjectName() throws Exception {
        final String projectName = "subversion";

        when(view.getProjectName()).thenReturn(projectName);

        presenter.projectNameChanged(projectName);

        verify(dataObject.getProject()).setName(eq(projectName));
        verify(updateDelegate).updateControls();
        verify(view, never()).showNameError();
        verify(view).hideNameError();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectUrlChanged(String)} with an empty value.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEmptyProjectUrl() throws Exception {
        final String projectUrl = "";

        when(view.getProjectName()).thenReturn(projectUrl);

        presenter.projectUrlChanged(projectUrl);

        verify(view).setProjectName(eq(projectUrl));
        verify(dataObject.getProject()).setName(eq(projectUrl));
        verify(view).showNameError();
        verify(dataObject.getSource().getProject()).setLocation(projectUrl);
        verify(updateDelegate, times(2)).updateControls();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectUrlChanged(String)} with an non-URL value.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNonUrlProjectUrl() throws Exception {
        final String projectUrl = "subversion";

        when(view.getProjectName()).thenReturn(projectUrl);

        presenter.projectUrlChanged(projectUrl);

        verify(view).setProjectName(eq(projectUrl));
        verify(dataObject.getProject()).setName(eq(projectUrl));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(dataObject.getSource().getProject()).setLocation(projectUrl);
        verify(updateDelegate, times(2)).updateControls();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectUrlChanged(String)} with a valid URL value.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testValidProjectUrl() throws Exception {
        final String projectUrl = "https://svn.apache.org/repos/asf/subversion/trunk";
        final String projectName = "trunk";

        when(view.getProjectName()).thenReturn(projectName);

        presenter.projectUrlChanged(projectUrl);

        verify(view).setProjectName(eq(projectName));
        verify(dataObject.getProject()).setName(eq(projectName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(dataObject.getSource().getProject()).setLocation(projectUrl);
        verify(updateDelegate, times(2)).updateControls();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectDescriptionChanged(String)}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testProjectDescription() throws Exception {
        final String description = "Some description.";

        presenter.projectDescriptionChanged(description);

        verify(dataObject.getProject()).setDescription(eq(description));
        verify(view, never()).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#projectVisibilityChanged(boolean)}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testProjectVisibility() throws Exception {
        final boolean visibility = false;

        presenter.projectVisibilityChanged(visibility);

        verify(dataObject.getProject()).setVisibility(eq(SubversionProjectImporterPresenter.PRIVATE_VISIBILITY));
        verify(view, never()).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    /**
     * Test for {@link SubversionProjectImporterPresenter#go(AcceptsOneWidget)}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGo() throws Exception {
        final String importerDescription = "Some description.";
        final AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        final ProjectImporterDescriptor projectImporter = mock(ProjectImporterDescriptor.class);

        when(projectImporter.getDescription()).thenReturn(importerDescription);

        presenter.go(container);

        verify(container).setWidget(eq(view));
        verify(view).setProjectName(anyString());
        verify(view).setProjectDescription(anyString());
        verify(view).setProjectVisibility(anyBoolean());
        verify(view).setProjectUrl(anyString());
        verify(view).setInputsEnableState(eq(true));
        verify(view).focusInUrlInput();
    }

}
