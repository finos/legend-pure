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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Add extends AbstractNative
{
    public Add()
    {
        super("add_T_MANY__T_1__T_$1_MANY$_", "add_T_MANY__Integer_1__T_1__T_$1_MANY$_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        boolean isIndexedVersion = transformedParams.size() == 3;

        String list = transformedParams.get(0);
        String element = isIndexedVersion ? transformedParams.get(2) : transformedParams.get(1);
        String index = isIndexedVersion ? transformedParams.get(1) + ", " : "";
        String typeCast = "";

        if (!isIndexedVersion)
        {
            typeCast = processorContext.getSupport().valueSpecification_instanceOf(parametersValues.get(0), M3Paths.Nil)
                    ? "(RichIterable<" + TypeProcessor.typeToJavaObjectSingle(parametersValues.get(1).getValueForMetaPropertyToOne(M3Properties.genericType), true, processorSupport) + ">)"
                    : "";
        }

        return "CompiledSupport.add(CompiledSupport.toPureCollection(" + typeCast + list + "), " + index + element + ")";
    }
}
