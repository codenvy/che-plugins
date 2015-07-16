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
package org.eclipse.che.ide.ext.java;


import org.eclipse.che.jdt.BinaryTypeConvector;
import org.eclipse.che.jdt.BindingASTVisitor;
import org.eclipse.che.jdt.JsonSearchRequester;
import org.eclipse.che.jdt.TypeBindingConvector;
import org.eclipse.che.jdt.internal.core.SourceTypeElementInfo;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CodenvyCompilationUnitResolver;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.fest.assertions.Assertions;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
public class SearchableEnvironmentTest extends BaseTest{

    @Test
    public void testAccessRule() throws Exception {
        JsonSearchRequester storage = new JsonSearchRequester();
        project.getNameEnvironment().findTypes("st".toCharArray(), true, true, 0, storage);
        Assertions.assertThat(storage.toJsonString()).doesNotContain("com.sun.xml.internal.stream.buffer.stax.StreamWriterBufferProcessor")
                                                     .doesNotContain("sun.font.StandardGlyphVector")
                                                     .doesNotContain("sun.nio.cs.StreamDecoder ");
    }

    @Test
    public void testSourceAnnotation() throws Exception {
        NameEnvironmentAnswer answer = project.getNameEnvironment().findType("GenerateLink".toCharArray(),
                                                                           new char[][]{"com".toCharArray(), "codenvy".toCharArray(),
                                                                                        "test".toCharArray()});
        String type = processAnswer(answer, project, project.getNameEnvironment());
        Assertions.assertThat(type).contains("\"tagBits\":\"53118008391424\"");

    }

    private String processAnswer(NameEnvironmentAnswer answer, IJavaProject project, INameEnvironment environment)
            throws JavaModelException {
        if (answer == null) return null;
        if (answer.isBinaryType()) {
            IBinaryType binaryType = answer.getBinaryType();
            return BinaryTypeConvector.toJsonBinaryType(binaryType);
        } else if (answer.isCompilationUnit()) {
            ICompilationUnit compilationUnit = answer.getCompilationUnit();
            return getSourceTypeInfo(project, environment, compilationUnit);
        } else if (answer.isSourceType()) {
            ISourceType[] sourceTypes = answer.getSourceTypes();
            if (sourceTypes.length == 1) {
                ISourceType sourceType = sourceTypes[0];
                SourceTypeElementInfo elementInfo = (SourceTypeElementInfo)sourceType;
                IType handle = elementInfo.getHandle();
                org.eclipse.jdt.core.ICompilationUnit unit = handle.getCompilationUnit();
                return getSourceTypeInfo(project, environment, (ICompilationUnit)unit);
            }
        }
        return null;
    }

    private String getSourceTypeInfo(IJavaProject project, INameEnvironment environment, ICompilationUnit compilationUnit)
            throws JavaModelException {
        CompilationUnit result = getCompilationUnit(project, environment, compilationUnit);

        BindingASTVisitor visitor = new BindingASTVisitor();
        result.accept(visitor);
        Map<TypeBinding, ?> bindings = (Map<TypeBinding, ?>)result.getProperty("compilerBindingsToASTBindings");
        SourceTypeBinding binding = null;
        for (Map.Entry<TypeBinding, ?> entry : bindings.entrySet()) {
            if (entry.getValue().equals(visitor.getTypeBinding())) {
                binding = (SourceTypeBinding)entry.getKey();
                break;
            }
        }
        if (binding == null) return null;
        return TypeBindingConvector.toJsonBinaryType(binding);
    }

    private CompilationUnit getCompilationUnit(IJavaProject project, INameEnvironment environment,
                                               ICompilationUnit compilationUnit) throws JavaModelException {
        int flags = 0;
        flags |= org.eclipse.jdt.core.ICompilationUnit.ENABLE_STATEMENTS_RECOVERY;
        flags |= org.eclipse.jdt.core.ICompilationUnit.IGNORE_METHOD_BODIES;
        flags |= org.eclipse.jdt.core.ICompilationUnit.ENABLE_BINDINGS_RECOVERY;
        HashMap<String, String> opts = new HashMap<>(options);
        CompilationUnitDeclaration compilationUnitDeclaration =
                CodenvyCompilationUnitResolver.resolve(compilationUnit, project, environment, opts, flags, null);
        return CodenvyCompilationUnitResolver.convert(
                compilationUnitDeclaration,
                compilationUnit.getContents(),
                flags, opts);
    }
}
