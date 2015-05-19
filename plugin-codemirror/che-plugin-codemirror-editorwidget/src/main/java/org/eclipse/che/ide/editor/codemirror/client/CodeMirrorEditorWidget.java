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


import elemental.dom.DOMTokenList;
import elemental.events.MouseEvent;
import elemental.js.events.JsMouseEvent;
import elemental.js.html.JsDivElement;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.text.Region;
import org.eclipse.che.ide.api.text.RegionImpl;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.editor.codemirror.client.minimap.MinimapFactory;
import org.eclipse.che.ide.editor.codemirror.client.minimap.MinimapPresenter;
import org.eclipse.che.ide.editor.codemirror.resources.client.BasePathConstant;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMCommandOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMEditorOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMKeymapOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMKeymapOverlay.CMKeyBindingAction;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMKeymapSetOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMModeInfoOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMPositionOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMRangeOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMSetSelectionOptions;
import org.eclipse.che.ide.editor.codemirrorjso.client.CodeMirrorOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.EventHandlers;
import org.eclipse.che.ide.editor.codemirrorjso.client.dialog.CMDialogOptionsOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.dialog.CMDialogOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.event.BeforeSelectionEventParamOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.event.CMChangeEventOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.marks.CMTextMarkerOptionOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.marks.CMTextMarkerOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.options.CMEditorOptionsOverlay;
import org.eclipse.che.ide.editor.codemirrorjso.client.options.CMMatchTagsConfig;
import org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey;
import org.eclipse.che.ide.jseditor.client.codeassist.AdditionalInfoCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionResources;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionsSource;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.editortype.EditorType;
import org.eclipse.che.ide.jseditor.client.events.BeforeSelectionChangeEvent;
import org.eclipse.che.ide.jseditor.client.events.BeforeSelectionChangeHandler;
import org.eclipse.che.ide.jseditor.client.events.CursorActivityEvent;
import org.eclipse.che.ide.jseditor.client.events.CursorActivityHandler;
import org.eclipse.che.ide.jseditor.client.events.GutterClickEvent;
import org.eclipse.che.ide.jseditor.client.events.GutterClickHandler;
import org.eclipse.che.ide.jseditor.client.events.HasBeforeSelectionChangeHandlers;
import org.eclipse.che.ide.jseditor.client.events.HasCursorActivityHandlers;
import org.eclipse.che.ide.jseditor.client.events.HasGutterClickHandlers;
import org.eclipse.che.ide.jseditor.client.events.HasScrollHandlers;
import org.eclipse.che.ide.jseditor.client.events.HasViewPortChangeHandlers;
import org.eclipse.che.ide.jseditor.client.events.ScrollEvent;
import org.eclipse.che.ide.jseditor.client.events.ScrollHandler;
import org.eclipse.che.ide.jseditor.client.events.ViewPortChangeEvent;
import org.eclipse.che.ide.jseditor.client.events.ViewPortChangeHandler;
import org.eclipse.che.ide.jseditor.client.gutter.Gutter;
import org.eclipse.che.ide.jseditor.client.gutter.HasGutter;
import org.eclipse.che.ide.jseditor.client.keymap.KeyBindingAction;
import org.eclipse.che.ide.jseditor.client.keymap.Keybinding;
import org.eclipse.che.ide.jseditor.client.keymap.Keymap;
import org.eclipse.che.ide.jseditor.client.keymap.KeymapChangeEvent;
import org.eclipse.che.ide.jseditor.client.keymap.KeymapChangeHandler;
import org.eclipse.che.ide.jseditor.client.minimap.HasMinimap;
import org.eclipse.che.ide.jseditor.client.minimap.Minimap;
import org.eclipse.che.ide.jseditor.client.position.PositionConverter;
import org.eclipse.che.ide.jseditor.client.prefmodel.KeymapPrefReader;
import org.eclipse.che.ide.jseditor.client.requirejs.ModuleHolder;
import org.eclipse.che.ide.jseditor.client.requirejs.RequireJsLoader;
import org.eclipse.che.ide.jseditor.client.text.TextRange;
import org.eclipse.che.ide.jseditor.client.texteditor.CompositeEditorWidget;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorWidget;
import org.eclipse.che.ide.jseditor.client.texteditor.LineStyler;
import org.eclipse.che.ide.util.loging.Log;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.BEFORE_SELECTION_CHANGE;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.BLUR;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.CHANGE;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.CURSOR_ACTIVITY;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.FOCUS;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.GUTTER_CLICK;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.SCROLL;
import static org.eclipse.che.ide.editor.codemirrorjso.client.EventTypes.VIEWPORT_CHANGE;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.AUTOCLOSE_BRACKETS;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.AUTOCLOSE_TAGS;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.FOLD_GUTTER;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.KEYMAP;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.MATCH_BRACKETS;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.READONLY;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.SHOW_CURSOR_WHEN_SELECTING;
import static org.eclipse.che.ide.editor.codemirrorjso.client.options.OptionKey.STYLE_ACTIVE_LINE;

