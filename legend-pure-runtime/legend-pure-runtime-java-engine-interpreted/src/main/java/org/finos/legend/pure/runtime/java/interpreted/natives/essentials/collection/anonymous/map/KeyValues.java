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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class KeyValues extends NativeFunction
{
    private final ModelRepository repository;

    public KeyValues(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance pairType = processorSupport.package_getByUserPath(M3Paths.Pair);

        CoreInstance genericType = processorSupport.newGenericType(null, pairType, false);
        Instance.addValueToProperty(genericType, M3Properties.rawType, pairType, processorSupport);
        Instance.addValueToProperty(genericType, M3Properties.typeArguments, params.get(0).getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToMany(M3Properties.typeArguments), processorSupport);

        RichIterable<CoreInstance> res = executeMap(this.repository, params, processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification(res, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
    }

    public static RichIterable<CoreInstance> executeMap(ModelRepository currentRepo, ListIterable<? extends CoreInstance> params, ProcessorSupport processorSupport) throws PureExecutionException
    {
        MutableMap<CoreInstance, CoreInstance> map = ((MapCoreInstance) params.get(0).getValueForMetaPropertyToOne(M3Properties.values)).getMap();
        CoreInstance pairType = processorSupport.package_getByUserPath(M3Paths.Pair);

        CoreInstance genericType = processorSupport.newGenericType(null, pairType, false);
        Instance.addValueToProperty(genericType, M3Properties.rawType, pairType, processorSupport);
        Instance.addValueToProperty(genericType, M3Properties.typeArguments, params.get(0).getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToMany(M3Properties.typeArguments), processorSupport);

        MutableList<CoreInstance> kvPairs = Lists.mutable.ofInitialCapacity(map.size());
        map.forEachKeyValue((key, value) ->
        {
            CoreInstance pair = currentRepo.newAnonymousCoreInstance(null, pairType, false);
            Instance.addValueToProperty(pair, M3Properties.classifierGenericType, genericType, processorSupport);
            Instance.addValueToProperty(pair, M3Properties.first, key, processorSupport);
            Instance.addValueToProperty(pair, M3Properties.second, value, processorSupport);
            kvPairs.add(pair);
        });
        return kvPairs;
    }
}
