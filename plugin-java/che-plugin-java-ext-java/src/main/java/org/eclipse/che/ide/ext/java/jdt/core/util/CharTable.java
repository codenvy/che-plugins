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
 * @author Evgen Vidolob
 */
abstract class CharTable {

    abstract int getProp(int ch);

    abstract int getNumericValue(int ch);

    abstract boolean isWhitespace(int ch);

    abstract boolean isJavaIdentifierStart(int ch);

    abstract boolean isJavaIdentifierPart(int ch);

    static final CharTable of(int ch) {
        if (ch >>> 8 == 0) {     // fast-path
            return CharTableLatin1.INSTANCE;
        } else {
            switch(ch >>> 16) {  //plane 00-16
                case(0):
                    return CharTable0.INSTANCE;
                case(1):
                    return CharTable1.INSTANCE;
                case(2):
                    return CharTable2.INSTANCE;
                case(14):
                    return CharTableE.INSTANCE;
                case(15):
                case(16):
                    return CharTablePrivate.INSTANCE;
                default:
                    return CharTableUndefined.INSTANCE;
            }
        }
    }
}
