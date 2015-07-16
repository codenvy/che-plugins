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

package org.eclipse.che.ide.ext.java.client.core.model;

import org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionResult;

import org.junit.Test;

import static org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionResult.Type;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Evgen Vidolob
 */
public class ResolveTest extends AbstractJavaModelTests {

    @Test
    public void testArgumentName1() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("public class ResolveArgumentName {\n");
        builder.append("\tpublic void foo(Object var1, int var2){\n");
        builder.append("\t\n");
        builder.append("\t}\n");
        builder.append("}");

        SelectionResult result = codeSelect(builder.toString(), "ResolveArgumentName", "var1", "var1");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(builder.indexOf("var1"));
    }

    @Test
    public void testArrayLength() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveArrayLength.java");
        SelectionResult result = codeSelect(cu, "ResolveArrayLength", "length", "length");
        assertThat(result).isNull();
    }

    @Test
    public void testClass1() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveClass1.java");
        SelectionResult result = codeSelect(cu, "ResolveClass1", "AtomicBoolean", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testClass2() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveClass2.java");
        SelectionResult result = codeSelect(cu, "ResolveClass2", "AtomicBoolean", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testClass3() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveClass3.java");
        SelectionResult result = codeSelect(cu, "ResolveClass3", "AtomicBoolean[]{", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testClass4() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveClass4.java");
        SelectionResult result = codeSelect(cu, "ResolveClass4", "AtomicBoolean", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testClass5() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveClass5.java");
        SelectionResult result = codeSelect(cu, "ResolveClass5", "AtomicBoolean", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testClass6() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveClass6.java");
        SelectionResult result = codeSelect(cu, "ResolveClass6", "AtomicBoolean", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testResolveConstructor() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveConstructor.java");
        SelectionResult result =
                codeSelect(cu, "ResolveConstructor", "ResolveConstructor(\"", "ResolveConstructor");
        assertThat(result).isNotNull();
        assertThat(result.isSource()).isTrue();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(81);
        assertThat(result.getFqn()).isEqualTo("ResolveConstructor");
        assertThat(result.getKey()).isEqualTo("LResolveConstructor;.ResolveConstructor(Ljava/lang/String;)V");
    }

    @Test
    public void testUnknownConstructor() throws Exception {
        String cu = "public class Type {\n" +
                    "  void foo() {\n" +
                    "    new AClass(unknown) {};\n" +
                    "  }\n" +
                    "}\n" +
                    "class AClass {\n" +
                    "}\n";
        SelectionResult result =
                codeSelect(cu, "Type", "AClass(unknown)", "AClass");
        assertThat(result).isNotNull();
        assertThat(result.isSource()).isTrue();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("AClass "));
        assertThat(result.getFqn()).isEqualTo("AClass");
    }

    @Test
    public void testConstructorUnknownParameter() throws Exception {

        String cu = "package test.p1;" +
                    "public class Type {\n" +
                    "  void foo() {\n" +
                    "    new AClass(unknown) {};\n" +
                    "  }\n" +
                    "}\n" +
                    "class AClass {\n" +
                    "  public AClass(Object o) {}\n" +
                    "}\n";
        SelectionResult result =
                codeSelect(cu, "Type", "AClass(unknown)", "AClass");
        assertThat(result).isNotNull();
        assertThat(result.isSource()).isTrue();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("AClass(Object o)"));
        assertThat(result.getFqn()).isEqualTo("test.p1.AClass");
    }

    @Test
    public void testEmptyCU() throws Exception {
        String cu = "//this CU must contain only an unknown type name" +
                    "Unknown\n" +
                    "\n";
        SelectionResult result =
                codeSelect(cu, "Type", "Unknown", "Unknown");
        assertThat(result).isNull();

    }

    @Test
    public void testEmptySelection1() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveTypeEmptySelection.java");
        SelectionResult result = codeSelect(cu, "ResolveTypeEmptySelection", "ject", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(-1);
        assertThat(result.isSource()).isFalse();
        assertThat(result.getFqn()).isEqualTo("java.lang.Object");
    }

    @Test
    public void testEmptySelection2() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveTypeEmptySelection2.java");
        SelectionResult result = codeSelect(cu, "ResolveTypeEmptySelection2", "Obj", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(-1);
        assertThat(result.isSource()).isFalse();
        assertThat(result.getKey()).isEqualTo("Ljava/lang/Object;");
    }

    @Test
    public void testExplicitSuperConstructorCall() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveExplicitSuperConstructorCall.java");
        SelectionResult result = codeSelect(cu, "ResolveExplicitSuperConstructorCall", "super(", "super");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(-1);
        assertThat(result.isSource()).isFalse();
        assertThat(result.getKey()).isEqualTo("Ljava/math/BigDecimal;.BigDecimal(I)V");
    }

    @Test
    public void testExplicitThisConstructorCall() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveExplicitThisConstructorCall.java");
        SelectionResult result = codeSelect(cu, "ResolveExplicitThisConstructorCall", "this(", "this");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("ResolveExplicitThisConstructorCall()"));
        assertThat(result.isSource()).isTrue();
        assertThat(result.getKey()).isEqualTo("LResolveExplicitThisConstructorCall;.ResolveExplicitThisConstructorCall()V");
    }

    @Test
    public void testField() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveField.java");
        SelectionResult result = codeSelect(cu, "ResolveField", "foo =", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getFqn()).isEqualTo("ResolveField");
        assertThat(result.getOffset()).isEqualTo(40);
        assertThat(result.getType()).isEqualTo(Type.FIELD);
    }

    @Test
    public void testInnerClassAsParamater() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveInnerClassAsParamater.java");
        SelectionResult result = codeSelect(cu, "ResolveInnerClassAsParamater",  "foo(i)", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo(Inner i)"));
        assertThat(result.getType()).isEqualTo(Type.METHOD);
    }

    @Test
    public void testInterface() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveInterface.java");
        SelectionResult result = codeSelect(cu, "ResolveInterface",  "RandomAccess", "RandomAccess");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(-1);
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.getKey()).isEqualTo("Ljava/util/RandomAccess;");
    }

    @Test
    public void testLocalClass1() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass1.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass1",  "Y[]", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Y");
    }

    @Test
    public void testLocalClass2() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass2.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass2",  "Y y", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
    }

    @Test
    public void testLocalClass3() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass3.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass3",  "Y[]{", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
    }

    @Test
    public void testLocalClass4() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass4.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass4",  "Y bar()", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
    }

    @Test
    public void testLocalClass5() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass5.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass5",  "Y y", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
    }

    @Test
    public void testLocalClass6() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass6.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass6",  "Y { // superclass", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
    }

    @Test
    public void testLocalClass7() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalClass7.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalClass7",  "X var", "X");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("X {"));
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
    }


    @Test
    public void testResolveLocalConstructor() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalConstructor.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalConstructor",  "Y(\"", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y(String s)"));
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
    }


    @Test
    public void testLocalField() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalField.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalField", "fred =", "fred");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("fred;"));
    }

    @Test
    public void testLocalField2() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalField2.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalField2", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void testLocalFieldDeclaration() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalFieldDeclaration.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalFieldDeclaration", "fred", "fred");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("fred;"));
    }

    @Test
    public void testLocalMethod() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalMethod.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalMethod", "foo(\"", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo(String"));
    }

    @Test
    public void testLocalMethod2() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalMethod2.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalMethod2", "bar();", "bar");
        assertThat(result).isNotNull();
        assertThat(result.isSource()).isTrue();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("bar() {"));
    }

    @Test
    public void testLocalName1() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var1 = new Object();", "var1");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var1 = new Object();"));
    }

    @Test
    public void testLocalName2() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var2 = 1;", "var2");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.isDeclaration()).isFalse();
    }

    @Test
    public void testLocalName3() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var1.toString();", "var1");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var1 = new Object();"));
    }

    @Test
    public void testLocalName4() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var2++;", "var2");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var2 = 1;"));
        assertThat(result.getFqn()).isNull();
    }

    @Test
    public void testLocalName5() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var3.hashCode();", "var3");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var3 = var1;"));
    }

    @Test
    public void testLocalName6() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var3.toString();", "var3");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var3 = new Object();"));
    }

    @Test
    public void testLocalName7() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveLocalName.java");
        SelectionResult result = codeSelect(cu, "ResolveLocalName", "var4;", "var4");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var4 = 1;"));
    }

    @Test
    public void testMessageSendOnBaseType() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveMessageSendOnBaseType.java");
        SelectionResult result = codeSelect(cu, "ResolveMessageSendOnBaseType", "hello", "hello");
        assertThat(result).isNull();
    }

    @Test
    public void testPartiallyQualifiedType() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolvePartiallyQualifiedType.java");
        SelectionResult result = codeSelect(cu, "ResolvePartiallyQualifiedType", "lang.Object", "lang.Object");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.getOffset()).isEqualTo(-1);
        assertThat(result.getKey()).isEqualTo("Ljava/lang/Object;");
    }

    @Test
    public void testQualifiedType() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveQualifiedType.java");
        SelectionResult result = codeSelect(cu, "ResolveQualifiedType", "java.lang.Object", "java.lang.Object");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.getOffset()).isEqualTo(-1);
        assertThat(result.getKey()).isEqualTo("Ljava/lang/Object;");
    }

    @Test
    public void testTypeInComment() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src/ResolveTypeInComment.java");
        SelectionResult result = codeSelect(cu, "ResolveTypeInComment", "AtomicBoolean */", "AtomicBoolean");
        assertAtomicClass(result);
    }

    @Test
    public void testDuplicateLocals1() throws Exception {
        String cu ="package test;"+
                   "public class Test {\n" +
                   "	void foo() {\n" +
                   "		int x = 0;\n" +
                   "		String x = null;\n" +
                   "		x.indexOf;\n" +
                   "	}\n" +
                   "}";
        SelectionResult result = codeSelect(cu, "Test", "x.", "x");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("x = null;"));
    }

    private void assertAtomicClass(SelectionResult result) {
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isDeclaration()).isFalse();
        assertThat(result.isSource()).isFalse();
        assertThat(result.getFqn()).isEqualTo("java.util.concurrent.atomic.AtomicBoolean");
        assertThat(result.getKey()).isEqualTo("Ljava/util/concurrent/atomic/AtomicBoolean;");
        assertThat(result.getOffset()).isEqualTo(-1);
    }
}
