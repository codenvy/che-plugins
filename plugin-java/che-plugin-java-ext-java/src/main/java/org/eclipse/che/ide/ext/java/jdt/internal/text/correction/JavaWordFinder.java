/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.text.correction;


import org.eclipse.che.ide.api.text.BadLocationException;
import org.eclipse.che.ide.api.text.Region;
import org.eclipse.che.ide.api.text.RegionImpl;
import org.eclipse.che.ide.ext.java.jdt.core.util.CharUtil;
import org.eclipse.che.ide.ext.java.jdt.text.Document;

public class JavaWordFinder {

    private static final int SURROGATE_BITMASK = 0xFFFFF800;

    private static final int SURROGATE_BITS = 0xD800;

    public static Region findWord(Document document, int offset) {

        int start = -2;
        int end = -1;

        try {
            int pos = offset;
            char c;

            while (pos >= 0) {
                c = document.getChar(pos);
                if (!CharUtil.isJavaIdentifierPart(c)) {
                    // Check for surrogates
                    if (isSurrogate(c)) {
                  /*
                   * XXX: Here we should create the code point and test whether
                   * it is a Java identifier part. Currently this is not possible
                   * because java.lang.Character in 1.4 does not support surrogates
                   * and because com.ibm.icu.lang.UCharacter.isJavaIdentifierPart(int)
                   * is not correctly implemented.
                   */
                    } else {
                        break;
                    }
                }
                --pos;
            }
            start = pos;

            pos = offset;
            int length = document.getLength();

            while (pos < length) {
                c = document.getChar(pos);
                if (!CharUtil.isJavaIdentifierPart(c)) {
                    if (isSurrogate(c)) {
                  /*
                   * XXX: Here we should create the code point and test whether
                   * it is a Java identifier part. Currently this is not possible
                   * because java.lang.Character in 1.4 does not support surrogates
                   * and because com.ibm.icu.lang.UCharacter.isJavaIdentifierPart(int)
                   * is not correctly implemented.
                   */
                    } else {
                        break;
                    }

                }
                ++pos;
            }
            end = pos;

        } catch (BadLocationException x) {
        }

        if (start >= -1 && end > -1) {
            if (start == offset && end == offset)
                return new RegionImpl(offset, 0);
            else if (start == offset)
                return new RegionImpl(start, end - start);
            else
                return new RegionImpl(start + 1, end - start - 1);
        }

        return null;
    }

    public static boolean isSurrogate(char char16) {
        return (char16 & SURROGATE_BITMASK) == SURROGATE_BITS;
    }
}
