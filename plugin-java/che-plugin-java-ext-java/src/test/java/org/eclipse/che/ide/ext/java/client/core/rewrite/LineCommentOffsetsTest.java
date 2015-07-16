/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.client.core.rewrite;

import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Block;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CatchClause;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Expression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ExpressionStatement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.IfStatement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.InfixExpression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.MethodDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.MethodInvocation;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.SimpleType;
import org.eclipse.che.ide.ext.java.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Statement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TryStatement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TypeParameter;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.internal.core.dom.rewrite.LineCommentEndOffsets;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class LineCommentOffsetsTest extends ASTRewritingTest {

    @Test
    public void testEmptyLineComments() throws Exception {

        StringBuffer buf = new StringBuffer();
        buf.append("\n");

        LineCommentEndOffsets offsets = new LineCommentEndOffsets(null);
        boolean res = offsets.isEndOfLineComment(0);
        assertFalse(res);
        res = offsets.remove(0);
        assertFalse(res);
    }

    @Test
    public void testRemove() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;//comment Y\n");
        buf.append("public class E//comment Y\n");
        buf.append("{//comment Y\n");
        buf.append("}//comment Y");
        String contents = buf.toString();
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);

        LineCommentEndOffsets offsets = new LineCommentEndOffsets(astRoot.getCommentList());

        int p1 = contents.indexOf('Y') + 1;
        int p2 = contents.indexOf('Y', p1) + 1;
        int p3 = contents.indexOf('Y', p2) + 1;
        int p4 = contents.indexOf('Y', p3) + 1;

        assertFalse(offsets.isEndOfLineComment(0));
        assertTrue(offsets.isEndOfLineComment(p1));
        assertTrue(offsets.isEndOfLineComment(p2));
        assertTrue(offsets.isEndOfLineComment(p3));
        assertTrue(offsets.isEndOfLineComment(p4));

        boolean res = offsets.remove(p2);
        assertTrue(res);

        res = offsets.remove(p2);
        assertFalse(res);

        assertFalse(offsets.isEndOfLineComment(0));
        assertTrue(offsets.isEndOfLineComment(p1));
        assertFalse(offsets.isEndOfLineComment(p2));
        assertTrue(offsets.isEndOfLineComment(p3));
        assertTrue(offsets.isEndOfLineComment(p4));

        res = offsets.remove(p4);
        assertTrue(res);

        assertFalse(offsets.isEndOfLineComment(0));
        assertTrue(offsets.isEndOfLineComment(p1));
        assertFalse(offsets.isEndOfLineComment(p2));
        assertTrue(offsets.isEndOfLineComment(p3));
        assertFalse(offsets.isEndOfLineComment(p4));

        res = offsets.remove(p1);
        assertTrue(res);

        assertFalse(offsets.isEndOfLineComment(0));
        assertFalse(offsets.isEndOfLineComment(p1));
        assertFalse(offsets.isEndOfLineComment(p2));
        assertTrue(offsets.isEndOfLineComment(p3));
        assertFalse(offsets.isEndOfLineComment(p4));
    }

    @Test
    public void testLineCommentEndOffsets() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("/* comment */\n");
        buf.append("// comment Y\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (i == 0) {\n");
        buf.append("            foo();\n");
        buf.append("            i++; // comment Y\n");
        buf.append("            i++;\n");
        buf.append("        }// comment// comment Y\n");
        buf.append("        return;\n");
        buf.append("    }\n");
        buf.append("} // comment Y");
        String content = buf.toString();

        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");
        CompilationUnit astRoot = createAST(cu);

        LineCommentEndOffsets offsets = new LineCommentEndOffsets(astRoot.getCommentList());
        HashSet expectedOffsets = new HashSet();

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == 'Y') {
                expectedOffsets.add(new Integer(i + 1));
            }
        }

        int count = 0;

        char[] charContent = content.toCharArray();
        for (int i = 0; i <= content.length() + 5; i++) {
            boolean expected = i > 0 && i <= content.length() && charContent[i - 1] == 'Y';
            boolean actual = offsets.isEndOfLineComment(i, charContent);
            assertEquals(expected, actual);

            actual = offsets.isEndOfLineComment(i);
            assertEquals(expected, actual);

            if (expected) {
                count++;
            }

        }
        assertEquals(4, count);
    }

    @Test
    public void testLineCommentEndOffsetsMixedLineDelimiter() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("/* comment */\r\n");
        buf.append("// comment Y\n");
        buf.append("public class E {\r\n");
        buf.append("    public void foo() {\n");
        buf.append("        while (i == 0) {\n");
        buf.append("            foo();\n");
        buf.append("            i++; // comment Y\r\n");
        buf.append("            i++;\n");
        buf.append("        }// comment// comment Y\r");
        buf.append("        return;\n");
        buf.append("    }\r\n");
        buf.append("} // comment Y");
        String content = buf.toString();

        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");
        CompilationUnit astRoot = createAST(cu);

        LineCommentEndOffsets offsets = new LineCommentEndOffsets(astRoot.getCommentList());
        HashSet expectedOffsets = new HashSet();

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == 'Y') {
                expectedOffsets.add(new Integer(i + 1));
            }
        }

        int count = 0;

        char[] charContent = content.toCharArray();
        for (int i = 0; i <= content.length() + 5; i++) {
            boolean expected = i > 0 && i <= content.length() && charContent[i - 1] == 'Y';
            boolean actual = offsets.isEndOfLineComment(i, charContent);
            assertEquals(expected, actual);
            if (expected) {
                count++;
            }

        }
        assertEquals(4, count);
    }

    @Test
    public void testCommentInLists() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E implements A //comment\n");
        buf.append("{\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");

        ListRewrite listRewrite = rewrite.getListRewrite(type, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
        SimpleType newInterface = ast.newSimpleType(ast.newSimpleName("B"));
        listRewrite.insertLast(newInterface, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E implements A //comment\n");
        buf.append(", B\n");
        buf.append("{\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testCommentInType() throws Exception {

        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E //comment\n");
        buf.append("{\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");

        ListRewrite listRewrite = rewrite.getListRewrite(type, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
        SimpleType newInterface = ast.newSimpleType(ast.newSimpleName("B"));
        listRewrite.insertLast(newInterface, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E //comment\n");
        buf.append(" implements B\n");
        buf.append("{\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testBug103340() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E //implements List\n");
        buf.append("{\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");

        ListRewrite listRewrite = rewrite.getListRewrite(type, TypeDeclaration.TYPE_PARAMETERS_PROPERTY);
        TypeParameter newType = ast.newTypeParameter();
        newType.setName(ast.newSimpleName("X"));
        listRewrite.insertLast(newType, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E //implements List\n");
        buf.append("<X>\n");
        buf.append("{\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testBug95839() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    object.method(\n");
        buf.append("      param1, // text about param1\n");
        buf.append("      param2  // text about param2\n");
        buf.append("    );\n");
        buf.append("  }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        ExpressionStatement statement =
                (ExpressionStatement)((MethodDeclaration)type.bodyDeclarations().get(0)).getBody().statements().get(0);
        MethodInvocation inv = (MethodInvocation)statement.getExpression();

        ListRewrite listRewrite = rewrite.getListRewrite(inv, MethodInvocation.ARGUMENTS_PROPERTY);
        listRewrite.insertLast(ast.newSimpleName("param3"), null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    object.method(\n");
        buf.append("      param1, // text about param1\n");
        buf.append("      param2  // text about param2\n");
        buf.append(", param3\n");
        buf.append("    );\n");
        buf.append("  }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testBug114418() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    try {\n");
        buf.append("    } catch (IOException e) {\n");
        buf.append("    }\n");
        buf.append("    // comment\n");
        buf.append("  }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        TryStatement statement =
                (TryStatement)((MethodDeclaration)type.bodyDeclarations().get(0)).getBody().statements().get(0);

        ListRewrite listRewrite = rewrite.getListRewrite(statement, TryStatement.CATCH_CLAUSES_PROPERTY);
        CatchClause clause = ast.newCatchClause();
        SingleVariableDeclaration newSingleVariableDeclaration = ast.newSingleVariableDeclaration();
        newSingleVariableDeclaration.setName(ast.newSimpleName("e"));
        newSingleVariableDeclaration.setType(ast.newSimpleType(ast.newSimpleName("MyException")));
        clause.setException(newSingleVariableDeclaration);

        listRewrite.insertLast(clause, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    try {\n");
        buf.append("    } catch (IOException e) {\n");
        buf.append("    }\n");
        buf.append("    // comment\n");
        buf.append(" catch (MyException e) {\n");
        buf.append("    }\n");
        buf.append("  }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testBug128818() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    if (true) {\n");
        buf.append("    } // comment\n");
        buf.append("    else\n");
        buf.append("      return;\n");
        buf.append("  }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        IfStatement statement =
                (IfStatement)((MethodDeclaration)type.bodyDeclarations().get(0)).getBody().statements().get(0);

        rewrite.set(statement, IfStatement.ELSE_STATEMENT_PROPERTY, ast.newBlock(), null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    if (true) {\n");
        buf.append("    } // comment\n");
        buf.append(" else {\n");
        buf.append("    }\n");
        buf.append("  }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testBug128422() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    if (i != 0 //I don't like 0\n");
        buf.append("                 && i != 10) {\n");
        buf.append("    }\n");
        buf.append("  }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        IfStatement statement =
                (IfStatement)((MethodDeclaration)type.bodyDeclarations().get(0)).getBody().statements().get(0);
        Expression expression = ((InfixExpression)statement.getExpression()).getLeftOperand();

        ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
        parenthesizedExpression.setExpression((Expression)rewrite.createCopyTarget(expression));
        rewrite.replace(expression, parenthesizedExpression, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    if ((i != 0 //I don't like 0\n");
        buf.append(")\n");
        buf.append("                 && i != 10) {\n");
        buf.append("    }\n");
        buf.append("  }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    @Test
    public void testBug128422b() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    foo(); //comment\n");
        buf.append("    foo();\n");
        buf.append("  }\n");
        buf.append("}\n");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);
        TypeDeclaration type = findTypeDeclaration(astRoot, "E");
        MethodDeclaration method = (MethodDeclaration)type.bodyDeclarations().get(0);
        List statements = method.getBody().statements();
        ASTNode copy = rewrite.createCopyTarget((ASTNode)statements.get(0));

        Block newBlock = ast.newBlock();
        newBlock.statements().add(newStatement(ast));
        newBlock.statements().add(copy);
        newBlock.statements().add(newStatement(ast));

        rewrite.getListRewrite(method.getBody(), Block.STATEMENTS_PROPERTY).insertLast(newBlock, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("  void foo() {\n");
        buf.append("    foo(); //comment\n");
        buf.append("    foo();\n");
        buf.append("    {\n");
        buf.append("        bar();\n");
        buf.append("        foo(); //comment\n");
        buf.append("        bar();\n");
        buf.append("    }\n");
        buf.append("  }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    private Statement newStatement(AST ast) {
        MethodInvocation inv = ast.newMethodInvocation();
        inv.setName(ast.newSimpleName("bar"));
        return ast.newExpressionStatement(inv);
    }

    @Test
    public void testCommentAtEnd() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E \n");
        buf.append("{\n");
        buf.append("}//comment");
        ICompilationUnit cu =
                new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(buf.toString().toCharArray(), "E.java", "");

        CompilationUnit astRoot = createAST3(cu);
        ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

        AST ast = astRoot.getAST();

        assertTrue("Parse errors", (astRoot.getFlags() & ASTNode.MALFORMED) == 0);

        ListRewrite listRewrite = rewrite.getListRewrite(astRoot, CompilationUnit.TYPES_PROPERTY);
        TypeDeclaration newType = ast.newTypeDeclaration();
        newType.setName(ast.newSimpleName("B"));
        listRewrite.insertLast(newType, null);

        String preview = evaluateRewrite(cu, rewrite);

        buf = new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E \n");
        buf.append("{\n");
        buf.append("}//comment\n");
        buf.append("\n");
        buf.append("class B {\n");
        buf.append("}");
        assertEqualString(preview, buf.toString());
    }

}
