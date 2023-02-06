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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GenericTypeClass extends NativeFunction
{
    private final ModelRepository repository;

    public GenericTypeClass(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);

        if (Instance.instanceOf(rawType, M3Paths.Class, processorSupport))
        {
            CoreInstance clsGenericType = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
            Instance.addValueToProperty(clsGenericType, M3Properties.typeArguments, genericType, processorSupport);
            Instance.addValueToProperty(clsGenericType, M3Properties.rawType, processorSupport.package_getByUserPath(M3Paths.Class), processorSupport);

            CoreInstance cls = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.InstanceValue));
            Instance.addValueToProperty(cls, M3Properties.values, rawType, processorSupport);
            Instance.addValueToProperty(cls, M3Properties.genericType, clsGenericType, processorSupport);
            Instance.addValueToProperty(cls, M3Properties.multiplicity, processorSupport.package_getByUserPath(M3Paths.PureOne), processorSupport);
            return cls;
        }
        else
        {
            return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);
        }
    }
}
