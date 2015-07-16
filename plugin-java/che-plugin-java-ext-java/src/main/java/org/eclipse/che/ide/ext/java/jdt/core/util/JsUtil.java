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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;

/**
 * @author Evgen Vidolob
 */
public class JsUtil {

    public static char[] toCharArray(JsArrayString stringArray) {
        char[] result = new char[stringArray.length()];
        for (int i = 0; i < stringArray.length(); i++) {
            result[i] = stringArray.get(i).charAt(0);
        }
        return result;
    }

    public static byte[] toByteArray(JsArrayString stringArray) {
        byte[] result = new byte[stringArray.length()];
        for (int i = 0; i < stringArray.length(); i++) {
            result[i] = Byte.valueOf(stringArray.get(i));
        }
        return result;
    }

    public static long[] toLongArray(JsArrayNumber numberArray) {
        long[] result = new long[numberArray.length()];
        for (int i = 0; i < numberArray.length(); i++) {
            result[i] = (long)numberArray.get(i);
        }
        return result;
    }

    public static String[] toStringArray(JsArrayString stringArray) {
        String[] result = new String[stringArray.length()];
        for (int i = 0; i < stringArray.length(); i++) {
            result[i] = stringArray.get(i);
        }
        return result;
    }
}
