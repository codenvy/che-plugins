/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.core;

import org.eclipse.che.ide.ext.java.jdt.core.compiler.CategorizedProblem;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.problem.DefaultProblemFactory;

public class CancelableProblemFactory extends DefaultProblemFactory {
    public CancelableProblemFactory() {
        super();
    }

    public CategorizedProblem createProblem(char[] originatingFileName, int problemId, String[] problemArguments,
                                            String[] messageArguments, int severity, int startPosition, int endPosition, int lineNumber,
                                            int columnNumber) {
        return super.createProblem(originatingFileName, problemId, problemArguments, messageArguments, severity,
                                   startPosition, endPosition, lineNumber, columnNumber);
    }

    public CategorizedProblem createProblem(char[] originatingFileName, int problemId, String[] problemArguments,
                                            int elaborationId, String[] messageArguments, int severity, int startPosition, int endPosition,
                                            int lineNumber,
                                            int columnNumber) {
        return super.createProblem(originatingFileName, problemId, problemArguments, elaborationId, messageArguments,
                                   severity, startPosition, endPosition, lineNumber, columnNumber);
    }
}
