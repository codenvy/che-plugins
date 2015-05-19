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
package org.eclipse.che.ide.editor.codemirror.client;

import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.NodeList;
import elemental.html.ClientRect;
import elemental.html.SpanElement;
import elemental.js.dom.JsElement;
import elemental.js.util.JsMapFromStringTo;
import elemental.util.Timer;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;

import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMEditorOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMKeymapOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMPositionOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CodeMirrorOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.EventHandlers;
import org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMCompletionObjectOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintApplyOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintApplyOverlay.HintApplyFunction;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintCallback;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintFunctionOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintFunctionOverlay.AsyncHintFunction;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintFunctionOverlay.HintFunction;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintOptionsOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMHintResultsOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMRenderFunctionOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.hints.CMRenderFunctionOverlay.RenderFunction;
import org.eclipse.che.ide.jseditor.client.codeassist.AdditionalInfoCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.Completion;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal.CompletionCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposalExtension;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionReadyCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionResources.CompletionCss;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionsSource;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.util.dom.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Component that handles the showCompletion(...) operations.
 */
public final class ShowCompletion {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(ShowCompletion.class.getName());

    /** Property name where the additional info is stored in the completion object. */
    private static final String PROP_ADDITIONAL_INFO = "additionalInfo";

    /** Marker class name for additional info popups. */
    private static final String ADDITIONAL_INFO_MARKER = "completion-additional-info-do-not-use-your-element-will-be-removed-anytime";

    private final CompletionCss completionCss;

    private final CodeMirrorEditorWidget editorWidget;
    private boolean insert = true;

    public ShowCompletion(final CodeMirrorEditorWidget editorWidget,
                          final CompletionCss css) {
        this.completionCss = css;
        this.editorWidget = editorWidget;
    }

    public void showCompletionProposals(final List<CompletionProposal> proposals,
                                        final AdditionalInfoCallback additionalInfoCallback) {
        if (!editorWidget.getEditorOverlay().hasShowHint() || proposals == null || proposals.isEmpty()) {
            // no support for hints or no proposals
            return;
        }

        final CMHintOptionsOverlay hintOptions = createDefaultHintOptions();

        final CMHintFunctionOverlay hintFunction = CMHintFunctionOverlay.createFromHintFunction(new HintFunction() {

            @Override
            public CMHintResultsOverlay getHints(final CMEditorOverlay editor,
                                                 final CMHintOptionsOverlay options) {
                final CMHintResultsOverlay result = CMHintResultsOverlay.create();
                final JsArrayMixed list = result.getList();
                for (final CompletionProposal proposal : proposals) {

                    final CMHintApplyOverlay hintApply = createApplyHintFunc(proposal);
                    final CMRenderFunctionOverlay renderFunc = createRenderHintFunc(proposal,
                                                                                    additionalInfoCallback);

                    final CMCompletionObjectOverlay completionObject = JavaScriptObject.createObject().cast();

                    completionObject.setHint(hintApply);
                    completionObject.setRender(renderFunc);
                    setAdditionalInfo(completionObject, proposal.getAdditionalProposalInfo());

                    list.push(completionObject);
                }
                result.setFrom(editor.getDoc().getCursor());
                setupShowAdditionalInfo(result, additionalInfoCallback);
                return result;
            }


        });
        hintOptions.setHint(hintFunction);

        editorWidget.getEditorOverlay().showHint(hintOptions);
    }

    private CMHintOptionsOverlay createDefaultHintOptions() {
        final CMHintOptionsOverlay hintOptions = CMHintOptionsOverlay.create();
        hintOptions.setCloseOnUnfocus(false); // default=true
        hintOptions.setAlignWithWord(true); //default
        hintOptions.setCompleteSingle(false);
        CMKeymapOverlay keymapOverlay = CMKeymapOverlay.create();
        addTabKey(keymapOverlay, this);
        hintOptions.setExtraKeys(keymapOverlay);
        return hintOptions;
    }

    private void setCompletionInsert(boolean insert) {
        ShowCompletion.this.insert = insert;
    }


    private native void addTabKey(CMKeymapOverlay keymap, ShowCompletion show) /*-{
        keymap["Tab"] = function (cm, handler) {
            show.@org.eclipse.che.ide.editor.codemirror.client.ShowCompletion::setCompletionInsert(*)(false);
            handler.pick();
        }
    }-*/;

