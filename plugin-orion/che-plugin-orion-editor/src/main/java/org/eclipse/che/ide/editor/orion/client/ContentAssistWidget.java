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

package org.eclipse.che.ide.editor.orion.client;

import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.CustomEvent;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;
import elemental.html.SpanElement;
import elemental.html.Window;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.texteditor.UndoableEditor;
import org.eclipse.che.ide.editor.orion.client.jso.OrionKeyModeOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionModelChangedEventOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionPixelPositionOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionTextViewOverlay;
import org.eclipse.che.ide.jseditor.client.codeassist.Completion;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposalExtension;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionResources;
import org.eclipse.che.ide.jseditor.client.events.CompletionRequestEvent;
import org.eclipse.che.ide.jseditor.client.popup.PopupResources;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.util.dom.Elements;
import org.eclipse.che.ide.util.loging.Log;

import javax.inject.Inject;

import static elemental.css.CSSStyleDeclaration.Unit.PX;

/**
 * @author Evgen Vidolob
 */
public class ContentAssistWidget implements EventListener {
    /**
     * Custom event type.
     */
    private static final String CUSTOM_EVT_TYPE_VALIDATE = "itemvalidate";
    private static final String DOCUMENTATION            = "documentation";

    /** The related editor. */
    private final OrionEditorWidget   textEditor;
    private       OrionKeyModeOverlay assistMode;

    /** The main element for the popup. */
    protected final Element popupElement;

    /** The list (ul) element for the popup. */
    protected final Element listElement;

    protected final EventListener  popupListener;
    private final   PopupResources popupResources;

    @Inject
    private CompletionResources completionResources;
    private boolean             state;
    private boolean insert = true;

    /**
     * The previously focused element.
     */
    protected Element    selectedElement;
    private   PopupPanel docWidget;

    private OrionTextViewOverlay.EventHandler<OrionModelChangedEventOverlay>
            handler;

    private Timer callCodeAssist = new Timer() {
        @Override
        public void run() {
            hide();
            textEditor.getDocument().getDocumentHandle().getDocEventBus().fireEvent(new CompletionRequestEvent());
        }
    };
    private boolean showingDoc;

    @AssistedInject
    public ContentAssistWidget(final PopupResources popupResources,
                               @Assisted final OrionEditorWidget textEditor,
                               @Assisted OrionKeyModeOverlay assistMode) {
        this.popupElement = Elements.createDivElement(popupResources.popupStyle().window());
        this.listElement = Elements.createUListElement();
        this.popupElement.appendChild(this.listElement);
        this.popupResources = popupResources;
        docWidget = new PopupPanel(false);
        docWidget.setSize("400px", "205px");
        Style style = docWidget.getElement().getStyle();
        style.setProperty("resize", "both");
        style.setPaddingBottom(0, Style.Unit.PX);
        style.setPaddingTop(3, Style.Unit.PX);
        style.setPaddingLeft(3, Style.Unit.PX);
        style.setPaddingRight(3, Style.Unit.PX);
        this.popupListener = new EventListener() {
            @Override
            public void handleEvent(final Event evt) {
                if (evt instanceof MouseEvent) {
                    final MouseEvent mouseEvent = (MouseEvent)evt;
                    final EventTarget target = mouseEvent.getTarget();
                    if (target instanceof Element) {
                        final Element elementTarget = (Element)target;
                        if (elementTarget.equals(docWidget.getElement()) && docWidget.isShowing()) {
                            return;
                        }
                        if (!ContentAssistWidget.this.popupElement.contains(elementTarget)) {
                            hide();
                            evt.preventDefault();
                        }
                    }
                }
                // else won't happen
            }
        };
        this.textEditor = textEditor;
        this.assistMode = assistMode;

        handler = new OrionTextViewOverlay.EventHandler<OrionModelChangedEventOverlay>() {
            @Override
            public void onEvent(OrionModelChangedEventOverlay event) {
                callCodeAssist.cancel();
                callCodeAssist.schedule(500);
                hide();
            }
        };
    }

