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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRepositoryComparator extends AbstractPureTestWithCoreCompiledPlatform
{
    private static MapIterable<String, CodeRepository> repositoriesByName;

    @BeforeClass
    public static void setUp()
    {
        ImmutableList<CodeRepository> repositories = Lists.immutable.<CodeRepository>with(
                GenericCodeRepository.build("datamart_dtm", "((datamarts::dtm::(domain|mapping|store))|(apps::dtm))(::.*)?", ""),//newDatamartCodeRepository("dtm"),
                GenericCodeRepository.build("datamart_datamt", "((datamarts::datamt::(domain|mapping|store))|(apps::datamt))(::.*)?", ""),//SVNCodeRepository.newDatamartCodeRepository("datamt"),
                GenericCodeRepository.build("model", "(model::(domain|mapping|store|producers|consumers|external)||(apps::model))(::.*)?", ""),//SVNCodeRepository.newModelCodeRepository(""),
                GenericCodeRepository.build("candidate", "(model_candidate::(domain|mapping|store|producers|consumers|external)||(apps::model_candidate))(::.*)?"),//SVNCodeRepository.newModelCodeRepository("candidate", Sets.immutable.with("")),
                GenericCodeRepository.build("legacy", "(model_legacy::(domain|mapping|store|producers|consumers|external)||(apps::model_legacy))(::.*)?"),//SVNCodeRepository.newModelCodeRepository("legacy", Sets.immutable.with("")),
                GenericCodeRepository.build("model_validation", "(model::producers)(::.*)?"),//SVNCodeRepository.newModelValidationCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?")//SVNCodeRepository.newSystemCodeRepository()
        ).newWithAll(CodeRepositoryProviderHelper.findCodeRepositories());
        repositoriesByName = repositories.groupByUniqueKey(CodeRepository::getName);
        setUpRuntime(new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories)), getExtra());
    }

    @Test
    public void testModelRepoCompilesBeforeModelValidation()
    {
        assertRepoExists("model");
        assertRepoExists("model_validation");
        assertRepoExists("system");
        assertRepoExists("datamart_datamt");
        RepositoryComparator comp = new RepositoryComparator(repositoriesByName.valuesView());
        assertComparison(0, comp, "model", "model");
        assertComparison(-1, comp, "model", "model_validation");
        assertComparison(1, comp, "model_validation", "model");

        MutableList<String> sortedRepos = Lists.mutable.with("model", "model_validation", "datamart_datamt", "system").sortThis(comp);
        assertIsSorted(sortedRepos);
    }

    @Test
    public void testSortAllRepos()
    {
        RepositoryComparator comp = new RepositoryComparator(repositoriesByName.valuesView());
        MutableList<String> repoNames = repositoriesByName.keysView().toSortedList(comp);
        assertIsSorted(repoNames);
    }

    private void assertComparison(int expected, RepositoryComparator comparator, String first, String second)
    {
        int actual = comparator.compare(first, second);
        int expectedSignum = Integer.signum(expected);
        int actualSignum = Integer.signum(actual);
        if (expectedSignum != actualSignum)
        {
            Assert.assertEquals("comparing \"" + first + "\" and \"" + second + "\"", expected, actual);
        }
    }

    private void assertIsSorted(ListIterable<String> repoNames)
    {
        repoNames.forEachWithIndex((repoName, i) ->
        {
            int start = ++i;
            CodeRepository repo1 = repositoriesByName.get(repoName);
            // check that no repo later in the list is visible to the current repo
            if ((start < repoNames.size()) && repoNames.subList(start, repoNames.size()).anySatisfy(rn2 -> repo1.isVisible(repositoriesByName.get(rn2))))
            {
                Assert.fail(repoNames.makeString("Repositories not properly sorted: ", ", ", ""));
            }
        });
    }
}
