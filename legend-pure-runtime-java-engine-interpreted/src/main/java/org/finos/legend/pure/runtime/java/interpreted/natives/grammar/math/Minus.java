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

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Minus extends NativeFunction
{
    private final ModelRepository repository;

    public Minus(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> numbers = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        boolean bigDecimalToPureDecimal = numbers.anySatisfy(NumericUtilities.IS_DECIMAL_CORE_INSTANCE(processorSupport));
        int size = numbers.size();
        switch (size)
        {
            case 0:
            {
                return ValueSpecificationBootstrap.newIntegerLiteral(this.repository, 0, processorSupport);
            }
            case 1:
            {
                NumericAccumulator accumulator = NumericAccumulator.newAccumulator(0);
                accumulator.subtract(NumericUtilities.toJavaNumber(numbers.get(0), processorSupport));
                return NumericUtilities.toPureNumberValueExpression(accumulator.getValue(), bigDecimalToPureDecimal, this.repository, processorSupport);
            }
            default:
            {
                NumericAccumulator accumulator = NumericAccumulator.newAccumulator(NumericUtilities.toJavaNumber(numbers.get(0), processorSupport));
                for (int i = 1; i < size; i++)
                {
                    accumulator.subtract(NumericUtilities.toJavaNumber(numbers.get(i), processorSupport));
                }
                return NumericUtilities.toPureNumberValueExpression(accumulator.getValue(), bigDecimalToPureDecimal, this.repository, processorSupport);
            }
        }
    }
}