    public Element getEmptyDisplay() {
        final Element noProposalMessage = Elements.createLiElement(getItemStyle());
        noProposalMessage.setTextContent("No proposals");
        return noProposalMessage;
    }

    public void validateItem(boolean replace) {
        this.insert = replace;
        selectedElement.dispatchEvent(createValidateEvent(CUSTOM_EVT_TYPE_VALIDATE));
    }

    private native CustomEvent createValidateEvent(String eventType) /*-{
        return new CustomEvent(eventType);
    }-*/;

    private Element createItem(final CompletionProposal proposal) {
        final Element element = Elements.createLiElement(getItemStyle());
        final SpanElement icon = Elements.createSpanElement(completionResources.completionCss().proposalIcon());
        final SpanElement label = Elements.createSpanElement(completionResources.completionCss().proposalLabel());
        final SpanElement group = Elements.createSpanElement(completionResources.completionCss().proposalGroup());
        if (proposal.getIcon() != null && proposal.getIcon().getSVGImage() != null) {
            icon.appendChild((Node)proposal.getIcon().getSVGImage().getElement());
        } else if (proposal.getIcon() != null && proposal.getIcon().getImage() != null) {
            icon.appendChild((Node)proposal.getIcon().getImage().getElement());
        }
        label.setInnerHTML(proposal.getDisplayString());
        element.appendChild(icon);
        element.appendChild(label);
        element.appendChild(group);


        final EventListener validateListener = new EventListener() {
            @Override
            public void handleEvent(final Event evt) {

                CompletionProposal.CompletionCallback callback = new CompletionProposal.CompletionCallback() {
                    @Override
                    public void onCompletion(final Completion completion) {
                        HandlesUndoRedo undoRedo = null;
                        UndoableEditor undoableEditor = ContentAssistWidget.this.textEditor;
                        undoRedo = undoableEditor.getUndoRedo();

                        try {
                            if (undoRedo != null) {
                                undoRedo.beginCompoundChange();
                            }
                            completion.apply(textEditor.getDocument());
                            final LinearRange selection = completion.getSelection(textEditor.getDocument());
                            if (selection != null) {
                                textEditor.getDocument().setSelectedRange(selection, true);
                            }
                        } catch (final Exception e) {
                            Log.error(getClass(), e);
                        } finally {
                            if (undoRedo != null) {
                                undoRedo.endCompoundChange();
                            }
                        }
                    }
                };
                if (proposal instanceof CompletionProposalExtension) {
                    ((CompletionProposalExtension)proposal).getCompletion(insert, callback);
                } else {
                    proposal.getCompletion(callback);
                }
                hide();
            }
        };
        element.addEventListener(Event.DBLCLICK, validateListener, false);
        element.addEventListener(CUSTOM_EVT_TYPE_VALIDATE, validateListener, false);
        element.addEventListener(Event.CLICK, new EventListener() {
            @Override
            public void handleEvent(Event event) {
                selectElement(element);
            }
        }, false);
        element.addEventListener(DOCUMENTATION, new EventListener() {
            @Override
            public void handleEvent(Event event) {
                Widget info = proposal.getAdditionalProposalInfo();
                if (info != null) {
                    showingDoc = true;
                    docWidget.setWidget(info);
                    docWidget.setPopupPosition(popupElement.getOffsetLeft() + popupElement.getOffsetWidth() + 3,
                                               popupElement.getOffsetTop());
                    docWidget.show();
                }
            }
        }, false);

        return element;
    }

