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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel;

import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;

import static org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachineTreeNode.ROOT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class MachineTreeNodeTest {

    private final static String SOME_TEXT = "someText";

    @Mock
    private MachineTreeNode                  parent;
    @Mock
    private Machine                          data;
    @Mock
    private Collection<MachineTreeNode>      children;
    @Mock
    private TreeNodeElement<MachineTreeNode> treeNodeElement;

    private MachineTreeNode treeNode;

    @Before
    public void setUp() {
        when(data.getId()).thenReturn(SOME_TEXT);
        when(data.getDisplayName()).thenReturn(SOME_TEXT);
    }

    @Test
    public void constructorShouldBeVerifiedWhenWeCreateNodeWithMachineData() {
        treeNode = new MachineTreeNode(parent, data, children);

        verify(data).getId();
        verify(data).getDisplayName();

        assertThat(treeNode.getId(), equalTo(SOME_TEXT));
        assertThat(treeNode.getName(), equalTo(SOME_TEXT));
    }

    @Test
    public void constructorShouldBeVerifiedWhenWeCreateNodeWithNoMachineData() {
        treeNode = new MachineTreeNode(parent, SOME_TEXT, children);

        verify(data, never()).getId();
        verify(data, never()).getDisplayName();

        assertThat(treeNode.getId(), equalTo(ROOT));
        assertThat(treeNode.getName(), equalTo(ROOT));
    }

    @Test
    public void nodeParametersShouldBeReturned() {
        treeNode = new MachineTreeNode(parent, data, children);
        treeNode.setTreeNodeElement(treeNodeElement);

        assertThat(treeNode.getId(), equalTo(SOME_TEXT));
        assertThat(treeNode.getName(), equalTo(SOME_TEXT));
        assertThat(treeNode.getParent(), sameInstance(parent));
        assertThat(treeNode.getData().equals(data), is(true));
        assertThat(treeNode.getChildren(), sameInstance(children));
        assertThat(treeNode.getTreeNodeElement(), sameInstance(treeNodeElement));
    }
}