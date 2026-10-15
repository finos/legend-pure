// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m4.tools;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class TestFileWriteTools
{
    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testFirstWriteCreatesFileWithContent() throws IOException
    {
        Path target = tmp.getRoot().toPath().resolve("out.txt");
        Assert.assertFalse(Files.exists(target));

        boolean replaced = FileWriteTools.writeIfModified(target, "hello".getBytes(StandardCharsets.UTF_8));

        Assert.assertTrue(replaced);
        Assert.assertTrue(Files.exists(target));
        Assert.assertEquals("hello", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        Assert.assertFalse("tmp file should not be left behind", Files.exists(target.resolveSibling("out.txt.tmp")));
    }

    @Test
    public void testIdenticalContentLeavesTimestampUnchanged() throws IOException, InterruptedException
    {
        Path target = tmp.getRoot().toPath().resolve("out.txt");
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        Assert.assertTrue(FileWriteTools.writeIfModified(target, content));
        FileTime before = Files.getLastModifiedTime(target);

        Thread.sleep(50);

        boolean replaced = FileWriteTools.writeIfModified(target, content);

        Assert.assertFalse("identical content should not report a replace", replaced);
        Assert.assertEquals(before, Files.getLastModifiedTime(target));
        Assert.assertEquals("hello", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    public void testDifferentContentReplacesFileAndAdvancesTimestamp() throws IOException, InterruptedException
    {
        Path target = tmp.getRoot().toPath().resolve("out.txt");

        Assert.assertTrue(FileWriteTools.writeIfModified(target, "hello".getBytes(StandardCharsets.UTF_8)));
        FileTime before = Files.getLastModifiedTime(target);

        Thread.sleep(50);

        boolean replaced = FileWriteTools.writeIfModified(target, "goodbye".getBytes(StandardCharsets.UTF_8));

        Assert.assertTrue("different content should report a replace", replaced);
        Assert.assertTrue(Files.getLastModifiedTime(target).compareTo(before) > 0);
        Assert.assertEquals("goodbye", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    public void testDifferentSizeSameLeadingBytesIsDetectedAsModified() throws IOException
    {
        Path target = tmp.getRoot().toPath().resolve("out.txt");

        Assert.assertTrue(FileWriteTools.writeIfModified(target, "hello".getBytes(StandardCharsets.UTF_8)));
        boolean replaced = FileWriteTools.writeIfModified(target, "hello world".getBytes(StandardCharsets.UTF_8));

        Assert.assertTrue(replaced);
        Assert.assertEquals("hello world", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    public void testParentDirectoriesAreCreated() throws IOException
    {
        Path target = tmp.getRoot().toPath().resolve("a/b/c/out.txt");
        Assert.assertFalse(Files.exists(target.getParent()));

        FileWriteTools.writeIfModified(target, "hello".getBytes(StandardCharsets.UTF_8));

        Assert.assertTrue(Files.exists(target));
        Assert.assertEquals("hello", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    public void testFailedWriterLeavesTargetUntouched() throws IOException
    {
        Path target = tmp.getRoot().toPath().resolve("out.txt");
        FileWriteTools.writeIfModified(target, "hello".getBytes(StandardCharsets.UTF_8));
        byte[] originalBytes = Files.readAllBytes(target);
        FileTime originalTime = Files.getLastModifiedTime(target);

        IOException thrown = Assert.assertThrows(IOException.class, () -> FileWriteTools.writeIfModified(target, stream ->
        {
            stream.write("partial".getBytes(StandardCharsets.UTF_8));
            throw new IOException("boom");
        }));

        Assert.assertEquals("boom", thrown.getMessage());
        Assert.assertArrayEquals(originalBytes, Files.readAllBytes(target));
        Assert.assertEquals(originalTime, Files.getLastModifiedTime(target));
    }
}
