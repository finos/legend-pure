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

import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GroupBy extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public GroupBy(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> collection = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        boolean isExecutable = ValueSpecification.isExecutable(params.get(0), processorSupport);
        CoreInstance listType = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, processorSupport);
        CoreInstance listClassifierGenericType = listClassifierGenericType(listType, this.functionExecution.getRuntime().getModelRepository(), functionExpressionToUseInStack.getSourceInformation(), processorSupport);

        CoreInstance keyType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(params.get(1)), M3Properties.returnType, processorSupport);

        MapCoreInstance results = newMapInstance(keyType, listClassifierGenericType, this.functionExecution.getRuntime().getModelRepository(), functionExpressionToUseInStack.getSourceInformation(), processorSupport);

        if (!collection.isEmpty())
        {
            CoreInstance keyFn = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

            VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, keyFn);
            FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance)null);

            for (CoreInstance instance : collection)
            {
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(instance, isExecutable, processorSupport));
                CoreInstance keyInstanceValue = this.functionExecution.executeFunction(false, LambdaFunctionCoreInstanceWrapper.toLambdaFunction(keyFn), parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                CoreInstance key = Instance.getValueForMetaPropertyToOneResolved(keyInstanceValue, M3Properties.values, processorSupport);
                List<CoreInstance> list = (List) results.getMap().getIfAbsentPut(key, newListInstance(listClassifierGenericType, this.functionExecution.getRuntime().getModelRepository(), functionExpressionToUseInStack.getSourceInformation(), processorSupport));
                list._valuesAdd(instance);
            }
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(results, isExecutable, processorSupport);
    }



    private static MapCoreInstance newMapInstance(CoreInstance keyType, CoreInstance listType, ModelRepository modelRepo, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        CoreInstance mapClassifier = processorSupport.package_getByUserPath(M3Paths.Map);

        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);
        CoreInstance classifierGenericType = modelRepo.newEphemeralAnonymousCoreInstance(sourceInfo, genericTypeType);
        Instance.setValueForProperty(classifierGenericType, M3Properties.rawType, mapClassifier, processorSupport);
        Instance.setValuesForProperty(classifierGenericType, M3Properties.typeArguments, Lists.immutable.with(keyType, listType), processorSupport);

        MapCoreInstance map = new MapCoreInstance(Lists.immutable.<CoreInstance>empty(), "", sourceInfo, mapClassifier, -1, modelRepo, false, processorSupport);
        Instance.setValueForProperty(map, M3Properties.classifierGenericType, classifierGenericType, processorSupport);
        return map;
    }

    private static List<CoreInstance> newListInstance(CoreInstance listClassifierGenericType, ModelRepository modelRepo, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        CoreInstance listClassifier = processorSupport.package_getByUserPath(M3Paths.List);
        List<CoreInstance> list = (List<CoreInstance>)modelRepo.newEphemeralAnonymousCoreInstance(sourceInfo, listClassifier);
        Instance.setValueForProperty(list, M3Properties.classifierGenericType, listClassifierGenericType, processorSupport);
        return list;
    }

    private static CoreInstance listClassifierGenericType(CoreInstance listGenericType, ModelRepository modelRepo, SourceInformation sourceInfo, ProcessorSupport processorSupport)
    {
        CoreInstance listClassifier = processorSupport.package_getByUserPath(M3Paths.List);
        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);
        CoreInstance classifierGenericType = modelRepo.newEphemeralAnonymousCoreInstance(sourceInfo, genericTypeType);
        Instance.setValueForProperty(classifierGenericType, M3Properties.rawType, listClassifier, processorSupport);
        Instance.setValueForProperty(classifierGenericType, M3Properties.typeArguments, listGenericType, processorSupport);
        return classifierGenericType;
    }
}