/**
 * The CodeMirror implementation of {@link EditorWidget}.
 *
 * @author "Mickaël Leduque"
 */
public class CodeMirrorEditorWidget extends CompositeEditorWidget implements 
                                                                             /* handler interfaces */
                                                                                     HasBeforeSelectionChangeHandlers,
                                                                                     HasBlurHandlers,
                                                                                     HasChangeHandlers,
                                                                                     HasCursorActivityHandlers,
                                                                                     HasFocusHandlers,
                                                                                     HasGutterClickHandlers,
                                                                                     HasScrollHandlers,
                                                                                     HasViewPortChangeHandlers,
                                                                             /* capabilities */
                                                                                     HasMinimap,
                                                                                     HasGutter {

    private static final String TAB_SIZE_OPTION = "tabSize";

    /** The UI binder instance. */
    private static final CodeMirrorEditorWidgetUiBinder
            UIBINDER =
            GWT.create(CodeMirrorEditorWidgetUiBinder.class);

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(
            CodeMirrorEditorWidget.class.getSimpleName());

    /** The prefix for mode specific style overrides. */
    private static final String CODEMIRROR_MODE_STYLE_PREFIX =
            "cm-mode";

    @UiField
    SimplePanel panel;

    @UiField
    Element rightGutter;

    /** The native editor object. */
    private final CMEditorOverlay editorOverlay;


    /** The EmbeddededDocument instance. */
    private       CodeMirrorDocument embeddedDocument;
    /** The position converter instance. */
    private final PositionConverter  positionConverter;

    private final CodeMirrorOverlay codeMirror;

    /** Component that handles undo/redo. */
    private final HandlesUndoRedo undoRedo;
    /** Component that handles line styling. */
    private       LineStyler      lineStyler;

    /** Component to read the keymap preference. */
    private final KeymapPrefReader keymapPrefReader;

    // flags to know if an event type has already be added
    // to the native editor
    private boolean changeHandlerAdded          = false;
    private boolean focusHandlerAdded           = false;
    private boolean blurHandlerAdded            = false;
    private boolean scrollHandlerAdded          = false;
    private boolean cursorHandlerAdded          = false;
    private boolean beforeSelectionHandlerAdded = false;
    private boolean viewPortHandlerAdded        = false;
    private boolean gutterClickHandlerAdded     = false;

    /** The 'generation', marker to ask if changes where
     * done since if was set. */
    private int generationMarker;

    private Keymap keymap;

    private final ShowCompletion showCompletion;

    private CMKeymapOverlay keyBindings;

    private String mode;

    /**
     * The minimap.
     */
    private final MinimapPresenter minimap;

    /**
     * The gutter.
     */
    private final Gutter gutter;

    private final RequireJsLoader requirejs;

    /**
     * The base path of codemirror resources.
     */
    private final String codemirrorBasePath;

    @AssistedInject
    public CodeMirrorEditorWidget(
            final ModuleHolder moduleHolder,
            final EventBus eventBus,
            final KeymapPrefReader keymapPrefReader,
            final CompletionResources completionResources,
            final EditorAgent editorAgent,
            @Assisted final List<String> editorModes,
            final RequireJsLoader requirejs,
            final MinimapFactory minimapFactory,
            final BasePathConstant basePathConstant) {
        initWidget(UIBINDER.createAndBindUi(this));

        this.keymapPrefReader = keymapPrefReader;
        this.requirejs = requirejs;
        this.showCompletion = new ShowCompletion(this,
                                                 completionResources
                                                         .completionCss());
        this.codemirrorBasePath = basePathConstant.basePath();

        this.codeMirror = moduleHolder.getModule(
                CodeMirrorEditorExtension
                        .CODEMIRROR_MODULE_KEY)
                                      .cast();

        this.editorOverlay = this.codeMirror
                .createEditor(this.panel.getElement(),
                              getConfiguration());
        this.editorOverlay.setSize("100%", "100%");
        this.editorOverlay.refresh();

        this.positionConverter =
                new CodemirrorPositionConverter(
                        this.editorOverlay);

        this.minimap = minimapFactory.createMinimap(
                rightGutter.<JsDivElement>cast());
        this.minimap.setEditor(this);


        this.gutter = new CodemirrorGutter(this.codeMirror,
                                           this.editorOverlay);

        // just first choice for the moment
        if (editorModes != null && !editorModes.isEmpty()) {
            setMode(editorModes.get(0));
        }

        initKeyBindings();

        setupKeymap();
        eventBus.addHandler(KeymapChangeEvent.TYPE,
                            new KeymapChangeHandler() {

                                @Override
                                public void onKeymapChanged(
                                        final KeymapChangeEvent event) {
                                    final String
                                            editorTypeKey =
                                            event.getEditorTypeKey();
                                    if (CodeMirrorEditorExtension.CODEMIRROR_EDITOR_KEY
                                            .equals(editorTypeKey)) {
                                        setupKeymap();
                                    }
                                }
                            });
        this.generationMarker = this.editorOverlay.getDoc()
                                                  .changeGeneration(
                                                          true);

        // configure the save command to launch the save action
        // so the alternate keybinding save shortcut work (for example :w in vim)
        this.codeMirror.commands().put("save", CMCommandOverlay
                .create(new CMCommandOverlay.CommandFunction() {
                    @Override
                    public void execCommand(
                            final CMEditorOverlay editor) {
                        editorAgent.getActiveEditor().doSave();
                    }
                }));

        buildKeybindingInfo();
        this.undoRedo = new CodeMirrorUndoRedo(this.editorOverlay.getDoc(), this);

    }

    private void initKeyBindings() {

        this.keyBindings = CMKeymapOverlay.create();

        this.keyBindings.addBinding("Shift-Ctrl-K", this,
                                    new CMKeyBindingAction<CodeMirrorEditorWidget>() {

                                        public void action(
                                                final CodeMirrorEditorWidget editorWidget) {
                                            LOG.fine(
                                                    "Keybindings help binding used.");
                                            editorWidget
                                                    .keybindingHelp();
                                        }
                                    });

        this.editorOverlay.addKeyMap(this.keyBindings);
    }

    @Override
    public String getValue() {
        return this.editorOverlay.getValue();
    }

    @Override
    public void setValue(final String newValue) {
        this.editorOverlay.setValue(newValue);
        // reset history, else the setValue is undo-able
        this.editorOverlay.getDoc().clearHistory();
        this.generationMarker = this.editorOverlay.getDoc()
                                                  .changeGeneration(
                                                          true);
        LOG.fine("Set value - state clean=" +
                 editorOverlay.getDoc()
                              .isClean(getGenerationMarker())
                 + " (generation=" + getGenerationMarker() +
                 ").");
    }

    private CMEditorOptionsOverlay getConfiguration() {
        final CMEditorOptionsOverlay options =
                CMEditorOptionsOverlay.create();

        // show line numbers
        options.setLineNumbers(true);

        // set a theme
        options.setTheme("codenvy");

        // autoclose brackets/tags, match brackets/tags
        options.setProperty(AUTOCLOSE_BRACKETS, true);
        options.setProperty(MATCH_BRACKETS, true);
        options.setProperty(AUTOCLOSE_TAGS, true);

        // folding
        options.setProperty(FOLD_GUTTER, true);

        // gutters - define 2 : line and fold
        final JsArrayString gutters =
                JsArray.createArray(4).cast();
        gutters.push(
                CodemirrorGutter.CODE_MIRROR_GUTTER_BREAKPOINTS);
        gutters.push(
                CodemirrorGutter.CODE_MIRROR_GUTTER_ANNOTATIONS);
        gutters.push(
                CodemirrorGutter.CODE_MIRROR_GUTTER_LINENUMBERS);
        gutters.push(
                CodemirrorGutter.CODE_MIRROR_GUTTER_FOLDGUTTER);
        options.setGutters(gutters);

        // highlight matching tags
        final CMMatchTagsConfig matchTagsConfig =
                CMMatchTagsConfig.create();
        matchTagsConfig.setBothTags(true);
        options.setProperty(OptionKey.MATCH_TAGS,
                            matchTagsConfig);

        // highlight active line
        options.setProperty(STYLE_ACTIVE_LINE, true);

        // activate continueComments addon
        options.setProperty(OptionKey.CONTINUE_COMMENT, true);

        /* simple and overlay style scrollbar fix appearance on firefox */
        options.setScrollbarStyle("simple");

        return options;
    }

    protected void autoComplete() {
        this.editorOverlay.showHint();
    }

    public void setMode(final String modeDesc) {
        LOG.fine("Setting editor mode : " + modeDesc);
        this.mode = modeDesc;

        String actualFileType = modeDesc;
        // special-casing dockerfile
        if ("text/x-dockerfile-config".equals(modeDesc)) {
            actualFileType = "text/x-dockerfile";
        }

        final CMModeInfoOverlay modeInfo =
                this.codeMirror.findModeByMIME(actualFileType);
        if (modeInfo != null) {
            if (!modePresent(modeInfo.getMode())) {
                loadMode(modeInfo.getMode(), actualFileType);
            } else {
                this.editorOverlay
                        .setOption("mode", actualFileType);
            }

            // try to add mode specific style
            final String modeName = modeInfo.getMode();
            final DOMTokenList classes =
                    this.editorOverlay.getWrapperElement()
                                      .getClassList();
            classes.add(CODEMIRROR_MODE_STYLE_PREFIX + "-" +
                        modeName);
        }
    }

    private boolean modePresent(final String modeName) {
        if (modeName == null) {
            return false;
        }
        final JsArrayString modeNames =
                this.codeMirror.modeNames();
        for (int i = 0; i < modeNames.length(); i++) {
            if (modeName.equals(modeNames.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void loadMode(final String modeName,
                          final String mime) {
        this.requirejs.require(
                new Callback<JavaScriptObject[], Throwable>() {

                    @Override
                    public void onSuccess(
                            final JavaScriptObject[] result) {
                        if (result != null) {
                            editorOverlay
                                    .setOption("mode", mime);
                        } else {
                            Log.warn(
                                    CodeMirrorEditorWidget.class,
                                    "Require result is null.");
                        }
                    }

                    @Override
                    public void onFailure(
                            final Throwable reason) {
                        Log.warn(CodeMirrorEditorWidget.class,
                                 "Require " + modeName +
                                 " mode failed.");
                    }
                }, new String[]{
                        codemirrorBasePath + "lib/codemirror",
                        codemirrorBasePath + "mode/" + modeName + "/" +
                        modeName});
    }

    @Override
    public String getMode() {
        return this.mode;
    }

    public void selectVimKeymap() {
        if (CodeMirrorKeymaps.isVimLoaded()) {
            doSelectVimKeymap();
        } else {
            this.requirejs.require(
                    new Callback<JavaScriptObject[], Throwable>() {
                        @Override
                        public void onSuccess(
                                final JavaScriptObject[] result) {
                            doSelectVimKeymap();
                        }

                        @Override
                        public void onFailure(
                                final Throwable reason) {
                            Window.alert(
                                    "Could not load vim keymap, reverting to the default");
                        }
                    }, new String[]{
                            codemirrorBasePath + "lib/codemirror",
                            codemirrorBasePath + "keymap/vim"});
        }
    }

    private void doSelectVimKeymap() {
        this.editorOverlay.setOption(KEYMAP, CodeMirrorKeymaps
                .getNativeMapping(CodeMirrorKeymaps.VIM));
    }

    public void selectEmacsKeymap() {
        if (CodeMirrorKeymaps.isEmacsLoaded()) {
            doSelectEmacsKeymap();
        } else {
            this.requirejs.require(
                    new Callback<JavaScriptObject[], Throwable>() {
                        @Override
                        public void onSuccess(
                                final JavaScriptObject[] result) {
                            doSelectEmacsKeymap();
                        }

                        @Override
                        public void onFailure(
                                final Throwable reason) {
                            Window.alert(
                                    "Could not load emacs keymap, reverting to the default");
                        }
                    }, new String[]{
                            codemirrorBasePath + "lib/codemirror",
                            codemirrorBasePath + "keymap/emacs"});
        }
    }

    private void doSelectEmacsKeymap() {
        this.editorOverlay.setOption(KEYMAP, CodeMirrorKeymaps
                .getNativeMapping(CodeMirrorKeymaps.EMACS));
    }

    public void selectSublimeKeymap() {
        if (CodeMirrorKeymaps.isSublimeLoaded()) {
            doSelectSublimeKeymap();
        } else {
            this.requirejs.require(
                    new Callback<JavaScriptObject[], Throwable>() {
                        @Override
                        public void onSuccess(final JavaScriptObject[] result) {
                            doSelectSublimeKeymap();
                        }

                        @Override
                        public void onFailure(final Throwable reason) {
                            Window.alert("Could not load sublime keymap, reverting to the default");
                        }
                    }, new String[]{codemirrorBasePath + "lib/codemirror",
                                    codemirrorBasePath + "keymap/sublime"});
        }
    }

    private void doSelectSublimeKeymap() {
        this.editorOverlay.setOption(KEYMAP, CodeMirrorKeymaps.getNativeMapping(CodeMirrorKeymaps.SUBLIME));
    }

    public void selectDefaultKeymap() {
        this.editorOverlay.setOption(KEYMAP, CodeMirrorKeymaps.getNativeMapping(CodeMirrorKeymaps.DEFAULT));
    }

    private void selectKeymap(final Keymap keymap) {
        resetKeymap();
        Keymap usedKeymap = keymap;
        if (usedKeymap == null) {
            usedKeymap = CodeMirrorKeymaps.DEFAULT;
            selectDefaultKeymap();
        } else if (CodeMirrorKeymaps.DEFAULT.equals(usedKeymap)) {
            selectDefaultKeymap();
        } else if (CodeMirrorKeymaps.EMACS.equals(usedKeymap)) {
            selectEmacsKeymap();
        } else if (CodeMirrorKeymaps.VIM.equals(usedKeymap)) {
            selectVimKeymap();
        } else if (CodeMirrorKeymaps.SUBLIME.equals(usedKeymap)) {
            selectSublimeKeymap();
        } else {
            usedKeymap = CodeMirrorKeymaps.DEFAULT;
            selectDefaultKeymap();
            Log.error(CodeMirrorEditorWidget.class, "Unknown keymap: " + keymap + " - replacing by default one.");
        }
        this.keymap = usedKeymap;
    }

    private void resetKeymap() {
        if (this.editorOverlay.getBooleanOption("vimMode")) {
            this.editorOverlay.setOption("vimMode", false);
            this.editorOverlay.setOption(SHOW_CURSOR_WHEN_SELECTING, false);
        }

    }

    @Override
    public HandlerRegistration addChangeHandler(final ChangeHandler handler) {
        if (!changeHandlerAdded) {
            changeHandlerAdded = true;
            this.codeMirror.on(this.editorOverlay, CHANGE, new EventHandlers.EventHandlerOneParameter<CMChangeEventOverlay>() {

                @Override
                public void onEvent(final CMChangeEventOverlay param) {
                    LOG.fine("Change event - state clean=" + editorOverlay.getDoc().isClean(getGenerationMarker())
                             + " (generation=" + getGenerationMarker() + ").");
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
    public HandlerRegistration addFocusHandler(final FocusHandler handler) {
        if (!focusHandlerAdded) {
            focusHandlerAdded = true;
            this.codeMirror.on(this.editorOverlay, FOCUS, new EventHandlers.EventHandlerNoParameters() {

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
    public HandlerRegistration addBlurHandler(final BlurHandler handler) {
        if (!blurHandlerAdded) {
            blurHandlerAdded = true;
            this.codeMirror.on(this.editorOverlay, BLUR, new EventHandlers.EventHandlerNoParameters() {

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
            this.codeMirror.on(this.editorOverlay, SCROLL, new EventHandlers.EventHandlerNoParameters() {

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

    @Override
    public HandlerRegistration addCursorActivityHandler(final CursorActivityHandler handler) {
        if (!cursorHandlerAdded) {
            cursorHandlerAdded = true;
            this.codeMirror.on(this.editorOverlay, CURSOR_ACTIVITY, new EventHandlers.EventHandlerNoParameters() {

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
    public HandlerRegistration addBeforeSelectionChangeHandler(final BeforeSelectionChangeHandler handler) {
        if (!beforeSelectionHandlerAdded) {
            beforeSelectionHandlerAdded = true;
            this.codeMirror.on(this.editorOverlay, BEFORE_SELECTION_CHANGE,
                               new EventHandlers.EventHandlerOneParameter<BeforeSelectionEventParamOverlay>() {

                                   @Override
                                   public void onEvent(final BeforeSelectionEventParamOverlay param) {
                                       fireBeforeSelectionChangeEvent();
                                   }
                               });
        }
        return addHandler(handler, BeforeSelectionChangeEvent.TYPE);
    }

    private void fireBeforeSelectionChangeEvent() {
        fireEvent(new BeforeSelectionChangeEvent());
    }

    @Override
    public HandlerRegistration addViewPortChangeHandler(ViewPortChangeHandler handler) {
        if (!viewPortHandlerAdded) {
            viewPortHandlerAdded = true;
            this.codeMirror.on(this.editorOverlay, VIEWPORT_CHANGE, new EventHandlers.EventHandlerMixedParameters() {

                @Override
                public void onEvent(final JsArrayMixed param) {
                    final int from = Double.valueOf(param.getNumber(0)).intValue();
                    final int to = Double.valueOf(param.getNumber(1)).intValue();
                    fireViewPortChangeEvent(from, to);
                }
            });
        }
        return addHandler(handler, ViewPortChangeEvent.TYPE);
    }

    private void fireViewPortChangeEvent(final int from, final int to) {
        fireEvent(new ViewPortChangeEvent(from, to));
    }

    @Override
    public HandlerRegistration addGutterClickHandler(GutterClickHandler handler) {
        if (!gutterClickHandlerAdded) {
            gutterClickHandlerAdded = true;
            this.editorOverlay.on(GUTTER_CLICK, new EventHandlers.EventHandlerMixedParameters() {

                @Override
                public void onEvent(final JsArrayMixed params) {
                    // param 0 is codemirror instance
                    final int line = Double.valueOf(params.getNumber(1)).intValue();
                    final String gutterId = params.getString(2);
                    // param 3 is click event
                    final JsMouseEvent event = params.getObject(3).cast();
                    fireGutterClickEvent(line, gutterId, event);
                }
            });
        }
        return addHandler(handler, GutterClickEvent.TYPE);
    }

    private void fireGutterClickEvent(final int line, final String internalGutterId, final MouseEvent event) {
        String gutterId = CodemirrorGutter.GUTTER_MAP.cmToLogical(internalGutterId);
        final GutterClickEvent gutterEvent = new GutterClickEvent(line, gutterId, event);
        fireEvent(gutterEvent);
        this.embeddedDocument.getDocEventBus().fireEvent(gutterEvent);
    }

    @Override
    public void setReadOnly(final boolean isReadOnly) {
        this.editorOverlay.setOption(READONLY, isReadOnly);
    }

    @Override
    public boolean isReadOnly() {
        return this.editorOverlay.getBooleanOption(READONLY);
    }

    @Override
    public boolean isDirty() {
        return !this.editorOverlay.getDoc().isClean(this.generationMarker);
    }

    @Override
    public void markClean() {
        boolean beforeDirty = false;
        int beforeGeneration = 0;
        if (LOG.isLoggable(Level.FINE)) {
            beforeDirty = isDirty();
            beforeGeneration = this.generationMarker;
        }

        // Use changeGeneration instead of markClean: codemirror author's recommandation
        this.generationMarker = this.editorOverlay.getDoc().changeGeneration(true);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("markClean - Before dirty=" + beforeDirty + " gen=" + beforeGeneration
                     + " After dirty=" + isDirty() + " gen=" + this.generationMarker);
        }
    }


    @Override
    public int getTabSize() {
        return this.editorOverlay.getIntOption(TAB_SIZE_OPTION);
    }

    @Override
    public void setTabSize(int tabSize) {
        this.editorOverlay.setOption(TAB_SIZE_OPTION, tabSize);
    }

    @Override
    public EmbeddedDocument getDocument() {
        if (this.embeddedDocument == null) {
            this.embeddedDocument = new CodeMirrorDocument(this.editorOverlay.getDoc(), this.codeMirror, this);
        }
        return this.embeddedDocument;
    }

    @Override
    public Region getSelectedRange() {
        // will only support a single selection here

        /* multiple selection support would use listSelections() */
        final CMPositionOverlay from = this.editorOverlay.getDoc().getCursorFrom();
        final CMPositionOverlay to = this.editorOverlay.getDoc().getCursorTo();

        final int startOffset = this.editorOverlay.getDoc().indexFromPos(from);
        final int endOffset = this.editorOverlay.getDoc().indexFromPos(to);

        return new RegionImpl(startOffset, endOffset - startOffset);
    }

    public void setSelectedRange(final Region selection, final boolean show) {
        final CMPositionOverlay anchor = this.editorOverlay.getDoc().posFromIndex(selection.getOffset());
        final int headOffset = selection.getOffset() + selection.getLength();
        final CMPositionOverlay head = this.editorOverlay.getDoc().posFromIndex(headOffset);

        if (show) {
            this.editorOverlay.getDoc().setSelection(anchor, head);
        } else {
            this.editorOverlay.getDoc().setSelection(anchor, head, CMSetSelectionOptions.createNoScroll());
        }
    }

    public void setDisplayRange(final Region range) {
        final CMPositionOverlay from = this.editorOverlay.getDoc().posFromIndex(range.getOffset());
        final CMPositionOverlay to = this.editorOverlay.getDoc().posFromIndex(range.getOffset() + range.getLength());
        final CMRangeOverlay nativeRange = CMRangeOverlay.create(from, to);
        this.editorOverlay.scrollIntoView(nativeRange);
    }

    private void setupKeymap() {
        final String propertyValue = this.keymapPrefReader.readPref(CodeMirrorEditorExtension.CODEMIRROR_EDITOR_KEY);
        Keymap keymap;
        try {
            keymap = Keymap.fromKey(propertyValue);
        } catch (final IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Unknown value in keymap preference.", e);
            return;
        }
        selectKeymap(keymap);
    }

    /**
     * Returns the generation marker.<br>
     * As the field is not final, this is needed so the non-static inner class can see the value changes.
     *
     * @return the generation marker
     */
    private int getGenerationMarker() {
        return this.generationMarker;
    }

    /**
     * Generate a key bindings list for all keymaps.
     */
    public void keybindingHelp() {
        insertAtCursor(buildKeybindingInfo());
    }

    private String buildKeybindingInfo() {
        final StringBuilder sb = new StringBuilder();
        final CMKeymapSetOverlay keymapsObject = codeMirror.keyMap();
        for (final String keymapKey : keymapsObject.getKeys()) {
            if (keymapKey == null || keymapKey.startsWith("emacs") || keymapKey.startsWith("vim")) {
                continue;
            }
            sb.append("# ").append(keymapKey).append("\n\n");
            final CMKeymapOverlay keymap = keymapsObject.get(keymapKey);
            for (final String binding : keymap.getKeys()) {
                if ("fallthrough".equals(binding)
                    || "nofallthrough".equals(binding)
                    || "disableInput".equals(binding)
                    || "auto".equals(binding)) {
                    continue;
                }
                switch (keymap.getType(binding)) {
                    case COMMAND_NAME:
                        sb.append("**")
                          .append(binding)
                          .append("** ")
                          .append(keymap.getCommandName(binding))
                          .append("\n\n");
                        break;
                    case FUNCTION:
                        sb.append("**")
                          .append(binding)
                          .append("** ")
                          .append(keymap.getFunctionSource(binding))
                          .append("\n\n");
                        break;
                    default:
                        break;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void insertAtCursor(final String insertedText) {
        final CMPositionOverlay cursor = this.editorOverlay.getDoc().getCursor();
        this.editorOverlay.getDoc().replaceRange(insertedText, cursor);
    }

    @Override
    public EditorType getEditorType() {
        return EditorType.getInstance(CodeMirrorEditorExtension.CODEMIRROR_EDITOR_KEY);
    }

    @Override
    public Keymap getKeymap() {
        return this.keymap;
    }

    public PositionConverter getPositionConverter() {
        return this.positionConverter;
    }

    public void setFocus() {
        this.editorOverlay.focus();
    }

    @Override
    public void onResize() {
        this.editorOverlay.refresh();
    }

    @Override
    public HandlesUndoRedo getUndoRedo() {
        return this.undoRedo;
    }

    public void showMessage(final String message) {
        final CMDialogOptionsOverlay options = JavaScriptObject.createObject().cast();
        options.setBottom(true);
        final CMDialogOverlay dialog = this.editorOverlay.getDialog();
        if (dialog != null) {
            dialog.openNotification(message, options);
        } else {
            Log.info(CodeMirrorEditorWidget.class, message);
        }
    }

    public void addKeybinding(final Keybinding keybinding) {
        final String keySpec = KeybindingTranslator.translateKeyBinding(keybinding, this.codeMirror);
        if (keySpec == null) {
            LOG.warning("Couldn't bind key, keycode is unknown.");
            return;
        }
        final KeyBindingAction bindingAction = keybinding.getAction();
        if (bindingAction == null) {
            LOG.warning("Cannot bind null action on " + keySpec + ".");
            return;
        }
        LOG.info("Binding action on " + keySpec + ".");
        this.keyBindings.addBinding(keySpec, bindingAction, new CMKeyBindingAction<KeyBindingAction>() {

            @Override
            public void action(final KeyBindingAction action) {
                action.action();
            }
        });
    }

    public void showCompletionsProposals(final List<CompletionProposal> proposals,
                                         final AdditionalInfoCallback additionalInfoCallback) {
        this.showCompletion.showCompletionProposals(proposals, additionalInfoCallback);
    }

    public void showCompletionsProposals(final List<CompletionProposal> proposals) {
        showCompletionsProposals(proposals, null);
    }

    @Override
    public void showCompletionProposals(final CompletionsSource completionsSource,
                                        final AdditionalInfoCallback additionalInfoCallback) {
        this.showCompletion.showCompletionProposals(completionsSource, additionalInfoCallback);
    }

    @Override
    public void showCompletionProposals(final CompletionsSource completionsSource) {
        showCompletionProposals(completionsSource, null);
    }

    @Override
    public void showCompletionProposals() {
        this.showCompletion.showCompletionProposals();
    }

    public MarkerRegistration addMarker(final TextRange range, final String className) {
        final CMPositionOverlay from = CMPositionOverlay.create(range.getFrom().getLine(), range.getFrom().getCharacter());
        final CMPositionOverlay to = CMPositionOverlay.create(range.getTo().getLine(), range.getTo().getCharacter());
        final CMTextMarkerOptionOverlay options = JavaScriptObject.createObject().cast();
        options.setClassName(className);

        final CMTextMarkerOverlay textMark = this.editorOverlay.asMarksManager().markText(from, to, options);
        if (textMark == null) {
            LOG.warning("addMarker: markText returned a undefined TextMarker - range=" + range);
            return null;
        }
        return new MarkerRegistration() {
            @Override
            public void clearMark() {
                textMark.clear();
            }
        };
    }

    @Override
    public LineStyler getLineStyler() {
        if (this.lineStyler == null) {
            this.lineStyler = new CodeMirrorLineStyler(this.editorOverlay);
        }
        return this.lineStyler;
    }

    /**
     * Returns the editor overlay instance.
     *
     * @return the editor overlay
     */
    CMEditorOverlay getEditorOverlay() {
        return this.editorOverlay;
    }

    /**
     * Return the CodeMirror object.
     *
     * @return the CodeMirror
     */
    CodeMirrorOverlay getCodeMirror() {
        return this.codeMirror;
    }

    @Override
    protected void onLoad() {
        this.editorOverlay.refresh();
    }

    public void refresh() {
        this.editorOverlay.refresh();
    }

    public void scrollToLine(int line) {
        this.editorOverlay.scrollIntoView(CMPositionOverlay.create(line, 0));
    }

    @Override
    public Minimap getMinimap() {
        return this.minimap;
    }

    @Override
    public Gutter getGutter() {
        return this.gutter;
    }

    interface CodeMirrorEditorWidgetUiBinder extends UiBinder<HTMLPanel, CodeMirrorEditorWidget> {
    }
}
