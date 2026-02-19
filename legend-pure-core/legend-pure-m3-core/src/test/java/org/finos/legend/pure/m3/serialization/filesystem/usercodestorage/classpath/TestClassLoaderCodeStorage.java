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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.utility.LazyIterate;
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
        this.testCodeStorage = new ClassLoaderCodeStorage(new GenericCodeRepository("test", null, "platform"));
        this.platformCodeStorage = new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository());
        this.combinedCodeStorage = new ClassLoaderCodeStorage(LazyIterate.concatenate(this.testCodeStorage.getAllRepositories(), this.platformCodeStorage.getAllRepositories()));
    }

    @Test
    public void testGetFileOrFiles()
    {
        Verify.assertContainsAll(
                "unable to find all files under /platform",
                this.platformCodeStorage.getFileOrFiles("/platform"),
                "/platform/pure/grammar/m3.pure",
                "/platform/pure/anonymousCollections.pure");

        Verify.assertSetsEqual(
                "unable to find all files under /test",
                Sets.mutable.with(
                        "/test/codestorage/fake.pure",
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure",
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level2/level2.pure"),
                this.testCodeStorage.getFileOrFiles("/test").toSet());

        Verify.assertSetsEqual(
                this.platformCodeStorage.getFileOrFiles("/platform").toSet(),
                this.combinedCodeStorage.getFileOrFiles("/platform").toSet());
        Verify.assertSetsEqual(
                this.testCodeStorage.getFileOrFiles("/test").toSet(),
                this.combinedCodeStorage.getFileOrFiles("/test").toSet());

        Verify.assertSetsEqual(
                "unable to find all files for a non-directory path",
                Sets.mutable.with("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure"),
                this.testCodeStorage.getFileOrFiles("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure").toSet());

        Verify.assertSetsEqual(
                "unable to find all files under /test/com",
                Sets.mutable.with(
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure",
                        "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level2/level2.pure"),
                this.testCodeStorage.getFileOrFiles("/test/org").toSet());
    }

    @Test
    public void testGetFiles()
    {
        Verify.assertSetsEqual(
                "unable to find all files immediately under /test",
                Sets.mutable.with("codestorage", "org"),
                this.testCodeStorage.getFiles("/test").collect(CodeStorageNode.GET_NAME).toSet());
        Verify.assertSetsEqual(
                "unable to find all files immediately under /test",
                Sets.mutable.with("codestorage", "org"),
                this.combinedCodeStorage.getFiles("/test").collect(CodeStorageNode.GET_NAME).toSet());
    }

    @Test
    public void testGetUserFiles()
    {
        Verify.assertSetsEqual(
                Sets.mutable.with("/test/codestorage/fake.pure", "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure", "/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level2/level2.pure"),
                this.testCodeStorage.getUserFiles().toSet());
        Verify.assertEquals(239, this.combinedCodeStorage.getUserFiles().toSet().size());
    }

    @Test
    public void testGetContentAsText() throws Exception
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
        Verify.assertSetsEqual(Sets.mutable.with("platform"), this.platformCodeStorage.getAllRepositories().collect(CodeRepository::getName).toSet());
        Verify.assertSetsEqual(Sets.mutable.with("test"), this.testCodeStorage.getAllRepositories().collect(CodeRepository::getName).toSet());
        Verify.assertSetsEqual(Sets.mutable.with("platform", "test"), this.combinedCodeStorage.getAllRepositories().collect(CodeRepository::getName).toSet());
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
