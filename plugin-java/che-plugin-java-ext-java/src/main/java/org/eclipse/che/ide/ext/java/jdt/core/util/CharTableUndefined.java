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
public class CharTableUndefined extends CharTable{

    static final CharTableUndefined INSTANCE = new CharTableUndefined();

    @Override
    int getProp(int ch) {
        return 0;
    }

    @Override
    int getNumericValue(int ch) {
        return -1;
    }

    @Override
    boolean isWhitespace(int ch) {
        return false;
    }

    @Override
    boolean isJavaIdentifierStart(int ch) {
        return false;
    }

    @Override
    boolean isJavaIdentifierPart(int ch) {
        return false;
    }
}
