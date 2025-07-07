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

package org.finos.legend.pure.runtime.java.interpreted.natives;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utilities for handling Pure numbers.
 */
public class NumericUtilities
{
    @Deprecated
    public static Predicate<CoreInstance> IS_DECIMAL_CORE_INSTANCE(ProcessorSupport processorSupport)
    {
        return instance -> PrimitiveUtilities.isDecimal(instance, processorSupport);
    }

    @Deprecated
    public static Predicate<CoreInstance> IS_FLOAT_CORE_INSTANCE(ProcessorSupport processorSupport)
    {
        return instance -> PrimitiveUtilities.isFloat(instance, processorSupport);
    }

    private NumericUtilities()
    {
        // Utility class
    }

    /**
     * Convert a Java number into a Pure number and wrap it in a value specification.
     *
     * @param javaNumber              Java number
     * @param bigDecimalToPureDecimal whether Java {@link BigDecimal} should be converted to Pure Decimal (or Float)
     * @param repository              model repository
     * @return Pure number
     */
    public static CoreInstance toPureNumberValueExpression(Number javaNumber, boolean bigDecimalToPureDecimal, ModelRepository repository, ProcessorSupport processorSupport)
    {
        return ValueSpecificationBootstrap.wrapValueSpecification(toPureNumber(javaNumber, bigDecimalToPureDecimal, repository), true, processorSupport);
    }

    /**
     * Convert a Java number into a Pure number. Note that this does not wrap the Pure number in a value specification.
     *
     * @param javaNumber              Java number
     * @param bigDecimalToPureDecimal whether Java {@link BigDecimal} should be converted to Pure Decimal (or Float)
     * @param repository              model repository
     * @return Pure number
     */
    public static CoreInstance toPureNumber(Number javaNumber, boolean bigDecimalToPureDecimal, ModelRepository repository)
    {
        if (javaNumber == null)
        {
            throw new IllegalArgumentException("Cannot create Pure number from null");
        }
        if (javaNumber instanceof BigDecimal)
        {
            return bigDecimalToPureDecimal ? newPureDecimal((BigDecimal) javaNumber, repository) : newPureFloat((BigDecimal) javaNumber, repository);
        }
        if (javaNumber instanceof Double)
        {
            return newPureFloat(javaNumber.doubleValue(), repository);
        }
        if (javaNumber instanceof Float)
        {
            return newPureFloat(javaNumber.floatValue(), repository);
        }
        if (javaNumber instanceof BigInteger)
        {
            return newPureInteger((BigInteger) javaNumber, repository);
        }
        if (javaNumber instanceof Long)
        {
            return newPureInteger(javaNumber.longValue(), repository);
        }
        if (javaNumber instanceof Integer)
        {
            return newPureInteger(javaNumber.intValue(), repository);
        }
        throw new IllegalArgumentException("Not a number: " + javaNumber);
    }

    /**
     * Create a new Pure Float. Note that the number cannot be null.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Float
     */
    public static CoreInstance newPureFloat(BigDecimal number, ModelRepository repository)
    {
        if (number == null)
        {
            throw new IllegalArgumentException("Cannot create Float from null");
        }
        return repository.newFloatCoreInstance(number);
    }

    /**
     * Create a new Pure Float.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Float
     */
    public static CoreInstance newPureFloat(double number, ModelRepository repository)
    {
        return newPureFloat(BigDecimal.valueOf(number), repository);
    }

    /**
     * Create a new Pure Float.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Float
     */
    public static CoreInstance newPureFloat(float number, ModelRepository repository)
    {
        return newPureFloat(BigDecimal.valueOf(number), repository);
    }

    /**
     * Create a new Pure Decimal. Note that the number cannot be null.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Decimal
     */
    public static CoreInstance newPureDecimal(BigDecimal number, ModelRepository repository)
    {
        if (number == null)
        {
            throw new IllegalArgumentException("Cannot create Decimal from null");
        }
        return repository.newDecimalCoreInstance(number);
    }

    /**
     * Create a new Pure Integer. Note that the number cannot be null.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Integer
     */
    public static CoreInstance newPureInteger(BigInteger number, ModelRepository repository)
    {
        if (number == null)
        {
            throw new IllegalArgumentException("Cannot create Integer from null");
        }
        return repository.newIntegerCoreInstance(number);
    }

    /**
     * Create a new Pure Integer.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Integer
     */
    public static CoreInstance newPureInteger(long number, ModelRepository repository)
    {
        return repository.newIntegerCoreInstance(number);
    }

    /**
     * Create a new Pure Integer.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Integer
     */
    public static CoreInstance newPureInteger(int number, ModelRepository repository)
    {
        return repository.newIntegerCoreInstance(number);
    }
}
