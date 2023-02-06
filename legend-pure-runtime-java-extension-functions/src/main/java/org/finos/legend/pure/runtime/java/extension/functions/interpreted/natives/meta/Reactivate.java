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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Reactivate extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public Reactivate(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        MapIterable<CoreInstance, CoreInstance> openVariablesMap = ((MapCoreInstance)Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport).getFirst()).getMap();
        RichIterable<Pair<CoreInstance, CoreInstance>> openVariables = openVariablesMap.keyValuesView();
        VariableContext newVarContext = VariableContext.newVariableContext();

        for (Pair<CoreInstance, CoreInstance> pair: openVariables)
        {
            try
            {
                String name = pair.getOne().getName();
                //todo: Should not need to do this, but there seem to be duplicates in the open variables
                if (newVarContext.getValue(name) == null)
                {
                    CoreInstance list = pair.getTwo();
                    CoreInstance inst = processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.InstanceValue);
                    Instance.addValueToProperty(inst, M3Properties.genericType, list.getValueForMetaPropertyToOne(M3Properties.genericType) , processorSupport);
                    Instance.addValueToProperty(inst, M3Properties.multiplicity, list.getValueForMetaPropertyToOne(M3Properties.multiplicity), processorSupport);
                    Instance.addValueToProperty(inst, M3Properties.values, list.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);
                    newVarContext.registerValue(name, inst);
                }
            }
            catch (VariableContext.VariableNameConflictException e)
            {
                throw new PureExecutionException(e);
            }
        }

        return this.functionExecution.executeValueSpecification(params.get(0).getValueForMetaPropertyToOne(M3Properties.values),
                resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, newVarContext, profiler, instantiationContext, executionSupport);
    }
}
