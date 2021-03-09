package org.finos.legend.pure.m3.serialization.filesystem.genericrepository;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class TestGenericRepository
{
    @Test
    public void testRepositoryDetection()
    {
        RichIterable<CodeRepository> repositoryList = CodeRepositoryProviderHelper.findCodeRepositories();
        Assert.assertEquals(2, repositoryList.size());
        Verify.assertAllSatisfy(repositoryList, r -> r instanceof GenericCodeRepository);
        Verify.assertSetsEqual(Sets.mutable.with("test_generic_repository", "other_test_generic_repository"), repositoryList.collect(CodeRepository::getName).toSet());
    }

    @Test
    public void testGenericRepositoryDependency()
    {
        RichIterable<CodeRepository> repositoryList = CodeRepositoryProviderHelper.findCodeRepositories();
        CodeRepository other_test_generic_repository = repositoryList.detect(r -> r.getName().equals("other_test_generic_repository"));
        CodeRepository test_generic_repository = repositoryList.detect(r -> r.getName().equals("test_generic_repository"));
        Assert.assertFalse(test_generic_repository.isVisible(other_test_generic_repository));
        Assert.assertTrue(other_test_generic_repository.isVisible(test_generic_repository));
    }

    @Test
    public void testBuildCodeStorage() throws Exception
    {
        RichIterable<CodeRepository> repositoryList = CodeRepositoryProviderHelper.findCodeRepositories();
        PureCodeStorage codeStorage = PureCodeStorage.createCodeStorage(new File("").toPath(), repositoryList);
        Verify.assertSetsEqual(Sets.mutable.with("test_generic_repository", "other_test_generic_repository"), codeStorage.getAllRepoNames().toSet());
        Verify.assertSetsEqual(Sets.mutable.with("/test_generic_repository/testBla.pure", "/other_test_generic_repository/test.pure"), codeStorage.getUserFiles().toSet());
    }
}
