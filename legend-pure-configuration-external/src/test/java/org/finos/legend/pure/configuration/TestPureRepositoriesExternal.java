package org.finos.legend.pure.configuration;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TestPureRepositoriesExternal
{
    @Before
    public void setUp()
    {
        PureRepositoriesExternal.refresh();
    }

    @After
    public void cleanUp()
    {
        PureRepositoriesExternal.refresh();
    }

    @Test
    public void testBaseRepositories()
    {
        assertRepoNames("platform");
        Assert.assertNotNull(PureRepositoriesExternal.getRepository("platform"));
        Assert.assertTrue(PureRepositoriesExternal.getRepository("platform") instanceof PlatformCodeRepository);
    }

    @Test
    public void testAddRepositories()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository badDependenciesRepo = new GenericCodeRepository("test_repo_bad_deps", "test3::.*", "platform", "non_existent");
        GenericCodeRepository fakePlatformRepo = new GenericCodeRepository("platform", "meta::.*");
        GenericCodeRepository testRepoA2 = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");

        assertRepoNames("platform");

        // Try to add with name conflict with platform
        RuntimeException e1 = Assert.assertThrows(RuntimeException.class, () -> PureRepositoriesExternal.addRepositories(Lists.fixedSize.with(testRepoA, testRepoB, fakePlatformRepo)));
        Assert.assertEquals("The code repository platform already exists!", e1.getMessage());
        assertRepoNames("platform");

        // Try to add with name conflict among new repos
        RuntimeException e2 = Assert.assertThrows(RuntimeException.class, () -> PureRepositoriesExternal.addRepositories(Lists.fixedSize.with(testRepoA, testRepoB, testRepoA2)));
        Assert.assertEquals("The code repository test_repo_a already exists!", e2.getMessage());
        assertRepoNames("platform");

        // Try to add with a bad dependency
        RuntimeException e3 = Assert.assertThrows(RuntimeException.class, () -> PureRepositoriesExternal.addRepositories(Lists.fixedSize.with(testRepoA, testRepoB, badDependenciesRepo)));
        Assert.assertEquals("The dependency 'non_existent' required by the Code Repository 'test_repo_bad_deps' can't be found!", e3.getMessage());
        assertRepoNames("platform");

        // Add valid
        PureRepositoriesExternal.addRepositories(Lists.fixedSize.with(testRepoA, testRepoB));
        assertRepoNames("platform", "test_repo_a", "test_repo_b");

        // Refresh
        PureRepositoriesExternal.refresh();
        assertRepoNames("platform");
    }

    @Test
    public void testThreadLocality() throws Exception
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");

        assertRepoNames("platform");
        PureRepositoriesExternal.addRepositories(Lists.fixedSize.with(testRepoA, testRepoB));
        assertRepoNames("platform", "test_repo_a", "test_repo_b");

        MutableList<String> threadRepoNames = Lists.mutable.<String>empty().asSynchronized();
        Thread thread = new Thread(() ->
        {
            PureRepositoriesExternal.repositories().collect(CodeRepository::getName, threadRepoNames);
            PureRepositoriesExternal.refresh();
        });
        thread.start();
        thread.join();

        // Test that addRepositories does not affect the other thread
        Assert.assertEquals(Lists.fixedSize.with("platform"), threadRepoNames.sortThis());

        // Test that refresh in the other thread does not affect this thread
        assertRepoNames("platform", "test_repo_a", "test_repo_b");
    }

    private void assertRepoNames(String... expectedRepoNames)
    {
        Arrays.sort(expectedRepoNames);
        Assert.assertEquals(ArrayAdapter.adapt(expectedRepoNames), PureRepositoriesExternal.repositories().collect(CodeRepository::getName, Lists.mutable.empty()).sortThis());
    }
}