    /**
     * Show the widget at the given document position.
     *
     * @param Xcoord
     *         the horizontal pixel position in the document
     * @param Ycoord
     *         the vertical pixel position in the document
     */
    public void show(final float Xcoord, final float Ycoord) {
        textEditor.getTextView().addKeyMode(assistMode);
        if (this.listElement.getChildElementCount() == 0) {
            Element emptyElement = getEmptyDisplay();
            if (emptyElement != null) {
                emptyElement.setTabIndex(1);
                this.listElement.appendChild(emptyElement);
            } else {
                return;
            }
        }

        final Document document = Elements.getDocument();
        document.getBody().appendChild(this.popupElement);
        document.addEventListener(Event.MOUSEDOWN, this.popupListener, false);

        this.popupElement.getStyle().setTop(Ycoord, PX);
        this.popupElement.getStyle().setLeft(Xcoord, PX);
        popupElement.getStyle().setWidth("400px");
        popupElement.getStyle().setHeight("200px");
        // add key event listener on popup
        textEditor.getTextView().setAction("cheContentAssistCancel", new Action() {
            @Override
            public void onAction() {
                hide();
            }
        });

        textEditor.getTextView().setAction("cheContentAssistApply", new Action() {
            @Override
            public void onAction() {
                validateItem(true);
            }
        });

        textEditor.getTextView().setAction("cheContentAssistPreviousProposal", new Action() {
            @Override
            public void onAction() {
                selectPrevious();
            }
        });

        textEditor.getTextView().setAction("cheContentAssistNextProposal", new Action() {
            @Override
            public void onAction() {
                selectNext();
            }
        });

        textEditor.getTextView().setAction("cheContentAssistNextPage", new Action() {
            @Override
            public void onAction() {
                throw new UnsupportedOperationException("cheContentAssistNextPage");
            }
        });

        textEditor.getTextView().setAction("cheContentAssistPreviousPage", new Action() {
            @Override
            public void onAction() {
                throw new UnsupportedOperationException("cheContentAssistPreviousPage");
            }
        });

        textEditor.getTextView().setAction("cheContentAssistEnd", new Action() {
            @Override
            public void onAction() {
                selectLast();
            }
        });

        textEditor.getTextView().setAction("cheContentAssistHome", new Action() {
            @Override
            public void onAction() {
                selectFirst();
            }
        });

        textEditor.getTextView().setAction("cheContentAssistTab", new Action() {
            @Override
            public void onAction() {
                validateItem(false);
            }
        });

        textEditor.getTextView().addEventListener("ModelChanging", handler);
        selectFirst();
        this.listElement.addEventListener(Event.KEYDOWN, this, false);
        state = true;
    }

    private void selectPrevious() {
        Element previousElement = selectedElement.getPreviousElementSibling();
        if (previousElement != null) {
            selectElement(previousElement);
        } else {
            selectLast();
        }
    }

    private void selectNext() {

        Element nextElement = selectedElement.getNextElementSibling();
        if (nextElement != null) {
            selectElement(nextElement);
        } else {
            selectFirst();
        }
    }

    private void selectLast() {
        selectElement(listElement.getLastElementChild());
    }

    private void selectFirst() {
        selectElement(listElement.getFirstElementChild());
    }

    private void selectElement(Element newSelected) {
        if (selectedElement != null) {
            Elements.removeClassName("che-hint-active", selectedElement);
        }
        if (showingDoc && selectedElement != newSelected) {
            newSelected.dispatchEvent(createValidateEvent(DOCUMENTATION));
        }
        selectedElement = newSelected;
        Elements.addClassName("che-hint-active", selectedElement);

        if (selectedElement.getOffsetTop() < this.popupElement.getScrollTop()) {
            selectedElement.scrollIntoView(true);
        } else if ((selectedElement.getOffsetTop() + selectedElement.getOffsetHeight()) >
                   (this.popupElement.getScrollTop() + this.popupElement.getClientHeight())) {
            selectedElement.scrollIntoView(false);
        }

    }

