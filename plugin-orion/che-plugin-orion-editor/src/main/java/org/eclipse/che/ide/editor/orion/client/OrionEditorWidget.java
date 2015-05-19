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


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.text.Region;
import org.eclipse.che.ide.api.text.RegionImpl;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionKeyModeOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionSelectionOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionTextThemeOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionTextViewOverlay;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionsSource;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.editortype.EditorType;
import org.eclipse.che.ide.jseditor.client.events.CursorActivityEvent;
import org.eclipse.che.ide.jseditor.client.events.CursorActivityHandler;
import org.eclipse.che.ide.jseditor.client.events.GutterClickHandler;
import org.eclipse.che.ide.jseditor.client.events.HasCursorActivityHandlers;
import org.eclipse.che.ide.jseditor.client.events.HasScrollHandlers;
import org.eclipse.che.ide.jseditor.client.events.ScrollEvent;
import org.eclipse.che.ide.jseditor.client.events.ScrollHandler;
import org.eclipse.che.ide.jseditor.client.keymap.Keybinding;
import org.eclipse.che.ide.jseditor.client.keymap.Keymap;
import org.eclipse.che.ide.jseditor.client.keymap.KeymapChangeEvent;
import org.eclipse.che.ide.jseditor.client.keymap.KeymapChangeHandler;
import org.eclipse.che.ide.jseditor.client.position.PositionConverter;
import org.eclipse.che.ide.jseditor.client.prefmodel.KeymapPrefReader;
import org.eclipse.che.ide.jseditor.client.requirejs.ModuleHolder;
import org.eclipse.che.ide.jseditor.client.text.TextRange;
import org.eclipse.che.ide.jseditor.client.texteditor.CompositeEditorWidget;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorWidget;
import org.eclipse.che.ide.jseditor.client.texteditor.LineStyler;
import org.eclipse.che.ide.util.loging.Log;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orion implementation for {@link EditorWidget}.
 *
 * @author "Mickaël Leduque"
 */
public class OrionEditorWidget extends CompositeEditorWidget implements HasChangeHandlers, HasCursorActivityHandlers, HasScrollHandlers {

    /** The UI binder instance. */
    private static final OrionEditorWidgetUiBinder UIBINDER = GWT.create(OrionEditorWidgetUiBinder.class);

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(OrionEditorWidget.class.getSimpleName());

    @UiField
    SimplePanel panel;

    /** The instance of the orion editor native element style. */
    @UiField
    EditorElementStyle editorElementStyle;

    private final OrionEditorOverlay editorOverlay;
    private       String             modeName;
    private final KeyModeInstances   keyModeInstances;
    private final JavaScriptObject   orionEditorModule;
    private final KeymapPrefReader   keymapPrefReader;

    /** Component that handles undo/redo. */
    private final HandlesUndoRedo undoRedo;

    private OrionDocument embeddedDocument;

    private boolean changeHandlerAdded = false;
    private boolean focusHandlerAdded  = false;
    private boolean blurHandlerAdded   = false;
    private boolean scrollHandlerAdded = false;
    private boolean cursorHandlerAdded = false;

    private Keymap keymap;

    @AssistedInject
    public OrionEditorWidget(final ModuleHolder moduleHolder,
                             final KeyModeInstances keyModeInstances,
                             final EventBus eventBus,
                             final KeymapPrefReader keymapPrefReader,
                             @Assisted final List<String> editorModes) {
        initWidget(UIBINDER.createAndBindUi(this));

        this.keymapPrefReader = keymapPrefReader;

        this.orionEditorModule = moduleHolder.getModule("OrionEditor");

        // just first choice for the moment
        if (editorModes != null && !editorModes.isEmpty()) {
            setMode(editorModes.get(0));
        }

        panel.getElement().setId("orion-parent-" + Document.get().createUniqueId());
        panel.getElement().addClassName(this.editorElementStyle.editorParent());
        this.editorOverlay = OrionEditorOverlay.createEditor(panel.getElement(), getConfiguration(), orionEditorModule);

        this.keyModeInstances = keyModeInstances;
        final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
        this.keyModeInstances.add(KeyMode.VI, OrionKeyModeOverlay.getViKeyMode(moduleHolder.getModule("OrionVi"), textView));
        this.keyModeInstances.add(KeyMode.EMACS, OrionKeyModeOverlay.getEmacsKeyMode(moduleHolder.getModule("OrionEmacs"), textView));

        setupKeymode();
        eventBus.addHandler(KeymapChangeEvent.TYPE, new KeymapChangeHandler() {

            @Override
            public void onKeymapChanged(final KeymapChangeEvent event) {
                setupKeymode();
            }
        });
        this.undoRedo = new OrionUndoRedo(this.editorOverlay.getUndoStack());
        editorOverlay.setZoomRulerVisible(true);
    }

    @Override
    public String getValue() {
        return editorOverlay.getText();
    }

