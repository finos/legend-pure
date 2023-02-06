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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
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
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        PureDate date = PrimitiveUtilities.getDateValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
        Number number = PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport));
        if (number.intValue() == 0)
        {
            return params.get(0);
        }
        String unit = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, M3Properties.name, processorSupport).getName();
        PureDate result;
        switch (unit)
        {
            case "YEARS":
            {
                result = date.addYears(number.intValue());
                break;
            }
            case "MONTHS":
            {
                result = date.addMonths(number.intValue());
                break;
            }
            case "WEEKS":
            {
                result = date.addWeeks(number.intValue());
                break;
            }
            case "DAYS":
            {
                result = date.addDays(number.intValue());
                break;
            }
            case "HOURS":
            {
                result = date.addHours(number.intValue());
                break;
            }
            case "MINUTES":
            {
                result = date.addMinutes(number.intValue());
                break;
            }
            case "SECONDS":
            {
                result = date.addSeconds(number.intValue());
                break;
            }
            case "MILLISECONDS":
            {
                result = date.addMilliseconds(number.intValue());
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
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Unsupported duration unit: " + unit);
            }
        }
        return ValueSpecificationBootstrap.newDateLiteral(this.repository, result, processorSupport);
    }
}
