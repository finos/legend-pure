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

package org.finos.legend.pure.runtime.java.compiled.incremental;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.RuntimeVerifier;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.incremental.AbstractTestIncrementalCompilation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.compiled.CompiledClassloaderStateVerifier;
import org.finos.legend.pure.runtime.java.compiled.CompiledMetadataStateVerifier;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.BeforeClass;

public class TestIncrementalCompilationCompiled extends AbstractTestIncrementalCompilation
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getCodeRepositories());
    }
    @After
    public void cleanRuntime() {
        runtime.delete("s1.pure");
        runtime.delete("s2.pure");
        runtime.delete("s3.pure");
        runtime.delete("s4.pure");
        runtime.delete("s5.pure");
        runtime.delete("sourceId1.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("sourceId3.pure");
        runtime.delete("/model/sourceId1.pure");
        runtime.delete("/model/domain/sourceId3.pure");
        runtime.delete("/datamart_other/sourceId2.pure");
        runtime.delete("/system/tests/sourceId1.pure");
        runtime.delete("/system/tests/resources/sourceId2.pure");

        try{
            runtime.compile();
        } catch (PureCompilationException e) {
            setUp();
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    protected ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getAdditionalVerifiers()
    {
        return Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of(new CompiledMetadataStateVerifier(), new CompiledClassloaderStateVerifier());
    }

    public static Pair<String, String> getExtra()
    {
        return null;
    }
}
