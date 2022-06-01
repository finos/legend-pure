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
        CodeRepository repoA = GenericCodeRepository.build("repo_a", "meta::pure::\\.*", "platform");
        CodeRepository repoB = GenericCodeRepository.build("repo_b", "meta::pure::\\.*", "platform");
        CodeRepository repoC = GenericCodeRepository.build("repo_c", "meta::pure::\\.*", "platform", "repo_a");
        CodeRepository repoD = GenericCodeRepository.build("repo_d", "meta::pure::\\.*", "platform", "repo_a", "repo_b");

        assertSort(Lists.fixedSize.with(platformRepo), platformRepo);
        assertSort(Lists.fixedSize.with(repoA), repoA);
        assertSort(Lists.fixedSize.with(repoB), repoB);
        assertSort(Lists.fixedSize.with(repoC), repoC);
        assertSort(Lists.fixedSize.with(repoD), repoD);
    }

    @Test
    public void testToSortedRepositoriesList_General()
    {
        CodeRepository platformRepo = CodeRepository.newPlatformCodeRepository();
        CodeRepository repoA = GenericCodeRepository.build("repo_a", "meta::pure::\\.*", "platform");
        CodeRepository repoB = GenericCodeRepository.build("repo_b", "meta::pure::\\.*", "platform");
        CodeRepository repoC = GenericCodeRepository.build("repo_c", "meta::pure::\\.*", "platform", "repo_a");
        CodeRepository repoD = GenericCodeRepository.build("repo_d", "meta::pure::\\.*", "platform", "repo_a", "repo_b");
        CodeRepository repoE = GenericCodeRepository.build("repo_e", "meta::pure::\\.*", "platform", "repo_b");

        assertSort(Lists.fixedSize.with(platformRepo, repoA), repoA, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repoA), platformRepo, repoA);

        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB), repoA, repoB, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB), repoA, platformRepo, repoB);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB), repoB, platformRepo, repoA);

        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD), repoC, repoD, repoA, repoB, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD), repoC, repoA, repoD, repoB, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD), repoD, repoC, repoB, repoA, platformRepo);

        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoE, repoC, repoD), repoD, repoC, repoA, repoE, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoE, repoC, repoD), repoD, repoC, repoE, repoA, platformRepo);

        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD, repoE), repoD, repoC, repoB, repoA, repoE, platformRepo);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD, repoE), repoD, repoC, repoA, repoB, repoE, platformRepo);
    }

    @Test
    public void testToSortedRepositoriesList_Loop()
    {
        CodeRepository platformRepo = CodeRepository.newPlatformCodeRepository();
        CodeRepository repoA = GenericCodeRepository.build("repo_a", "meta::pure::\\.*", "platform");
        CodeRepository repoB = GenericCodeRepository.build("repo_b", "meta::pure::\\.*", "platform", "repo_a");
        CodeRepository repoC = GenericCodeRepository.build("repo_c", "meta::pure::\\.*", "platform", "repo_b", "repo_e");
        CodeRepository repoD = GenericCodeRepository.build("repo_d", "meta::pure::\\.*", "platform", "repo_a", "repo_c");
        CodeRepository repoE = GenericCodeRepository.build("repo_e", "meta::pure::\\.*", "platform", "repo_d", "repo_b");

        // No loops
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD), platformRepo, repoA, repoB, repoC, repoD);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoE, repoC), platformRepo, repoA, repoB, repoC, repoE);
        assertSort(Lists.fixedSize.with(platformRepo, repoA, repoB, repoD, repoE), platformRepo, repoA, repoB, repoD, repoE);

        // Loops
        RuntimeException e1 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(platformRepo, repoA, repoB, repoC, repoD, repoE)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_c (visible: repo_e), repo_d (visible: repo_c), repo_e (visible: repo_d)", e1.getMessage());

        RuntimeException e2 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(platformRepo, repoB, repoC, repoD, repoE)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_c (visible: repo_e), repo_d (visible: repo_c), repo_e (visible: repo_d)", e2.getMessage());

        RuntimeException e3 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(platformRepo, repoC, repoD, repoE)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_c (visible: repo_e), repo_d (visible: repo_c), repo_e (visible: repo_d)", e3.getMessage());

        RuntimeException e4 = Assert.assertThrows(RuntimeException.class, () -> CodeRepository.toSortedRepositoryList(Lists.fixedSize.with(repoC, repoD, repoE)));
        Assert.assertEquals("Could not consistently order the following repositories: repo_c (visible: repo_e), repo_d (visible: repo_c), repo_e (visible: repo_d)", e4.getMessage());
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
