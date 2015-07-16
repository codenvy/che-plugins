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

import org.eclipse.che.ide.ext.java.client.BaseTest;
import org.eclipse.che.ide.ext.java.jdt.core.util.CharUtil;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id:  10:02:58 AM 34360 2009-07-22 23:58:59Z evgen $
 */
@Ignore
public class CharOperationTest extends BaseTest {
    @Test
    public void testJavaIdentifierPart() {
        String s = "for";
        for (char c : s.toCharArray()) {
            if (!CharUtil.isJavaIdentifierPart(c))
                fail("Char '" + c + "' is valid Java identifier part");
        }
    }

    @Test
    public void testJavaIdentifierPartUnicode() {
        String s = "змінна";
        for (char c : s.toCharArray()) {
            if (!CharUtil.isJavaIdentifierPart(c))
                fail("Char '" + c + "' is valid Java identifier part");
        }
    }

    @Test
    public void testNotJavaIdentifierPart() {
        String s = "@#%*";
        for (char c : s.toCharArray()) {
            if (CharUtil.isJavaIdentifierPart(c))
                fail("Char '" + c + "' not valid Java identifier part");
        }
    }

    @Test
    public void testJavaIdentifierStart() {
        String s = "_$Ab";
        for (char c : s.toCharArray()) {
            if (!CharUtil.isJavaIdentifierStart(c))
                fail("Char '" + c + "' is valid Java identifier part");
        }
    }

    @Test
    public void testNotJavaIdentifierStart() {
        String s = "123@#&";
        for (char c : s.toCharArray()) {
            if (CharUtil.isJavaIdentifierStart(c))
                fail("Char '" + c + "' not valid Java identifier part");
        }
    }
}
