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

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GetAll extends NativeFunction
{

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance _theClass = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);

        SetIterable<CoreInstance> result = context.getClassifierInstances(_theClass);

        CoreInstance _Class = processorSupport.package_getByUserPath(M3Paths.Class);
        if (_theClass == _Class)
        {
            CoreInstance genericType = Type.wrapGenericType(_Class, processorSupport);
            Instance.addValueToProperty(genericType, M3Properties.typeArguments, processorSupport.package_getByUserPath(M3Paths.Any), processorSupport);

            CoreInstance inst = processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.InstanceValue);
            Instance.addValueToProperty(inst, M3Properties.genericType, genericType, processorSupport);
            Instance.addValueToProperty(inst, M3Properties.multiplicity, processorSupport.package_getByUserPath(M3Paths.ZeroMany), processorSupport);
            Instance.addValueToProperty(inst, M3Properties.values, result, processorSupport);
            return inst;
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(result.toList(), true, processorSupport);
    }
}
