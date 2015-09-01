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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class DockerFileTest {

    private static final String SOME_TEXT = "someText";

    @Mock
    private EventBus               eventBus;
    @Mock
    private ProjectServiceClient   serviceClient;
    @Mock
    private DtoUnmarshallerFactory unmarshallerFactory;
    @Mock
    private ItemReference          data;
    @Mock
    private TreeStructure          tree;
    @Mock
    private AsyncCallback<String>  callback;

    @InjectMocks
    private DockerFile dockerFile;

    @Test
    public void dockerStatusShouldBeReturned() throws Exception {
        assertThat(dockerFile.isReadOnly(), is(true));
    }

    @Test
    public void contentShouldBeReturned() throws Exception {
        Link link1 = mock(Link.class);
        Link link2 = mock(Link.class);

        when(data.getLinks()).thenReturn(Arrays.asList(link2, link1));
        when(link1.getRel()).thenReturn(DockerFile.GET_CONTENT);
        when(link1.getHref()).thenReturn(SOME_TEXT);
        when(link2.getRel()).thenReturn(SOME_TEXT);

        dockerFile.getContent();
    }

}