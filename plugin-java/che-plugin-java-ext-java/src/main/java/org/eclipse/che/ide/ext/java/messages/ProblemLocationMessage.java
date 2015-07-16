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
package org.eclipse.che.ide.ext.java.messages;

import org.eclipse.che.ide.collections.Array;
import com.google.gwt.webworker.client.messages.Message;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 */
public interface ProblemLocationMessage extends Message {
    /**
     * Returns the start offset of the problem.
     *
     * @return the start offset of the problem
     */
    int getOffset();

    /**
     * Returns the length of the problem.
     *
     * @return the length of the problem
     */
    int getLength();

    /**
     * Returns the marker type of this problem.
     *
     * @return The marker type of the problem.
     */
    String getMarkerType();

    /**
     * Returns the id of problem. Note that problem ids are defined per problem marker type.
     * See {@link org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem} for id definitions for problems of type
     * <code>org.eclipse.jdt.core.problem</code> and <code>org.eclipse.jdt.core.task</code>.
     *
     * @return The id of the problem.
     */
    int getProblemId();

    /**
     * Returns the original arguments recorded into the problem.
     *
     * @return String[] Returns the problem arguments.
     */
    Array<String> getProblemArguments();

    /**
     * Returns if the problem has error severity.
     *
     * @return <code>true</code> if the problem has error severity
     */
    boolean isError();
}
