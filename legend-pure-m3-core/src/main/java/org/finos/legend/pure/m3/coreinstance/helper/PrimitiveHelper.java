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

import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.*;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PrimitiveHelper
{
    public static final Function2<Number, ModelRepository, IntegerCoreInstance> INTEGER_TO_COREINSTANCE_FN = new Function2<Number, ModelRepository, IntegerCoreInstance>()
    {
        public IntegerCoreInstance value(Number number, ModelRepository repository)
        {
            if (number instanceof Integer)
            {
                return repository.newIntegerCoreInstance((Integer) number);
            }
            else if (number instanceof Long)
            {
                return repository.newIntegerCoreInstance((Long) number);
            }
            else if (number instanceof BigInteger)
            {
                return repository.newIntegerCoreInstance((BigInteger) number);
            }
            throw new IllegalArgumentException("Unhandled numeric type: " + number.getClass().getName() + " (value=" + number + ")");

        }
    };

    public static final Function2<String, ModelRepository, StringCoreInstance> STRING_TO_COREINSTANCE_FN = new Function2<String, ModelRepository, StringCoreInstance>()
    {
        public StringCoreInstance value(String string, ModelRepository repository)
        {
            return repository.newStringCoreInstance_cached(string);
        }
    };

    public static final Function2<Boolean, ModelRepository, BooleanCoreInstance> BOOLEAN_TO_COREINSTANCE_FN = new Function2<Boolean, ModelRepository, BooleanCoreInstance>()
    {
        public BooleanCoreInstance value(Boolean booleanValue, ModelRepository repository)
        {
            return repository.newBooleanCoreInstance(booleanValue);
        }
    };

    public static final Function2<PureDate, ModelRepository, DateCoreInstance> DATE_TO_COREINSTANCE_FN = new Function2<PureDate, ModelRepository, DateCoreInstance>()
    {
        public DateCoreInstance value(PureDate pureDate, ModelRepository repository)
        {
            return repository.newDateCoreInstance(pureDate);
        }
    };

    public static final Function2<BigDecimal, ModelRepository, FloatCoreInstance> FLOAT_TO_COREINSTANCE_FN = new Function2<BigDecimal, ModelRepository, FloatCoreInstance>()
    {
        public FloatCoreInstance value(BigDecimal bigDecimal, ModelRepository repository)
        {
            return repository.newFloatCoreInstance(bigDecimal);
        }
    };

    public static final Function2<PureStrictTime, ModelRepository, StrictTimeCoreInstance> STRICTTIME_TO_COREINSTANCE_FN = new Function2<PureStrictTime, ModelRepository, StrictTimeCoreInstance>()
    {
        public StrictTimeCoreInstance value(PureStrictTime pureStrictTime, ModelRepository repository)
        {
            return repository.newStrictTimeCoreInstance(pureStrictTime);
        }
    };

}
