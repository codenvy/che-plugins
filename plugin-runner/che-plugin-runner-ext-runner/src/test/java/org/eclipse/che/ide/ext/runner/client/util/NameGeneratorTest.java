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
package org.eclipse.che.ide.ext.runner.client.util;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Andrienko
 * @author Florent Benoit
 */
@RunWith(GwtMockitoTestRunner.class)
public class NameGeneratorTest {



    /**
     * First copy is named 'copy of'
     */
    @Test
    public void generateFirstName() {
        String generated = NameGenerator.generateCopy("hello", Collections.<String>emptyList());
        String expectedName = "Copy of hello";
        assertEquals(expectedName, generated);
    }

    /**
     * Second copy is named 'copy2 of ...'
     */
    @Test
    public void generateAlreadyExistsFirstName() {
        String existsName = "Copy of hello";
        String generated = NameGenerator.generateCopy("hello", Arrays.asList(existsName));
        String expectedName = "Copy2 of hello";

        assertEquals(expectedName, generated);
    }

    /**
     * Third copy is named 'copy of ... rev3'
     */
    @Test
    public void generateAlreadyExistsTwiceName() {
        String existsName = "Copy of hello";
        String existsName2 = "Copy2 of hello";
        String generated = NameGenerator.generateCopy("hello", Arrays.asList(existsName, existsName2));
        String expectedName = "Copy3 of hello";

        assertEquals(expectedName, generated);
    }

}
