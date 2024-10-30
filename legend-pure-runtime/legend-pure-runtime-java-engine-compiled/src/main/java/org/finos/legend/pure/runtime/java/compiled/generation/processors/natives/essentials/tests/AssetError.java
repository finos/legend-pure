// Copyright 2024 Goldman Sachs
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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class AssetError extends AbstractNative
{
    public AssetError()
    {
        super("assertError_Function_1__String_1__Integer_$0_1$__Integer_$0_1$__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        SourceInformation sourceInformation = functionExpression.getSourceInformation();
        return "CoreGen.assertError(es, " + transformedParams.get(0) + ", " +
                transformedParams.get(1) + ", " +
                (transformedParams.get(2).equals("null") ? "-1" : transformedParams.get(2)) + ", " +
                (transformedParams.get(3).equals("null") ? "-1" : transformedParams.get(3)) + ", " +
                NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) +
                ")";
    }
}
