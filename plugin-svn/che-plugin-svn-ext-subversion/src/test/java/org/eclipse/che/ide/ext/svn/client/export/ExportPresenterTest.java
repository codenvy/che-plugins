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
package org.eclipse.che.ide.ext.svn.client.export;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.export.ExportPresenter}.
 *
 * @author Vladyslav Zhukovskyi
 */
public class ExportPresenterTest extends BaseSubversionPresenterTest {
    @Captor
    private ArgumentCaptor<AsyncCallback<Array<TreeNode<?>>>> asyncRequestCallbackStatusCaptor;

    private ExportPresenter presenter;

    @Mock
    ExportView exportView;

    @Mock
    DialogFactory dialogFactory;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter = new ExportPresenter(appContext, eventBus, rawOutputPresenter, workspaceAgent, projectExplorerPart, exportView,
                                        notificationManager, constants, null, null);
    }

    @Test
    public void testExportViewShouldBeShowed() throws Exception {
        presenter.showExport(mock(FileNode.class));

        verify(exportView).onShow();
    }
 }
