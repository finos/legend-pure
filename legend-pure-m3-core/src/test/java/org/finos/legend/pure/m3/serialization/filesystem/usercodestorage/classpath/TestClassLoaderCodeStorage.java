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

import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestClassLoaderCodeStorage
{
    private ClassLoaderCodeStorage testCodeStorage;
    private ClassLoaderCodeStorage platformCodeStorage;
    private ClassLoaderCodeStorage combinedCodeStorage;

    @Before
    public void setUp()
    {
        this.testCodeStorage = new ClassLoaderCodeStorage(new TestCodeRepository("test"));
        this.platformCodeStorage = new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository());
        this.combinedCodeStorage = new ClassLoaderCodeStorage(LazyIterate.concatenate(this.testCodeStorage.getRepositories(), this.platformCodeStorage.getRepositories()));
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
        Verify.assertEquals(105, this.combinedCodeStorage.getUserFiles().toSet().size());
    }

    @Test
    public void testGetContentAsText()
    {
        String code = "// Copyright 2020 Goldman Sachs\n" +
                "//\n" +
                "// Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "// you may not use this file except in compliance with the License.\n" +
                "// You may obtain a copy of the License at\n" +
                "//\n" +
                "//      http://www.apache.org/licenses/LICENSE-2.0\n" +
                "//\n" +
                "// Unless required by applicable law or agreed to in writing, software\n" +
                "// distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "// See the License for the specific language governing permissions and\n" +
                "// limitations under the License.\n" +
                "\n" +
                "function test::level1::testFn():Any[*]\n" +
                "{\n" +
                "    'ok' + 'z'\n" +
                "}";

        Assert.assertEquals(code.replaceAll("\\r",""), this.testCodeStorage.getContentAsText("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure").replaceAll("\\r",""));
        Assert.assertNotNull("Unable to load content for file on classpath", this.platformCodeStorage.getContentAsText("/platform/pure/grammar/m3.pure"));
        Assert.assertEquals(code.replaceAll("\\r",""), this.combinedCodeStorage.getContentAsText("/test/org/finos/legend/pure/m3/serialization/filesystem/test/level1/level1.pure").replaceAll("\\r",""));
    }

    @Test
    public void testRepositoryName()
    {
        Verify.assertSetsEqual(Sets.mutable.with("platform"), this.platformCodeStorage.getRepositories().collect(CodeRepository::getName).toSet());
        Verify.assertSetsEqual(Sets.mutable.with("test"), this.testCodeStorage.getRepositories().collect(CodeRepository::getName).toSet());
        Verify.assertSetsEqual(Sets.mutable.with("platform", "test"), this.combinedCodeStorage.getRepositories().collect(CodeRepository::getName).toSet());
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidNode()
    {
        this.testCodeStorage.getFiles("/made/up/invalid/path");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidNodeContent()
    {
        this.testCodeStorage.getFileOrFiles("/made/up/invalid/path");
    }
}
