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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class Reactivate extends AbstractNative
{
    public Reactivate()
    {
        super("reactivate_ValueSpecification_1__Map_1__Any_MANY_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "CompiledSupport.toPureCollection(FunctionsGen.reactivate(" + transformedParams.get(0) + ", " + transformedParams.get(1) + ", es))";
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification, org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification input0, org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap input1, final ExecutionSupport es)\n" +
                "            {\n" +
                "                return CompiledSupport.toPureCollection(FunctionsGen.reactivate(input0, input1, es));\n" +
                "            }\n" +
                "        }";
    }
}
