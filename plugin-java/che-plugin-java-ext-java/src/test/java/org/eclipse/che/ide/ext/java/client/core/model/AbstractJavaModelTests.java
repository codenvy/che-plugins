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

import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.ide.ext.java.client.core.quickfix.TestOptions;
import org.eclipse.che.ide.ext.java.emul.FileSystem;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTParser;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.internal.codeassist.SelectionEngine;
import org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionRequestor;
import org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionResult;
import org.eclipse.che.ide.ext.java.worker.WorkerMessageHandler;
import com.googlecode.gwt.test.GwtModule;
import com.googlecode.gwt.test.GwtTestWithMockito;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Evgen Vidolob
 */
@GwtModule("org.eclipse.che.ide.ext.java.Java")
public abstract class AbstractJavaModelTests extends GwtTestWithMockito{
    protected static   FileSystem nameEnvironment =
            new FileSystem(new String[]{System.getProperty("java.home") + "/lib/rt.jar"}, null, "UTF-8");


    @Before
    public void createEnvironment() throws Exception {
        new WorkerMessageHandler(null);
        GwtReflectionUtils.setPrivateFieldValue(WorkerMessageHandler.get(), "nameEnvironment", nameEnvironment);

    }

    public static CompilationUnit parse(String content, String name){
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName(name);
        parser.setSource(content);
        parser.setNameEnvironment(nameEnvironment);
        parser.setResolveBindings(true);
        ASTNode ast = parser.createAST();
        return (CompilationUnit)ast;
    }

    public static SelectionResult codeSelect(String content, String className, String selectAt, String selection){
        org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit compilationUnit = parse(content, className);
        SelectionRequestor requestor = new SelectionRequestor(compilationUnit, content);
        SelectionEngine selectionEngine = new SelectionEngine(nameEnvironment, requestor, TestOptions.getDefaultOptions());
        int start = content.indexOf(selectAt);
        int length = selection.length();
        selectionEngine.select(new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(content.toCharArray(), className + ".java", "UTF-8"),start, start + length -1);
        return requestor.getSelectionResult();
    }

    public static String getCompilationUnit(String path){
        InputStream stream = null;
        try {
            stream = AbstractJavaModelTests.class.getResource(path).openStream();
            return IoUtil.readStream(stream);
        } catch (IOException e) {
            e.printStackTrace();
            if(stream != null){
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return "";
    }
}
