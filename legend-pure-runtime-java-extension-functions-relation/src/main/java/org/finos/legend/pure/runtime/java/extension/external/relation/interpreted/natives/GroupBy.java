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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GroupBy extends Shared
{
    public GroupBy(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        TestTDS tds = getTDS(params, 0, processorSupport);

        ListIterable<String> ids = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getValueForMetaPropertyToMany("names").collect(CoreInstance::getName);

        CoreInstance aggColSpec = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        String name = aggColSpec.getValueForMetaPropertyToOne("name").getName();
        LambdaFunction<CoreInstance> mapF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("map"));
        LambdaFunction<CoreInstance> reduceF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("reduce"));
        VariableContext mapFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, mapF);
        VariableContext reduceFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, reduceF);

        Type type = ((FunctionType) reduceF._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> res = tds.sort(ids.collect(c -> new SortInfo(c, SortDirection.ASC)));

        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null);

        int size = res.getTwo().size();
        DataType resType = null;
        Object _finalRes = null;
        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] finalRes = new String[size];
            for (int j = 0; j < size; j++)
            {
                Pair<Integer, Integer> r = res.getTwo().get(j);
                MutableList<CoreInstance> subList = Lists.mutable.empty();
                for (int i = r.getOne(); i < r.getTwo(); i++)
                {
                    parameters.set(0, new TDSWithCursorCoreInstance(res.getOne(), i, "", null, null, -1, repository, false));
                    subList.add(this.functionExecution.executeFunction(false, mapF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values"));
                }
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
                CoreInstance re = this.functionExecution.executeFunction(false, reduceF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                finalRes[j] = PrimitiveUtilities.getStringValue(re.getValueForMetaPropertyToOne("values"));
            }
            resType = DataType.STRING;
            _finalRes = finalRes;
        }

        if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] finalRes = new int[size];
            for (int j = 0; j < size; j++)
            {
                Pair<Integer, Integer> r = res.getTwo().get(j);
                MutableList<CoreInstance> subList = Lists.mutable.empty();
                for (int i = r.getOne(); i < r.getTwo(); i++)
                {
                    parameters.set(0, new TDSWithCursorCoreInstance(res.getOne(), i, "", null, null, -1, repository, false));
                    subList.add(this.functionExecution.executeFunction(false, mapF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values"));
                }
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
                CoreInstance re = this.functionExecution.executeFunction(false, reduceF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                finalRes[j] = PrimitiveUtilities.getIntegerValue(re.getValueForMetaPropertyToOne("values")).intValue();
            }
            resType = DataType.INT;
            _finalRes = finalRes;
        }

        if (type == _Package.getByUserPath("Float", processorSupport))
        {
            double[] finalRes = new double[size];
            for (int j = 0; j < size; j++)
            {
                Pair<Integer, Integer> r = res.getTwo().get(j);
                MutableList<CoreInstance> subList = Lists.mutable.empty();
                for (int i = r.getOne(); i < r.getTwo(); i++)
                {
                    parameters.set(0, new TDSWithCursorCoreInstance(res.getOne(), i, "", null, null, -1, repository, false));
                    subList.add(this.functionExecution.executeFunction(false, mapF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values"));
                }
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
                CoreInstance re = this.functionExecution.executeFunction(false, reduceF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                finalRes[j] = PrimitiveUtilities.getFloatValue(re.getValueForMetaPropertyToOne("values")).doubleValue();
            }
            resType = DataType.FLOAT;
            _finalRes = finalRes;
        }

        tds = res.getOne()._distinct(res.getTwo());
        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(tds.addColumn(name, resType, _finalRes), "", null, params.get(0).getValueForMetaPropertyToOne("values").getClassifier(), -1, repository, false), false, processorSupport);
    }
}
