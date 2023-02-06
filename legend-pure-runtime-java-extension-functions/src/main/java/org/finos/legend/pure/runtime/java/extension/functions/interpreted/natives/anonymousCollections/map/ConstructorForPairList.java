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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections.map;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class ConstructorForPairList extends NativeFunction
{
    private final ModelRepository repository;

    public ConstructorForPairList(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance mapRawType = processorSupport.package_getByUserPath(M3Paths.Map);
        MapCoreInstance map = new MapCoreInstance(params.size() > 1 ? params.get(1).getValueForMetaPropertyToMany(M3Properties.values) : Lists.immutable.<CoreInstance>empty(), "", functionExpressionToUseInStack.getSourceInformation(), mapRawType, -1, this.repository, false, processorSupport);
        CoreInstance genericType = processorSupport.newGenericType(null, map, false);
        Instance.addValueToProperty(genericType, M3Properties.rawType, mapRawType, processorSupport);
        Instance.addValueToProperty(genericType, M3Properties.typeArguments, getMapTypeArguments(functionExpressionToUseInStack, processorSupport), processorSupport);
        Instance.addValueToProperty(map, M3Properties.classifierGenericType, genericType, processorSupport);

        MutableMap<CoreInstance, CoreInstance> internalMap = map.getMap();
        RichIterable<? extends CoreInstance> values = params.get(0).getValueForMetaPropertyToMany(M3Properties.values);
        for (CoreInstance val : values)
        {
            CoreInstance first = val.getValueForMetaPropertyToOne(M3Properties.first);
            CoreInstance key = Instance.instanceOf(first, M3Paths.ValueSpecification, processorSupport) ? first.getValueForMetaPropertyToOne(M3Properties.values) : first;
            internalMap.put(key, val.getValueForMetaPropertyToOne(M3Properties.second));
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(map, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
    }

    private RichIterable<? extends CoreInstance> getMapTypeArguments(CoreInstance functionExpressionToUseInStack, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = functionExpressionToUseInStack.getValueForMetaPropertyToOne(M3Properties.genericType);
        RichIterable<? extends CoreInstance> typeArguments = genericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
        if (typeArguments.size() != 2)
        {
            StringBuilder message = new StringBuilder("Error getting type parameters for ");
            _Class.print(message, processorSupport.package_getByUserPath(M3Paths.Map));
            message.append(" from ");
            GenericType.print(message, genericType, processorSupport);
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), message.toString());
        }
        return typeArguments;
    }
}