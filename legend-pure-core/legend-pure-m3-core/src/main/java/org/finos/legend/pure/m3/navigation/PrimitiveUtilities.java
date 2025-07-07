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

package org.finos.legend.pure.m3.navigation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class PrimitiveUtilities
{
    private static final ImmutableSet<String> PRIMITIVE_TYPE_NAMES = ModelRepository.PRIMITIVE_TYPE_NAMES.newWith(M3Paths.Number);

    private PrimitiveUtilities()
    {
        // Utility class
    }

    public static boolean getBooleanValue(CoreInstance instance)
    {
        return (instance instanceof BooleanCoreInstance) ? ((BooleanCoreInstance) instance).getValue() : ModelRepository.BOOLEAN_TRUE.equals(instance.getName());
    }

    public static boolean getBooleanValue(CoreInstance instance, boolean defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getBooleanValue(instance);
    }

    public static byte getByteValue(CoreInstance instance)
    {
        return (instance instanceof ByteCoreInstance) ? ((ByteCoreInstance) instance).getValue() : Byte.parseByte(instance.getName());
    }

    public static byte getByteValue(CoreInstance instance, byte defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getByteValue(instance);
    }

    public static PureDate getDateValue(CoreInstance instance)
    {
        return (instance instanceof DateCoreInstance) ? ((DateCoreInstance) instance).getValue() : DateFunctions.parsePureDate(instance.getName());
    }

    public static PureDate getDateValue(CoreInstance instance, PureDate defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getDateValue(instance);
    }

    public static BigDecimal getFloatValue(CoreInstance instance)
    {
        return (instance instanceof FloatCoreInstance) ? ((FloatCoreInstance) instance).getValue() : new BigDecimal(instance.getName());
    }

    public static BigDecimal getFloatValue(CoreInstance instance, BigDecimal defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getFloatValue(instance);
    }

    public static BigDecimal getDecimalValue(CoreInstance instance)
    {
        return (instance instanceof DecimalCoreInstance) ? ((DecimalCoreInstance) instance).getValue() : new BigDecimal(instance.getName());
    }

    public static BigDecimal getDecimalValue(CoreInstance instance, BigDecimal defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getDecimalValue(instance);
    }

    public static Number getIntegerValue(CoreInstance instance)
    {
        if (instance instanceof IntegerCoreInstance)
        {
            return ((IntegerCoreInstance) instance).getValue();
        }

        String name = instance.getName();
        if (name.length() <= 20)
        {
            try
            {
                long l = Long.parseLong(name);
                if ((Integer.MIN_VALUE <= l) && (l <= Integer.MAX_VALUE))
                {
                    return (int) l;
                }
                return l;
            }
            catch (NumberFormatException ignore)
            {
                // not an Integer or Long, fall back to BigInteger
            }
        }
        return new BigInteger(name);
    }

    public static Number getIntegerValue(CoreInstance instance, Integer defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getIntegerValue(instance);
    }

    public static Number getIntegerValue(CoreInstance instance, Long defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getIntegerValue(instance);
    }

    public static Number getIntegerValue(CoreInstance instance, BigInteger defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getIntegerValue(instance);
    }

    public static PureStrictTime getStrictTimeValue(CoreInstance instance)
    {
        return (instance instanceof StrictTimeCoreInstance) ? ((StrictTimeCoreInstance) instance).getValue() : StrictTimeFunctions.parsePureStrictTime(instance.getName());
    }

    public static PureStrictTime getStrictTimeValue(CoreInstance instance, PureStrictTime defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getStrictTimeValue(instance);
    }

    public static String getStringValue(CoreInstance instance)
    {
        return instance.getName();
    }

    public static String getStringValue(CoreInstance instance, String defaultIfNull)
    {
        return (instance == null) ? defaultIfNull : getStringValue(instance);
    }

    public static boolean isPrimitiveTypeName(String name)
    {
        return PRIMITIVE_TYPE_NAMES.contains(name);
    }

    public static ImmutableSet<String> getPrimitiveTypeNames()
    {
        return PRIMITIVE_TYPE_NAMES;
    }

    public static RichIterable<CoreInstance> getPrimitiveTypes(ModelRepository repository)
    {
        return getPrimitiveTypes(repository, true);
    }

    public static RichIterable<CoreInstance> getPrimitiveTypes(ModelRepository repository, boolean errorIfNotFound)
    {
        return getPrimitiveTypes(repository, Lists.mutable.ofInitialCapacity(PRIMITIVE_TYPE_NAMES.size()), errorIfNotFound);
    }

    public static <T extends Collection<? super CoreInstance>> T getPrimitiveTypes(ModelRepository repository, T targetCollection)
    {
        return getPrimitiveTypes(repository, targetCollection, true);
    }

    public static <T extends Collection<? super CoreInstance>> T getPrimitiveTypes(ModelRepository repository, T targetCollection, boolean errorIfNotFound)
    {
        forEachPrimitiveType(repository, targetCollection::add, errorIfNotFound);
        return targetCollection;
    }

    public static void forEachPrimitiveType(ModelRepository repository, Consumer<? super CoreInstance> consumer)
    {
        forEachPrimitiveType(repository, consumer, true);
    }

    public static void forEachPrimitiveType(ModelRepository repository, Consumer<? super CoreInstance> consumer, boolean errorIfNotFound)
    {
        forEachPrimitiveType(repository::getTopLevel, consumer, errorIfNotFound);
    }

    public static RichIterable<CoreInstance> getPrimitiveTypes(ProcessorSupport processorSupport)
    {
        return getPrimitiveTypes(processorSupport, true);
    }

    public static RichIterable<CoreInstance> getPrimitiveTypes(ProcessorSupport processorSupport, boolean errorIfNotFound)
    {
        return getPrimitiveTypes(processorSupport, Lists.mutable.ofInitialCapacity(PRIMITIVE_TYPE_NAMES.size()), errorIfNotFound);
    }

    public static <T extends Collection<? super CoreInstance>> T getPrimitiveTypes(ProcessorSupport processorSupport, T targetCollection)
    {
        return getPrimitiveTypes(processorSupport, targetCollection, true);
    }

    public static <T extends Collection<? super CoreInstance>> T getPrimitiveTypes(ProcessorSupport processorSupport, T targetCollection, boolean errorIfNotFound)
    {
        forEachPrimitiveType(processorSupport, targetCollection::add, errorIfNotFound);
        return targetCollection;
    }

    public static void forEachPrimitiveType(ProcessorSupport processorSupport, Consumer<? super CoreInstance> consumer)
    {
        forEachPrimitiveType(processorSupport, consumer, true);
    }

    public static void forEachPrimitiveType(ProcessorSupport processorSupport, Consumer<? super CoreInstance> consumer, boolean errorIfNotFound)
    {
        forEachPrimitiveType(processorSupport::repository_getTopLevel, consumer, errorIfNotFound);
    }

    private static void forEachPrimitiveType(Function<String, CoreInstance> getTopLevel, Consumer<? super CoreInstance> consumer, boolean errorIfNotFound)
    {
        PRIMITIVE_TYPE_NAMES.forEach(name ->
        {
            CoreInstance primitiveType = getTopLevel.apply(name);
            if (primitiveType != null)
            {
                consumer.accept(primitiveType);
            }
            else if (errorIfNotFound)
            {
                throw new RuntimeException("Cannot find primitive type: " + name);
            }
        });
    }

    /**
     * Convert a Pure number into a Java number.
     *
     * @param pureNumber       Pure number
     * @param processorSupport processor support
     * @return Java number
     */
    public static Number toJavaNumber(CoreInstance pureNumber, ProcessorSupport processorSupport)
    {
        CoreInstance classifier = processorSupport.getClassifier(pureNumber);
        if (classifier.getValueForMetaPropertyToOne(M3Properties._package) == null)
        {
            String typeName = classifier.getName();
            if (typeName != null)
            {
                switch (typeName)
                {
                    case M3Paths.Integer:
                    {
                        return getIntegerValue(pureNumber);
                    }
                    case M3Paths.Float:
                    {
                        return getFloatValue(pureNumber);
                    }
                    case M3Paths.Decimal:
                    {
                        return getDecimalValue(pureNumber);
                    }
                }
            }
        }
        else
        {
            if (isInteger(pureNumber, processorSupport))
            {
                return getIntegerValue(pureNumber);
            }
            else if (isFloat(pureNumber, processorSupport))
            {
                return getFloatValue(pureNumber);
            }
            else if (isDecimal(pureNumber, processorSupport))
            {
                return getDecimalValue(pureNumber);
            }
        }
        throw new IllegalArgumentException("Not a number: " + pureNumber);
    }

    public static boolean isInteger(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isNumberOfType(instance, IntegerCoreInstance.class, M3Paths.Integer, processorSupport);
    }

    public static boolean isFloat(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isNumberOfType(instance, FloatCoreInstance.class, M3Paths.Float, processorSupport);
    }

    public static boolean isDecimal(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isNumberOfType(instance, DecimalCoreInstance.class, M3Paths.Decimal, processorSupport);
    }

    private static boolean isNumberOfType(CoreInstance instance, Class<? extends PrimitiveCoreInstance<? extends Number>> coreInstanceClass, String numberTypeName, ProcessorSupport processorSupport)
    {
        if (coreInstanceClass.isInstance(instance))
        {
            return true;
        }
        CoreInstance classifier = processorSupport.getClassifier(instance);
        return (classifier.getValueForMetaPropertyToOne(M3Properties._package) == null) && numberTypeName.equals(classifier.getName());
    }

    /**
     * Convert a collection of Pure numbers into a collection of Java numbers.
     *
     * @param pureNumbers Pure numbers
     * @return Java numbers
     */
    public static ListIterable<Number> toJavaNumber(ListIterable<? extends CoreInstance> pureNumbers, ProcessorSupport processorSupport)
    {
        return pureNumbers.collect(n -> toJavaNumber(n, processorSupport), Lists.mutable.ofInitialCapacity(pureNumbers.size()));
    }

    public static BigDecimal toBigDecimal(Number number)
    {
        if (number instanceof BigDecimal)
        {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger)
        {
            return new BigDecimal((BigInteger) number);
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long)
        {
            return new BigDecimal(number.longValue());
        }
        if (number instanceof Float || number instanceof Double)
        {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try
        {
            return new BigDecimal(number.toString());
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName() + ") does not have a parsable string representation", e);
        }
    }

    public static Number plus(Number left, Number right)
    {
        if (((left instanceof Long) || (left instanceof Integer)) && ((right instanceof Long) || (right instanceof Integer)))
        {
            return left.longValue() + right.longValue();
        }
        if ((left instanceof BigDecimal) || (right instanceof BigDecimal))
        {
            return toBigDecimal(left).add(toBigDecimal(right));
        }
        return left.doubleValue() + right.doubleValue();
    }

    /**
     * Compare two numbers. Returns a negative value if left is less than right, positive if right is less than left,
     * or 0 if they are equal.
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
            if ((right instanceof Float) || (right instanceof Double))
            {
                return BigDecimal.valueOf(left.longValue()).compareTo(BigDecimal.valueOf(right.doubleValue()));
            }
            if (right instanceof BigInteger)
            {
                return BigInteger.valueOf(left.longValue()).compareTo((BigInteger) right);
            }
            if (right instanceof BigDecimal)
            {
                return BigDecimal.valueOf(left.longValue()).compareTo((BigDecimal) right);
            }
            throw new RuntimeException("Number of an unhandled type: " + right);
        }
        if ((left instanceof Float) || (left instanceof Double))
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return BigDecimal.valueOf(left.doubleValue()).compareTo(BigDecimal.valueOf(right.longValue()));
            }
            if ((right instanceof Float) || (right instanceof Double))
            {
                return Double.compare(left.doubleValue(), right.doubleValue());
            }
            if (right instanceof BigInteger)
            {
                return BigDecimal.valueOf(left.doubleValue()).compareTo(new BigDecimal((BigInteger) right)); //NOSONAR
            }
            if (right instanceof BigDecimal)
            {
                return BigDecimal.valueOf(left.doubleValue()).compareTo((BigDecimal) right);
            }
            throw new RuntimeException("Number of an unhandled type: " + right);
        }
        if (left instanceof BigInteger)
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return ((BigInteger) left).compareTo(BigInteger.valueOf(right.longValue()));
            }
            if ((right instanceof Float) || (right instanceof Double))
            {
                return new BigDecimal((BigInteger) left).compareTo(BigDecimal.valueOf(right.doubleValue()));
            }
            if (right instanceof BigInteger)
            {
                return ((BigInteger) left).compareTo((BigInteger) right);
            }
            if (right instanceof BigDecimal)
            {
                return new BigDecimal((BigInteger) left).compareTo((BigDecimal) right);
            }
            throw new RuntimeException("Number of an unhandled type: " + right);
        }
        if (left instanceof BigDecimal)
        {
            if ((right instanceof Integer) || (right instanceof Long))
            {
                return ((BigDecimal) left).compareTo(BigDecimal.valueOf(right.longValue()));
            }
            if ((right instanceof Float) || (right instanceof Double))
            {
                return ((BigDecimal) left).compareTo(BigDecimal.valueOf(right.doubleValue()));
            }
            if (right instanceof BigInteger)
            {
                return ((BigDecimal) left).compareTo(new BigDecimal((BigInteger) right));
            }
            if (right instanceof BigDecimal)
            {
                return ((BigDecimal) left).compareTo((BigDecimal) right);
            }
            throw new RuntimeException("Number of an unhandled type: " + right);
        }
        throw new RuntimeException("Number of an unhandled type: " + left);
    }
}