    @Override
    public void setValue(String newValue) {
        this.editorOverlay.setText(newValue);
        this.editorOverlay.getUndoStack().reset();
    }

    private JavaScriptObject getConfiguration() {
        final JSONObject json = new JSONObject();

        json.put("theme", new JSONObject(OrionTextThemeOverlay.getDefautTheme()));
        json.put("contentType", new JSONString(this.modeName));
        json.put("noComputeSize", JSONBoolean.getInstance(true));
        json.put("showZoomRuler", JSONBoolean.getInstance(true));

        return json.getJavaScriptObject();
    }

    protected void autoComplete(OrionEditorOverlay editor) {
        // TODO
    }

    public void setMode(final String modeName) {
        String mode = modeName;
        if (modeName.equals("text/x-java")) {
            mode = "text/x-java-source";
        }
        LOG.fine("Requested mode: " + modeName + " kept " + mode);

        this.modeName = mode;
    }

    @Override
    public String getMode() {
        return modeName;
    }

    @Override
    public void setReadOnly(final boolean isReadOnly) {
        this.editorOverlay.getTextView().getOptions().setReadOnly(isReadOnly);
        this.editorOverlay.getTextView().update();
    }


    @Override
    public boolean isReadOnly() {
        return this.editorOverlay.getTextView().getOptions().isReadOnly();
    }

    @Override
    public boolean isDirty() {
        return this.editorOverlay.isDirty();
    }

    @Override
    public void markClean() {
        this.editorOverlay.setDirty(false);
    }

    private void selectKeyMode(Keymap keymap) {
        resetKeyModes();
        Keymap usedKeymap = keymap;
        if (usedKeymap == null) {
            usedKeymap = KeyMode.DEFAULT;
        }
        if (KeyMode.DEFAULT.equals(usedKeymap)) {
            // nothing to do
        } else if (KeyMode.EMACS.equals(usedKeymap)) {
            this.editorOverlay.getTextView().addKeyMode(keyModeInstances.getInstance(KeyMode.EMACS));
        } else if (KeyMode.VI.equals(usedKeymap)) {
            this.editorOverlay.getTextView().addKeyMode(keyModeInstances.getInstance(KeyMode.VI));
        } else {
            usedKeymap = KeyMode.DEFAULT;
            Log.error(OrionEditorWidget.class, "Unknown keymap type: " + keymap + " - changing to defaut one.");
        }
        this.keymap = usedKeymap;
    }

    private void resetKeyModes() {
        this.editorOverlay.getTextView().removeKeyMode(keyModeInstances.getInstance(KeyMode.VI));
        this.editorOverlay.getTextView().removeKeyMode(keyModeInstances.getInstance(KeyMode.EMACS));
    }

    @Override
    public EmbeddedDocument getDocument() {
        if (this.embeddedDocument == null) {
            this.embeddedDocument = new OrionDocument(this.editorOverlay.getTextView(), this);
        }
        return this.embeddedDocument;
    }

    @Override
    public Region getSelectedRange() {
        final OrionSelectionOverlay selection = this.editorOverlay.getSelection();

        final int start = selection.getStart();
        final int end = selection.getEnd();

        if (start < 0 || end > this.editorOverlay.getModel().getCharCount() || start > end) {
            throw new RuntimeException("Invalid selection");
        }
        return new RegionImpl(start, end - start);
    }

    public void setSelectedRange(final Region selection, final boolean show) {
        this.editorOverlay.setSelection(selection.getOffset(), selection.getLength(), show);
    }

    public void setDisplayRange(final Region range) {
        // show the line at the head of the range
        final int headOffset = range.getOffset() + range.getLength();
        if (range.getLength() < 0) {
            this.editorOverlay.getTextView().setTopIndex(headOffset);
        } else {
            this.editorOverlay.getTextView().setBottomIndex(headOffset);
        }
    }

    @Override
    public int getTabSize() {
        return this.editorOverlay.getTextView().getOptions().getTabSize();
    }

    @Override
    public void setTabSize(int tabSize) {
        this.editorOverlay.getTextView().getOptions().setTabSize(tabSize);
    }

    @Override
    public HandlerRegistration addChangeHandler(final ChangeHandler handler) {
        if (!changeHandlerAdded) {
            changeHandlerAdded = true;
            final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
            textView.addEventListener(OrionEventContants.MODEL_CHANGED_EVENT, new OrionTextViewOverlay.EventHandlerNoParameter() {

                @Override
                public void onEvent() {
                    fireChangeEvent();
                }
            });
        }
        return addHandler(handler, ChangeEvent.getType());
    }

    private void fireChangeEvent() {
        DomEvent.fireNativeEvent(Document.get().createChangeEvent(), this);
    }

