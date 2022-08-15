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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.tools;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utilities for handling Pure numbers.
 */
public class NumericUtilities
{
    public static Predicate<CoreInstance> IS_DECIMAL_CORE_INSTANCE(ProcessorSupport processorSupport)
    {
        return new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance each)
            {
                return M3Paths.Decimal.equals(PackageableElement.getUserPathForPackageableElement(processorSupport.getClassifier(each)));
            }
        };
    }

    public static Predicate<CoreInstance> IS_FLOAT_CORE_INSTANCE(ProcessorSupport processorSupport)
    {
        return new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance each)
            {
                return M3Paths.Float.equals(PackageableElement.getUserPathForPackageableElement(processorSupport.getClassifier(each)));
            }
        };
    }

    private NumericUtilities()
    {
        // Utility class
    }

    /**
     * Convert a Java number into a Pure number and wrap it in a
     * value specification.
     *
     * @param javaNumber              Java number
     * @param bigDecimalToPureDecimal
     * @param repository              model repository
     * @return Pure number
     */
    public static CoreInstance toPureNumberValueExpression(Number javaNumber,
                                                           boolean bigDecimalToPureDecimal, ModelRepository repository, ProcessorSupport processorSupport)
    {
        return ValueSpecificationBootstrap.wrapValueSpecification(toPureNumber(javaNumber, bigDecimalToPureDecimal, repository), true, processorSupport);
    }

    /**
     * Convert a Java number into a Pure number.  Note that this
     * does not wrap the Pure number in a value specification.
     *
     * @param javaNumber Java number
     * @param repository model repository
     * @return Pure number
     */
    public static CoreInstance toPureNumber(Number javaNumber, boolean bigDecimalToPureDecimal, ModelRepository
            repository)
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
     * Create a new Pure Float.  Note that the number cannot be null, but
     * the source information can be.
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
     * Create a new Pure Float.  Source information can be null.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Float
     */
    public static CoreInstance newPureFloat(double number, ModelRepository repository)
    {
        return newPureFloat(new BigDecimal(Double.toString(number)), repository);
    }

    /**
     * Create a new Pure Float.  Source information can be null.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Float
     */
    public static CoreInstance newPureFloat(float number, ModelRepository repository)
    {
        return newPureFloat(new BigDecimal(Float.toString(number)), repository);
    }

    /**
     * Create a new Pure Decimal.  Note that the number cannot be null, but
     * the source information can be.
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
     * Create a new Pure Integer.  Note that the number cannot be null, but
     * the source information can be.
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
     * Create a new Pure Integer.  Source information can be null.
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
     * Create a new Pure Integer.  Source information can be null.
     *
     * @param number     Java number
     * @param repository model repository
     * @return new Pure Integer
     */
    public static CoreInstance newPureInteger(int number, ModelRepository repository)
    {
        return repository.newIntegerCoreInstance(number);
    }

    /**
     * Convert a Pure number into a Java number.
     *
     * @param pureNumber       Pure number
     * @param processorSupport
     * @return Java number
     */
    public static Number toJavaNumber(CoreInstance pureNumber, ProcessorSupport processorSupport)
    {
        String typeName = processorSupport.getClassifier(pureNumber).getName();
        if (M3Paths.Integer.equals(typeName))
        {
            return PrimitiveUtilities.getIntegerValue(pureNumber);
        }
        if (M3Paths.Float.equals(typeName))
        {
            return PrimitiveUtilities.getFloatValue(pureNumber);
        }
        if (M3Paths.Decimal.equals(typeName))
        {
            return PrimitiveUtilities.getDecimalValue(pureNumber);
        }
        throw new IllegalArgumentException("Not a number: " + pureNumber);
    }

    /**
     * Convert a collection of Pure numbers into a collection of Java numbers.
     *
     * @param pureNumbers Pure numbers
     * @return Java numbers
     */
    public static ListIterable<Number> toJavaNumber(ListIterable<? extends CoreInstance> pureNumbers, ProcessorSupport processorSupport)
    {
        MutableList<Number> javaNumbers = Lists.mutable.empty();
        for (CoreInstance pureNumber : pureNumbers)
        {
            javaNumbers.add(toJavaNumber(pureNumber, processorSupport));
        }
        return javaNumbers;
    }

    /**
     * Compare two numbers.  Returns a negative value if
     * left is less than right, positive if right is less
     * than left, or 0 if they are equal.
     *
     * @param left  left number
     * @param right right number
     * @return comparison
     */
    public static int compare(Number left, Number right)
    {
        if ((left instanceof Integer) || (left instanceof Long))
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return Long.compare(left.longValue(), right.longValue());
            }
            else if ((right instanceof Float) || (right instanceof Double))
            {
                return new BigDecimal(left.longValue()).compareTo(BigDecimal.valueOf(right.doubleValue()));
            }
            else if (right instanceof BigInteger)
            {
                return new BigInteger(left.toString()).compareTo((BigInteger) right);
            }
            else if (right instanceof BigDecimal)
            {
                return new BigDecimal(left.longValue()).compareTo((BigDecimal) right);
            }
            else
            {
                throw new RuntimeException("Number of an unhandled type: " + right);
            }
        }
        else if ((left instanceof Float) || (left instanceof Double))
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return BigDecimal.valueOf(left.doubleValue()).compareTo(BigDecimal.valueOf(right.longValue()));
            }
            else if ((right instanceof Float) || (right instanceof Double))
            {
                return Double.compare(left.doubleValue(), right.doubleValue());
            }
            else if (right instanceof BigInteger)
            {
                return new BigDecimal(left.doubleValue()).compareTo(new BigDecimal((BigInteger) right)); //NOSONAR
            }
            else if (right instanceof BigDecimal)
            {
                return BigDecimal.valueOf(left.doubleValue()).compareTo((BigDecimal) right);
            }
            else
            {
                throw new RuntimeException("Number of an unhandled type: " + right);
            }
        }
        else if (left instanceof BigInteger)
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return ((BigInteger) left).compareTo(new BigInteger(right.toString()));
            }
            else if ((right instanceof Float) || (right instanceof Double))
            {
                return new BigDecimal((BigInteger) left).compareTo(BigDecimal.valueOf(right.doubleValue()));
            }
            else if (right instanceof BigInteger)
            {
                return ((BigInteger) left).compareTo((BigInteger) right);
            }
            else if (right instanceof BigDecimal)
            {
                return new BigDecimal((BigInteger) left).compareTo((BigDecimal) right);
            }
            else
            {
                throw new RuntimeException("Number of an unhandled type: " + right);
            }
        }
        else if (left instanceof BigDecimal)
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return ((BigDecimal) left).compareTo(new BigDecimal(right.longValue()));
            }
            else if ((right instanceof Float) || (right instanceof Double))
            {
                return ((BigDecimal) left).compareTo(BigDecimal.valueOf(right.doubleValue()));
            }
            else if (right instanceof BigInteger)
            {
                return ((BigDecimal) left).compareTo(new BigDecimal((BigInteger) right));
            }
            else if (right instanceof BigDecimal)
            {
                return ((BigDecimal) left).compareTo((BigDecimal) right);
            }
            else
            {
                throw new RuntimeException("Number of an unhandled type: " + right);
            }
        }
        else
        {
            throw new RuntimeException("Number of an unhandled type: " + left);
        }
    }
}
