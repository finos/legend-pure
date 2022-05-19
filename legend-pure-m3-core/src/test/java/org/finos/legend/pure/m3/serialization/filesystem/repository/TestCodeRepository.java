package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.junit.Assert;
import org.junit.Test;

public class TestCodeRepository
{
    @Test
    public void testToSortedRepositoriesList_Empty()
    {
        assertSort(Lists.fixedSize.empty());
    }

    @Test
    public void testToSortedRepositoriesList_Single()
    {
        CodeRepository platformRepo = CodeRepository.newPlatformCodeRepository();
        CodeRepository repo1 = GenericCodeRepository.build("repo_one", "meta::pure::\\.*", "platform");
        CodeRepository repo2 = GenericCodeRepository.build("repo_two", "meta::pure::\\.*", "platform");
        CodeRepository repo3 = GenericCodeRepository.build("repo_three", "meta::pure::\\.*", "platform", "repo_one");
        CodeRepository repo4 = GenericCodeRepository.build("repo_four", "meta::pure::\\.*", "platform", "repo_one", "repo_two");

        assertSort(Lists.fixedSize.with(platformRepo), platformRepo);
        assertSort(Lists.fixedSize.with(repo1), repo1);
        assertSort(Lists.fixedSize.with(repo2), repo2);
        assertSort(Lists.fixedSize.with(repo3), repo3);
        assertSort(Lists.fixedSize.with(repo4), repo4);
    }

    @Test
    public void testToSortedRepositoriesList_General()
    {
        CodeRepository platformRepo = CodeRepository.newPlatformCodeRepository();
        CodeRepository repo1 = GenericCodeRepository.build("repo_one", "meta::pure::\\.*", "platform");
        CodeRepository repo2 = GenericCodeRepository.build("repo_two", "meta::pure::\\.*", "platform");
        CodeRepository repo3 = GenericCodeRepository.build("repo_three", "meta::pure::\\.*", "platform", "repo_one");
        CodeRepository repo4 = GenericCodeRepository.build("repo_four", "meta::pure::\\.*", "platform", "repo_one", "repo_two");
        CodeRepository repo5 = GenericCodeRepository.build("repo_five", "meta::pure::\\.*", "platform", "repo_two");

        assertSort(Lists.fixedSize.with(platformRepo, repo1), repo1, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repo1), platformRepo, repo1);

        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2), repo1, repo2, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2), repo1, platformRepo, repo2);
        assertSort(Lists.fixedSize.with(platformRepo, repo2, repo1), repo2, platformRepo, repo1);

        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2, repo3, repo4), repo3, repo4, repo1, repo2, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2, repo3, repo4), repo3, repo1, repo4, repo2, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repo2, repo1, repo4, repo3), repo4, repo3, repo2, repo1, platformRepo);

        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo5, repo4, repo3), repo4, repo3, repo1, repo5, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repo5, repo1, repo4, repo3), repo4, repo3, repo5, repo1, platformRepo);

        assertSort(Lists.fixedSize.with(platformRepo, repo2, repo1, repo5, repo4, repo3), repo4, repo3, repo2, repo1, repo5, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2, repo5, repo4, repo3), repo4, repo3, repo1, repo2, repo5, platformRepo);
    }

    @Test
    public void testToSortedRepositoriesList_Loop()
    {
        CodeRepository platformRepo = CodeRepository.newPlatformCodeRepository();
        CodeRepository repo1 = GenericCodeRepository.build("repo_one", "meta::pure::\\.*", "platform");
        CodeRepository repo2 = GenericCodeRepository.build("repo_two", "meta::pure::\\.*", "platform", "repo_one");
        CodeRepository repo3 = GenericCodeRepository.build("repo_three", "meta::pure::\\.*", "platform", "repo_two", "repo_five");
        CodeRepository repo4 = GenericCodeRepository.build("repo_four", "meta::pure::\\.*", "platform", "repo_one", "repo_three");
        CodeRepository repo5 = GenericCodeRepository.build("repo_five", "meta::pure::\\.*", "platform", "repo_four", "repo_two");

        // No loops
        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2, repo3, repo4), platformRepo, repo1, repo2, repo3, repo4);
        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2, repo5, repo3), platformRepo, repo1, repo2, repo3, repo5);
        assertSort(Lists.fixedSize.with(platformRepo, repo1, repo2, repo4, repo5), platformRepo, repo1, repo2, repo4, repo5);

        // Loops
        RuntimeException e1 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(platformRepo, repo1, repo2, repo3, repo4, repo5)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_three (visible: repo_five), repo_four (visible: repo_three), repo_five (visible: repo_four)", e1.getMessage());

        RuntimeException e2 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(platformRepo, repo2, repo3, repo4, repo5)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_three (visible: repo_five), repo_four (visible: repo_three), repo_five (visible: repo_four)", e2.getMessage());

        RuntimeException e3 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(platformRepo, repo3, repo4, repo5)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_three (visible: repo_five), repo_four (visible: repo_three), repo_five (visible: repo_four)", e3.getMessage());

        RuntimeException e4 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(repo3, repo4, repo5)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_three (visible: repo_five), repo_four (visible: repo_three), repo_five (visible: repo_four)", e4.getMessage());
    }

    private void assertSort(ListIterable<? extends CodeRepository> expected, CodeRepository... repositories)
    {
        assertSort(expected, ArrayAdapter.adapt(repositories));
    }

    private void assertSort(ListIterable<? extends CodeRepository> expected, Iterable<? extends CodeRepository> repositories)
    {
        MutableList<? extends CodeRepository> actual = CodeRepository.toSortedRepositoryList(repositories);
        Assert.assertEquals(expected, actual);
        if (!isSorted(actual))
        {
            Assert.fail("Not properly sorted: " + actual);
        }
    }

    private static boolean isSorted(ListIterable<? extends CodeRepository> repositories)
    {
        int i = 0;
        for (CodeRepository repo : repositories)
        {
            int start = ++i;
            if ((start < repositories.size()) && repositories.subList(start, repositories.size()).anySatisfy(repo::isVisible))
            {
                return false;
            }
        }
        return true;
    }
}
