/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.extension.machine.client.processes;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.SpanElement;

import com.google.inject.Inject;

import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.ui.Tooltip;
import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;
import org.eclipse.che.ide.ui.tree.NodeRenderer;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.dom.Elements;
import org.vectomatic.dom.svg.ui.SVGImage;

/**
 * Renderer for {@ProcessTreeNode} UI presentation.
 *
 * @author Anna Shumilova
 * @author Roman Nikitenko
 */
public class ProcessTreeRenderer implements NodeRenderer<ProcessTreeNode> {

    private final MachineResources            resources;
    private final MachineLocalizationConstant locale;
    private final PartStackUIResources        partStackUIResources;
    private       AddTerminalClickHandler     addTerminalClickHandler;
    private       StopProcessHandler          stopProcessHandler;

    @Inject
    public ProcessTreeRenderer(MachineResources resources, MachineLocalizationConstant locale,
                               PartStackUIResources partStackUIResources) {
        this.resources = resources;
        this.locale = locale;
        this.partStackUIResources = partStackUIResources;
    }

    @Override
    public Element getNodeKeyTextContainer(SpanElement treeNodeLabel) {
        return (Element)treeNodeLabel.getChildNodes().item(1);
    }

    @Override
    public SpanElement renderNodeContents(ProcessTreeNode node) {
        ProcessTreeNode.ProcessNodeType type = node.getType();
        switch (type) {
            case MACHINE_NODE:
                return createMachineElement((MachineDto)node.getData());
            case COMMAND_NODE:
                return createCommandElement(node);
            case TERMINAL_NODE:
                return createTerminalElement(node);
            default:
                return Elements.createSpanElement();
        }
    }


    private SpanElement createMachineElement(final MachineDto machine) {
        SpanElement root = Elements.createSpanElement();
        if (machine.isDev()) {
            SpanElement devLabel = Elements.createSpanElement(resources.getCss().devMachineLabel());
            devLabel.setTextContent(locale.viewProcessesDevTitle());
            root.appendChild(devLabel);
        }
        Element nameElement = Elements.createSpanElement();
        nameElement.setTextContent(machine.getName());

        SpanElement newTerminalButton = Elements.createSpanElement(resources.getCss().processButton());
        newTerminalButton.setTextContent("+");
        root.appendChild(newTerminalButton);

        Tooltip.create((elemental.dom.Element) newTerminalButton,
                BOTTOM,
                MIDDLE,
                locale.viewNewTerminalTooltip());


        Element statusElement = Elements.createSpanElement(resources.getCss().machineStatus());
        root.appendChild(statusElement);

        Tooltip.create((elemental.dom.Element)statusElement,
                BOTTOM,
                MIDDLE,
                locale.viewMachineRunningTooltip());

        newTerminalButton.addEventListener(Event.CLICK, new EventListener() {
            @Override
            public void handleEvent(Event event) {
                if (addTerminalClickHandler != null) {
                    addTerminalClickHandler.onAddTerminalClick(machine.getId());
                }
            }
        }, true);


        root.appendChild(nameElement);

        return root;
    }

    private SpanElement createCommandElement(ProcessTreeNode node) {
        SpanElement root = Elements.createSpanElement();
        root.appendChild(createCloseElement(node));
        root.appendChild(createStopProcessElement(node));

        SpanElement iconElement = Elements.createSpanElement(resources.getCss().processIcon());
        SVGImage icon = new SVGImage(resources.output());
        iconElement.appendChild((Node)icon.getElement());
        iconElement.setClassName(resources.getCss().processIcon());
        root.appendChild(iconElement);

        Element nameElement = Elements.createSpanElement();
        nameElement.setTextContent(node.getName());
        root.appendChild(nameElement);

        return root;
    }

    private SpanElement createTerminalElement(ProcessTreeNode node) {
        SpanElement root = Elements.createSpanElement();
        root.appendChild(createCloseElement(node));

        SpanElement iconElement = Elements.createSpanElement();
        SVGImage icon = new SVGImage(resources.terminal());
        iconElement.appendChild((Node)icon.getElement());
        iconElement.setClassName(resources.getCss().processIcon());
        root.appendChild(iconElement);

        Element nameElement = Elements.createSpanElement();
        nameElement.setTextContent(node.getName());
        root.appendChild(nameElement);
        return root;
    }

    private SpanElement createCloseElement(final ProcessTreeNode node) {
        SpanElement closeButton = Elements.createSpanElement();
        SVGImage icon = new SVGImage(partStackUIResources.closeIcon());
        icon.setClassNameBaseVal(resources.getCss().processesPanelCloseButtonForProcess());
        closeButton.appendChild((Node) icon.getElement());
        Tooltip.create((elemental.dom.Element)closeButton,
                BOTTOM,
                MIDDLE,
                locale.viewCloseProcessOutputTooltip());

        closeButton.addEventListener(Event.CLICK, new EventListener() {
            @Override
            public void handleEvent(Event event) {
                if (stopProcessHandler != null) {
                    stopProcessHandler.onCloseProcessOutputClick(node);
                }
            }
        }, true);
        return closeButton;
    }

    private SpanElement createStopProcessElement(final ProcessTreeNode node) {
        SpanElement stopProcessButton = Elements.createSpanElement(resources.getCss().processesPanelStopButtonForProcess());
        Tooltip.create((elemental.dom.Element) stopProcessButton,
                BOTTOM,
                MIDDLE,
                locale.viewStropProcessTooltip());

        stopProcessButton.addEventListener(Event.CLICK, new EventListener() {
            @Override
            public void handleEvent(Event event) {
                if (stopProcessHandler != null) {
                    stopProcessHandler.onStopProcessClick(node);
                }
            }
        }, true);
        return stopProcessButton;
    }


    @Override
    public void updateNodeContents(TreeNodeElement<ProcessTreeNode> treeNode) {
    }

    public void setAddTerminalClickHandler(AddTerminalClickHandler addTerminalClickHandler) {
        this.addTerminalClickHandler = addTerminalClickHandler;
    }

    public void setStopProcessHandler(StopProcessHandler stopProcessHandler) {
        this.stopProcessHandler = stopProcessHandler;
    }
}
