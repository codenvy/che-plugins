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

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeSettings;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseNodeTest {
    protected static final String PROJECT_PATH = "/project";
    @Mock
    protected EventBus                  eventBus;
    @Mock
    protected ProjectServiceClient      projectServiceClient;
    @Mock
    protected DtoUnmarshallerFactory    dtoUnmarshallerFactory;
    @Mock
    protected IconRegistry              iconRegistry;
    @Mock
    protected ProjectDescriptor         projectDescriptor;
    @Mock
    protected ProjectNode               projectNode;
    @Mock
    protected MavenProjectTreeStructure treeStructure;
    @Mock
    protected JavaTreeSettings          javaTreeSettings;

    @Before
    public void setUp() {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("maven.source.folder", Collections.singletonList("src/main/java"));
        when(projectDescriptor.getAttributes()).thenReturn(attributes);
        when(projectDescriptor.getPath()).thenReturn(PROJECT_PATH);

        when(projectNode.getData()).thenReturn(projectDescriptor);

        Icon icon = mock(Icon.class);
        when(icon.getSVGImage()).thenReturn(null);
        when(iconRegistry.getIcon(anyString())).thenReturn(icon);

        when(treeStructure.getSettings()).thenReturn(javaTreeSettings);
    }
}
