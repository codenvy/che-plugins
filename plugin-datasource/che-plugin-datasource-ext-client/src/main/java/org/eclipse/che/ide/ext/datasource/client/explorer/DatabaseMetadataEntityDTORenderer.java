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
package org.eclipse.che.ide.ext.datasource.client.explorer;

import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.ui.SVGResource;

import org.eclipse.che.ide.api.parts.PartStackUIResources;

import org.eclipse.che.ide.ext.datasource.shared.ColumnDTO;
import org.eclipse.che.ide.ext.datasource.shared.SchemaDTO;
import org.eclipse.che.ide.ext.datasource.shared.TableDTO;
import org.eclipse.che.ide.ui.tree.NodeRenderer;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.dom.Elements;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

import elemental.dom.Element;
import elemental.html.SpanElement;
import elemental.js.svg.JsSVGSVGElement;

/**
 * Node renderer for the datasource explorer tree.
 */
public class DatabaseMetadataEntityDTORenderer implements NodeRenderer<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> {

    /** The tree CSS resource. */
    private final Css       css;
    private final Resources resources;

    @Inject
    public DatabaseMetadataEntityDTORenderer(final Resources resources) {
        this.css = resources.getCss();
        this.resources = resources;
        resources.getCss().ensureInjected();
    }

    @Override
    public Element getNodeKeyTextContainer(final SpanElement treeNodeLabel) {
        return (Element)treeNodeLabel.getChildNodes().item(1);
    }

    @Override
    public SpanElement renderNodeContents(final DatabaseMetadataEntityDTODataAdapter.EntityTreeNode data) {
        String iconClassName = css.schemaIcon();
        String labelClassName = css.schemaLabel();
        SVGResource iconResource = resources.schema();

        if (data.getData() instanceof TableDTO) {
            iconClassName = css.tableIcon();
            labelClassName = css.tableLabel();
            iconResource = resources.table();
        } else if (data.getData() instanceof ColumnDTO) {
            iconClassName = getColumnIconClassName((ColumnDTO)data.getData());
            labelClassName = css.columnLabel();
            iconResource = resources.column();
        }

        return renderNodeContents(css, data.getData().getName(), iconClassName, true, labelClassName, iconResource);
    }

    private String getColumnIconClassName(final ColumnDTO dto) {
        // will check if primary or foreign key
        if (dto.isPartOfForeignKey()) {
            return css.columnIconFK();
        }
        if (dto.isPartOfPrimaryKey()) {
            return css.columnIconPK();
        }
        return css.columnIcon();
    }

    @Override
    public void updateNodeContents(final TreeNodeElement<DatabaseMetadataEntityDTODataAdapter.EntityTreeNode> treeNode) {
        SVGResource iconResource = null;
        String itemTypeClass = null;
        if (treeNode.getData().getData() instanceof TableDTO) {
            iconResource = resources.table();
            itemTypeClass = css.tableIcon();
        } else if (treeNode.getData().getData() instanceof SchemaDTO) {
            iconResource = resources.schema();
            itemTypeClass = css.schemaIcon();
        } else if (treeNode.getData().getData() instanceof ColumnDTO) {
            iconResource = resources.column();
            itemTypeClass = css.columnIcon();
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
     * @param css the CSS resource for this tree
     * @param contents the node contents
     * @param iconClassName the class name for the icon
     * @param renderIcon true to render the icon
     * @param labelClassName the class name for the abel part
     * @return the HTML element for the treer node
     */
    public static SpanElement renderNodeContents(final Css css, final String contents, final String iconClassName,
                                                 final boolean renderIcon, final String labelClassName,
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
     * @param iconResource the SVGResource that will provide the element
     * @param classNames the CSS classes that will be added to the element
     */
    private static JsSVGSVGElement generateSvgIconElement(final SVGResource iconResource, final String... classNames) {
        final OMSVGSVGElement iconSvg = iconResource.getSvg();
        for (final String className : classNames) {
            iconSvg.addClassNameBaseVal(className);
        }
        final com.google.gwt.dom.client.Element iconElement = iconSvg.getElement();

        final JsSVGSVGElement jsIconElement = iconElement.<JsSVGSVGElement> cast();
        return jsIconElement;
    }

    /**
     * The CSSResource interface for the datasource explorer tree.
     */
    public interface Css extends CssResource {

        /**
         * Returns the CSS class for schema icon.
         * 
         * @return class name
         */
        String schemaIcon();

        /**
         * Returns the CSS class for table icon.
         * 
         * @return class name
         */
        String tableIcon();

        /**
         * Returns the CSS class for column icon.
         * 
         * @return class name
         */
        String columnIcon();

        /**
         * Returns the CSS class for primary key icon.
         * 
         * @return class name
         */
        String columnIconPK();

        /**
         * Returns the CSS class for foreign key icon.
         * 
         * @return class name
         */
        String columnIconFK();

        /**
         * Returns the CSS class for schema label.
         * 
         * @return class name
         */
        String schemaLabel();

        /**
         * Returns the CSS class for table label.
         * 
         * @return class name
         */
        String tableLabel();

        /**
         * Returns the CSS class for column label.
         * 
         * @return class name
         */
        String columnLabel();

        /**
         * Returns the CSS class for tree root.
         * 
         * @return class name
         */
        String root();

        /**
         * Returns the CSS class for tree icons.
         * 
         * @return class name
         */
        String icon();

        /**
         * Returns the CSS class for tree labels.
         * 
         * @return class name
         */
        String label();

    }

    /**
     * The resource interface for the datasource explorer tree.
     */
    public interface Resources extends Tree.Resources, PartStackUIResources {

        /** Returns the CSS resource for the datasource explorer tree. */
        @Source({"DatabaseMetadataEntityDTORenderer.css", "org/eclipse/che/ide/ui/constants.css",
                 "org/eclipse/che/ide/api/ui/style.css"})
        DatabaseMetadataEntityDTORenderer.Css getCss();

        /** Returns the icon for schema nodes. */
        @Source("schema.svg")
        SVGResource schema();

        /** Returns the icon for table nodes. */
        @Source("table.svg")
        SVGResource table();

        /** Returns the icon for column nodes. */
        @Source("column.svg")
        SVGResource column();
    }
}
