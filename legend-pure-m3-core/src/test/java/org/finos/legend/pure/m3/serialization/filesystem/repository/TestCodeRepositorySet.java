// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.factory.Sets;
import org.junit.Assert;
import org.junit.Test;

public class TestCodeRepositorySet
{
    @Test
    public void testBaseRepositories()
    {
        CodeRepositorySet set = CodeRepositorySet.newBuilder().build();
        Assert.assertEquals(1, set.size());
        Assert.assertEquals("platform", set.getRepositories().getAny().getName());
        Assert.assertTrue(set.getRepository("platform") instanceof PlatformCodeRepository);

        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> CodeRepositorySet.newBuilder().withoutCodeRepository("platform"));
        Assert.assertEquals("The code repository platform may not be removed", e.getMessage());
    }

    @Test
    public void testBuilder()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository badDependenciesRepo = new GenericCodeRepository("test_repo_bad_deps", "test3::.*", "platform", "non_existent");

        CodeRepositorySet set1 = CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build();
        Assert.assertEquals(3, set1.size());
        Assert.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b"), set1.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assert.assertSame(testRepoA, set1.getRepository("test_repo_a"));
        Assert.assertSame(testRepoB, set1.getRepository("test_repo_b"));

        CodeRepositorySet set2 = CodeRepositorySet.newBuilder()
                .withCodeRepositories(testRepoA, testRepoB, badDependenciesRepo)
                .withoutCodeRepository("test_repo_bad_deps")
                .build();
        Assert.assertEquals(3, set2.size());
        Assert.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b"), set2.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assert.assertSame(testRepoA, set2.getRepository("test_repo_a"));
        Assert.assertSame(testRepoB, set2.getRepository("test_repo_b"));

        Assert.assertEquals(set1, set2);
    }

    @Test
    public void testBuilder_NameConflict()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository fakePlatformRepo = new GenericCodeRepository("platform", "meta::.*");
        GenericCodeRepository testRepoA2 = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");

        // Try to add with name conflict with platform
        IllegalStateException e1 = Assert.assertThrows(IllegalStateException.class, () -> CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB, fakePlatformRepo));
        Assert.assertEquals("The code repository platform already exists!", e1.getMessage());

        // Try to add with name conflict among new repos
        IllegalStateException e2 = Assert.assertThrows(IllegalStateException.class, () -> CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB, testRepoA2));
        Assert.assertEquals("The code repository test_repo_a already exists!", e2.getMessage());
    }

    @Test
    public void testBuilder_BadDependency()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository badDependenciesRepo = new GenericCodeRepository("test_repo_bad_deps", "test3::.*", "platform", "non_existent");

        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB, badDependenciesRepo);
        IllegalStateException e = Assert.assertThrows(IllegalStateException.class, builder::build);
        Assert.assertEquals("The dependency 'non_existent' required by the Code Repository 'test_repo_bad_deps' can't be found!", e.getMessage());
    }

    @Test
    public void testBuilderFromManager()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository testRepoC = new GenericCodeRepository("test_repo_c", "test::c::.*", "platform", "test_repo_b");

        CodeRepositorySet set1 = CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build();
        Assert.assertEquals(3, set1.size());
        Assert.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b"), set1.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assert.assertSame(testRepoA, set1.getRepository("test_repo_a"));
        Assert.assertSame(testRepoB, set1.getRepository("test_repo_b"));

        CodeRepositorySet set2 = CodeRepositorySet.newBuilder(set1).withCodeRepository(testRepoC).build();
        Assert.assertEquals(4, set2.size());
        Assert.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b", "test_repo_c"), set2.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assert.assertSame(testRepoA, set2.getRepository("test_repo_a"));
        Assert.assertSame(testRepoB, set2.getRepository("test_repo_b"));
        Assert.assertSame(testRepoC, set2.getRepository("test_repo_c"));
        set1.forEach(repo1 -> Assert.assertSame(repo1, set2.getRepository(repo1.getName())));
    }

    @Test
    public void testGetRepository()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build();

        Assert.assertSame(testRepoA, set.getRepository("test_repo_a"));
        Assert.assertSame(testRepoB, set.getRepository("test_repo_b"));

        Assert.assertFalse(set.getOptionalRepository("test_repo_c").isPresent());
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> set.getRepository("test_repo_c"));
        Assert.assertEquals("The code repository 'test_repo_c' can't be found!", e.getMessage());
    }

    @Test
    public void testHasRepository()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build();
        Assert.assertTrue(set.hasRepository("platform"));
        Assert.assertTrue(set.hasRepository("test_repo_a"));
        Assert.assertTrue(set.hasRepository("test_repo_b"));
        Assert.assertFalse(set.hasRepository("test_repo_c"));
    }

    @Test
    public void testSubset()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository testRepoC = new GenericCodeRepository("test_repo_c", "test::c::.*", "platform", "test_repo_b");

        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB, testRepoC).build();
        Assert.assertEquals(4, set.size());
        Assert.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b", "test_repo_c"), set.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assert.assertSame(testRepoA, set.getRepository("test_repo_a"));
        Assert.assertSame(testRepoB, set.getRepository("test_repo_b"));
        Assert.assertSame(testRepoC, set.getRepository("test_repo_c"));

        // Full subset
        Assert.assertSame(set, set.subset(set.getRepositoryNames()));
        Assert.assertSame(set, set.subset("platform", "test_repo_a", "test_repo_b", "test_repo_c"));
        Assert.assertSame(set, set.subset("test_repo_a", "test_repo_b", "test_repo_c"));
        Assert.assertSame(set, set.subset("test_repo_a", "test_repo_c"));
        Assert.assertSame(set, set.subset("test_repo_b", "test_repo_c"));
        Assert.assertSame(set, set.subset("test_repo_c"));

        // Minimal subset
        Assert.assertEquals(CodeRepositorySet.newBuilder().build(), set.subset());
        Assert.assertEquals(CodeRepositorySet.newBuilder().build(), set.subset("platform"));

        // In between
        Assert.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build(), set.subset("platform", "test_repo_a", "test_repo_b"));
        Assert.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build(), set.subset("test_repo_a", "test_repo_b"));
        Assert.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA, testRepoB).build(), set.subset("test_repo_b"));

        Assert.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA).build(), set.subset("platform", "test_repo_a"));
        Assert.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(testRepoA).build(), set.subset("test_repo_a"));
    }
}
