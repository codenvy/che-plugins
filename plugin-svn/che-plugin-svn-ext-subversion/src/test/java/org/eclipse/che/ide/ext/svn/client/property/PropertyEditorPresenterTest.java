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
package org.eclipse.che.ide.ext.svn.client.property;

import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.property.PropertyEditorPresenter}.
 *
 * @author Vladyslav Zhukovskyi
 */
public class PropertyEditorPresenterTest extends BaseSubversionPresenterTest {

    private PropertyEditorPresenter presenter;

    @Mock
    PropertyEditorView view;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter =
                new PropertyEditorPresenter(appContext, eventBus, rawOutputPresenter, workspaceAgent, projectExplorerPart, view, service,
                                            dtoUnmarshallerFactory, notificationManager, constants);
    }

    @Test
    public void testCopyViewShouldBeShowed() throws Exception {
        presenter.showEditor();

        verify(view).onShow();
    }
}
