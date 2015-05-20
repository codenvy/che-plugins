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
package org.eclipse.che.ide.extension.maven.client.projecttree;

import com.googlecode.gwt.test.Mock;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.part.editor.EditorPartStackPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import com.google.web.bindery.event.shared.EventBus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Iterator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(MockitoJUnitRunner.class)
public class ModuleNodeTest {
    private static final String TEXT      = "some text";
    private static final String SOME_PATH = "/some path/to/module node";

    @Mock
    private TreeNode<?>               parent;
    @Mock
    private ProjectDescriptor         data;
    @Mock
    private MavenProjectTreeStructure treeStructure;
    @Mock
    private EventBus                  eventBus;
    @Mock
    private ProjectServiceClient      projectServiceClient;
    @Mock
    private DtoUnmarshallerFactory    dtoUnmarshallerFactory;
    @Mock
    private IconRegistry              iconRegistry;
    @Mock
    private AppContext                appContext;
    @Mock
    private EditorPartStackPresenter  editorPartStackPresenter;
    @Mock
    private EditorAgent               editorAgent;

    @Mock
    private Icon                           icon;
    @Mock
    private ProjectDescriptor              projectDescriptor;
    @Mock
    private TreeNode.RenameCallback        renameCallback;
    @Mock
    private StringMap<EditorPartPresenter> editors;
    @Mock
    private Array<EditorPartPresenter>     editorPartPresenterArray;
    @Mock
    private Iterable<EditorPartPresenter>  partStackPresenterIterator;
    @Mock
    private Iterator<EditorPartPresenter>  iterator;
    @Mock
    private EditorPartPresenter            editorPartPresenter;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>> argumentCaptor;

    private ModuleNode moduleNode;

    @Before
    public void setUp() {
        when(iconRegistry.getIcon("maven.module")).thenReturn(icon);
        when(projectDescriptor.getPath()).thenReturn(SOME_PATH);
        when(editorAgent.getOpenedEditors()).thenReturn(editors);
        when(editors.getValues()).thenReturn(editorPartPresenterArray);
        when(editorPartPresenterArray.asIterable()).thenReturn(partStackPresenterIterator);
        when(partStackPresenterIterator.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true).thenReturn(false);
        when(iterator.next()).thenReturn(editorPartPresenter);

        moduleNode = new ModuleNode(parent,
                                    data,
                                    treeStructure,
                                    eventBus,
                                    projectServiceClient,
                                    dtoUnmarshallerFactory,
                                    iconRegistry,
                                    appContext,
                                    editorPartStackPresenter,
                                    editorAgent);
        moduleNode.setData(projectDescriptor);
    }

    @Test
    public void nodeShouldBeRenamed() throws Exception {
        moduleNode.rename(TEXT, renameCallback);

        verify(projectServiceClient).rename(eq(SOME_PATH), eq(TEXT), isNull(String.class), argumentCaptor.capture());

        AsyncRequestCallback<Void> asyncRequestCallback = argumentCaptor.getValue();
        Method method = asyncRequestCallback.getClass().getDeclaredMethod("onSuccess", Void.class);
        method.setAccessible(true);
        method.invoke(asyncRequestCallback, (Void)null);

        verify(renameCallback).onRenamed();
        verify(editorAgent).getOpenedEditors();
        verify(editors).getValues();
        verify(editorPartPresenterArray).asIterable();
        verify(editorPartStackPresenter).removePart(editorPartPresenter);
        verify(eventBus, times(2)).fireEvent(any(RefreshProjectTreeEvent.class));
    }

    @Test
    public void nodeShouldNotBeRenamed() throws Exception {
        moduleNode.rename(TEXT, renameCallback);

        verify(projectServiceClient).rename(eq(SOME_PATH), eq(TEXT), isNull(String.class), argumentCaptor.capture());

        AsyncRequestCallback<Void> asyncRequestCallback = argumentCaptor.getValue();
        Method method = asyncRequestCallback.getClass().getDeclaredMethod("onFailure", Throwable.class);
        method.setAccessible(true);
        Throwable caught = mock(Throwable.class);
        method.invoke(asyncRequestCallback, caught);

        verify(renameCallback).onFailure(caught);
    }
}