    /* async version */
    public void showCompletionProposals(final CompletionsSource completionsSource,
                                        final AdditionalInfoCallback additionalInfoCallback) {
        if (!editorWidget.getEditorOverlay().hasShowHint()) {
            // no support for hints
            return;
        }
        if (completionsSource == null) {
            showCompletionProposals();
            return;
        }

        final CMHintOptionsOverlay hintOptions = createDefaultHintOptions();
        final CMHintFunctionOverlay hintFunction = CMHintFunctionOverlay.createFromAsyncHintFunction(new AsyncHintFunction() {

            @Override
            public void getHints(final CMEditorOverlay editor,
                                 final CMHintCallback callback,
                                 final CMHintOptionsOverlay options) {
                completionsSource.computeCompletions(new CompletionReadyCallback() {

                    @Override
                    public void onCompletionReady(final List<CompletionProposal> proposals) {
                        final CMHintResultsOverlay result = CMHintResultsOverlay.create();
                        final JsArrayMixed list = result.getList();
                        for (final CompletionProposal proposal : proposals) {

                            final CMHintApplyOverlay hintApply = createApplyHintFunc(proposal);
                            final CMRenderFunctionOverlay renderFunc = createRenderHintFunc(proposal,
                                                                                            additionalInfoCallback);

                            final CMCompletionObjectOverlay completionObject = JavaScriptObject.createObject().cast();

                            completionObject.setHint(hintApply);
                            completionObject.setRender(renderFunc);
                            setAdditionalInfo(completionObject, proposal.getAdditionalProposalInfo());

                            list.push(completionObject);
                        }
                        result.setFrom(editor.getDoc().getCursor());
                        setupShowAdditionalInfo(result, additionalInfoCallback);
                        callback.call(result);
                    }
                });
            }
        });

        // set the async hint function and trigger the delayed display of hints
        hintOptions.setHint(hintFunction);
        editorWidget.getEditorOverlay().showHint(hintOptions);
    }

    public void showCompletionProposals() {
        if (!editorWidget.getEditorOverlay().hasShowHint()) {
            // no support for hints
            return;
        }
        final CMHintFunctionOverlay hintAuto = CMHintFunctionOverlay.createFromName(editorWidget.getCodeMirror(), "auto");
        final CMHintResultsOverlay result = hintAuto.apply(editorWidget.getEditorOverlay());
        if (result != null) {
            final List<String> proposals = new ArrayList<>();
            final JsArrayMixed list = result.getList();
            int nonStrings = 0;
            //jsarray aren't iterable
            for (int i = 0; i < list.length(); i++) {
                if (result.isString(i)) {
                    proposals.add(result.getCompletionItemAsString(i));
                } else {
                    nonStrings++;
                }
            }
            LOG.info("CM Completion returned " + list.length() + " items, of which " + nonStrings + " were not strings.");

            showCompletionProposals(proposals, result.getFrom(), result.getTo());
        }
    }

    private void showCompletionProposals(final List<String> proposals,
                                         final CMPositionOverlay from,
                                         final CMPositionOverlay to) {
        if (!editorWidget.getEditorOverlay().hasShowHint() || proposals == null || proposals.isEmpty()) {
            // no support for hints or no proposals
            return;
        }

        final CMHintOptionsOverlay hintOptions = createDefaultHintOptions();

        final CMHintFunctionOverlay hintFunction = CMHintFunctionOverlay.createFromHintFunction(new HintFunction() {

            @Override
            public CMHintResultsOverlay getHints(final CMEditorOverlay editor,
                                                 final CMHintOptionsOverlay options) {
                final CMHintResultsOverlay result = CMHintResultsOverlay.create();
                final JsArrayMixed list = result.getList();
                for (final String proposal : proposals) {


                    final CMCompletionObjectOverlay completionObject = JavaScriptObject.createObject().cast();

                    completionObject.setText(proposal);
                    final CMRenderFunctionOverlay renderFunc = createRenderHintFunc(proposal);
                    completionObject.setRender(renderFunc);

                    list.push(completionObject);
                }
                result.setFrom(from);
                result.setTo(to);
                return result;
            }


        });
        hintOptions.setHint(hintFunction);

        editorWidget.getEditorOverlay().showHint(hintOptions);
    }

    private CMHintApplyOverlay createApplyHintFunc(final CompletionProposal proposal) {
        return CMHintApplyOverlay.create(new HintApplyFunction() {

            @Override
            public void applyHint(final CMEditorOverlay editor, final CMHintResultsOverlay data,
                                  final JavaScriptObject completion) {
                CompletionCallback callback = new CompletionCallback() {

                    @Override
                    public void onCompletion(final Completion completion) {
                        HandlesUndoRedo undoRedo = editorWidget.getUndoRedo();
                        try {
                            if(undoRedo != null){
                                undoRedo.beginCompoundChange();
                            }

                            EmbeddedDocument document = editorWidget.getDocument();
                            // apply the completion
                            completion.apply(document);
                            // set the selection
                            final LinearRange selection = completion.getSelection(document);
                            if (selection != null) {
                                editorWidget.getDocument().setSelectedRange(selection, true);
                            }
                        } finally {
                            if (undoRedo != null) {
                                undoRedo.endCompoundChange();
                            }
                        }
                    }
                };
                if (proposal instanceof CompletionProposalExtension) {
                    ((CompletionProposalExtension)proposal).getCompletion(insert, callback);
                    insert = true; // default
                } else {
                    proposal.getCompletion(callback);
                }
            }
        });
    }

