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
public class CharTableLatin1 extends CharTable {

    static final CharTableLatin1 INSTANCE = new CharTableLatin1();

    static final int    A[]    = new int[256];
    static final String A_DATA =
            "\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800" +
            "\u100F\u4800\u100F\u4800\u100F\u5800\u400F\u5000\u400F\u5800\u400F\u6000\u400F" +
            "\u5000\u400F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800" +
            "\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F" +
            "\u4800\u100F\u4800\u100F\u5000\u400F\u5000\u400F\u5000\u400F\u5800\u400F\u6000" +
            "\u400C\u6800\030\u6800\030\u2800\030\u2800\u601A\u2800\030\u6800\030\u6800" +
            "\030\uE800\025\uE800\026\u6800\030\u2800\031\u3800\030\u2800\024\u3800\030" +
            "\u2000\030\u1800\u3609\u1800\u3609\u1800\u3609\u1800\u3609\u1800\u3609\u1800" +
            "\u3609\u1800\u3609\u1800\u3609\u1800\u3609\u1800\u3609\u3800\030\u6800\030" +
            "\uE800\031\u6800\031\uE800\031\u6800\030\u6800\030\202\u7FE1\202\u7FE1\202" +
            "\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1" +
            "\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202" +
            "\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1" +
            "\202\u7FE1\uE800\025\u6800\030\uE800\026\u6800\033\u6800\u5017\u6800\033\201" +
            "\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2" +
            "\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201" +
            "\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2" +
            "\201\u7FE2\201\u7FE2\201\u7FE2\uE800\025\u6800\031\uE800\026\u6800\031\u4800" +
            "\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u5000\u100F" +
            "\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800" +
            "\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F" +
            "\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800" +
            "\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F\u4800\u100F" +
            "\u3800\014\u6800\030\u2800\u601A\u2800\u601A\u2800\u601A\u2800\u601A\u6800" +
            "\034\u6800\034\u6800\033\u6800\034\000\u7002\uE800\035\u6800\031\u6800\u1010" +
            "\u6800\034\u6800\033\u2800\034\u2800\031\u1800\u060B\u1800\u060B\u6800\033" +
            "\u07FD\u7002\u6800\034\u6800\030\u6800\033\u1800\u050B\000\u7002\uE800\036" +
            "\u6800\u080B\u6800\u080B\u6800\u080B\u6800\030\202\u7001\202\u7001\202\u7001" +
            "\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202" +
            "\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001" +
            "\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\u6800\031\202\u7001\202" +
            "\u7001\202\u7001\202\u7001\202\u7001\202\u7001\202\u7001\u07FD\u7002\201\u7002" +
            "\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201" +
            "\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002" +
            "\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\u6800" +
            "\031\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002\201\u7002" +
            "\u061D\u7002";


    static {

        char[] data = A_DATA.toCharArray();
        assert (data.length == (256 * 2));
        int i = 0, j = 0;
        while (i < (256 * 2)) {
            int entry = data[i++] << 16;
            A[j++] = entry | data[i++];
        }


    }

    @Override
    int getProp(int ch) {
        char offset = (char)ch;
        return A[offset];
    }

    @Override
    int getNumericValue(int ch) {
        int val = getProp(ch);
        int result;

        switch (val & 0xC00) {
            default:
            case (0x00000000):
                result = -1;
                break;
            case (0x00000400):
                result = ch + ((val & 0x3E0) >> 5) & 0x1F;
                break;
            case (0x00000800):
                result = -2;
                break;
            case (0x00000C00):
                result = (ch + ((val & 0x3E0) >> 5) & 0x1F) + 10;
                break;
        }
        return result;
    }

    @Override
    boolean isWhitespace(int ch) {
        int props = getProp(ch);
        return ((props & 0x00007000) == 0x00004000);
    }

    @Override
    boolean isJavaIdentifierStart(int ch) {
        int props = getProp(ch);
        return ((props & 0x00007000) >= 0x00005000);
    }

    @Override
    boolean isJavaIdentifierPart(int ch) {
        int props = getProp(ch);
        return ((props & 0x00003000) != 0);
    }
}
