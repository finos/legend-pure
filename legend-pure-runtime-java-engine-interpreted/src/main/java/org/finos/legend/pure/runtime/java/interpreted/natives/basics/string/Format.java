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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.string;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.FormatTools;
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

public class Format extends NativeFunction
{
    private final ModelRepository repository;
    private final FunctionExecutionInterpreted functionExecution;

    public Format(ModelRepository repository, FunctionExecutionInterpreted functionExecution)
    {
        this.repository = repository;
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        String formatString = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
        ListIterable<? extends CoreInstance> formatArgs = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);
        return ValueSpecificationBootstrap.newStringLiteral(this.repository, format(formatString, formatArgs, resolvedTypeParameters, resolvedMultiplicityParameters, getParentOrEmptyVariableContext(variableContext), functionExpressionToUseInStack, profiler, processorSupport, instantiationContext, executionSupport), processorSupport);
    }

    private String format(String formatString, ListIterable<? extends CoreInstance> formatArgs, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, ProcessorSupport processorSupport, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        CoreInstance toStringFunction = processorSupport.package_getByUserPath("meta::pure::functions::string::toString_Any_1__String_1_");
        CoreInstance toRepresentationFunction = processorSupport.package_getByUserPath("meta::pure::functions::string::toRepresentation_Any_1__String_1_");
        int argCounter = 0;
        int index = 0;
        int length = formatString.length();
        StringBuilder builder = new StringBuilder(length * 2);

        try
        {


            while (index < length)
            {
                char character = formatString.charAt(index++);
                if (character == '%')
                {
                    switch (formatString.charAt(index++))
                    {
                        case '%':
                        {
                            builder.append('%');
                            break;
                        }
                        case 's':
                        {
                            CoreInstance arg = formatArgs.get(argCounter++);
                            ListIterable<CoreInstance> params = Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(arg, true, processorSupport));
                            CoreInstance argString = this.functionExecution.executeLambdaFromNative(toStringFunction, params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                            builder.append(Instance.getValueForMetaPropertyToOneResolved(argString, M3Properties.values, processorSupport).getName());
                            break;
                        }
                        case 'r':
                        {
                            CoreInstance arg = formatArgs.get(argCounter++);
                            ListIterable<CoreInstance> params = Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(arg, true, processorSupport));
                            CoreInstance argString = this.functionExecution.executeLambdaFromNative(toRepresentationFunction, params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                            builder.append(Instance.getValueForMetaPropertyToOneResolved(argString, M3Properties.values, processorSupport).getName());
                            break;
                        }
                        case 'd':
                        {
                            CoreInstance arg = formatArgs.get(argCounter++);
                            if (!Instance.instanceOf(arg, M3Paths.Integer, processorSupport))
                            {
                                throw new IllegalArgumentException("Expected Integer, got: " + arg);
                            }
                            FormatTools.appendIntegerString(builder, arg.getName(), 0);
                            break;
                        }
                        case 't':
                        {
                            CoreInstance arg = formatArgs.get(argCounter++);
                            if (!Instance.instanceOf(arg, M3Paths.Date, processorSupport))
                            {
                                throw new IllegalArgumentException("Expected Date, got: " + arg);
                            }
                            PureDate date = PrimitiveUtilities.getDateValue(arg);
                            int dateFormatEnd = FormatTools.findEndOfDateFormatString(formatString, index);
                            if (dateFormatEnd == -1)
                            {
                                builder.append(date);
                            }
                            else
                            {
                                date.format(builder, formatString.substring(index + 1, dateFormatEnd));
                                index = dateFormatEnd + 1;
                            }
                            break;
                        }
                        case '0':
                        {
                            int j = index;
                            while (Character.isDigit(formatString.charAt(j)))
                            {
                                j++;
                            }
                            if (formatString.charAt(j) != 'd')
                            {
                                throw new IllegalArgumentException("Invalid format specifier: %" + formatString.substring(index, j + 1));
                            }
                            int zeroPad = Integer.valueOf(formatString.substring(index, j));
                            CoreInstance arg = formatArgs.get(argCounter++);
                            if (!Instance.instanceOf(arg, M3Paths.Integer, processorSupport))
                            {
                                throw new IllegalArgumentException("Expected Integer, got: " + arg);
                            }
                            FormatTools.appendIntegerString(builder, arg.getName(), zeroPad);
                            index = j + 1;
                            break;
                        }
                        case 'f':
                        {
                            CoreInstance arg = formatArgs.get(argCounter++);
                            if (!Instance.instanceOf(arg, M3Paths.Float, processorSupport))
                            {
                                throw new IllegalArgumentException("Expected Float, got: " + arg);
                            }
                            FormatTools.appendFloatString(builder, arg.getName());
                            break;
                        }
                        case '.':
                        {
                            int j = index;
                            while (Character.isDigit(formatString.charAt(j)))
                            {
                                j++;
                            }
                            if (formatString.charAt(j) != 'f')
                            {
                                throw new IllegalArgumentException("Invalid format specifier: %" + formatString.substring(index, j + 1));
                            }
                            int precision = Integer.valueOf(formatString.substring(index, j));
                            CoreInstance arg = formatArgs.get(argCounter++);
                            if (!Instance.instanceOf(arg, M3Paths.Float, processorSupport))
                            {
                                throw new IllegalArgumentException("Expected Float, got: " + arg);
                            }
                            FormatTools.appendFloatString(builder, arg.getName(), precision);
                            index = j + 1;
                            break;
                        }
                        default:
                        {
                            throw new IllegalArgumentException("Invalid format specifier: %" + formatString.charAt(index - 1));
                        }
                    }
                }
                else
                {
                    builder.append(character);
                }
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Too few arguments passed to format function. Format expression \"" + formatString + "\", number of arguments [" + formatArgs.size() + "]");
        }
        if (argCounter < formatArgs.size())
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Unused format args. [" + formatArgs.size() + "] arguments provided to expression \"" + formatString + "\"");
        }
        return builder.toString();
    }
}
