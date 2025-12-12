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

package org.finos.legend.pure.m3.coreinstance.helper;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PrimitiveHelper
{
    public static final Function2<Boolean, ModelRepository, BooleanCoreInstance> BOOLEAN_TO_COREINSTANCE_FN = PrimitiveHelper::booleanToCoreInstance;
    public static final Function2<Byte, ModelRepository, ByteCoreInstance> BYTE_TO_COREINSTANCE_FN = PrimitiveHelper::byteToCoreInstance;
    public static final Function2<PureDate, ModelRepository, DateCoreInstance> DATE_TO_COREINSTANCE_FN = PrimitiveHelper::dateToCoreInstance;
    public static final Function2<DateTime, ModelRepository, DateCoreInstance> DATETIME_TO_COREINSTANCE_FN = PrimitiveHelper::dateTimeToCoreInstance;
    public static final Function2<BigDecimal, ModelRepository, DecimalCoreInstance> DECIMAL_TO_COREINSTANCE_FN = PrimitiveHelper::decimalToCoreInstance;
    public static final Function2<BigDecimal, ModelRepository, FloatCoreInstance> FLOAT_TO_COREINSTANCE_FN = PrimitiveHelper::floatToCoreInstance;
    public static final Function2<Number, ModelRepository, IntegerCoreInstance> INTEGER_TO_COREINSTANCE_FN = PrimitiveHelper::integerToCoreInstance;
    public static final Function2<LatestDate, ModelRepository, DateCoreInstance> LATESTDATE_TO_COREINSTANCE_FN = PrimitiveHelper::latestDateToCoreInstance;
    public static final Function2<StrictDate, ModelRepository, DateCoreInstance> STRICTDATE_TO_COREINSTANCE_FN = PrimitiveHelper::strictDateToCoreInstance;
    public static final Function2<PureStrictTime, ModelRepository, StrictTimeCoreInstance> STRICTTIME_TO_COREINSTANCE_FN = PrimitiveHelper::strictTimeToCoreInstance;
    public static final Function2<String, ModelRepository, StringCoreInstance> STRING_TO_COREINSTANCE_FN = PrimitiveHelper::stringToCoreInstance;

    // Boolean

    public static BooleanCoreInstance booleanToCoreInstance(boolean booleanValue, ModelRepository repository)
    {
        return repository.newBooleanCoreInstance(booleanValue);
    }

    public static RichIterable<BooleanCoreInstance> booleansToCoreInstances(RichIterable<? extends Boolean> booleans, ModelRepository repository)
    {
        return booleans.collect(b -> booleanToCoreInstance(b, repository));
    }


    public static Boolean optionalInstanceToBoolean(BooleanCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue();
    }

    public static boolean requiredInstanceToBoolean(BooleanCoreInstance instance)
    {
        return instance.getValue();
    }

    public static RichIterable<Boolean> instancesToBoolean(RichIterable<? extends BooleanCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToBoolean);
    }

    // Byte

    public static ByteCoreInstance byteToCoreInstance(byte b, ModelRepository repository)
    {
        return repository.newByteCoreInstance(b);
    }

    public static RichIterable<ByteCoreInstance> bytesToCoreInstances(RichIterable<? extends Byte> bytes, ModelRepository repository)
    {
        return bytes.collect(b -> byteToCoreInstance(b, repository));
    }


    public static Byte optionalInstanceToByte(ByteCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue();
    }

    public static byte requiredInstanceToByte(ByteCoreInstance instance)
    {
        return instance.getValue();
    }

    public static RichIterable<Byte> instancesToByte(RichIterable<? extends ByteCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToByte);
    }

    // Date

    public static DateCoreInstance dateToCoreInstance(PureDate pureDate, ModelRepository repository)
    {
        return repository.newDateCoreInstance(pureDate);
    }

    public static RichIterable<DateCoreInstance> datesToCoreInstances(RichIterable<? extends PureDate> dates, ModelRepository repository)
    {
        return dates.collect(date -> dateToCoreInstance(date, repository));
    }


    public static PureDate optionalInstanceToDate(DateCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue();
    }

    public static PureDate requiredInstanceToDate(DateCoreInstance instance)
    {
        return instance.getValue();
    }

    public static RichIterable<PureDate> instancesToDate(RichIterable<? extends DateCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToDate);
    }

    // DateTime

    public static DateCoreInstance dateTimeToCoreInstance(DateTime pureDate, ModelRepository repository)
    {
        return repository.newDateTimeCoreInstance(pureDate);
    }

    public static RichIterable<DateCoreInstance> dateTimesToCoreInstances(RichIterable<? extends DateTime> dateTimes, ModelRepository repository)
    {
        return dateTimes.collect(dateTime -> dateToCoreInstance(dateTime, repository));
    }


    public static DateTime optionalInstanceToDateTime(DateCoreInstance instance)
    {
        return (instance == null) ? null : (DateTime) instance.getValue();
    }

    public static DateTime requiredInstanceToDateTime(DateCoreInstance instance)
    {
        return (DateTime) instance.getValue();
    }

    public static RichIterable<DateTime> instancesToDateTime(RichIterable<? extends DateCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToDateTime);
    }

    // Decimal

    public static DecimalCoreInstance decimalToCoreInstance(BigDecimal decimal, ModelRepository repository)
    {
        return repository.newDecimalCoreInstance(decimal);
    }

    public static RichIterable<DecimalCoreInstance> decimalsToCoreInstances(RichIterable<? extends BigDecimal> decimals, ModelRepository repository)
    {
        return decimals.collect(decimal -> decimalToCoreInstance(decimal, repository));
    }


    public static BigDecimal optionalInstanceToDecimal(DecimalCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue();
    }

    public static BigDecimal requiredInstanceToDecimal(DecimalCoreInstance instance)
    {
        return instance.getValue();
    }

    public static RichIterable<BigDecimal> instancesToDecimal(RichIterable<? extends DecimalCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToDecimal);
    }

    // Float

    public static FloatCoreInstance floatToCoreInstance(double d, ModelRepository repository)
    {
        return floatToCoreInstance(BigDecimal.valueOf(d), repository);
    }

    public static RichIterable<FloatCoreInstance> floatsToCoreInstances(RichIterable<? extends Double> floats, ModelRepository repository)
    {
        return floats.collect(f -> floatToCoreInstance(f, repository));
    }


    public static FloatCoreInstance floatToCoreInstance(BigDecimal bigDecimal, ModelRepository repository)
    {
        return repository.newFloatCoreInstance(bigDecimal);
    }

    public static Double optionalInstanceToFloat(FloatCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue().doubleValue();
    }

    public static double requiredInstanceToFloat(FloatCoreInstance instance)
    {
        return instance.getValue().doubleValue();
    }

    public static RichIterable<Double> instancesToFloat(RichIterable<? extends FloatCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToFloat);
    }

    // Integer

    public static IntegerCoreInstance integerToCoreInstance(Number number, ModelRepository repository)
    {
        if (number instanceof Integer)
        {
            return integerToCoreInstance(number.intValue(), repository);
        }
        if (number instanceof Long)
        {
            return integerToCoreInstance(number.longValue(), repository);
        }
        if (number instanceof BigInteger)
        {
            return integerToCoreInstance((BigInteger) number, repository);
        }
        throw new IllegalArgumentException("Unhandled numeric type: " + number.getClass().getName() + " (value=" + number + ")");
    }

    public static IntegerCoreInstance integerToCoreInstance(int i, ModelRepository repository)
    {
        return repository.newIntegerCoreInstance(i);
    }

    public static IntegerCoreInstance integerToCoreInstance(long l, ModelRepository repository)
    {
        return repository.newIntegerCoreInstance(l);
    }

    public static IntegerCoreInstance integerToCoreInstance(BigInteger bigInteger, ModelRepository repository)
    {
        return repository.newIntegerCoreInstance(bigInteger);
    }

    public static RichIterable<IntegerCoreInstance> integersToCoreInstances(RichIterable<? extends Number> integers, ModelRepository repository)
    {
        return integers.collect(i -> integerToCoreInstance(i, repository));
    }


    public static Long optionalInstanceToInteger(IntegerCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue().longValue();
    }

    public static long requiredInstanceToInteger(IntegerCoreInstance instance)
    {
        return instance.getValue().longValue();
    }

    public static RichIterable<Long> instancesToInteger(RichIterable<? extends IntegerCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToInteger);
    }

    // LatestDate

    public static DateCoreInstance latestDateToCoreInstance(LatestDate latestDate, ModelRepository repository)
    {
        return (DateCoreInstance) repository.newLatestDateCoreInstance();
    }

    public static RichIterable<DateCoreInstance> latestDatesToCoreInstances(RichIterable<? extends LatestDate> latestDates, ModelRepository repository)
    {
        return latestDates.collect(latestDate -> latestDateToCoreInstance(latestDate, repository));
    }


    public static LatestDate optionalInstanceToLatestDate(DateCoreInstance instance)
    {
        return (instance == null) ? null : (LatestDate) instance.getValue();
    }

    public static LatestDate requiredInstanceToLatestDate(DateCoreInstance instance)
    {
        return (LatestDate) instance.getValue();
    }

    public static RichIterable<LatestDate> instancesToLatestDate(RichIterable<? extends DateCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToLatestDate);
    }

    // StrictDate

    public static DateCoreInstance strictDateToCoreInstance(StrictDate pureDate, ModelRepository repository)
    {
        return repository.newStrictDateCoreInstance(pureDate);
    }

    public static RichIterable<DateCoreInstance> strictDatesToCoreInstances(RichIterable<? extends StrictDate> strictDates, ModelRepository repository)
    {
        return strictDates.collect(strictDate -> strictDateToCoreInstance(strictDate, repository));
    }


    public static StrictDate optionalInstanceToStrictDate(DateCoreInstance instance)
    {
        return (instance == null) ? null : (StrictDate) instance.getValue();
    }

    public static StrictDate requiredInstanceToStrictDate(DateCoreInstance instance)
    {
        return (StrictDate) instance.getValue();
    }

    public static RichIterable<StrictDate> instancesToStrictDate(RichIterable<? extends DateCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToStrictDate);
    }

    // StrictTime

    public static StrictTimeCoreInstance strictTimeToCoreInstance(PureStrictTime pureStrictTime, ModelRepository repository)
    {
        return repository.newStrictTimeCoreInstance(pureStrictTime);
    }

    public static RichIterable<StrictTimeCoreInstance> strictTimesToCoreInstances(RichIterable<? extends PureStrictTime> strictTimes, ModelRepository repository)
    {
        return strictTimes.collect(strictTime -> strictTimeToCoreInstance(strictTime, repository));
    }


    public static PureStrictTime optionalInstanceToStrictTime(StrictTimeCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue();
    }

    public static PureStrictTime requiredInstanceToStrictTime(StrictTimeCoreInstance instance)
    {
        return instance.getValue();
    }

    public static RichIterable<PureStrictTime> instancesToStrictTime(RichIterable<? extends StrictTimeCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToStrictTime);
    }

    // String

    public static StringCoreInstance stringToCoreInstance(String string, ModelRepository repository)
    {
        return repository.newStringCoreInstance_cached(string);
    }

    public static RichIterable<StringCoreInstance> stringsToCoreInstances(RichIterable<? extends String> strings, ModelRepository repository)
    {
        return strings.collect(string -> stringToCoreInstance(string, repository));
    }


    public static String optionalInstanceToString(StringCoreInstance instance)
    {
        return (instance == null) ? null : instance.getValue();
    }

    public static String requiredInstanceToString(StringCoreInstance instance)
    {
        return instance.getValue();
    }

    public static RichIterable<String> instancesToString(RichIterable<? extends StringCoreInstance> instances)
    {
        return instances.collect(PrimitiveHelper::optionalInstanceToString);
    }
}
