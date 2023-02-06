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

package org.finos.legend.pure.runtime.java.interpreted.function.base.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.meta.AbstractTestCompileValueSpecification;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;

public class TestCompileValueSpecification extends AbstractTestCompileValueSpecification
{

    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }
    @After
    public void cleanRuntime() {
        runtime.delete("testSource.pure");
        runtime.delete("exec1.pure");
        runtime.delete("source1.pure");
        runtime.delete("source2.pure");
        runtime.delete("source3.pure");
    }
     protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    @Override
    public ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getExecutionVerifiers()
    {
        return Lists.fixedSize.empty();
    }
}
