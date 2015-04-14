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
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import org.eclipse.che.ide.ui.tree.NodeRenderer;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.dom.Elements;

import javax.annotation.Nonnull;

/**
 * The rendered for debug variable node.
 *
 * @author Andrey Plotnikov
 * @@author Dmitry Shnurenko
 */
public class VariableTreeNodeRenderer implements NodeRenderer<DebuggerVariable> {
    public interface Css extends CssResource {
        @ClassName("variable-root")
        String variableRoot();

        @ClassName("variable-icon")
        String variableIcon();

        @ClassName("variable-label")
        String variableLabel();
    }

    public interface Resources extends Tree.Resources {
        @Source("Debug.css")
        Css variableCss();

        @Source("localVariable.png")
        ImageResource localVariable();
    }

    private final Css css;

    public VariableTreeNodeRenderer(@Nonnull Resources res) {
        this.css = res.variableCss();
        this.css.ensureInjected();
    }

    /** {@inheritDoc} */
    @Override
    public Element getNodeKeyTextContainer(@Nonnull SpanElement treeNodeLabel) {
        return (Element)treeNodeLabel.getChildNodes().item(1);
    }

    /** {@inheritDoc} */
    @Override
    public SpanElement renderNodeContents(@Nonnull DebuggerVariable data) {
        SpanElement root = Elements.createSpanElement(css.variableRoot());
        DivElement icon = Elements.createDivElement(css.variableIcon());
        SpanElement label = Elements.createSpanElement(css.variableLabel());
        String content = data.getName() + ": " + data.getValue();
        label.setTextContent(content);

        root.appendChild(icon);
        root.appendChild(label);

        return root;
    }

    /** {@inheritDoc} */
    @Override
    public void updateNodeContents(@Nonnull TreeNodeElement<DebuggerVariable> treeNode) {
        // do nothing
    }
}