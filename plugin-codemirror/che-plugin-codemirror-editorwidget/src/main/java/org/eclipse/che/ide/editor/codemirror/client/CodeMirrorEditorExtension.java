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

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.inject.Inject;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.Notification.Type;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.editor.codemirror.base.client.BaseCodemirrorInitializer;
import org.eclipse.che.ide.editor.codemirror.base.client.BaseCodemirrorPromise;
import org.eclipse.che.ide.editor.codemirror.resources.client.BasePathConstant;
import org.eclipse.che.ide.editor.codemirrorjso.client.CodeMirrorOverlay;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionResources;
import org.eclipse.che.ide.jseditor.client.defaulteditor.EditorBuilder;
import org.eclipse.che.ide.jseditor.client.editorconfig.AutoSaveTextEditorConfiguration;
import org.eclipse.che.ide.jseditor.client.editortype.EditorType;
import org.eclipse.che.ide.jseditor.client.editortype.EditorTypeRegistry;
import org.eclipse.che.ide.jseditor.client.requirejs.RequireJsLoader;
import org.eclipse.che.ide.jseditor.client.requirejs.RequirejsErrorHandler.RequireError;
import org.eclipse.che.ide.jseditor.client.texteditor.AbstractEditorModule.EditorInitializer;
import org.eclipse.che.ide.jseditor.client.texteditor.AbstractEditorModule.InitializerCallback;
import org.eclipse.che.ide.jseditor.client.texteditor.ConfigurableTextEditor;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.util.loging.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

@Extension(title = "CodeMirror Editor", version = "1.1.0")
public class CodeMirrorEditorExtension {

    /** The logger. */
    private static final Logger  LOG = Logger.getLogger(CodeMirrorEditorExtension.class.getSimpleName());
    /** The editor type key. */
    public static final String                CODEMIRROR_EDITOR_KEY = "codemirror";

    /** The codemirror javascript module key. */
    public static final String                CODEMIRROR_MODULE_KEY = "CodeMirror";

    /** The base path for codemirror resources. */
    private final String codemirrorBase;

    private final NotificationManager         notificationManager;
    private final RequireJsLoader             requireJsLoader;
    private final EditorTypeRegistry          editorTypeRegistry;
    private final CodeMirrorEditorModule      editorModule;

    private final CodeMirrorTextEditorFactory codeMirrorTextEditorFactory;

    private boolean initFailedWarnedOnce = false;

    @Inject
    public CodeMirrorEditorExtension(final EditorTypeRegistry editorTypeRegistry,
                                     final RequireJsLoader requireJsLoader,
                                     final NotificationManager notificationManager,
                                     final CodeMirrorEditorModule editorModule,
                                     final BaseCodemirrorPromise basePromise,
                                     final BaseCodemirrorInitializer baseInitializer,
                                     final CodeMirrorTextEditorFactory codeMirrorTextEditorFactory,
                                     final CompletionResources completionResources,
                                     final BasePathConstant basePathConstant) {
        this.notificationManager = notificationManager;
        this.requireJsLoader = requireJsLoader;
        this.editorModule = editorModule;
        this.editorTypeRegistry = editorTypeRegistry;
        this.codeMirrorTextEditorFactory = codeMirrorTextEditorFactory;
        this.codemirrorBase = basePathConstant.basePath();

        completionResources.completionCss().ensureInjected();

        Log.debug(CodeMirrorEditorExtension.class, "Codemirror extension module=" + editorModule);
        editorModule.setEditorInitializer(new EditorInitializer() {
            @Override
            public void initialize(final InitializerCallback callback) {
                // add code-splitting of the whole orion editor
                GWT.runAsync(new RunAsyncCallback() {
                    @Override
                    public void onSuccess() {
                        initBaseCodeMirror(basePromise, baseInitializer, callback);
                    }
                    @Override
                    public void onFailure(final Throwable reason) {
                        callback.onFailure(reason);
                    }
                });
            }
        });
        // must not delay
        registerEditor();
        CodeMirrorKeymaps.init();
    }

