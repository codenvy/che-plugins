/*******************************************************************************
 * Copyright (c) 2014-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.editor.codemirror.client.minimap;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Cursor;
import elemental.css.CSSStyleDeclaration.Position;
import elemental.dom.DocumentFragment;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;
import elemental.html.ClientRect;
import elemental.html.DivElement;
import elemental.util.Mappable;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class MinimapViewImpl implements MinimapView {

    private static final String MARKER_HEIGHT = "3px";

    private static final String DATASET_KEY_LINE  = "line";
    private static final String DATASET_KEY_TITLE = "title";

    /**
     * Minimal size of the minimap to react on clicks.
     */
    private static final int MIN_MINIMAP_SIZE = 10;

    /**
     * The canvas element.
     */
    private final Element visible;

    /**
     * Canvas element for offscreen rendering.
     */
    private final DocumentFragment offscreen;

    /**
     * The action delegate.
     */
    private Delegate delegate;

    /**
     * Tells if the visible element needs to be synchronized with the invisible one.
     */
    private boolean changed;

    /**
     * The click listener on the marks.
     */
    private final EventListener markClickListener;

    public MinimapViewImpl(final Element element) {
        this.visible = element;
        this.offscreen = Browser.getDocument().createDocumentFragment();

        this.visible.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(final Event evt) {
                if (evt instanceof MouseEvent) {
                    final MouseEvent mouseEvt = (MouseEvent)evt;
                    final EventTarget target = mouseEvt.getTarget();
                    if (visible.equals(target)) {
                        handleClick(mouseEvt);
                    }
                }
            }
        }, false);

        this.markClickListener = new EventListener() {
            @Override
            public void handleEvent(final Event evt) {
                if (evt instanceof MouseEvent) {
                    final MouseEvent mouseEvt = (MouseEvent)evt;
                    handleMarkClick(mouseEvt);

                    // don't let the event reach the clickListener on 'visible'
                    mouseEvt.stopPropagation();
                }
            }
        };
    }

    @Override
    public void setDelegate(final Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addMark(final double relativePos, final String style, final int offset, String title) {
        addMark(relativePos, style, offset, null, title);
    }

    @Override
    public void addMark(final double relativePos, final String style, final int offset, final Integer level, final String title) {
        changed();
        final DivElement mark = Browser.getDocument().createDivElement();
        mark.setClassName(style);
        mark.getStyle().setPosition(Position.ABSOLUTE);
        mark.getStyle().setTop(Double.toString(relativePos * 100) + "%");
        mark.getStyle().setHeight(MARKER_HEIGHT); // could be proportional to the document size
        mark.getStyle().setMarginTop("0");
        mark.getStyle().setMarginBottom("0");
        mark.getStyle().setWidth("100%");
        mark.getStyle().setLeft("0");
        mark.getStyle().setCursor(Cursor.POINTER);
        if (level != null) {
            mark.getStyle().setZIndex(level);
        }

        mark.getDataset().setAt(DATASET_KEY_LINE, offset);
        this.offscreen.appendChild(mark);
        if (title != null) {
            mark.setTitle(title);
        }


    }

    @Override
    public void clearMarks() {
        changed();
        emptyNode(this.visible);
        emptyNode(this.offscreen);
    }

    @Override
    public void removeMarks(final int lineStart, final int lineEnd) {
        // naive implementation
        Node current = this.offscreen.getFirstChild();
        while (current != null) {
            final Integer line = getLine(current);
            if (lineStart <= line && lineEnd >= line) {
                this.offscreen.removeChild(current);
                changed();
            }
            current = current.getNextSibling();
        }
    }

    private static void emptyNode(final Node node) {
        while (node.hasChildNodes()) {
            node.removeChild(node.getLastChild());
        }
    }

    private void changed() {
        if (!this.changed) {
            this.changed = true;
            // set up deferred sync
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    sync();
                    MinimapViewImpl.this.changed = false;
                }
            });
        }
    }

    private void sync() {
        final Node copy = this.offscreen.cloneNode(true);
        emptyNode(this.visible);
        this.visible.appendChild(copy);

        Node current = this.visible.getFirstChild();
        while (current != null) {
            current.addEventListener("click", this.markClickListener, false);
            current = current.getNextSibling();
        }
    }

    private void handleClick(final MouseEvent event) {
        if (this.delegate != null) {
            final int clickY = event.getClientY();

            final ClientRect rect = visible.getBoundingClientRect();
            final float top = rect.getTop();
            final float bottom = rect.getBottom();

            final float total = bottom - top;
            if (total < MIN_MINIMAP_SIZE) {
                return;
            }

            if (clickY < top || clickY > bottom) {
                return;
            }

            final float offset = clickY - top;
            final float position = offset / total;

            delegate.handleClick(position);
        }
    }

    private void handleMarkClick(final MouseEvent mouseEvt) {
        if (this.delegate != null) {
            final EventTarget target = mouseEvt.getCurrentTarget();
            final Integer offset = getLine(target);
            if (offset != null) {
                this.delegate.handleMarkClick(offset);
            }
        }
    }

    private static Integer getLine(final EventTarget node) {
        if (node instanceof Element) {
            final Element element = (Element)node;
            final Mappable dataset = element.getDataset();
            final String lineAsString = (String)(dataset.at(DATASET_KEY_LINE));
            try {
                int line = Integer.parseInt(lineAsString);
                return line;
            } catch (final NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
