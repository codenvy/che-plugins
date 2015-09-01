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
package org.eclipse.che.ide.ext.svn.client.remove;

import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.add.AddPresenter}.
 */
public class RemovePresenterTest extends BaseSubversionPresenterTest {

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<CLIOutputResponse>> asyncRequestCallbackStatusCaptor;

    private RemovePresenter presenter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter = new RemovePresenter(appContext, dtoUnmarshallerFactory, eventBus, notificationManager,
                                        rawOutputPresenter, constants, service, workspaceAgent, projectExplorerPart);
    }

    @Test
    public void testAddNothingSelected() throws Exception {
        // We cannot test this since the SelectionAgent has a bug where something always appears selected
    }
}