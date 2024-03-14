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

package org.finos.legend.pure.m3.tests.incremental;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;

public class TestMultipleRepoIncrementalCompilation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getCodeStorage(), getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("/model/sourceId.pure");
        runtime.delete("/datamart_other/file1.pure");
        runtime.delete("/datamart_other/file2.pure");
        runtime.compile();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository platform = repositories.detect(x -> x.getName().equals("platform"));
        CodeRepository core = new GenericCodeRepository("zcore", null, "platform");
        CodeRepository system = new GenericCodeRepository("system", null, "platform", "zcore");
        CodeRepository model = new GenericCodeRepository("model", null, "platform", "zcore", "system");
        CodeRepository other = new GenericCodeRepository("datamart_other", null, "platform", "zcore", "system", "model");
        repositories.add(platform);
        repositories.add(core);
        repositories.add(system);
        repositories.add(model);
        repositories.add(other);
        return repositories;
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void verifyUnbindingRecompilationOrderSuccessAcrossRepos() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/sourceId.pure", "Class domain::A{version : Integer[1];}")
                        .createInMemorySource("/datamart_other/file1.pure", "function datamarts::dmt::doStuff1():Any[*]{domain::A.all();}")
                        // This function refers to the function above, so will be triggered for unbinding even though we don't change it's source
                        // This test verifies that we don't try to rebind it, until doStuff1 is back in scope
                        .createInMemorySource("/datamart_other/file2.pure", "function datamarts::dmt::doStuff2():Any[*]{datamarts::dmt::doStuff1();}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("/datamart_other/file1.pure", "function datamarts::dmt::doStuff1():Any[*]{domain::A.all();}")
                        .updateSource("/model/sourceId.pure", "Class domain::A{version : Integer[*];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void verifyUnbindingRecompilationOrderSuccessAcrossReposWithCompileFailureInSecondRepo() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/sourceId.pure", "Class domain::A{cats : Integer[1];}")
                        .createInMemorySource("/datamart_other/file1.pure", "function datamarts::dmt::doStuff1():Any[*]{domain::A.all().cats;}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        //Rename property
                        .updateSource("/model/sourceId.pure", "Class domain::A{dogs : Integer[1];}")
                        .compileWithExpectedCompileFailure("Can't find the property 'cats' in the class domain::A", "/datamart_other/file1.pure", 1, 60)
                        //Compile again - error should be the same - this checks that we are keeping the state of
                        //what has not been compiled correctly across runs
                        .compileWithExpectedCompileFailure("Can't find the property 'cats' in the class domain::A", "/datamart_other/file1.pure", 1, 60)
                        //Put it back
                        .updateSource("/model/sourceId.pure", "Class domain::A{cats : Integer[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }
}
