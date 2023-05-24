// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.dsl.path.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class PathExtensionInterpreted extends BaseInterpretedExtension
{
    public PathExtensionInterpreted()
    {
        super(Lists.mutable.empty());
    }

    public static InterpretedExtension extension()
    {
        return new PathExtensionInterpreted();
    }

    @Override
    public CoreInstance getExtraFunctionExecution(Function<CoreInstance> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, FunctionExecutionInterpreted interpreted)
    {
        if (Instance.instanceOf(function, M2PathPaths.Path, processorSupport))
        {
            boolean executable = ValueSpecification.isExecutable(params.get(0), processorSupport);
            CoreInstance value = params.get(0).getValueForMetaPropertyToOne(M3Properties.values);
            MutableList<CoreInstance> res = function.getValueForMetaPropertyToMany(M3Properties.path).injectInto(Lists.mutable.with(value), (instances, pathElement) ->
                    instances.flatCollect(instance ->
                    {
                        CoreInstance property = Instance.getValueForMetaPropertyToOneResolved(pathElement, M3Properties.property, processorSupport);
                        MutableList<CoreInstance> parameters = Lists.mutable.with(ValueSpecificationBootstrap.wrapValueSpecification(instance, executable, processorSupport));
                        parameters.addAllIterable(Instance.getValueForMetaPropertyToManyResolved(pathElement, M3Properties.parameters, processorSupport).collect(ci -> ValueSpecificationBootstrap.wrapValueSpecification(Instance.getValueForMetaPropertyToManyResolved(ci, M3Properties.values, processorSupport), executable, processorSupport)));
                        return (Iterable<CoreInstance>) interpreted.executeFunction(false, PropertyCoreInstanceWrapper.toProperty(property), parameters, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToMany(M3Properties.values);
                    }));
            return ValueSpecificationBootstrap.wrapValueSpecification(res, executable, processorSupport);
        }
        return null;
    }
}
