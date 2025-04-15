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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.meta.type;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class GenericType extends AbstractNative
{
    public GenericType()
    {
        super("genericType_Any_MANY__GenericType_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        CoreInstance parameter = functionExpression.getValueForMetaPropertyToMany(M3Properties.parametersValues).getFirst();
        if (processorContext.getSupport().instance_instanceOf(functionExpression.getValueForMetaPropertyToMany(M3Properties.parametersValues).getFirst(), M3Paths.InstanceValue))
        {
            if (parameter.getValueForMetaPropertyToMany(M3Properties.values).isEmpty() && parameter.getValueForMetaPropertyToOne(M3Properties.genericType) instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType)
            {
                return "CoreGen.safeGetGenericType(" + ValueSpecificationProcessor.generateGenericTypeBuilder((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) parameter.getValueForMetaPropertyToOne(M3Properties.genericType), processorContext) + ", es)";
            }
        }
        return "CoreGen.safeGetGenericType(" + transformedParams.get(0) + ", es)";
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction1<Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object o, ExecutionSupport es)\n" +
                "            {\n" +
                "               return CoreGen.safeGetGenericType(o, es);\n" +
                "            }\n" +
                "        }";
    }
}
