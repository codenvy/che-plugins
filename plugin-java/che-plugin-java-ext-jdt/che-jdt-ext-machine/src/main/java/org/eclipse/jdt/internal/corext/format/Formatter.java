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
package org.eclipse.jdt.internal.corext.format;

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Nikitenko
 */

public class Formatter {

    public List<Change> format(int offset, int length, String content) throws BadLocationException {
        IDocument document = new Document(content);
        TextEdit textEdit = CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, content, offset, length, 0, null, null);
        return getChanges(document, textEdit);
    }

    private List<Change> getChanges(IDocument document, TextEdit textEdit) throws BadLocationException {
        final List<Change> changes = new ArrayList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        document.addDocumentListener(new IDocumentListener() {
            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                Change dto = dtoFactory.createDto(Change.class);
                dto.setLength(event.getLength());
                dto.setOffset(event.getOffset());
                dto.setText(event.getText());
                changes.add(dto);
            }
        });
        textEdit.apply(document);
        return changes;
    }
}
