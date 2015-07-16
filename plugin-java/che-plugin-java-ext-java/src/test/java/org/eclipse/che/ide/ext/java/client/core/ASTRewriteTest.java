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
package org.eclipse.che.ide.ext.java.client.core;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.che.ide.api.text.BadLocationException;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTParser;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ImportDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.che.ide.ext.java.worker.WorkerDocument;
import org.eclipse.che.ide.ext.java.jdt.text.Document;
import org.eclipse.che.ide.ext.java.jdt.text.edits.MalformedTreeException;
import org.eclipse.che.ide.ext.java.jdt.text.edits.TextEdit;
import org.eclipse.che.ide.ext.java.jdt.text.edits.UndoEdit;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: 12:52:10 PM 34360 2009-07-22 23:58:59Z evgen $
 */
public class ASTRewriteTest extends ParserBaseTest {

    @Test
    public void testRewrite() {

        Document document = new WorkerDocument("import java.util.List;\nclass X {}\n");
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(document.get().toCharArray());
        CompilationUnit cu = (CompilationUnit)parser.createAST();
        AST ast = cu.getAST();
        ImportDeclaration id = ast.newImportDeclaration();
        id.setName(ast.newName(new String[]{"java", "util", "Set"}));
        ASTRewrite rewriter = ASTRewrite.create(ast);
        TypeDeclaration td = (TypeDeclaration)cu.types().get(0);
        ITrackedNodePosition tdLocation = rewriter.track(td);
        ListRewrite lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
        lrw.insertLast(id, null);
        TextEdit edits = rewriter.rewriteAST(document, null);
        UndoEdit undo = null;
        try {
            undo = edits.apply(document);
        } catch (MalformedTreeException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        Assert.assertTrue("import java.util.List;\nimport java.util.Set;\nclass X {}\n".equals(document.get()));
        // tdLocation.getStartPosition() and tdLocation.getLength()
        // are new source range for &quot;class X {}&quot; in document.get()
    }

}
