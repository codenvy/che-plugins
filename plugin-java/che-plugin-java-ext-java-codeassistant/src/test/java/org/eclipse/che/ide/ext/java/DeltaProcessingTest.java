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

import org.eclipse.che.api.vfs.server.observation.CreateEvent;
import org.eclipse.che.api.vfs.server.observation.DeleteEvent;
import org.eclipse.che.jdt.core.resources.ResourceChangedEvent;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Evgen Vidolob
 */
public class DeltaProcessingTest extends BaseTest {


    @After
    public void tearDown() throws Exception {
        File workspace = new File(BaseTest.class.getResource("/projects").getFile());
        File newFile = new File(workspace, "/test/src/main/java/com/codenvy/test/NewClass.java");
        if(newFile.exists()){
            newFile.delete();
        }

    }

    @Test
    public void testRemoveClass() throws Exception {
        ResourceChangedEvent event = new ResourceChangedEvent(new File(BaseTest.class.getResource("/projects").getFile()),new DeleteEvent("projects", "/test/src/main/java/com/codenvy/test/MyClass.java", false));
        NameEnvironmentAnswer answer =
                project.getNameEnvironment().findType(CharOperation.splitOn('.', "com.codenvy.test.MyClass".toCharArray()));

        assertThat(answer).isNotNull();

        project.getJavaModelManager().deltaState.resourceChanged(event);
        project.creteNewNameEnvironment();

        answer =
                project.getNameEnvironment().findType(CharOperation.splitOn('.', "com.codenvy.test.MyClass".toCharArray()));
        assertThat(answer).isNull();
    }

    @Test
    public void testRemoveFolder() throws Exception {
        ResourceChangedEvent event = new ResourceChangedEvent(new File(BaseTest.class.getResource("/projects").getFile()),new DeleteEvent("projects", "/test/src/main/java/com/codenvy/test", true));
        NameEnvironmentAnswer answer =
                project.getNameEnvironment().findType(CharOperation.splitOn('.', "com.codenvy.test.MyClass".toCharArray()));

        assertThat(answer).isNotNull();
        project.getJavaModelManager().deltaState.resourceChanged(event);
        project.creteNewNameEnvironment();
        answer =
                project.getNameEnvironment().findType(CharOperation.splitOn('.', "com.codenvy.test.MyClass".toCharArray()));
        assertThat(answer).isNull();
    }

    @Test
    public void testAddClass() throws Exception {


        File workspace = new File(BaseTest.class.getResource("/projects").getFile());
        ResourceChangedEvent event = new ResourceChangedEvent(workspace,new CreateEvent("projects", "/test/src/main/java/com/codenvy/test/NewClass.java", false));


        NameEnvironmentAnswer answer =
                project.getNameEnvironment().findType(CharOperation.splitOn('.', "com.codenvy.test.NewClass".toCharArray()));
        assertThat(answer).isNull();

        FileOutputStream outputStream = new FileOutputStream(new File(workspace, "/test/src/main/java/com/codenvy/test/NewClass.java"));
        outputStream.write("packagecom.codenvy.test;\n public class NewClass{}\n".getBytes());
        outputStream.close();

        project.getJavaModelManager().deltaState.resourceChanged(event);
        project.creteNewNameEnvironment();
        answer =
                project.getNameEnvironment().findType(CharOperation.splitOn('.', "com.codenvy.test.NewClass".toCharArray()));
        assertThat(answer).isNotNull();
    }


}