    private void initBaseCodeMirror(final BaseCodemirrorPromise basePromise,
                                    final BaseCodemirrorInitializer baseInitializer,
                                    final InitializerCallback callback) {
        if (basePromise.getPromise() == null) {
            baseInitializer.init();
        }
        final Promise<CodeMirrorOverlay> editorPromise = basePromise.getPromise().then(new Operation<CodeMirrorOverlay>() {
            
            @Override
            public void apply(final CodeMirrorOverlay codemirror) throws OperationException {
                setupFullCodeMirror(callback);
            }
        });
        editorPromise.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(final PromiseError arg) throws OperationException {
                editorModule.setError();
            }
        });
    }

    private void setupFullCodeMirror(final InitializerCallback callback) {
        /*
         * This could be simplified and optimized with a all-in-one minified js from http://codemirror.net/doc/compress.html but at least
         * while debugging, unmodified source is necessary. Another option would be to include all-in-one minified along with a source map
         */
        final String[] scripts = new String[]{

                // base script
                codemirrorBase + "lib/codemirror",

                // library of modes
                codemirrorBase + "mode/meta",
                // mode autoloading
                codemirrorBase + "addon/mode/loadmode",


                /* We will preload modes that have extensions */
                // language modes
                codemirrorBase + "mode/xml/xml",
                codemirrorBase + "mode/htmlmixed/htmlmixed", // must be defined after xml

                codemirrorBase + "mode/javascript/javascript",
                codemirrorBase + "mode/coffeescript/coffeescript",

                codemirrorBase + "mode/css/css",

                codemirrorBase + "mode/sql/sql",

                codemirrorBase + "mode/clike/clike",

                codemirrorBase + "mode/markdown/markdown",
                codemirrorBase + "mode/gfm/gfm", // markdown extension for github

                // hints
                codemirrorBase + "addon/hint/show-hint",
                codemirrorBase + "addon/hint/xml-hint",
                codemirrorBase + "addon/hint/html-hint",
                codemirrorBase + "addon/hint/javascript-hint",
                codemirrorBase + "addon/hint/css-hint",
                codemirrorBase + "addon/hint/anyword-hint",
                codemirrorBase + "addon/hint/sql-hint",

                // pair matching
                codemirrorBase + "addon/edit/closebrackets",
                codemirrorBase + "addon/edit/closetag",
                codemirrorBase + "addon/edit/matchbrackets",
                codemirrorBase + "addon/edit/matchtags",
                // the two following are added to repair actual functionality in 'classic' editor
                codemirrorBase + "addon/selection/mark-selection",
                codemirrorBase + "addon/selection/active-line",

                // for search
                codemirrorBase + "addon/search/search",
                codemirrorBase + "addon/dialog/dialog",
                codemirrorBase + "addon/search/searchcursor",
                codemirrorBase + "addon/search/match-highlighter",
                // comment management
                codemirrorBase + "addon/comment/comment",
                codemirrorBase + "addon/comment/continuecomment",
                // folding
                codemirrorBase + "addon/fold/foldcode",
                codemirrorBase + "addon/fold/foldgutter",
                codemirrorBase + "addon/fold/brace-fold",
                codemirrorBase + "addon/fold/xml-fold", // also required by matchbrackets and closebrackets
                codemirrorBase + "addon/fold/comment-fold",
                codemirrorBase + "addon/fold/indent-fold",
                codemirrorBase + "addon/fold/markdown-fold",

                codemirrorBase + "addon/scroll/simplescrollbars",
                codemirrorBase + "addon/scroll/annotatescrollbar",
                codemirrorBase + "addon/scroll/scrollpastend",
                codemirrorBase + "addon/search/matchesonscrollbar",
        };


        this.requireJsLoader.require(new Callback<JavaScriptObject[], Throwable>() {
            @Override
            public void onSuccess(final JavaScriptObject[] result) {
                editorModule.setReady();
                callback.onSuccess();
            }

            @Override
            public void onFailure(final Throwable e) {
                if (e instanceof JavaScriptException) {
                    final JavaScriptException jsException = (JavaScriptException)e;
                    final Object nativeException = jsException.getThrown();
                    if (nativeException instanceof RequireError) {
                        final RequireError requireError = (RequireError)nativeException;
                        final String errorType = requireError.getRequireType();
                        String message = "Codemirror injection failed: " + errorType + " ";
                        final JsArrayString modules = requireError.getRequireModules();
                        if (modules != null) {
                            message += modules.join(",");
                        }
                        Log.debug(CodeMirrorEditorExtension.class, message);
                    }
                }
                initializationFailed(callback, "Unable to initialize CodeMirror", e);
            }
        }, scripts, new String[]{CODEMIRROR_MODULE_KEY});

    }

    private void registerEditor() {
        LOG.fine("Registering CodeMirror editor type.");
        this.editorTypeRegistry.registerEditorType(EditorType.fromKey(CODEMIRROR_EDITOR_KEY), "CodeMirror", new EditorBuilder() {

            @Override
            public ConfigurableTextEditor buildEditor() {
                final EmbeddedTextEditorPresenter<CodeMirrorEditorWidget> editor = codeMirrorTextEditorFactory.createTextEditor();
                editor.initialize(new AutoSaveTextEditorConfiguration(), notificationManager);
                return editor;
            }
        });
    }

    private void initializationFailed(final InitializerCallback callback, final String errorMessage, Throwable e) {
        if (this.initFailedWarnedOnce) {
            return;
        }
        this.initFailedWarnedOnce = true;

        this.notificationManager.showNotification(new Notification(errorMessage, Type.ERROR));
        this.notificationManager.showNotification(new Notification("CodeMirror editor is not available", Type.WARNING));
        LOG.log(Level.SEVERE, errorMessage + " - ", e);
        callback.onFailure(e);
    }
}
