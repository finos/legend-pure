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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.type;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class InstanceOf extends NativeFunction
{
    private final ModelRepository modelRepository;

    public InstanceOf(ModelRepository modelRepository)
    {
        this.modelRepository = modelRepository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance sourceRawType = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, M3Properties.rawType, processorSupport);
        if (Measure.isUnitInstance(params.get(0), processorSupport))
        {
            return ValueSpecificationBootstrap.newBooleanLiteral(this.modelRepository,
                    processorSupport.type_subTypeOf(sourceRawType, Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport)),
                    processorSupport);
        }
        return ValueSpecificationBootstrap.newBooleanLiteral(this.modelRepository,
                Instance.instanceOf(
                        Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport),
                        Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport),
                        processorSupport),
                processorSupport);
    }
}

