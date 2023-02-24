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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Zip extends AbstractNativeFunctionGeneric
{
    public Zip() {
        super("FunctionsGen.zip", new Class[]{Object.class, Object.class}, "zip_T_MANY__U_MANY__Pair_MANY_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        CoreInstance param1VS = parametersValues.get(0);
        String param1 = transformedParams.get(0);
        param1 = castToIterableIfParamIsNull(param1, param1VS, processorSupport);
        CoreInstance param2VS = parametersValues.get(1);
        String param2 = transformedParams.get(1);
        param2 = castToIterableIfParamIsNull(param2, param2VS, processorSupport);

        return "FunctionsGen.zip(" + param1 + ", " + param2 + ")";
    }

    private static String castToIterableIfParamIsNull(String paramStr, CoreInstance param, ProcessorSupport processorSupport)
    {
        if( Multiplicity.isToZero(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport)))
        {
            String castType = TypeProcessor.typeToJavaObjectWithMul(param.getValueForMetaPropertyToOne(M3Properties.genericType), param.getValueForMetaPropertyToOne(M3Properties.multiplicity), processorSupport);
            paramStr = "(RichIterable<? extends " + castType  + ">)" + paramStr;
        }
        return paramStr;
    }
}
