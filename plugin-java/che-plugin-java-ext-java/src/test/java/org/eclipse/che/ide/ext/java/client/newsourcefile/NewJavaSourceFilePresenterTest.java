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
package org.eclipse.che.ide.ext.java.client.newsourcefile;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.SourceFolderNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.List;

import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.ANNOTATION;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.CLASS;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.ENUM;
import static org.eclipse.che.ide.ext.java.client.newsourcefile.JavaSourceFileType.INTERFACE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link NewJavaSourceFilePresenter} functionality.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class NewJavaSourceFilePresenterTest {
    private static String FILE_NAME            = "TestClass";
    private static String SRC_FOLDER_PATH      = "/project/src/main/java";
    private static String CODENVY_PACKAGE_PATH = "/project/src/main/java/org/eclipse/che";
    private static String PACKAGE_NAME         = "org.eclipse.che";
    //mocks for constructor
    @Mock
    private NewJavaSourceFileView      view;
    @Mock
    private EventBus                   eventBus;
    @Mock
    private SelectionAgent             selectionAgent;
    @Mock
    private ProjectServiceClient       projectServiceClient;
    @Mock
    private DtoUnmarshallerFactory     dtoUnmarshallerFactory;
    @Mock
    private AppContext                 appContext;

    @Mock
    private SourceFolderNode           srcFolder;
    @Mock
    private PackageNode                codenvyPackage;
    @Mock
    private ItemReference              createdFile;
    @Mock
    private Selection                  selection;

    @InjectMocks
    private NewJavaSourceFilePresenter presenter;

    @Before
    public void setUp() {
        when(srcFolder.getPath()).thenReturn(SRC_FOLDER_PATH);
        PackageNode comPackage = mock(PackageNode.class);
        when(codenvyPackage.getParent()).thenReturn((AbstractTreeNode)comPackage);
        when(codenvyPackage.getName()).thenReturn("codenvy");
        when(codenvyPackage.getQualifiedName()).thenReturn("org.eclipse.che");
        when(codenvyPackage.getPath()).thenReturn(CODENVY_PACKAGE_PATH);
        when(comPackage.getParent()).thenReturn((AbstractTreeNode)srcFolder);

        TreeStructure tree = mock(TreeStructure.class);
        CurrentProject currentProject = mock(CurrentProject.class);
        when(currentProject.getCurrentTree()).thenReturn(tree);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncCallback<TreeNode<?>> callback = (AsyncCallback<TreeNode<?>>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, mock(FileNode.class));
                return callback;
            }
        }).when(tree).getNodeByPath(anyString(), (AsyncCallback<TreeNode<?>>)anyObject());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<ItemReference> callback = (AsyncRequestCallback<ItemReference>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(projectServiceClient).createFolder(anyString(), (AsyncRequestCallback<ItemReference>)anyObject());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<ItemReference> callback = (AsyncRequestCallback<ItemReference>)arguments[4];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, createdFile);
                return callback;
            }
        }).when(projectServiceClient)
          .createFile(anyString(), anyString(), anyString(), anyString(), (AsyncRequestCallback<ItemReference>)anyObject());
    }

    @Test
    public void shouldShowDialog() {
        presenter.showDialog();
        verify(view).setTypes(Matchers.<List<JavaSourceFileType>>anyObject());
        verify(view).showDialog();
    }

    @Test
    public void shouldCloseDialogOnCancelClicked() throws Exception {
        presenter.onCancelClicked();
        verify(view).close();
    }

    @Test
    public void shouldShowHintWhenNameIsInvalid() throws Exception {
        when(view.getName()).thenReturn('#' + FILE_NAME);
        presenter.onNameChanged();
        verify(view).showErrorHint(anyString());
    }

    @Test
    public void shouldHideHintWhenNameIsValid() throws Exception {
        when(view.getName()).thenReturn(FILE_NAME);
        presenter.onNameChanged();
        verify(view).hideErrorHint();
    }

    @Test
    public void shouldCreateClassInsideSourceFolder() throws Exception {
        when(selection.getFirstElement()).thenReturn(srcFolder);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "\npublic class " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(CLASS);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(SRC_FOLDER_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                                                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateClassInsidePackage() throws Exception {
        when(selection.getFirstElement()).thenReturn(codenvyPackage);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "package " + PACKAGE_NAME + ";\n\npublic class " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(CLASS);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(CODENVY_PACKAGE_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                                                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateAnnotationInsideSourceFolder() throws Exception {
        when(selection.getFirstElement()).thenReturn(srcFolder);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "\npublic @interface " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(ANNOTATION);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(SRC_FOLDER_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateAnnotationInsidePackage() throws Exception {
        when(selection.getFirstElement()).thenReturn(codenvyPackage);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "package " + PACKAGE_NAME + ";\n\npublic @interface " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(ANNOTATION);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(CODENVY_PACKAGE_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateInterfaceInsideSourceFolder() throws Exception {
        when(selection.getFirstElement()).thenReturn(srcFolder);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "\npublic interface " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(INTERFACE);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(SRC_FOLDER_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                                                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateInterfaceInsidePackage() throws Exception {
        when(selection.getFirstElement()).thenReturn(codenvyPackage);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "package " + PACKAGE_NAME + ";\n\npublic interface " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(INTERFACE);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(CODENVY_PACKAGE_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                                                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateEnumInsideSourceFolder() throws Exception {
        when(selection.getFirstElement()).thenReturn(srcFolder);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "\npublic enum " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(ENUM);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(SRC_FOLDER_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                                                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }

    @Test
    public void shouldCreateEnumInsidePackage() throws Exception {
        when(selection.getFirstElement()).thenReturn(codenvyPackage);
        when(selectionAgent.getSelection()).thenReturn(selection);

        final String fileContent = "package " + PACKAGE_NAME + ";\n\npublic enum " + FILE_NAME + " {\n}\n";

        when(view.getName()).thenReturn(FILE_NAME);
        when(view.getSelectedType()).thenReturn(ENUM);

        presenter.onOkClicked();

        verify(view).close();
        verify(projectServiceClient).createFile(eq(CODENVY_PACKAGE_PATH), eq(FILE_NAME + ".java"), eq(fileContent), anyString(),
                                                Matchers.<AsyncRequestCallback<ItemReference>>anyObject());
        verify(eventBus, times(2)).fireEvent(any(Event.class));
    }
}
