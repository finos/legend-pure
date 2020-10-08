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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.math;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.tools.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Stack;

public class Rem extends NativeFunction
{
    private final ModelRepository repository;

    public Rem(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance param0 = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance param1 = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        Number dividend = NumericUtilities.toJavaNumber(param0);
        Number divisor = NumericUtilities.toJavaNumber(param1);
        boolean bigDecimalToPureDecimal = NumericUtilities.IS_DECIMAL_CORE_INSTANCE.accept(param0) || NumericUtilities.IS_DECIMAL_CORE_INSTANCE.accept(param1);
        if (divisor.equals(0))
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Cannot divide " + dividend.toString() + " by zero");
        }
        if ((dividend instanceof Integer || dividend instanceof Long || dividend instanceof BigInteger)
                && (divisor instanceof Integer || divisor instanceof Long || divisor instanceof BigInteger))
        {
            if (dividend instanceof BigInteger || divisor instanceof BigInteger)
            {
                BigInteger dividendBigInteger = dividend instanceof BigInteger ? (BigInteger)dividend : BigInteger.valueOf(dividend.longValue());
                BigInteger divisorBigInteger = divisor instanceof BigInteger ? (BigInteger)divisor : BigInteger.valueOf(divisor.longValue());
                BigInteger result = dividendBigInteger.remainder(divisorBigInteger);
                return NumericUtilities.toPureNumberValueExpression(result, false, this.repository, processorSupport);
            }
            long result = dividend.longValue() % divisor.longValue();
            return NumericUtilities.toPureNumberValueExpression(result, false, this.repository, processorSupport);
        }
        BigDecimal dividendBigDecimal = dividend instanceof BigDecimal ? (BigDecimal)dividend : new BigDecimal(dividend.toString());
        BigDecimal divisorBigDecimal = divisor instanceof BigDecimal ? (BigDecimal)divisor : new BigDecimal(divisor.toString());
        BigDecimal result = dividendBigDecimal.remainder(divisorBigDecimal);
        return NumericUtilities.toPureNumberValueExpression(result, bigDecimalToPureDecimal, this.repository, processorSupport);
    }
}
