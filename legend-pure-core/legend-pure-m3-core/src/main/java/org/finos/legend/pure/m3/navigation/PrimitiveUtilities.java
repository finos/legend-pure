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
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
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
}