    @Override
    public HandlerRegistration addCursorActivityHandler(CursorActivityHandler handler) {
        if (!cursorHandlerAdded) {
            cursorHandlerAdded = true;
            final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
            textView.addEventListener(OrionEventContants.SELECTION_EVENT, new OrionTextViewOverlay.EventHandlerNoParameter() {

                @Override
                public void onEvent() {
                    fireCursorActivityEvent();
                }
            });
        }
        return addHandler(handler, CursorActivityEvent.TYPE);
    }

    private void fireCursorActivityEvent() {
        fireEvent(new CursorActivityEvent());
    }

    @Override
    public HandlerRegistration addFocusHandler(FocusHandler handler) {
        if (!focusHandlerAdded) {
            focusHandlerAdded = true;
            final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
            textView.addEventListener(OrionEventContants.FOCUS_EVENT, new OrionTextViewOverlay.EventHandlerNoParameter() {

                @Override
                public void onEvent() {
                    fireFocusEvent();
                }
            });
        }
        return addHandler(handler, FocusEvent.getType());
    }

    private void fireFocusEvent() {
        DomEvent.fireNativeEvent(Document.get().createFocusEvent(), this);
    }

    @Override
    public HandlerRegistration addBlurHandler(BlurHandler handler) {
        if (!blurHandlerAdded) {
            blurHandlerAdded = true;
            final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
            textView.addEventListener(OrionEventContants.BLUR_EVENT, new OrionTextViewOverlay.EventHandlerNoParameter() {

                @Override
                public void onEvent() {
                    fireBlurEvent();
                }
            });
        }
        return addHandler(handler, BlurEvent.getType());
    }

    private void fireBlurEvent() {
        DomEvent.fireNativeEvent(Document.get().createBlurEvent(), this);
    }


    @Override
    public HandlerRegistration addScrollHandler(final ScrollHandler handler) {
        if (!scrollHandlerAdded) {
            scrollHandlerAdded = true;
            final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
            textView.addEventListener(OrionEventContants.SCROLL_EVENT, new OrionTextViewOverlay.EventHandlerNoParameter() {

                @Override
                public void onEvent() {
                    fireScrollEvent();
                }
            });
        }
        return addHandler(handler, ScrollEvent.TYPE);
    }

    private void fireScrollEvent() {
        fireEvent(new ScrollEvent());
    }

    private void setupKeymode() {
        final String propertyValue = this.keymapPrefReader.readPref(OrionEditorExtension.ORION_EDITOR_KEY);
        Keymap keymap;
        try {
            keymap = Keymap.fromKey(propertyValue);
        } catch (final IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Unknown value in keymap preference.", e);
            return;
        }
        selectKeyMode(keymap);
    }

    @Override
    public EditorType getEditorType() {
        return EditorType.getInstance(OrionEditorExtension.ORION_EDITOR_KEY);
    }

    @Override
    public Keymap getKeymap() {
        return this.keymap;
    }

    public PositionConverter getPositionConverter() {
        return this.getPositionConverter();
    }

    public void setFocus() {
        this.editorOverlay.focus();
    }

    public void showMessage(final String message) {
        this.editorOverlay.reportStatus(message);
    }

    @Override
    protected void onLoad() {

        // fix for native editor height
        if (panel.getElement().getChildCount() > 0) {
            final Element child = panel.getElement().getFirstChildElement();
            child.setId("orion-editor-" + Document.get().createUniqueId());
            child.getStyle().clearHeight();

        } else {
            LOG.severe("Orion insertion failed.");
        }
    }

    public void onResize() {
        // redraw text and rulers
        // maybe just redrawing the text would be enough
        this.editorOverlay.getTextView().redraw();
    }

    public HandlesUndoRedo getUndoRedo() {
        return this.undoRedo;
    }

    public void addKeybinding(final Keybinding keybinding) {
        // not (yet) handled in orion editor

    }

    public MarkerRegistration addMarker(final TextRange range, final String className) {
        // currently not implemented
        return null;
    }

    public void showCompletionsProposals(final List<CompletionProposal> proposals) {
        // currently not implemented
    }

    public void showCompletionProposals(final CompletionsSource completionsSource) {
        // currently not implemented
    }

    @Override
    public LineStyler getLineStyler() {
        return null;
    }

    @Override
    public HandlerRegistration addGutterClickHandler(final GutterClickHandler handler) {
        return null;
    }

    public void refresh() {
        this.editorOverlay.getTextView().redraw();
    }


    public void scrollToLine(int line) {
        this.editorOverlay.getTextView().setTopIndex(line);
    }

    /**
     * UI binder interface for this component.
     *
     * @author "Mickaël Leduque"
     */
    interface OrionEditorWidgetUiBinder extends UiBinder<SimplePanel, OrionEditorWidget> {
    }

    /**
     * CSS style for the orion native editor element.
     *
     * @author "Mickaël Leduque"
     */
    public interface EditorElementStyle extends CssResource {

        @ClassName("editor-parent")
        String editorParent();
    }
}
