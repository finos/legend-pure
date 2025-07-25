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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.operation;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class AdjustDate extends NativeFunction
{
    private final ModelRepository repository;

    public AdjustDate(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        try
        {
            PureDate date = PrimitiveUtilities.getDateValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
            Number number = PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport));
            String unit = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, M3Properties.name, processorSupport).getName();
            PureDate adjustedDate = adjustDate(date, number, unit, functionExpressionCallStack);
            return ValueSpecificationBootstrap.newDateLiteral(this.repository, adjustedDate, processorSupport);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    public static PureDate adjustDate(PureDate date, Number number, String unit, MutableStack<CoreInstance> functionExpressionCallStack)
    {
        if (number.intValue() == 0)
        {
            return date;
        }
        PureDate result;
        switch (unit)
        {
            case "YEARS":
            {
                result = date.addYears(number.longValue());
                break;
            }
            case "MONTHS":
            {
                result = date.addMonths(number.longValue());
                break;
            }
            case "WEEKS":
            {
                result = date.addWeeks(number.longValue());
                break;
            }
            case "DAYS":
            {
                result = date.addDays(number.longValue());
                break;
            }
            case "HOURS":
            {
                result = date.addHours(number.longValue());
                break;
            }
            case "MINUTES":
            {
                result = date.addMinutes(number.longValue());
                break;
            }
            case "SECONDS":
            {
                result = date.addSeconds(number.longValue());
                break;
            }
            case "MILLISECONDS":
            {
                result = date.addMilliseconds(number.longValue());
                break;
            }
            case "MICROSECONDS":
            {
                result = date.addMicroseconds(number.longValue());
                break;
            }
            case "NANOSECONDS":
            {
                result = date.addNanoseconds(number.longValue());
                break;
            }
            default:
            {
                throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Unsupported duration unit: " + unit, functionExpressionCallStack);
            }
        }
        return result;
    }
}
