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
package org.eclipse.che.ide.ext.java.jdt.core.util;

/**
 * Implements methods from Character class, thad GWT doesn't emulate.
 * @author Evgen Vidolob
 */
public class CharUtil {

    public static int getNumericValue(int ch) {
        return CharTable.of(ch).getNumericValue(ch);
    }

    public static boolean isWhitespace(int cp) {
       return CharTable.of(cp).isWhitespace(cp);
    }

    public static boolean isJavaIdentifierStart(int ch){
        return CharTable.of(ch).isJavaIdentifierStart(ch);
    }

    public static boolean isJavaIdentifierPart(int ch){
        return CharTable.of(ch).isJavaIdentifierPart(ch);
    }
}
