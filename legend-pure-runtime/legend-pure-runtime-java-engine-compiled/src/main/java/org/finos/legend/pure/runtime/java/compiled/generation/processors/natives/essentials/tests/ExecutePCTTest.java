// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.tests;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

public class ExecutePCTTest extends AbstractNativeFunctionGeneric
{
    public ExecutePCTTest()
    {
        super(getMethod(CompiledSupport.class, "executePCTTest",
                        org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction.class,
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function.class,
                        org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction.class,
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function.class,
                        Object.class,
                        org.finos.legend.pure.m3.execution.ExecutionSupport.class),
                true, true, false, "executePCTTest_Function_1__Function_1__Map_1__TestResult_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "((org.finos.legend.pure.generated.Root_meta_pure_test_surveyor_TestResult) CompiledSupport.executePCTTest(" +
                "CoreGen.getSharedPureFunction(" + transformedParams.get(0) + ", es), " + transformedParams.get(0) + ", " +
                "CoreGen.getSharedPureFunction(" + transformedParams.get(1) + ", es), " + transformedParams.get(1) + ", " +
                transformedParams.get(2) + ", es))";
    }
}