    public void positionAndShow() {
        OrionTextViewOverlay textView = textEditor.getTextView();

        int offset = textView.getCaretOffset();

        OrionPixelPositionOverlay caretLocation = textView.getLocationAtOffset(offset);
        caretLocation.setY(caretLocation.getY() + textView.getLineHeight());
        caretLocation = textView.convert(caretLocation, "document", "page");

        show(caretLocation.getX(), caretLocation.getY());

        final Window window = Elements.getWindow();
        final int viewportWidth = window.getInnerWidth();
        final int viewportHeight = window.getInnerHeight();

        int spaceBelow = viewportHeight - caretLocation.getY();
        if (this.popupElement.getOffsetHeight() > spaceBelow) {
            // Check if div is too large to fit above
            int spaceAbove = caretLocation.getY() - textView.getLineHeight();
            if (this.popupElement.getOffsetHeight() > spaceAbove) {
                // Squeeze the div into the larger area
                if (spaceBelow > spaceAbove) {
                    this.popupElement.getStyle().setProperty("maxHeight", spaceBelow + "px");
                } else {
                    this.popupElement.getStyle().setProperty("maxHeight", spaceAbove + "px");
                    this.popupElement.getStyle().setTop("0");
                }
            } else {
                // Put the div above the line
                this.popupElement.getStyle()
                                 .setTop((caretLocation.getY() - this.popupElement.getOffsetHeight() - textView.getLineHeight()) + "px");
                this.popupElement.getStyle().setProperty("maxHeight", spaceAbove + "px");
            }
        } else {
            this.popupElement.getStyle().setProperty("maxHeight", spaceBelow + "px");
        }

        if (caretLocation.getX() + this.popupElement.getOffsetWidth() > viewportWidth) {
            int leftSide = viewportWidth - this.popupElement.getOffsetWidth();
            if (leftSide < 0) {
                leftSide = 0;
            }
            this.popupElement.getStyle().setLeft(leftSide + "px");
            this.popupElement.getStyle().setProperty("maxWidth", (viewportWidth - leftSide) + "px");
        } else {
            this.popupElement.getStyle().setProperty("maxWidth", viewportWidth + caretLocation.getX() + "px");
        }
    }

    /** Remove all items. */
    public void clear() {
        Node lastChild = this.listElement.getLastChild();
        while (lastChild != null) {
            this.listElement.removeChild(lastChild);
            lastChild = this.listElement.getLastChild();
        }
        docWidget.hide();
    }

    /** Returns the style to add to all items. */
    protected String getItemStyle() {
        return this.popupResources.popupStyle().item();
    }

    /**
     * Add an item in the popup view.
     *
     * @param itemModel
     *         the data for the item
     */
    public void addItem(final CompletionProposal itemModel) {
        if (itemModel == null) {
            return;
        }
        final Element itemElement = createItem(itemModel);
        if (itemElement != null) {
            // makes the element focusable
            itemElement.setTabIndex(1);
            this.listElement.appendChild(itemElement);
        }
    }

    /** Hide the popup. */
    public void hide() {
        if (showingDoc) {
            docWidget.hide();
            showingDoc = false;
            return;
        }
        state = false;
        textEditor.getTextView().removeKeyMode(assistMode);
        textEditor.getTextView().removeEventListener("ModelChanging", handler, false);

        // remove the keyboard listener
        this.listElement.removeEventListener(Event.KEYDOWN, this, false);

        // remove the element from dom
        final Document document = Elements.getDocument();
        final Node parent = this.popupElement.getParentNode();
        if (parent != null) {
            parent.removeChild(this.popupElement);
        }
        showingDoc = false;
        docWidget.hide();
        // remove the mouse listener
        document.removeEventListener(Event.MOUSEDOWN, this.popupListener);
    }

    @Override
    public void handleEvent(Event evt) {
        if (evt instanceof KeyboardEvent) {
            final KeyboardEvent keyEvent = (KeyboardEvent)evt;
            switch (keyEvent.getKeyCode()) {
                case KeyCodes.KEY_ESCAPE:
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            hide();
                        }
                    });
                    break;
                case KeyCodes.KEY_DOWN:
                    selectNext();
                    evt.preventDefault();
                    break;
                case KeyCodes.KEY_UP:
                    selectPrevious();
                    evt.preventDefault();
                    break;
                case KeyCodes.KEY_HOME:
                    selectFirst();
                    break;
                case KeyCodes.KEY_END:
                    selectLast();
                    break;
                case KeyCodes.KEY_ENTER:
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    validateItem(true);
                    break;
                case KeyCodes.KEY_TAB:
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    validateItem(false);
                    break;
                default:
            }
        }
    }

    public boolean isActive() {
        return state;
    }

    public void showCompletionInfo() {
        if (state && selectedElement != null) {
            selectedElement.dispatchEvent(createValidateEvent(DOCUMENTATION));
        }
    }
}
