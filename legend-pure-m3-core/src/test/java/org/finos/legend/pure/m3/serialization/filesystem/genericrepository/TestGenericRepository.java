// Copyright 2021 Goldman Sachs
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
