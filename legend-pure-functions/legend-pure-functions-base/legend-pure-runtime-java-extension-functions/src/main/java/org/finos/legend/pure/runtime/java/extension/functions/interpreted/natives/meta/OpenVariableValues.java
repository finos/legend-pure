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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.LambdaWithContext;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class OpenVariableValues extends NativeFunction
{
    private final ModelRepository repository;

    public OpenVariableValues(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance f = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);

        CoreInstance mapRawType = processorSupport.package_getByUserPath(M3Paths.Map);
        MapCoreInstance map = new MapCoreInstance(params.size() > 1 ? params.get(1).getValueForMetaPropertyToMany(M3Properties.values) : Lists.immutable.<CoreInstance>empty(), "", functionExpressionToUseInStack.getSourceInformation(), mapRawType, -1, this.repository, false, processorSupport);
        MutableMap<CoreInstance, CoreInstance> internalMap = map.getMap();
        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);
        CoreInstance classifierGenericType = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), genericTypeType);
        Instance.addValueToProperty(classifierGenericType, M3Properties.rawType, mapRawType, processorSupport);
        Instance.addValueToProperty(classifierGenericType, M3Properties.typeArguments, Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.String), processorSupport), processorSupport);
        CoreInstance listGenericType = Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.List), processorSupport);
        Instance.addValueToProperty(listGenericType, M3Properties.typeArguments, Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Any), processorSupport), processorSupport);
        Instance.addValueToProperty(classifierGenericType, M3Properties.typeArguments, listGenericType, processorSupport);
        Instance.addValueToProperty(map, M3Properties.classifierGenericType, classifierGenericType, processorSupport);

        if (Instance.instanceOf(f, M3Paths.LambdaFunction, processorSupport) && f instanceof LambdaWithContext)
        {

            LambdaWithContext l = (LambdaWithContext)f;
            ListIterable<? extends CoreInstance> vars = l.getValueForMetaPropertyToMany(M3Properties.openVariables);
            VariableContext lambdaVariableContext = l.getVariableContext();
            for (CoreInstance var : vars)
            {
                CoreInstance instanceVal = lambdaVariableContext.getValue(var.getName());
                if (instanceVal != null)
                {
                    CoreInstance list = this.repository.newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.List));
                    Instance.setValuesForProperty(list, M3Properties.values, instanceVal.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);
                    CoreInstance varListGenericType = Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.List), processorSupport);
                    CoreInstance varGenType = instanceVal.getValueForMetaPropertyToOne(M3Properties.genericType);
                    Instance.setValueForProperty(varListGenericType, M3Properties.typeArguments, varGenType == null ? Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Any), processorSupport) : varGenType, processorSupport);
                    Instance.setValueForProperty(list, M3Properties.classifierGenericType, varListGenericType, processorSupport);
                    internalMap.put(this.repository.newStringCoreInstance(var.getName()), list);
                }
            }
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(map, true, processorSupport);
    }
}
