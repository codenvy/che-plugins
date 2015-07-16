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
import org.eclipse.che.ide.ext.java.jdt.codeassistant.CompletionProposalCollector;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.FillArgumentNamesCompletionProposalCollector;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.api.JavaCompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.core.JavaCore;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTParser;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.che.ide.ext.java.worker.WorkerDocument;
import org.eclipse.che.ide.ext.java.worker.WorkerMessageHandler;
import org.eclipse.che.ide.ext.java.jdt.text.Document;
import com.googlecode.gwt.test.GwtModule;
import com.googlecode.gwt.test.GwtTestWithMockito;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Evgen Vidolob
 */
@GwtModule("org.eclipse.che.ide.ext.java.Java")
public class CodeAssistantTest extends GwtTestWithMockito {


    private static FileSystem nameEnvironment =
            new FileSystem(new String[]{System.getProperty("java.home") + "/lib/rt.jar"}, null, "UTF-8");


    @Before
    public void setUp() throws Exception {
        new WorkerMessageHandler(null);
        GwtReflectionUtils.setPrivateFieldValue(WorkerMessageHandler.get(), "nameEnvironment", nameEnvironment);

    }

    @Test
    public void testName() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("import org.omg.CORBA.portable.InputStream;\n");
        builder.append("\n");
        builder.append("public class Test {\n");
        builder.append("  public Test(){\n");
        builder.append("    InputStream s = new \n");
        builder.append("  }\n");
        builder.append("}");
        JavaCompletionProposal[] completionProposals = computeCompletionProposals(builder.toString(), 105);
        Document document = new WorkerDocument(builder.toString());
        completionProposals[1].apply(document);
        Assertions.assertThat(document.get()).doesNotContain("@Overrid@Overrid@Overrid@Overrid@Overrid@Overrid@Overrid@Overrid@Overrid@Overrid@Overr");
    }

    public static JavaCompletionProposal[] computeCompletionProposals(String content, int offset) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("/P/org/test/CreateJavaClassPresenter.java");
        parser.setSource(content);
        parser.setNameEnvironment(nameEnvironment);
        parser.setResolveBindings(true);
        ASTNode ast = parser.createAST();
        CompilationUnit unit = (CompilationUnit)ast;
        Document document = new WorkerDocument(content);
        CompletionProposalCollector collector =
                new FillArgumentNamesCompletionProposalCollector(unit, document, offset, "projectPath", "docContext", "vfsId");

        collector
                .setAllowsRequiredProposals(
                        CompletionProposal.CONSTRUCTOR_INVOCATION,
                        CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION,
                                             CompletionProposal.TYPE_REF, true);
        collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_DECLARATION,
                                             CompletionProposal.TYPE_REF,
                                             true);

        collector.setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, false);
        collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, false);
        collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, false);
        collector.setIgnored(CompletionProposal.FIELD_REF, false);
        collector.setIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, false);
        collector.setIgnored(CompletionProposal.KEYWORD, false);
        collector.setIgnored(CompletionProposal.LABEL_REF, false);
        collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
        collector.setIgnored(CompletionProposal.METHOD_DECLARATION, false);
        collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, false);
        collector.setIgnored(CompletionProposal.METHOD_REF, false);
        collector.setIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, false);
        collector.setIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, false);
        collector.setIgnored(CompletionProposal.PACKAGE_REF, false);
        collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, false);
        collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, false);
        collector.setIgnored(CompletionProposal.TYPE_REF, false);
        CompletionEngine e = new CompletionEngine(nameEnvironment, collector, JavaCore.getOptions());
        e.complete(new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(
                content.toCharArray(),
                "name", "UTF-8"), offset, 0);
        return collector.getJavaCompletionProposals();
    }

}
