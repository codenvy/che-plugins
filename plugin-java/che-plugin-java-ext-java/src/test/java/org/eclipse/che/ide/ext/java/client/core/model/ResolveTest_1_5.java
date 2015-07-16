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
import org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionResult.Type;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Evgen Vidolob
 */
public class ResolveTest_1_5 extends AbstractJavaModelTests {

    @Test
    public void test0001() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0001/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "iii){ //", "iii");
        assertThat(result).isNotNull();
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("iii"));
    }

    @Test
    public void test0002() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0002/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Y", "Y");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Y <TY> {"));
    }

    @Test
    public void test0003() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0003/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "X", "X");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("X <TX> {"));
    }

    @Test
    public void test0004() throws Exception {
        String cu = "package test0004;\n" +
                    "public class Test <T> {\n" +
                    "	test0004.Test.X<Object>.Y<Object> var;\n" +
                    "	public class X <TX> {\n" +
                    "		public class Y <TY> {\n" +
                    "		}\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test <T>"));
    }

    @Test
    public void test0005() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0005/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "test0005", "test0005");
        //we don't support selection on package
        assertThat(result).isNull();
    }

    @Test
    public void test0006() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0006/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0006>", "Test0006");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0006> {"));
    }

    @Test
    public void test0007() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0007/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0007 var;", "Test0007");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0007");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0007> {"));
    }

    @Test
    public void test0008() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0008/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0008>", "Test0008");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0008");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0008> {"));
    }

    @Test
    public void test0009() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0009/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0009 var", "Test0009");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0009");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0009> {"));
    }

    @Test
    public void test0010() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0010/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0010 var", "Test0010");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0010");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0010> {"));
    }

    @Test
    public void test0011() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0011/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0011> void", "Test0011");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0011");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0011> void"));
    }

    @Test
    public void test0012() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0012/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0012 var;", "Test0012");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0012");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0012> void"));
    }

    @Test
    public void test0013() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0013/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0013>", "Test0013");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0013");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0013>"));
    }

    @Test
    public void test0014() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0014/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test0014 var", "Test0014");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("Test0014");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test0014> void"));
    }

    @Test
    public void test0015() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0015/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.VARIABLE);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0016() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0016/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "T>", "T");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("T");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("T> void"));
    }

    @Test
    public void test0017() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0017/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "T f", "T");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("T");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("T>"));
    }

    @Test
    public void test0018() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0018/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "T t", "T");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("T");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("T> v"));
    }

    @Test
    public void test0019() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0019/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "T t", "T");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("T");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("T> v"));
    }

    @Test
    public void test0020() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0020/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "T> x", "T");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("T");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("T> v"));
    }

    @Test
    public void test0021() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0021/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "T {", "T");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD_TYPE_PARAMETER);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("T");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("T extends"));
    }

    @Test
    public void test0024() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0024/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("test0024.Test");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test {"));
    }

    @Test
    public void test0025() throws Exception {
        String cu = getCompilationUnit("/workspace/resolve/src2/test0025/Test.java");
        SelectionResult result = codeSelect(cu, "Test", "Test", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("test0025.Test");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test {"));
    }

    @Test
    public void test0026() throws Exception {
        String cu = "package test0026;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test.Inner x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner x", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("test0026.Test.Inner");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0027() throws Exception {
        String cu = "package test0027;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<Object", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("test0027.Test.Inner");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }


    @Test
    public void test0028() throws Exception {
        String cu = "package test0028;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test<Object>.Inner x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner x", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("test0028.Test.Inner");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0029() throws Exception {
        String cu = "package test0029;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test<Object>.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<Obj", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getFqn()).isEqualTo("test0029.Test.Inner");
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0030() throws Exception {
        String cu = "package test0030;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test.Inner x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner x", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0031() throws Exception {
        String cu = "package test0031;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<Obj", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0032() throws Exception {
        String cu = "package test0032;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {	\n" +
                    "	}\n" +
                    "	Test<Object>.Inner x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner x", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0033() throws Exception {
        String cu = "package test0033;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test<Object>.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<Objec", "");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0034() throws Exception {
        String cu = "package test0034;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test.Inner x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test.Inner", "Test.Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0035() throws Exception {
        String cu = "package test0035;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {	\n" +
                    "	}\n" +
                    "	Test.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test.Inner<Object>", "Test.Inner<Object>");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }


    @Test
    public void test0036() throws Exception {
        String cu = "package test0036;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {	\n" +
                    "	}\n" +
                    "	Test<Object>.Inner x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<Object>.Inner", "Test<Object>.Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0037() throws Exception {
        String cu = "package test0037;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {	\n" +
                    "	}\n" +
                    "	Test<Object>.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<Object>.Inner<Object>", "Test<Object>.Inner<Object>");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0038() throws Exception {
        String cu = "package test0038;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test.Inner", "Test.Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0039() throws Exception {
        String cu = "package test0039;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test<Object>.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<Object>.Inner", "Test<Object>.Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0040() throws Exception {
        String cu = "package test0040;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {\n" +
                    "	}\n" +
                    "	Test<Object>.Inner<Object> x;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<Object>", "Inner<Object>");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0041() throws Exception {
        String cu = "package test0041;\n" +
                    "public class Test<T> {\n" +
                    "	void foo() {\n" +
                    "		class Local1<T1> {\n" +
                    "			class Local2<T2> {\n" +
                    "			}\n" +
                    "		}\n" +
                    "		class Local3<T3> {\n" +
                    "		} \n" +
                    "		Local1<Local3<Object>>.Local2<Local3<Object>> l;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Local1<Local3<Object>>.Local2<Local3<Object>>", "Local1<Local3<Object>>.Local2<Local3<Object>>");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Local2<T2>"));
    }

    @Test
    public void test0042() throws Exception {
        String cu = "package test0042;\n" +
                    "public class Test<T> {\n" +
                    "	public class Inner<U> {	\n" +
                    "	}\n" +
                    "	Test<? super String>.Inner<? extends String> v;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<? super String>.Inner<? extends String>", "Test<? super String>.Inner<? extends String>");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U>"));
    }

    @Test
    public void test0043() throws Exception {
        String cu = "package test0043;\n" +
                    "public class Test<T> {\n" +
                    "	Test<T> var;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<T> v", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test<T> {"));
    }

    @Test
    public void test0044() throws Exception {
        String cu ="package test0044;\n" +
                   "public class Test<T1> {\n" +
                   "}\n" +
                   "class Test2<T2> {\n" +
                   "	Test<T2> var;\n" +
                   "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<T2>", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test<T1> {"));
    }

    @Test
    public void test0045() throws Exception {
        String cu = "package test0045;\n" +
                    "public class Test<T1> {\n" +
                    "	String var;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var"));
    }

    @Test
    public void test0046() throws Exception {
        String cu = "package test0046;\n" +
                    "public class Test<T1> {\n" +
                    "	String var;\n" +
                    "	void foo() {\n" +
                    "	  var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0047() throws Exception {
        String cu = "package test0047;\n" +
                    "public class Test<T1> {\n" +
                    "	public String var;\n" +
                    "	void foo() {\n" +
                    "	  Test<String> t = null;\n" +
                    "	  t.var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0048() throws Exception {
        String cu = "package test0048;\n" +
                    "public class Test<T1> {\n" +
                    "	public String var;\n" +
                    "	void foo() {\n" +
                    "	  Test<?> t = new Test<String>;\n" +
                    "	  t.var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0049() throws Exception {
        String cu = "package test0049;\n" +
                    "public class Test<T1> {\n" +
                    "	public String var;\n" +
                    "	void foo() {\n" +
                    "	  Test<T1> t = null;\n" +
                    "	  t.var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0050() throws Exception {
        String cu = "package test0050;\n" +
                    "public class Test<T1> {\n" +
                    "	public String var;\n" +
                    "	void foo() {\n" +
                    "	  Test t = null;\n" +
                    "	  t.var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0051() throws Exception {
        String cu = "package test0051;\n" +
                    "public class Test {\n" +
                    "	void foo() {\n" +
                    "	  class Inner<T> {\n" +
                    "	    public String var;\n" +
                    "	  }" +
                    "	  Inner<Object> i = null;\n" +
                    "	  i.var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0052() throws Exception {
        String cu = "package test0052;\n" +
                    "public class Test {\n" +
                    "	void foo() {\n" +
                    "	  class Inner<T> {\n" +
                    "	    public T var;\n" +
                    "	  }" +
                    "	  Inner<Object> i = null;\n" +
                    "	  i.var = null;\n" +
                    "	}\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "var =", "var");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.FIELD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("var;"));
    }

    @Test
    public void test0053() throws Exception {
        String cu = "package test0053;\n" +
                    "public class Test<T> {\n" +
                    "	public void foo() {\n" +
                    "   }\n" +
                    "}\n" +
                    "class Test2<T> {\n" +
                    "  void bar() {\n" +
                    "    Test<String> var = null;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0054() throws Exception {
        String cu = "package test0054;\n" +
                    "public class Test<T> {\n" +
                    "	public void foo() {\n" +
                    "   }\n" +
                    "}\n" +
                    "class Test2<T> {\n" +
                    "  void bar() {\n" +
                    "    Test var = null;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0055() throws Exception {
        String cu = "package test0055;\n" +
                    "public class Test<T> {\n" +
                    "	public void foo() {\n" +
                    "   }\n" +
                    "}\n" +
                    "class Test2<T> {\n" +
                    "  void bar() {\n" +
                    "    Test<T> var = null;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0056() throws Exception {
        String cu = "package test0056;\n" +
                    "public class Test<T> {\n" +
                    "  public void foo() {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    Test<T> var = null;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0057() throws Exception {
        String cu = "package test0057;\n" +
                    "public class Test<T1> {\n" +
                    "  public <T2> void foo() {\n" +
                    "  }\n" +
                    "}\n" +
                    "class Test2 {\n" +
                    "  void bar() {\n" +
                    "    Test<String> var = null;\n" +
                    "    var.<Object>foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0058() throws Exception {
        String cu = "package test0058;\n" +
                    "public class Test<T1> {\n" +
                    "  public <T2> void foo() {\n" +
                    "  }\n" +
                    "}\n" +
                    "class Test2 {\n" +
                    "  void bar() {\n" +
                    "    Test<String> var = null;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0059() throws Exception {
        String cu = "package test0059;\n" +
                    "public class Test {\n" +
                    "  public <T2> void foo() {\n" +
                    "  }\n" +
                    "}\n" +
                    "class Test2 {\n" +
                    "  void bar() {\n" +
                    "    Test var = null;\n" +
                    "    var.<String>foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0060() throws Exception {
        String cu = "package test0060;\n" +
                    "public class Test {\n" +
                    "  public <T2> void foo() {\n" +
                    "  }\n" +
                    "}\n" +
                    "class Test2 {\n" +
                    "  void bar() {\n" +
                    "    Test var = null;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0061() throws Exception {
        String cu = "package test0061;\n" +
                    "public class Test {\n" +
                    "  public <T2> void foo() {\n" +
                    "    Test var;\n" +
                    "    var.<T2>foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0062() throws Exception {
        String cu = "package test0062;\n" +
                    "public class Test<T1> {\n" +
                    "  public <T2> void foo() {\n" +
                    "    Test var;\n" +
                    "    var.<T1>foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0063() throws Exception {
        String cu = "package test0063;\n" +
                    "public class Test<T1> {\n" +
                    "  public void foo() {\n" +
                    "  }\n" +
                    "}\n" +
                    "class Test2 {\n" +
                    "  void bar() {\n" +
                    "    Test<String> var;\n" +
                    "    var.foo();\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "foo();", "foo");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("foo() {"));
    }

    @Test
    public void test0064() throws Exception {
        String cu = "package test0064;\n" +
                    "public class Test {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new <String>Test(null);\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test(n", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0065() throws Exception {
        String cu = "package test0065;\n" +
                    "public class Test {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new Test(null);\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test(n", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0066() throws Exception {
        String cu = "package test0066;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new <String>Test<String>(null);\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<S", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0067() throws Exception {
        String cu = "package test0067;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new Test<String>(null);\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<S", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0068() throws Exception {
        String cu = "package test0068;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new Test(null);\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test(n", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0069() throws Exception {
        String cu = "package test0069;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  public class Inner<V> {\n" +
                    "    public <W> Inner(W w) {\n" +
                    "    }\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new <String>Test<String>(null).new <String>Inner<String>(null);\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<St", "Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner(W"));
    }

    @Test
    public void test0070() throws Exception {
        String cu = "package test0070;\n" +
                    "public class Test {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new <String>Test(null){};\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test(n", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0071() throws Exception {
        String cu = "package test0071;\n" +
                    "public class Test {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new Test(null){};\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test(n", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0072() throws Exception {
        String cu = "package test0072;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new <String>Test<String>(null){};\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<S", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0073() throws Exception {
        String cu = "package test0073;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new Test<String>(null){};\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test<S", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0074() throws Exception {
        String cu = "package test0074;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new Test(null){};\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Test(n", "Test");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Test(U"));
    }

    @Test
    public void test0075() throws Exception {
        String cu = "package test0075;\n" +
                    "public class Test<T> {\n" +
                    "  public <U> Test(U u) {\n" +
                    "  }\n" +
                    "  public class Inner<V> {\n" +
                    "    public <W> Inner(W w) {\n" +
                    "    }\n" +
                    "  }\n" +
                    "  void bar() {\n" +
                    "    new <String>Test<String>(null).new <String>Inner<String>(null){};\n" +
                    "  }\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<S", "Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner(W"));
    }

    @Test
    public void test0076() throws Exception {
        String cu = "package test0076;\n" +
                    "public class Test<T> {\n" +
                    "  public class Inner<U, V> {\n" +
                    "  }\n" +
                    "  Test<? super String>.Inner<int[][], Test<String[]>> var;\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "Inner<i", "Inner");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("Inner<U, V"));
    }

    @Test
    public void test0077() throws Exception {
        String cu = "package test0077;\n" +
                    "@interface MyAnn {\n" +
                    "}\n" +
                    "public @MyAnn class Test {\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "MyAnn c", "MyAnn");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("MyAnn {"));
    }

    @Test
    public void test0078() throws Exception {
        String cu = "package test0078;\n" +
                    "@interface MyAnn {\n" +
                    "  String value();\n" +
                    "}\n" +
                    "public @MyAnn(\"\") class Test {\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "MyAnn(", "MyAnn");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("MyAnn {"));
    }

    @Test
    public void test0079() throws Exception {
        String cu = "package test0079;\n" +
                    "@interface MyAnn {\n" +
                    "  String value();\n" +
                    "}\n" +
                    "public @MyAnn class Test {\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "MyAnn c", "MyAnn");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("MyAnn {"));
    }

    @Test
    public void test0080() throws Exception {
        String cu = "package test0080;\n" +
                    "@interface MyAnn {\n" +
                    "  String value1();\n" +
                    "  String value2();\n" +
                    "}\n" +
                    "public @MyAnn(value1 = \"\", value2 = \"\") class Test {\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "MyAnn(v", "MyAnn");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.CLASS);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("MyAnn {"));
    }

    @Test
    public void test0081() throws Exception {
        String cu = "package test0080;\n" +
                    "@interface MyAnn {\n" +
                    "  String value1();\n" +
                    "  String value2();\n" +
                    "}\n" +
                    "public @MyAnn(value1 = \"\", value2 = \"\") class Test {\n" +
                    "}";
        SelectionResult result = codeSelect(cu, "Test", "value1 = ", "value1");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Type.METHOD);
        assertThat(result.isSource()).isTrue();
        assertThat(result.getOffset()).isEqualTo(cu.indexOf("value1();"));
    }
}
