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
package org.eclipse.che.ide.editor.codemirror.client.inject;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.editor.codemirror.client.CodeMirrorEditorExtension;
import org.eclipse.che.ide.editor.codemirror.client.CodeMirrorEditorModule;
import org.eclipse.che.ide.editor.codemirror.client.CodeMirrorEditorPresenter;
import org.eclipse.che.ide.editor.codemirror.client.CodeMirrorEditorWidget;
import org.eclipse.che.ide.editor.codemirror.client.CodeMirrorTextEditorFactory;
import org.eclipse.che.ide.editor.codemirror.client.minimap.MinimapFactory;
import org.eclipse.che.ide.jseditor.client.JsEditorExtension;
import org.eclipse.che.ide.jseditor.client.defaulteditor.EditorBuilder;
import org.eclipse.che.ide.jseditor.client.editorconfig.DefaultTextEditorConfiguration;
import org.eclipse.che.ide.jseditor.client.texteditor.ConfigurableTextEditor;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorModule;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorWidgetFactory;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenterFactory;

import javax.inject.Named;

@ExtensionGinModule
public class CodeMirrorEditorGinModule extends AbstractGinModule {

    @Override
    protected void configure() {
        // Bind the CodeMirror EditorWidget factory
        install(new GinFactoryModuleBuilder().build(new TypeLiteral<EditorWidgetFactory<CodeMirrorEditorWidget>>() {}));
        bind(new TypeLiteral<EditorModule<CodeMirrorEditorWidget>>() {}).to(CodeMirrorEditorModule.class);

        install(new GinFactoryModuleBuilder()
            .implement(new TypeLiteral<EmbeddedTextEditorPresenter<CodeMirrorEditorWidget>>() {}, CodeMirrorEditorPresenter.class)
            .build(new TypeLiteral<EmbeddedTextEditorPresenterFactory<CodeMirrorEditorWidget>>() {}));

        bind(MinimapFactory.class);
    }

    @Provides
    @Singleton
    @Named(JsEditorExtension.DEFAULT_EDITOR_TYPE_INJECT_NAME)
    protected String defaultEditorTypeKey() {
        return CodeMirrorEditorExtension.CODEMIRROR_EDITOR_KEY;
    }

    @Provides
    @Singleton
    @Named(JsEditorExtension.EMBEDDED_EDITOR_BUILDER)
    protected EditorBuilder embeddedEditor(final CodeMirrorTextEditorFactory codeMirrorTextEditorFactory, final NotificationManager notificationManager){
        return new EditorBuilder() {
            @Override
            public ConfigurableTextEditor buildEditor() {
                final EmbeddedTextEditorPresenter<CodeMirrorEditorWidget> editor = codeMirrorTextEditorFactory.createTextEditor();
                editor.initialize(new DefaultTextEditorConfiguration(), notificationManager);
                return editor;
            }
        };
    }
}