    private CMRenderFunctionOverlay createRenderHintFunc(final CompletionProposal proposal,
                                                         final AdditionalInfoCallback additionalInfoCallback) {
        return CMRenderFunctionOverlay.create(new RenderFunction() {

            @Override
            public void renderHint(final Element element, final CMHintResultsOverlay data,
                                   final JavaScriptObject completion) {
                final SpanElement icon = Elements.createSpanElement(completionCss.proposalIcon());
                final SpanElement label = Elements.createSpanElement(completionCss.proposalLabel());
                final SpanElement group = Elements.createSpanElement(completionCss.proposalGroup());
                if (proposal.getIcon() != null && proposal.getIcon().getSVGImage() != null) {
                    icon.appendChild((Node)proposal.getIcon().getSVGImage().getElement());
                } else if (proposal.getIcon() != null && proposal.getIcon().getImage() != null) {
                    icon.appendChild((Node)proposal.getIcon().getImage().getElement());
                }
                label.setInnerHTML(proposal.getDisplayString());
                element.appendChild(icon);
                element.appendChild(label);
                element.appendChild(group);

            }
        });
    }

    private CMRenderFunctionOverlay createRenderHintFunc(final String proposal) {
        return CMRenderFunctionOverlay.create(new RenderFunction() {

            @Override
            public void renderHint(final Element element, final CMHintResultsOverlay data,
                                   final JavaScriptObject completion) {
                final SpanElement label = Elements.createSpanElement(completionCss.proposalLabel());
                label.setInnerHTML(proposal);
                element.appendChild(label);
            }
        });
    }

    private void setupShowAdditionalInfo(final CMHintResultsOverlay data,
                                         final AdditionalInfoCallback additionalInfoCallback) {

        if (additionalInfoCallback != null) {
            final CodeMirrorOverlay codeMirror = editorWidget.getCodeMirror();
            final Element bodyElement = Elements.getBody();
            codeMirror.on(data, EventTypes.COMPLETION_SELECT, new EventHandlers.EventHandlerMixedParameters() {
                @Override
                public void onEvent(final JsArrayMixed param) {
                    // param 0 -> completion object (string or object)
                    final CMCompletionObjectOverlay completionObject = param.getObject(0);
                    // param 1 -> DOM node in the menu
                    final JsElement itemElement = param.getObject(1);
                    final ClientRect itemRect = itemElement.getBoundingClientRect();
                    Element popup = itemElement;
                    while (popup.getParentElement() != null && !popup.getParentElement().equals(bodyElement)) {
                        popup = popup.getParentElement();
                    }
                    final ClientRect popupRect = popup.getBoundingClientRect();
                    final float pixelX = Math.max(itemRect.getRight(), popupRect.getRight());
                    final float pixelY = itemRect.getTop();
                    final Element info = getAdditionalInfo(completionObject);

                    // there can be only one
                    // remove any other body child with the additional info marker
                    removeStaleInfoPopups(ADDITIONAL_INFO_MARKER);

                    // Don't show anything if there is no additional info
                    if (info == null) {
                        return;
                    }

                    final Element infoDisplayElement = additionalInfoCallback.onAdditionalInfoNeeded(pixelX, pixelY, info);
                    // set the additional info marker on the popup element
                    infoDisplayElement.getClassList().add(ADDITIONAL_INFO_MARKER);
                }
            });

            // close the additional info along with the completion popup
            codeMirror.on(data, EventTypes.COMPLETION_CLOSE, new EventHandlers.EventHandlerNoParameters() {
                @Override
                public void onEvent() {
                    delayedRemoveStaleInfoPopups(ADDITIONAL_INFO_MARKER);
                }
            });
        }
    }

    private static void delayedRemoveStaleInfoPopups(final String markerClass) {
        new Timer() {
            @Override
            public void run() {
                removeStaleInfoPopups(markerClass);
            }
        }.schedule(100);
    }

    private static void removeStaleInfoPopups(final String markerClass) {
        final Document documentElement = Elements.getDocument();
        final NodeList markersToRemove = documentElement.getElementsByClassName(markerClass);
        for (int i = 0; i < markersToRemove.getLength(); i++) {
            final Node childToRemove = markersToRemove.item(i);
            final Node parent = childToRemove.getParentNode();
            if (parent != null) {
                parent.removeChild(childToRemove);
            }
        }
    }

    private static void setAdditionalInfo(final CMCompletionObjectOverlay completion, final Element value) {
        JsMapFromStringTo<Element> element = completion.cast();
        element.put(PROP_ADDITIONAL_INFO, value);
    }

    private static Element getAdditionalInfo(final CMCompletionObjectOverlay completion) {
        JsMapFromStringTo<Element> element = completion.cast();
        return element.get(PROP_ADDITIONAL_INFO);
    }
}
