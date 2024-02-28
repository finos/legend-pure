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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.anonymousCollections;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
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

public class ReplaceTreeNode extends NativeFunction
{
    private final ModelRepository repository;

    public ReplaceTreeNode(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance sourceTree = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance targetNode = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance subTree = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);

        CoreInstance classifierGenericType = Instance.extractGenericTypeFromInstance(sourceTree, processorSupport);
        CoreInstance sourceClassifier = Instance.getValueForMetaPropertyToOneResolved(classifierGenericType, M3Properties.rawType, processorSupport);
        CoreInstance newInstance;

        if (sourceTree == targetNode)
        {
            newInstance = subTree;
        }
        else
        {
            newInstance = this.repository.newAnonymousCoreInstance(null, sourceClassifier);
            if (!this.copy(sourceTree, newInstance, targetNode, subTree, processorSupport))
            {
                throw new RuntimeException("Copy failed ... node not found!");
            }
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(newInstance, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
    }


    private boolean copy(CoreInstance instance, CoreInstance newInstance, CoreInstance targetNode, CoreInstance subTree, ProcessorSupport processorSupport)
    {
        boolean flag = false;

        for (String key : instance.getKeys())
        {
            if (M3Properties.childrenData.equals(key))
            {
                for (CoreInstance val : Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport))
                {
                    if (val == targetNode)
                    {
                        Instance.addValueToProperty(newInstance, key, subTree, processorSupport);
                        flag = true;
                    }
                    else
                    {
                        CoreInstance classifierGenericType = Instance.extractGenericTypeFromInstance(val, processorSupport);
                        CoreInstance sourceClassifier = Instance.getValueForMetaPropertyToOneResolved(classifierGenericType, M3Properties.rawType, processorSupport);
                        CoreInstance newChildInstance = this.repository.newAnonymousCoreInstance(null, sourceClassifier);
                        Instance.addValueToProperty(newInstance, key, newChildInstance, processorSupport);
                        flag = this.copy(val, newChildInstance, targetNode, subTree, processorSupport) || flag;
                    }
                }
            }
            else
            {
                for (CoreInstance val : Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport))
                {
                    Instance.addValueToProperty(newInstance, key, val, processorSupport);
                }
            }
        }
        return flag;
    }
}

