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

import org.eclipse.che.ide.ext.java.emul.FileSystem;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTParser;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.worker.WorkerMessageHandler;
import com.googlecode.gwt.test.GwtModule;
import com.googlecode.gwt.test.GwtTestWithMockito;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Evgen Vidolob
 */
@GwtModule("org.eclipse.che.ide.ext.java.Java")
public class ParserTest extends GwtTestWithMockito {

    private static FileSystem nameEnvironment =
            new FileSystem(new String[]{System.getProperty("java.home") + "/lib/rt.jar"}, null, "UTF-8");


    @Before
    public void setUp() throws Exception {
        new WorkerMessageHandler(null);
        GwtReflectionUtils.setPrivateFieldValue(WorkerMessageHandler.get(), "nameEnvironment", nameEnvironment);

    }

    @Test
    public void testAnnotationWithConstant() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("public class Test{\n");
        builder.append("@java.beans.ConstructorProperties(java.beans.DesignMode.PROPERTYNAME)");
        builder.append("public Test(){\n");
        builder.append("}\n}\n");
        CompilationUnit unit = parse(builder.toString());
        for (IProblem problem : unit.getProblems()) {
            System.out.println(problem.getMessage());
        }
        IProblem[] tasks = (IProblem[])unit.getProperty("tasks");
        if(tasks != null) {
            for (IProblem task : tasks) {
                System.out.println(task.getMessage());
            }
        }
    }

    protected static CompilationUnit parse(String content){
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("/P/org/test/Test.java");
        parser.setSource(content);
        parser.setNameEnvironment(nameEnvironment);
        parser.setResolveBindings(true);
        ASTNode ast = parser.createAST();
        return (CompilationUnit)ast;
    }
}
