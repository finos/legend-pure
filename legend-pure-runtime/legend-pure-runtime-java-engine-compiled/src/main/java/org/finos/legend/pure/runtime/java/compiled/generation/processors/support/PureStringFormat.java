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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.tools.FormatTools;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

public class PureStringFormat
{
    public static String format(String formatString, Iterable<?> formatArgs, BiFunction<Object, ? super ExecutionSupport, ? extends String> toRepresentationFunction, ExecutionSupport executionSupport)
    {
        Iterator<?> argIterator = formatArgs.iterator();
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
                            Object arg = argIterator.next();
                            builder.append(CompiledSupport.pureToString(arg, executionSupport));
                            break;
                        }
                        case 'r':
                        {
                            Object arg = argIterator.next();
                            builder.append(toRepresentationFunction.apply(arg, executionSupport));
                            break;
                        }
                        case 'd':
                        {
                            Object arg = argIterator.next();
                            if (arg instanceof Long)
                            {
                                builder.append(((Long) arg).longValue());
                            }
                            else if (arg instanceof Integer)
                            {
                                builder.append(((Integer) arg).intValue());
                            }
                            else if (arg instanceof BigInteger)
                            {
                                builder.append(arg);
                            }
                            else
                            {
                                throw new IllegalArgumentException("Expected Integer, got: " + arg);
                            }
                            break;
                        }
                        case 't':
                        {
                            Object arg = argIterator.next();
                            if (!(arg instanceof PureDate))
                            {
                                throw new IllegalArgumentException("Expected Date, got: " + arg);
                            }
                            int dateFormatEnd = FormatTools.findEndOfDateFormatString(formatString, index);
                            if (dateFormatEnd == -1)
                            {
                                builder.append(arg);
                            }
                            else
                            {
                                ((PureDate) arg).format(builder, formatString.substring(index + 1, dateFormatEnd));
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
                            int zeroPad = Integer.parseInt(formatString.substring(index, j));
                            Object arg = argIterator.next();
                            if (!(arg instanceof Long) && !(arg instanceof Integer) && !(arg instanceof BigInteger))
                            {
                                throw new IllegalArgumentException("Expected Integer, got: " + arg);
                            }
                            FormatTools.appendIntegerString(builder, arg.toString(), zeroPad);
                            index = j + 1;
                            break;
                        }
                        case 'f':
                        {
                            Object arg = argIterator.next();
                            if (!(arg instanceof Double) && !(arg instanceof Float) && !(arg instanceof BigDecimal))
                            {
                                throw new IllegalArgumentException("Expected Float, got: " + arg);
                            }
                            FormatTools.appendFloatString(builder, CompiledSupport.pureToString(arg, executionSupport));
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
                            int precision = Integer.parseInt(formatString.substring(index, j));
                            Object arg = argIterator.next();
                            if (!(arg instanceof Double) && !(arg instanceof Float) && !(arg instanceof BigDecimal))
                            {
                                throw new IllegalArgumentException("Expected Float, got: " + arg);
                            }
                            FormatTools.appendFloatString(builder, CompiledSupport.pureToString(arg, executionSupport), precision);
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
        catch (NoSuchElementException e)
        {
            throw new PureExecutionException("Too few arguments passed to format function. Format expression \"" + formatString + "\", number of arguments [" + Iterate.sizeOf(formatArgs) + "]");
        }
        if (argIterator.hasNext())
        {
            throw new PureExecutionException("Unused format args. [" + Iterate.sizeOf(formatArgs) + "] arguments provided to expression \"" + formatString + "\"");
        }
        return builder.toString();
    }
}
