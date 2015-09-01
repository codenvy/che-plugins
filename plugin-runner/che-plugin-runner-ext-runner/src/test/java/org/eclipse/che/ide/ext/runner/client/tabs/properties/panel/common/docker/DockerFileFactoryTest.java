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

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile.GET_CONTENT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory.NAME;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory.PATH;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory.TYPE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class DockerFileFactoryTest {

    private static final String SOME_TEXT = "someText";

    @Captor
    private ArgumentCaptor<List<Link>> linkListCaptor;

    //constructor mocks
    @Mock
    private EventBus               eventBus;
    @Mock
    private ProjectServiceClient   projectServiceClient;
    @Mock
    private DtoUnmarshallerFactory dtoUnmarshallerFactory;
    @Mock
    private DtoFactory             dtoFactory;
    @Mock
    private AppContext             appContext;

    //additional mocks
    @Mock
    private CurrentProject currentProject;
    @Mock
    private Link           link;
    @Mock
    private ItemReference  itemReference;

    @InjectMocks
    private DockerFileFactory factory;

    @Before
    public void setUp() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        when(dtoFactory.createDto(Link.class)).thenReturn(link);
        when(link.withHref(SOME_TEXT)).thenReturn(link);
        when(link.withRel(GET_CONTENT)).thenReturn(link);

        when(dtoFactory.createDto(ItemReference.class)).thenReturn(itemReference);
        when(itemReference.withName(NAME)).thenReturn(itemReference);
        when(itemReference.withPath(PATH)).thenReturn(itemReference);
        when(itemReference.withMediaType(TYPE)).thenReturn(itemReference);
        when(itemReference.withLinks(Matchers.<List<Link>>anyObject())).thenReturn(itemReference);
    }

    @Test(expected = IllegalStateException.class)
    public void illegalStateExceptionShouldBeThrownWhenCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        factory.newInstance(SOME_TEXT);
    }

    @Test
    public void dockerFileShouldBeCreated() throws Exception {
        VirtualFile fileNode = factory.newInstance(SOME_TEXT);

        verify(dtoFactory).createDto(Link.class);
        verify(link).withHref(SOME_TEXT);
        verify(link).withRel(GET_CONTENT);


        verify(dtoFactory).createDto(ItemReference.class);
        verify(itemReference).withName(NAME);
        verify(itemReference).withPath(PATH);
        verify(itemReference).withMediaType(TYPE);

        verify(itemReference).withLinks(linkListCaptor.capture());

        List<Link> links = linkListCaptor.getValue();

        assertThat(links.get(0), equalTo(link));

        assertThat(fileNode, notNullValue());
        assertThat(fileNode, instanceOf(DockerFile.class));
    }
}