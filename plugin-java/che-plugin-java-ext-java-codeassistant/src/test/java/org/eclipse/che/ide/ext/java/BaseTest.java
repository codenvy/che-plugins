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
package org.eclipse.che.ide.ext.java;

import org.eclipse.che.jdt.internal.core.JavaProject;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.codeassist.impl.AssistOptions;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
public class BaseTest {

    protected static Map<String, String> options = new HashMap<>();
    protected static JavaProject         project;



    @Before
    public void setUp() throws Exception {
        project = new JavaProject(new File(BaseTest.class.getResource("/projects").getFile()), "/test",BaseTest.class.getResource("/temp").getPath(),
                                  "ws", options);
    }

    @After
    public void tearDown() throws Exception {
        project.close();
    }

    public BaseTest() {
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.CORE_ENCODING, "UTF-8");
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        options.put(CompilerOptions.OPTION_TargetPlatform, JavaCore.VERSION_1_7);
        options.put(AssistOptions.OPTION_PerformVisibilityCheck, AssistOptions.ENABLED);
        options.put(CompilerOptions.OPTION_ReportUnusedLocal, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_TaskTags, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_ReportUnusedPrivateMember, CompilerOptions.WARNING);
        options.put(CompilerOptions.OPTION_SuppressWarnings, CompilerOptions.DISABLED);
        options.put(JavaCore.COMPILER_TASK_TAGS, "TODO,FIXME,XXX");
        options.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, JavaCore.ENABLED);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        options.put(CompilerOptions.OPTION_Process_Annotations, JavaCore.DISABLED);
    }

    protected static String getHandldeForRtJarStart() {
        String javaHome = System.getProperty("java.home") + "/lib/rt.jar";
        javaHome = javaHome.replaceAll("/","\\\\/");
        return "☂/" + javaHome;
    }


}
