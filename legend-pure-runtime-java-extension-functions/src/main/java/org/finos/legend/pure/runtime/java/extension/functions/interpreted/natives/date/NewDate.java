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

import org.finos.legend.pure.m4.coreinstance.primitive.date.Year;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class NewDate extends NativeFunction
{
    private final ModelRepository repository;

    public NewDate(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        PureDate date;
        switch (params.size())
        {
            case 1:
            {
                date = Year.newYear(
                        getInteger(params.get(0), processorSupport));
                break;
            }
            case 2:
            {
                date = DateFunctions.newPureDate(
                        getInteger(params.get(0), processorSupport),
                        getInteger(params.get(1), processorSupport));
                break;
            }
            case 3:
            {
                date = DateFunctions.newPureDate(
                        getInteger(params.get(0), processorSupport),
                        getInteger(params.get(1), processorSupport),
                        getInteger(params.get(2), processorSupport));
                break;
            }
            case 4:
            {
                date = DateFunctions.newPureDate(
                        getInteger(params.get(0), processorSupport),
                        getInteger(params.get(1), processorSupport),
                        getInteger(params.get(2), processorSupport),
                        getInteger(params.get(3), processorSupport));
                break;
            }
            case 5:
            {
                date = DateFunctions.newPureDate(
                        getInteger(params.get(0), processorSupport),
                        getInteger(params.get(1), processorSupport),
                        getInteger(params.get(2), processorSupport),
                        getInteger(params.get(3), processorSupport),
                        getInteger(params.get(4), processorSupport));
                break;
            }
            case 6:
            {
                IntObjectPair<String> secondAndSubSecond = getSecondAndSubSecond(params.get(5), processorSupport);
                int second = secondAndSubSecond.getOne();
                String subsecond = secondAndSubSecond.getTwo();
                if (subsecond == null)
                {
                    date = DateFunctions.newPureDate(
                            getInteger(params.get(0), processorSupport),
                            getInteger(params.get(1), processorSupport),
                            getInteger(params.get(2), processorSupport),
                            getInteger(params.get(3), processorSupport),
                            getInteger(params.get(4), processorSupport),
                            second);
                }
                else
                {
                    date = DateFunctions.newPureDate(
                            getInteger(params.get(0), processorSupport),
                            getInteger(params.get(1), processorSupport),
                            getInteger(params.get(2), processorSupport),
                            getInteger(params.get(3), processorSupport),
                            getInteger(params.get(4), processorSupport),
                            second,
                            subsecond);
                }
                break;
            }
            default:
            {
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Unexpected number of arguments: " + params.size());
            }
        }
        return ValueSpecificationBootstrap.newDateLiteral(this.repository, date, processorSupport);
    }

    private int getInteger(CoreInstance valueSpec, ProcessorSupport processorSupport) throws PureExecutionException
    {
        return PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(valueSpec, M3Properties.values, processorSupport)).intValue();
    }

    private IntObjectPair<String> getSecondAndSubSecond(CoreInstance valueSpec, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(valueSpec, M3Properties.values, processorSupport);
        if (Instance.instanceOf(value, M3Paths.Integer, processorSupport))
        {
            return PrimitiveTuples.pair(PrimitiveUtilities.getIntegerValue(value).intValue(), null);
        }

        String valueName = value.getName();
        int decimalIndex = valueName.indexOf('.');
        return PrimitiveTuples.pair(Integer.valueOf(valueName.substring(0, decimalIndex)), valueName.substring(decimalIndex + 1));
    }
}
