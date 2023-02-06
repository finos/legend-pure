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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Zip extends NativeFunction
{
    private final ModelRepository repository;

    public Zip(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance pairClassifier = processorSupport.package_getByUserPath(M3Paths.Pair);
        CoreInstance classifierGenericType = this.createPairClassifierGenericType(pairClassifier, params, functionExpressionToUseInStack, processorSupport);

        // instance values
        boolean exec1 = ValueSpecification.isExecutable(params.get(0), processorSupport);
        boolean exec2 = ValueSpecification.isExecutable(params.get(1), processorSupport);
        ListIterable<? extends CoreInstance> collection1 = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        ListIterable<? extends CoreInstance> collection2 = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);

        int collection1Size = collection1.size();
        int collection2Size = collection2.size();

        MutableList<CoreInstance> results = FastList.newList(collection1Size < collection2Size ? collection1Size : collection2Size);

        for (int i = 0; i < collection1Size && i < collection2Size; i++)
        {
            CoreInstance value1 = collection1.get(i);
            CoreInstance value2 = collection2.get(i);

            CoreInstance result = this.repository.newAnonymousCoreInstance(null, pairClassifier);
            Instance.addValueToProperty(result, M3Properties.classifierGenericType, classifierGenericType, processorSupport);
            Instance.addValueToProperty(result, M3Properties.first, exec1?value1:ValueSpecificationBootstrap.wrapValueSpecification(value1, false, processorSupport), processorSupport);
            Instance.addValueToProperty(result, M3Properties.second, exec2?value2:ValueSpecificationBootstrap.wrapValueSpecification(value2, false, processorSupport), processorSupport);

            results.add(result);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(results, true, processorSupport);
    }

    private CoreInstance createPairClassifierGenericType(CoreInstance pairClassifier,
                                                         ListIterable<? extends CoreInstance> params,
                                                         CoreInstance functionExpressionToUseInStack,
                                                         ProcessorSupport processorSupport)
    {
        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);
        CoreInstance classifierGenericType = this.repository.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), genericTypeType);
        Instance.addValueToProperty(classifierGenericType, M3Properties.rawType, pairClassifier, processorSupport);
        CoreInstance collection1Type = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, processorSupport);
        Instance.addValueToProperty(classifierGenericType, M3Properties.typeArguments, collection1Type, processorSupport);
        CoreInstance collection2Type = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.genericType, processorSupport);
        Instance.addValueToProperty(classifierGenericType, M3Properties.typeArguments, collection2Type, processorSupport);

        return classifierGenericType;
    }
}
