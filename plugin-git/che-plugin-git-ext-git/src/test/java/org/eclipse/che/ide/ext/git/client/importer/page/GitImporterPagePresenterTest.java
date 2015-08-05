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
package org.eclipse.che.ide.ext.git.client.importer.page;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link GitImporterPagePresenter} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(MockitoJUnitRunner.class)
public class GitImporterPagePresenterTest {
    @Mock
    private Wizard.UpdateDelegate    updateDelegate;
    @Mock
    private GitImporterPageView      view;
    @Mock
    private GitLocalizationConstant  locale;
    @Mock
    private ImportProject            dataObject;
    @Mock
    private ImportSourceDescriptor   importSourceDescriptor;
    @Mock
    private NewProject               newProject;
    @Mock
    private Map<String, String>      parameters;
    @InjectMocks
    private GitImporterPagePresenter presenter;

    @Before
    public void setUp() {
        Source source = mock(Source.class);
        when(importSourceDescriptor.getParameters()).thenReturn(parameters);
        when(source.getProject()).thenReturn(importSourceDescriptor);
        when(dataObject.getSource()).thenReturn(source);
        when(dataObject.getProject()).thenReturn(newProject);

        presenter.setUpdateDelegate(updateDelegate);
        presenter.init(dataObject);
    }

    @Test
    public void testGo() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(eq(view));
        verify(view).setProjectName(anyString());
        verify(view).setProjectDescription(anyString());
        verify(view).setProjectVisibility(anyBoolean());
        verify(view).setProjectUrl(anyString());
        verify(view).setInputsEnableState(eq(true));
        verify(view).focusInUrlInput();
    }

    @Test
    public void projectUrlStartWithWhiteSpaceEnteredTest() {
        String incorrectUrl = " https://github.com/codenvy/ide.git";
        String name = "ide";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(incorrectUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageStartWithWhiteSpace()));
        verify(newProject).setName(eq(name));
        verify(view).setProjectName(name);
        verify(updateDelegate).updateControls();
    }

    @Test
    public void testUrlMatchScpLikeSyntax() {
        // test for url with an alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
        String correctUrl = "host.xz:path/to/repo.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testUrlWithoutUsername() {
        String correctUrl = "git@hostname.com:projectName.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with ssh:// and has host between // and /
        String correctUrl = "ssh://host.com/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndColon() {
        //Check for type uri with host between // and :
        String correctUrl = "ssh://host.com:port/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testGitUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with git:// and has host between // and /
        String correctUrl = "git://host.com/user/repo";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndColon() {
        //Check for type uri with host between @ and :
        String correctUrl = "user@host.com:login/repo";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndSlash() {
        //Check for type uri with host between @ and /
        String correctUrl = "ssh://user@host.com/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void projectUrlWithIncorrectProtocolEnteredTest() {
        String incorrectUrl = "htps://github.com/codenvy/ide.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(incorrectUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageProtocolIncorrect()));
        verify(importSourceDescriptor).setLocation(eq(incorrectUrl));
        verify(view).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameEnteredTest() {
        String correctName = "angularjs";
        when(view.getProjectName()).thenReturn(correctName);

        presenter.projectNameChanged(correctName);

        verify(newProject).setName(eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameWithPointEnteredTest() {
        String correctName = "Test.project..ForCodenvy";
        when(view.getProjectName()).thenReturn(correctName);

        presenter.projectNameChanged(correctName);

        verify(newProject).setName(eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void emptyProjectNameEnteredTest() {
        String emptyName = "";
        when(view.getProjectName()).thenReturn(emptyName);

        presenter.projectNameChanged(emptyName);

        verify(newProject).setName(eq(emptyName));
        verify(updateDelegate).updateControls();
    }

    @Test
    public void incorrectProjectNameEnteredTest() {
        String incorrectName = "angularjs+";
        when(view.getProjectName()).thenReturn(incorrectName);

        presenter.projectNameChanged(incorrectName);

        verify(newProject).setName(eq(incorrectName));
        verify(view).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void projectDescriptionChangedTest() {
        String description = "description";
        presenter.projectDescriptionChanged(description);

        verify(newProject).setDescription(eq(description));
    }

    @Test
    public void projectVisibilityChangedTest() {
        presenter.projectVisibilityChanged(true);

        verify(newProject).setVisibility(eq("public"));
    }

    private void verifyInvocationsForCorrectUrl(String correctUrl) {
        verify(view, never()).showUrlError(anyString());
        verify(importSourceDescriptor).setLocation(eq(correctUrl));
        verify(view).hideUrlError();
        verify(view).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

}
