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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

class InstanceValueExecutor implements Executor
{
    static final InstanceValueExecutor INSTANCE = new InstanceValueExecutor();

    private InstanceValueExecutor()
    {
    }

    @Override
    public CoreInstance execute(CoreInstance instance, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, VariableContext variableContext, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, FunctionExecutionInterpreted functionExecutionInterpreted, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance newInstanceValue = instance.getRepository().newEphemeralAnonymousCoreInstance(instance.getSourceInformation(), processorSupport.package_getByUserPath(M3Paths.InstanceValue));
        Instance.addValueToProperty(newInstanceValue, M3Properties.genericType, Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, processorSupport), processorSupport);
        Instance.addValueToProperty(newInstanceValue, M3Properties.multiplicity, Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.multiplicity, processorSupport), processorSupport);
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, M3Properties.values, processorSupport);
        MutableList<CoreInstance> processedValues = FastList.newList(values.size());
        for (CoreInstance value : values)
        {
            if (Instance.instanceOf(value, M3Paths.ValueSpecification, processorSupport))
            {
                Executor executor = FunctionExecutionInterpreted.findValueSpecificationExecutor(value, functionExpressionToUseInStack, processorSupport, functionExecutionInterpreted);
                CoreInstance result = executor.execute(value, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, variableContext, profiler, instantiationContext, executionSupport, functionExecutionInterpreted, processorSupport);
                if (Measure.isUnitOrMeasureInstance(result, processorSupport))
                {
                    processedValues.add(result);
                }
                else
                {
                    processedValues.addAllIterable(Instance.getValueForMetaPropertyToManyResolved(result, M3Properties.values, processorSupport));
                }
            }
            else if (Function.isLambda(value, processorSupport) && !(value instanceof LambdaWithContext))
            {
                if (value.getValueForMetaPropertyToMany(M3Properties.openVariables).notEmpty())
                {
                    processedValues.add(new LambdaWithContext(value, variableContext));
                }
                else
                {
                    processedValues.add(value);
                }
            }
            else
            {
                processedValues.add(value);
            }
        }
        Instance.setValuesForProperty(newInstanceValue, M3Properties.values, processedValues, processorSupport);
        return newInstanceValue;
    }
}
