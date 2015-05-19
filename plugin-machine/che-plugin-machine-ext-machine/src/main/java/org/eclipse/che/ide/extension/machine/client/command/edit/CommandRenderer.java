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
package org.eclipse.che.ide.extension.machine.client.command.edit;

import elemental.dom.Element;
import elemental.html.SpanElement;
import elemental.js.svg.JsSVGSVGElement;

import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.ui.tree.NodeRenderer;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.dom.Elements;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Node renderer for the commands tree.
 *
 * @author Artem Zatsarynnyy
 */
public class CommandRenderer implements NodeRenderer<CommandDataAdapter.CommandTreeNode> {

    /** The tree CSS resource. */
    private final Css       css;

    @Inject
    public CommandRenderer(final Resources resources) {
        this.css = resources.getCss();

        resources.getCss().ensureInjected();
    }

    @Override
    public Element getNodeKeyTextContainer(final SpanElement treeNodeLabel) {
        return (Element)treeNodeLabel.getChildNodes().item(1);
    }

    @Override
    public SpanElement renderNodeContents(final CommandDataAdapter.CommandTreeNode data) {
        String iconClassName = null;
        String labelClassName = null;
        SVGResource iconResource = null;

        if (data.getData() instanceof CommandType) {
            final CommandType type = (CommandType)data.getData();

            iconClassName = css.commandTypeIcon();
            labelClassName = css.commandTypeLabel();
            iconResource = type.getIcon();
        } else if (data.getData() instanceof CommandConfiguration) {
            final CommandConfiguration conf = (CommandConfiguration)data.getData();

            iconClassName = css.commandConfigurationIcon();
            labelClassName = css.commandConfigurationLabel();
            iconResource = conf.getType().getIcon();
        }

        return renderNodeContents(css, data.getName(), iconClassName, true, labelClassName, iconResource);
    }

    @Override
    public void updateNodeContents(final TreeNodeElement<CommandDataAdapter.CommandTreeNode> treeNode) {
        SVGResource iconResource = null;
        String itemTypeClass = null;
        if (treeNode.getData() instanceof CommandType) {
            final CommandType type = (CommandType)treeNode.getData();

            iconResource = type.getIcon();
            itemTypeClass = css.commandTypeIcon();
        } else if (treeNode.getData() instanceof CommandConfiguration) {
            final CommandConfiguration conf = (CommandConfiguration)treeNode.getData();

            iconResource = conf.getType().getIcon();
            itemTypeClass = css.commandConfigurationIcon();
        }

        if (iconResource != null) {
            final JsSVGSVGElement jsIconElement = generateSvgIconElement(iconResource, css.icon(), itemTypeClass);
            final Element icon = treeNode.getNodeLabel().getFirstElementChild();
            treeNode.getNodeLabel().replaceChild(jsIconElement, icon);
        }
    }

    /**
     * Renders the tree node.
     *
     * @param css
     *         the CSS resource for this tree
     * @param contents
     *         the node contents
     * @param iconClassName
     *         the class name for the icon
     * @param renderIcon
     *         true to render the icon
     * @param labelClassName
     *         the class name for the label part
     * @param iconResource
     *         the icon for the node
     * @return the HTML element for the tree node
     */
    private static SpanElement renderNodeContents(final Css css,
                                                 final String contents,
                                                 final String iconClassName,
                                                 final boolean renderIcon,
                                                 final String labelClassName,
                                                 final SVGResource iconResource) {
        SpanElement root = Elements.createSpanElement(css.root());
        if (renderIcon) {
            JsSVGSVGElement jsIconElement = generateSvgIconElement(iconResource, css.icon(), iconClassName);
            root.appendChild(jsIconElement);
        }

        final Element label;
        label = Elements.createSpanElement(css.label(), labelClassName);
        label.setTextContent(contents);
        root.appendChild(label);

        return root;
    }

    /**
     * Configure the SVG icon element for the node.
     *
     * @param iconResource
     *         the SVGResource that will provide the element
     * @param classNames
     *         the CSS classes that will be added to the element
     */
    private static JsSVGSVGElement generateSvgIconElement(final SVGResource iconResource, final String... classNames) {
        final OMSVGSVGElement iconSvg = iconResource.getSvg();
        for (final String className : classNames) {
            iconSvg.addClassNameBaseVal(className);
        }
        final com.google.gwt.dom.client.Element iconElement = iconSvg.getElement();

        return iconElement.cast();
    }

    /** The CSSResource interface for the command tree. */
    public interface Css extends CssResource {

        /**
         * Returns the CSS class for command type icon.
         *
         * @return CSS class name
         */
        String commandTypeIcon();

        /**
         * Returns the CSS class for command configuration icon.
         *
         * @return CSS class name
         */
        String commandConfigurationIcon();

        /**
         * Returns the CSS class for command type label.
         *
         * @return CSS class name
         */
        String commandTypeLabel();

        /**
         * Returns the CSS class for command configuration label.
         *
         * @return CSS class name
         */
        String commandConfigurationLabel();

        /**
         * Returns the CSS class for tree root.
         *
         * @return CSS class name
         */
        String root();

        /**
         * Returns the CSS class for tree icons.
         *
         * @return CSS class name
         */
        String icon();

        /**
         * Returns the CSS class for tree labels.
         *
         * @return CSS class name
         */
        String label();
    }

    /** The resource interface for the commands tree. */
    public interface Resources extends Tree.Resources, PartStackUIResources {

        /** Returns the CSS resource for the commands tree. */
        @Source({
                "CommandRenderer.css", "org/eclipse/che/ide/ui/constants.css", "org/eclipse/che/ide/api/ui/style.css"})
        CommandRenderer.Css getCss();
    }
}
