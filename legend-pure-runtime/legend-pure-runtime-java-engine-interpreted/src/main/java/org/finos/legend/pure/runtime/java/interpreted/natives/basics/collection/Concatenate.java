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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Concatenate extends NativeFunction
{
    public Concatenate(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance param1 = params.get(0);
        CoreInstance param2 = params.get(1);

        ListIterable<? extends CoreInstance> collection1 = Instance.getValueForMetaPropertyToManyResolved(param1, M3Properties.values, processorSupport);
        int size1 = collection1.size();
        if (size1 == 0)
        {
            return param2;
        }

        ListIterable<? extends CoreInstance> collection2 = Instance.getValueForMetaPropertyToManyResolved(param2, M3Properties.values, processorSupport);
        int size2 = collection2.size();
        if (size2 == 0)
        {
            return param1;
        }

        MutableList<CoreInstance> results = FastList.newList(size1 + size2);
        results.addAllIterable(collection1);
        results.addAllIterable(collection2);
        boolean executable = ValueSpecification.isExecutable(param1, processorSupport) || ValueSpecification.isExecutable(param2, processorSupport);
        CoreInstance genericType = GenericType.findBestCommonCovariantNonFunctionTypeGenericType(
                Lists.fixedSize.<CoreInstance>of(Instance.getValueForMetaPropertyToOneResolved(param1, M3Properties.genericType, processorSupport),
                        Instance.getValueForMetaPropertyToOneResolved(param2, M3Properties.genericType, processorSupport)), null, null, processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(results, genericType, executable, processorSupport);
    }
}
