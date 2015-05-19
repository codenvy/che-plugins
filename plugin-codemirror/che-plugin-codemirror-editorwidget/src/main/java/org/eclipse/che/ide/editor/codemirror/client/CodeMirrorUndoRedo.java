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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;

import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.editor.codemirrorjso.client.CMDocumentOverlay;

import java.util.Stack;

/**
 * Undo/redo handler for codemirror editors.
 */
class CodeMirrorUndoRedo implements HandlesUndoRedo, ChangeHandler {

    private Stack<Integer> undoStack = new Stack<>();

    private Stack<Integer> redoStack = new Stack<>();

    private boolean compound;

    /** The document. */
    private final CMDocumentOverlay document;

    public CodeMirrorUndoRedo(final CMDocumentOverlay document, CodeMirrorEditorWidget codeMirror) {
        this.document = document;
        codeMirror.addChangeHandler(this);
    }

    @Override
    public boolean redoable() {
//        return (this.document.historyRedoSize() > 0);
        return  !redoStack.empty();
    }

    @Override
    public boolean undoable() {
//        return (this.document.historyUndoSize() > 0);
        return !undoStack.empty();
    }

    @Override
    public void redo() {
        if(!redoStack.empty()){
            Integer pop = redoStack.pop();
            for (int i = 0; i < pop; i++) {
                this.document.redo();
            }
            undoStack.push(pop);

        } else {
            this.document.redo();
            redoStack.push(1);
        }
    }

    @Override
    public void undo() {
        if(!undoStack.empty()){
            Integer pop = undoStack.pop();
            for (int i = 0; i < pop; i++) {
                this.document.undo();
            }
            redoStack.push(pop);

        } else {
            this.document.undo();
            undoStack.push(1);
        }
    }

    @Override
    public void beginCompoundChange() {
        undoStack.push(0);
        compound = true;
    }

    @Override
    public void endCompoundChange() {
        compound = false;
    }

    @Override
    public void onChange(ChangeEvent event) {
        if(compound){
            if(!undoStack.empty()){
                Integer peek = undoStack.pop();
                peek++;
                undoStack.push(peek);
                return;
            }
        }
        undoStack.push(1);
    }
}
