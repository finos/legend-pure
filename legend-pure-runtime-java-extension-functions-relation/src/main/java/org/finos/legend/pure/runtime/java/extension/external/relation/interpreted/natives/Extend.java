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
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
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
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Extend extends Shared
{
    public Extend(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, processorSupport);

        TestTDS tds = getTDS(params, 0, processorSupport);

        RelationType<?> relationType = getRelationType(params, 0);

        CoreInstance extendFunction = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(extendFunction.getValueForMetaPropertyToOne(M3Properties.function));
        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, extendFunction);

        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null);
        Type type = ((FunctionType) lambdaFunction._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        Object res = null;
        DataType resType = null;
        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] resStr = new String[(int) tds.getRowCount()];
            for (int i = 0; i < tds.getRowCount(); i++)
            {
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationType, -1, repository, false), true, processorSupport));
                CoreInstance newValue = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                resStr[i] = PrimitiveUtilities.getStringValue(newValue.getValueForMetaPropertyToOne("values"));
            }
            res = resStr;
            resType = DataType.STRING;
        }
        else if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] resInt = new int[(int) tds.getRowCount()];
            for (int i = 0; i < tds.getRowCount(); i++)
            {
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationType, -1, repository, false), true, processorSupport));
                CoreInstance newValue = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                resInt[i] = PrimitiveUtilities.getIntegerValue(newValue.getValueForMetaPropertyToOne("values")).intValue();
            }
            res = resInt;
            resType = DataType.INT;
        }
        else if (type == _Package.getByUserPath("Float", processorSupport))
        {
            double[] resDouble = new double[(int) tds.getRowCount()];
            for (int i = 0; i < tds.getRowCount(); i++)
            {
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationType, -1, repository, false), true, processorSupport));
                CoreInstance newValue = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                resDouble[i] = PrimitiveUtilities.getFloatValue(newValue.getValueForMetaPropertyToOne("values")).doubleValue();
            }
            res = resDouble;
            resType = DataType.DOUBLE;
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(tds.addColumn(extendFunction.getValueForMetaPropertyToOne(M3Properties.name).getName(), resType, res), returnGenericType, repository, processorSupport), false, processorSupport);
    }
}
