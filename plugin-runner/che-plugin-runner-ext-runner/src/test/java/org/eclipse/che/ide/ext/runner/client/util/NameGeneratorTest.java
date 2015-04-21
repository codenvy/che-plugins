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

import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Florent Benoit
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class NameGeneratorTest {

    private static final String HELLO       = "hello";
    private static final String COPY_HELLO  = "Copy of hello";
    private static final String COPY2_HELLO = "Copy2 of hello";
    private static final String COPY3_HELLO = "Copy3 of hello";
    private static final String COPY4_HELLO = "Copy4 of hello";

    @Mock
    private Environment environment1;
    @Mock
    private Environment environment2;
    @Mock
    private Environment environment3;

    /**
     * First copy is named 'copy of'
     */
    @Test
    public void generateFirstName() {
        when(environment1.getName()).thenReturn(HELLO);

        String generated = NameGenerator.generateCopy(HELLO, Arrays.asList(environment1));
        String expectedName = "Copy of hello";
        assertEquals(expectedName, generated);
    }

    /**
     * Second copy is named 'copy2 of ...'
     */
    @Test
    public void generateAlreadyExistsFirstName() {
        when(environment1.getName()).thenReturn(COPY_HELLO);

        String generated = NameGenerator.generateCopy(HELLO, Arrays.asList(environment1));

        assertEquals(COPY2_HELLO, generated);
    }

    @Test
    public void cutPlusPlusWhenCreateCopyFromCppEnvironment() {
        when(environment1.getName()).thenReturn(HELLO + "++");

        String generated = NameGenerator.generateCopy(HELLO, Arrays.asList(environment1));

        assertEquals(COPY_HELLO, generated);
    }

    /**
     * Third copy is named 'copy of ... rev3'
     */
    @Test
    public void generateAlreadyExistsTwiceName() {
        when(environment1.getName()).thenReturn(COPY_HELLO);
        when(environment2.getName()).thenReturn(COPY2_HELLO);

        String generated = NameGenerator.generateCopy(HELLO, Arrays.asList(environment1, environment2));

        assertEquals(COPY3_HELLO, generated);
    }

    /**
     * Copying a copy should result in a new increment of a copy, not copy of copy
     */
    @Test
    public void generateCopyOfCopy() {
        when(environment1.getName()).thenReturn(COPY_HELLO);
        when(environment2.getName()).thenReturn(COPY2_HELLO);
        when(environment3.getName()).thenReturn(COPY3_HELLO);

        String generated = NameGenerator.generateCopy(COPY3_HELLO, Arrays.asList(environment1, environment2, environment3));

        assertEquals(COPY4_HELLO, generated);
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
        String name = NameGenerator.removeCopyPrefix(COPY_HELLO);
        assertEquals(HELLO, name);
    }

    /**
     * Test remove copy prefix
     */
    @Test
    public void removeCopy2Prefix() {
        String name = NameGenerator.removeCopyPrefix(COPY2_HELLO);
        assertEquals(HELLO, name);
    }

    /**
     * Test remove copy prefix
     */
    @Test
    public void removeCopy1234Prefix() {
        String name = NameGenerator.removeCopyPrefix("Copy1234 of hello");
        assertEquals(HELLO, name);
    }

    @Test
    public void generateCustomEnvironmentName() throws Exception {
        String projectName = "project";
        String newName = "ENV3-project";

        when(environment1.getName()).thenReturn("ENV1-" + projectName);
        when(environment2.getName()).thenReturn("ENV2-" + projectName);
        List<Environment> environments = Arrays.asList(environment1, environment2);

        String generated = NameGenerator.generateCustomEnvironmentName(environments, projectName);

        assertEquals(newName, generated);
    }

    @Test
    public void generateCustomEnvironmentName2() throws Exception {
        String projectName = "project";
        String newName = "ENV2-project";

        when(environment1.getName()).thenReturn("ENV1-" + projectName);
        when(environment2.getName()).thenReturn("ENV3-" + projectName);
        List<Environment> environments = Arrays.asList(environment1, environment2);

        String generated = NameGenerator.generateCustomEnvironmentName(environments, projectName);

        assertEquals(newName, generated);
    }

    @Test
    public void generateCustomEnvironmentName3() throws Exception {
        String projectName = "project";
        String newName = "ENV11-project";

        Environment environment3 = mock(Environment.class);
        Environment environment4 = mock(Environment.class);
        Environment environment5 = mock(Environment.class);
        Environment environment6 = mock(Environment.class);
        Environment environment7 = mock(Environment.class);
        Environment environment8 = mock(Environment.class);
        Environment environment9 = mock(Environment.class);
        Environment environment10 = mock(Environment.class);

        when(environment1.getName()).thenReturn("ENV1-" + projectName);
        when(environment2.getName()).thenReturn("ENV2-" + projectName);
        when(environment3.getName()).thenReturn("ENV3-" + projectName);
        when(environment4.getName()).thenReturn("ENV4-" + projectName);
        when(environment5.getName()).thenReturn("ENV5-" + projectName);
        when(environment6.getName()).thenReturn("ENV6-" + projectName);
        when(environment7.getName()).thenReturn("ENV7-" + projectName);
        when(environment8.getName()).thenReturn("ENV8-" + projectName);
        when(environment9.getName()).thenReturn("ENV9-" + projectName);
        when(environment10.getName()).thenReturn("ENV10-" + projectName);

        List<Environment> environments = Arrays.asList(environment1,
                                                       environment2,
                                                       environment3,
                                                       environment4,
                                                       environment5,
                                                       environment6,
                                                       environment7,
                                                       environment8,
                                                       environment9,
                                                       environment10);

        String generated = NameGenerator.generateCustomEnvironmentName(environments, projectName);

        assertEquals(newName, generated);
    }
}