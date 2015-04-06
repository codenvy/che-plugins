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
package org.eclipse.che.ide.ext.git.server.nativegit;

import org.eclipse.che.ide.ext.git.server.DiffPage;
import org.eclipse.che.ide.ext.git.shared.DiffRequest;
import org.eclipse.che.ide.ext.git.shared.DiffRequest.DiffType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class DiffTest extends BaseTest {
    private NativeGit nativeGit;

    @BeforeMethod
    protected void setUp() throws Exception {
        addFile(getRepository(), "aaa", "AAA\n");
        nativeGit = new NativeGit(getRepository().toFile());
        nativeGit.createAddCommand().setFilePattern(Arrays.asList(".")).execute();
        addFile(getRepository(), "aaa", "BBB\n");
    }

    @Test
    public void testDiffNameStatus() throws Exception {
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withType(DiffType.NAME_STATUS)
                .withRenameLimit(0));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("M\taaa"));
    }

    @Test
    public void testDiffNameStatusWithCommits() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList(".")).execute();
        nativeGit.createRemoveCommand().setListOfItems(Arrays.asList("README.txt")).execute();
        nativeGit.createCommitCommand().setMessage("testDiffNameStatusWithCommits")
                .setAuthor(getUser()).setCommitter(getUser()).execute();
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.NAME_STATUS)
                .withRenameLimit(0)
                .withCommitA("HEAD^")
                .withCommitB("HEAD"));
        assertEquals(diff.size(), 2);
        assertTrue(diff.contains("D\tREADME.txt"));
        assertTrue(diff.contains("A\taaa"));
    }

    @Test
    public void testDiffNameStatusWithFileFilterAndCommits() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList("aaa")).execute();
        nativeGit.createRemoveCommand().setListOfItems(Arrays.asList("README.txt")).execute();
        nativeGit.createCommitCommand().setMessage("testDiffNameStatusWithCommits")
                .setAuthor(getUser()).setCommitter(getUser()).execute();
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(Arrays.asList("aaa"))
                .withType(DiffType.NAME_STATUS)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA("HEAD^1")
                .withCommitB("HEAD"));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("A\taaa"));
    }

    @Test
    public void testDiffNameOnly() throws Exception {
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("aaa"));
    }

    @Test
    public void testDiffNameOnlyWithCommits() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList("aaa")).execute();
        nativeGit.createRemoveCommand().setListOfItems(Arrays.asList("README.txt")).execute();
        nativeGit.createCommitCommand().setMessage("testDiffNameStatusWithCommits")
                .setAuthor(getUser()).setCommitter(getUser()).execute();
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA("HEAD^1")
                .withCommitB("HEAD"));
        assertEquals(diff.size(), 2);
        assertTrue(diff.contains("README.txt"));
        assertTrue(diff.contains("aaa"));
    }

    @Test
    public void testDiffNameOnlyCached() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList("aaa")).execute();
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA("HEAD")
                .withCached(true));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("aaa"));
    }

    @Test
    public void testDiffNameOnlyCachedNoCommit() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList("aaa")).execute();
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA(null)
                .withCached(true));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("aaa"));
    }

    @Test
    public void testDiffNameOnlyWorkingTree() throws Exception {
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA("HEAD")
                .withCached(false));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("aaa"));
    }

    @Test
    public void testDiffNameOnlyWithFileFilter() throws Exception {
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(Arrays.asList("aaa"))
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("aaa"));
    }


    @Test
    public void testDiffNameOnlyNotMatchingWithFileFilter() throws Exception {
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                                             .withFileFilter(Arrays.asList("anotherFile"))
                                             .withType(DiffType.NAME_ONLY)
                                             .withNoRenames(false)
                                             .withRenameLimit(0));
        assertEquals(diff.size(), 0);
    }

    @Test
    public void testDiffNameOnlyWithFileFilterAndCommits() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList("aaa")).execute();
        nativeGit.createRemoveCommand().setListOfItems(Arrays.asList("README.txt")).execute();
        nativeGit.createCommitCommand().setMessage("testDiffNameStatusWithCommits")
                .setAuthor(getUser()).setCommitter(getUser()).execute();
        List<String> diff = readDiff(newDTO(DiffRequest.class)
                .withFileFilter(Arrays.asList("aaa"))
                .withType(DiffType.NAME_ONLY)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA("HEAD^1")
                .withCommitB("HEAD"));
        assertEquals(diff.size(), 1);
        assertTrue(diff.contains("aaa"));
    }

    @Test
    public void testDiffRaw() throws Exception {
        DiffRequest request = newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.RAW)
                .withNoRenames(false)
                .withRenameLimit(0);
        DiffPage diffPage = getConnection().diff(request);
        diffPage.writeTo(System.out);
    }

    @Test
    public void testDiffRawWithCommits() throws Exception {
        nativeGit.createAddCommand().setFilePattern(Arrays.asList("aaa")).execute();
        nativeGit.createRemoveCommand().setListOfItems(Arrays.asList("README.txt")).execute();
        nativeGit.createCommitCommand().setMessage("testDiffNameStatusWithCommits")
                .setCommitter(getUser()).execute();
        DiffRequest request = newDTO(DiffRequest.class)
                .withFileFilter(null)
                .withType(DiffType.RAW)
                .withNoRenames(false)
                .withRenameLimit(0)
                .withCommitA("HEAD^1")
                .withCommitB("HEAD");
        DiffPage diffPage = getConnection().diff(request);
        diffPage.writeTo(System.out);
    }

    private List<String> readDiff(DiffRequest request) throws Exception {
        DiffPage diffPage = getConnection().diff(request);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        diffPage.writeTo(out);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));

        String line;
        List<String> diff = new ArrayList<>();
        while ((line = reader.readLine()) != null)
            diff.add(line);

        return diff;
    }
}
