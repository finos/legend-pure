// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.lang;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestEvaluate;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;
import org.junit.Ignore;

public class TestEvaluate extends AbstractTestEvaluate
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    public boolean checkLineNumbers()
    {
        return false;
    }

    @Override
    @Ignore
    @ToFix
    public void testEvaluateUnboundedMultiplicity()
    {
    }

    @Override
    @Ignore
    @ToFix
    public void testEvaluateViolateUpperBound()
    {
    }

    @Override
    @Ignore
    @ToFix
    public void testEvaluateViolateLowerBound()
    {
    }

    @Override
    @Ignore
    @ToFix
    public void testEvaluateAnyWrongMultiplicity()
    {
    }
}
