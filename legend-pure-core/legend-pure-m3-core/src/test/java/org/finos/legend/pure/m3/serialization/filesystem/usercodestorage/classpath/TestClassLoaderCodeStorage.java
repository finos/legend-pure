// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestClassLoaderCodeStorage
{
    private ClassLoaderCodeStorage testCodeStorage;
    private ClassLoaderCodeStorage platformCodeStorage;
    private ClassLoaderCodeStorage combinedCodeStorage;

    @Before
    public void setUp()
    {
        this.testCodeStorage = new ClassLoaderCodeStorage(new GenericCodeRepository("test", null, "platform"), new GenericCodeRepository("empty", null));
        this.platformCodeStorage = new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository());
        this.combinedCodeStorage = new ClassLoaderCodeStorage(this.testCodeStorage.getAllRepositories().asLazy().concatenate(this.platformCodeStorage.getAllRepositories()));
    }

    @Test
    public void testGetFileOrFiles()
    {
        Assert.assertTrue(this.platformCodeStorage.getFileOrFiles("/platform").contains("/platform/pure/grammar/m3.pure"));
        Assert.assertTrue(this.platformCodeStorage.getFileOrFiles("/platform").contains("/platform/pure/anonymousCollections.pure"));

        Assert.assertEquals(
                "unable to find all files under /test",
                Lists.mutable.with(
                        "/test/codestorage/fake.pure",
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure",
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level2/level2.pure"),
                this.testCodeStorage.getFileOrFiles("/test").toSortedList());
        Assert.assertEquals(
                Lists.mutable.empty(),
                this.testCodeStorage.getFileOrFiles("/empty").toSortedList());

        Assert.assertEquals(
                this.platformCodeStorage.getFileOrFiles("/platform").toSortedList(),
                this.combinedCodeStorage.getFileOrFiles("/platform").toSortedList());
        Assert.assertEquals(
                this.testCodeStorage.getFileOrFiles("/test").toSortedList(),
                this.combinedCodeStorage.getFileOrFiles("/test").toSortedList());
        Assert.assertEquals(
                this.testCodeStorage.getFileOrFiles("/empty").toSortedList(),
                this.combinedCodeStorage.getFileOrFiles("/empty").toSortedList());

        Assert.assertEquals(
                "unable to find all files for a non-directory path",
                Lists.mutable.with("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure"),
                this.testCodeStorage.getFileOrFiles("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure").toSortedList());

        Assert.assertEquals(
                "unable to find all files under /test/com",
                Lists.mutable.with(
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure",
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level2/level2.pure"),
                this.testCodeStorage.getFileOrFiles("/test/org").toSortedList());
    }

    @Test
    public void testGetFiles()
    {
        Assert.assertEquals(
                "unable to find all files immediately under /test",
                Lists.mutable.with("codestorage", "org"),
                this.testCodeStorage.getFiles("/test").collect(CodeStorageNode.GET_NAME, Lists.mutable.empty()).sortThis());
        Assert.assertEquals(
                "unable to find all files immediately under /test",
                Lists.mutable.with("codestorage", "org"),
                this.combinedCodeStorage.getFiles("/test").collect(CodeStorageNode.GET_NAME, Lists.mutable.empty()).sortThis());
    }

    @Test
    public void testGetUserFiles()
    {
        Assert.assertEquals(
                Lists.mutable.with("/test/codestorage/fake.pure", "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure", "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level2/level2.pure"),
                this.testCodeStorage.getUserFiles().toSortedList());
        Assert.assertEquals(238, this.combinedCodeStorage.getUserFiles().toSet().size());
    }

    @Test
    public void testGetContentAsText()
    {
        String level1_pure = readResource("test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure");
        String m3_pure = readResource("platform/pure/grammar/m3.pure");
        Assert.assertEquals(level1_pure, this.testCodeStorage.getContentAsText("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure"));
        Assert.assertEquals(m3_pure, this.platformCodeStorage.getContentAsText("/platform/pure/grammar/m3.pure"));
        Assert.assertEquals(level1_pure, this.combinedCodeStorage.getContentAsText("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure"));
    }

    private String readResource(String resourceName)
    {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)), StandardCharsets.UTF_8))
        {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1)
            {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Error reading resource: " + resourceName, e);
        }
    }

    @Test
    public void testRepositoryName()
    {
        Assert.assertEquals(Lists.mutable.with("platform"), this.platformCodeStorage.getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty()));
        Assert.assertEquals(Lists.mutable.with("test", "empty"), this.testCodeStorage.getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty()));
        Assert.assertEquals(Lists.mutable.with("empty", "platform", "test"), this.combinedCodeStorage.getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty()).sortThis());
    }

    @Test
    public void testInvalidNode()
    {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> this.testCodeStorage.getFiles("/made/up/invalid/path"));
        Assert.assertEquals("Cannot find path '/made/up/invalid/path'", e.getMessage());
    }

    @Test
    public void testInvalidNodeContent()
    {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> this.testCodeStorage.getFileOrFiles("/made/up/invalid/path"));
        Assert.assertEquals("Cannot find path '/made/up/invalid/path'", e.getMessage());
    }
}
