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

import java.math.BigInteger;
import java.util.Stack;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

abstract class AbstractRoundFunction extends NativeFunction
{
    protected static final BigInteger ONE = new BigInteger("1");

    private final ModelRepository repository;

    protected AbstractRoundFunction(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public final CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance integerType = processorSupport.package_getByUserPath(M3Paths.Integer);
        CoreInstance instance = params.get(0);
        CoreInstance number = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.values, processorSupport);
        if (processorSupport.getClassifier(number) == integerType)
        {
            return instance;
        }
        else
        {
            String name = number.getName();
            int decimalIndex = name.indexOf('.');
            if (decimalIndex == -1)
            {
                // If there's no decimal point, then no rounding is needed
                return ValueSpecificationBootstrap.wrapValueSpecification(this.repository.newCoreInstance(name, integerType, null), true, processorSupport);
            }
            else
            {
                // Split the numeric string into integer and decimal parts
                String integerPart = name.substring(0, decimalIndex);
                String decimalPart = name.substring(decimalIndex + 1, name.length());

                // If the decimal part is not zero, then round
                if (!isZero(decimalPart))
                {
                    integerPart = (integerPart.charAt(0) == '-') ? roundNegative(integerPart, decimalPart) : roundPositive(integerPart, decimalPart);
                }

                return ValueSpecificationBootstrap.wrapValueSpecification(this.repository.newCoreInstance(integerPart, integerType, null), true, processorSupport);
            }
        }
    }

    /**
     * Round the positive number with the given integer and decimal strings.
     * The integer string is guaranteed to not start with '-'.  The decimal
     * string is guaranteed to contain at least one non-zero character.
     *
     * @param integerString integer part string
     * @param decimalString decimal part string
     * @return rounded number string
     */
    abstract protected String roundPositive(String integerString, String decimalString);

    /**
     * Round the negative number with the given integer and decimal strings.
     * The integer string is guaranteed to start with '-'.  The decimal
     * string is guaranteed to contain at least one non-zero character.
     *
     * @param integerString integer part string
     * @param decimalString decimal part string
     * @return rounded number string
     */
    abstract protected String roundNegative(String integerString, String decimalString);

    /**
     * Return an integer string that is equal to the given integer string
     * plus one.
     *
     * @param integerString integer string
     * @return integer plus one string
     */
    protected final String addOneToIntegerString(String integerString)
    {
        try
        {
            long number = Long.valueOf(integerString);
            return (number == Long.MAX_VALUE) ? new BigInteger(integerString).add(ONE).toString() : String.valueOf(number + 1);
        }
        catch (NumberFormatException e)
        {
            return new BigInteger(integerString).add(ONE).toString();
        }
    }

    /**
     * Return an integer string that is equal to the given integer string
     * minus one.
     *
     * @param integerString integer string
     * @return integer minus one string
     */
    protected final String subtractOneFromIntegerString(String integerString)
    {
        try
        {
            long number = Long.valueOf(integerString);
            return (number == Long.MIN_VALUE) ? new BigInteger(integerString).subtract(ONE).toString() : String.valueOf(number - 1);
        }
        catch (NumberFormatException e)
        {
            return new BigInteger(integerString).subtract(ONE).toString();
        }
    }

    /**
     * Return whether the given digit string consists only
     * of zeros.
     *
     * @param digitString digit string
     * @return whether digit string consists only of 0's
     */
    private boolean isZero(String digitString)
    {
        for (int i = 0, length = digitString.length(); i < length; i++)
        {
            if (digitString.charAt(i) != '0')
            {
                return false;
            }
        }
        return true;
    }
}
