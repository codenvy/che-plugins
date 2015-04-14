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


    /**
     * Copying a copy should result in a new increment of a copy, not copy of copy
     */
    @Test
    public void generateCopyOfCopy() {
        String existsName = "Copy of hello";
        String existsName2 = "Copy2 of hello";
        String generated = NameGenerator.generateCopy("Copy of hello", Arrays.asList(existsName, existsName2));
        String expectedName = "Copy3 of hello";

        assertEquals(expectedName, generated);
    }

    /**
     * Copying a copy should result in a new increment of a copy, not copy of copy
     */
    @Test
    public void generateCopyOfCopy2() {
        String existsName = "Copy of hello";
        String existsName2 = "Copy2 of hello";
        String existsName3 = "Copy3 of hello";
        String generated = NameGenerator.generateCopy("Copy3 of hello", Arrays.asList(existsName, existsName2, existsName3));
        String expectedName = "Copy4 of hello";

        assertEquals(expectedName, generated);
    }


    /**
     * Test remove copy prefix
     */
    @Test
    public void removeNonExistingPrefix() {
        String name = NameGenerator.removeCopyPrefix("removeNonExistingPrefix of hello");
        assertEquals("removeNonExistingPrefix of hello", name);
    }

    /**
     * Test remove copy prefix
     */
    @Test
    public void removeCopyPrefix() {
        String name = NameGenerator.removeCopyPrefix("Copy of hello");
        assertEquals("hello", name);
    }


    /**
     * Test remove copy prefix
     */
    @Test
    public void removeCopy2Prefix() {
        String name = NameGenerator.removeCopyPrefix("Copy2 of hello");
        assertEquals("hello", name);
    }

    /**
     * Test remove copy prefix
     */
    @Test
    public void removeCopy1234Prefix() {
        String name = NameGenerator.removeCopyPrefix("Copy1234 of hello");
        assertEquals("hello", name);
    }


}
