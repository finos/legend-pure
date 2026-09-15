// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite;

import org.junit.Assert;
import org.junit.Test;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;

public class TestCompositeCodeStorage
{
    @Test
    public void testGetSourceRepoName()
    {
        Assert.assertNull(CompositeCodeStorage.getSourceRepoName(null));
        Assert.assertNull(CompositeCodeStorage.getSourceRepoName(""));

        Assert.assertEquals("platform", CompositeCodeStorage.getSourceRepoName("/platform/pure/corefunctions/lang.pure"));
        Assert.assertEquals("platform", CompositeCodeStorage.getSourceRepoName("platform/pure/corefunctions/lang.pure"));
        Assert.assertEquals("example", CompositeCodeStorage.getSourceRepoName("/example/of/a/source/path/someFile.pure"));
        Assert.assertEquals("example", CompositeCodeStorage.getSourceRepoName("example/of/a/source/path/someFile.pure"));
        Assert.assertNull(CompositeCodeStorage.getSourceRepoName("welcome.pure"));
        Assert.assertNull(CompositeCodeStorage.getSourceRepoName("/welcome.pure"));
    }

    @Test
    public void testIsSourceInRepo()
    {
        String[] vacuousSourceIds = {null, ""};
        String[] rootSourceIds = {"/welcome.pure", "welcome.pure", "/xyz.pure", "xyz.pure"};
        String[] platformSourceIds = {"/platform/someFile.pure", "platform/someFile.pure", "/platform/pure/grammar/functions/math/operation/minus.pure", "/platform/pure/corefunctions/lang.pure", "platform/pure/corefunctions/lang.pure"};
        String[] exampleSourceIds = {"/example/of/a/source/path/someFile.pure", "example/of/another/source/path.pure"};

        for (String sourceId : vacuousSourceIds)
        {
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, null));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "platform"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "example"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "not_a_module"));
        }

        for (String sourceId : rootSourceIds)
        {
            Assert.assertTrue(CompositeCodeStorage.isSourceInRepository(sourceId, null));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "platform"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "example"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "not_a_module"));
        }

        for (String sourceId : platformSourceIds)
        {
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, null));
            Assert.assertTrue(CompositeCodeStorage.isSourceInRepository(sourceId, "platform"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "example"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "not_a_module"));
        }

        for (String sourceId : exampleSourceIds)
        {
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, null));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "platform"));
            Assert.assertTrue(CompositeCodeStorage.isSourceInRepository(sourceId, "example"));
            Assert.assertFalse(CompositeCodeStorage.isSourceInRepository(sourceId, "not_a_module"));
        }
    }

    @Test
    public void testGetFileOrFilesRootHasNoDuplicates()
    {
        RichIterable<CodeRepository> repos = CodeRepositoryProviderHelper.findCodeRepositories(getClass().getClassLoader(), true);
        RepositoryCodeStorage classpathStorage = new ClassLoaderCodeStorage(repos);

        CompositeCodeStorage composite = new CompositeCodeStorage(classpathStorage);

        MutableSet<String> expected = Sets.mutable.empty();
        for (CodeRepository repo : classpathStorage.getAllRepositories())
        {
            expected.addAllIterable(classpathStorage.getFileOrFiles("/" + repo.getName()));
        }

        RichIterable<String> actual = composite.getFileOrFiles("/");
        Assert.assertEquals("Root file listing should not contain duplicates", actual.toSet().size(), actual.size());
        Assert.assertEquals(expected, actual.toSet());
    }
}
