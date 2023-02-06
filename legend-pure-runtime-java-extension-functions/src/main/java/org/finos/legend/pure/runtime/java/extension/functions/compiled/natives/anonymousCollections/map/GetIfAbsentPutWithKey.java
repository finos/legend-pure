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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;


public class GetIfAbsentPutWithKey extends AbstractNative
{
    public GetIfAbsentPutWithKey()
    {
        super("getIfAbsentPutWithKey_Map_1__U_1__Function_1__V_$0_1$_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        CoreInstance functionType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(2), M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport);
        String returnType = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), processorSupport);
        CoreInstance param = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst();
        String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), true, processorSupport);

        String valueFunc = "new DefendedFunction<" + type + ","+returnType+">(){public "+returnType+" valueOf(" + type + " key){ PureFunction1<" + type + ","+returnType+"> func=(PureFunction1<" + type + ","+returnType+">)CoreGen.getSharedPureFunction(" + transformedParams.get(2) + ",es); ((PureMap)" + transformedParams.get(0) + ").getStats().incrementGetIfAbsentCounter(); return func.value(key,es); }}))";
        return "((" + returnType + ")((PureMap)" + transformedParams.get(0) + ").getMap().getIfAbsentPutWithKey(" + transformedParams.get(1) + "," + valueFunc;
    }
}