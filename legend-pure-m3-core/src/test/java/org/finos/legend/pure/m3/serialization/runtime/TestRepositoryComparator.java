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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRepositoryComparator extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getCodeStorage(), getCodeRepositories(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(
                SVNCodeRepository.newDatamartCodeRepository("dtm"),
                SVNCodeRepository.newDatamartCodeRepository("datamt"),
                SVNCodeRepository.newModelCodeRepository(""),
                SVNCodeRepository.newModelCodeRepository("candidate", Sets.immutable.with("")),
                SVNCodeRepository.newModelCodeRepository("legacy", Sets.immutable.with("")),
                SVNCodeRepository.newModelValidationCodeRepository(),
                SVNCodeRepository.newSystemCodeRepository(),
                CodeRepository.newCoreCodeRepository(),
                CodeRepository.newPlatformCodeRepository()
        );
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void testModelRepoCompilesBeforeModelValidation()
    {
        assertRepoExists("model");
        assertRepoExists("model_validation");
        assertRepoExists("system");
        assertRepoExists("datamart_datamt");
        RepositoryComparator comp = new RepositoryComparator(getCodeRepositories());
        Assert.assertEquals(0, comp.compare("model", "model"));
        Assert.assertEquals(-1, comp.compare("model", "model_validation"));
        Assert.assertEquals(1, comp.compare("model_validation", "model"));
        Assert.assertEquals(Lists.immutable.with("system", "model", "model_validation", "datamart_datamt"), Lists.immutable.with("model", "model_validation", "datamart_datamt", "system").toSortedList(comp));
    }
}
