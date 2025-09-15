// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m4.coreinstance.primitive.simple;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SimplePrimitiveCoreInstances
{
    public static BooleanCoreInstance newBooleanCoreInstance(boolean value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleBooleanCoreInstance(value, classifier, internalSyntheticId);
    }

    public static ByteCoreInstance newByteCoreInstance(Byte value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleByteCoreInstance(value, classifier, internalSyntheticId);
    }

    public static DateCoreInstance newDateCoreInstance(PureDate value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleDateCoreInstance(value, classifier, internalSyntheticId);
    }

    public static DecimalCoreInstance newDecimalCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleDecimalCoreInstance(value, classifier, internalSyntheticId);
    }

    public static FloatCoreInstance newFloatCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleFloatCoreInstance(FloatCoreInstance.canonicalizeBigDecimal(value), classifier, internalSyntheticId);
    }

    public static IntegerCoreInstance newIntegerCoreInstance(Integer value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public static IntegerCoreInstance newIntegerCoreInstance(Long value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public static IntegerCoreInstance newIntegerCoreInstance(BigInteger value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public static StrictTimeCoreInstance newStrictTimeCoreInstance(PureStrictTime value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleStrictTimeCoreInstance(value, classifier, internalSyntheticId);
    }

    public static StringCoreInstance newStringCoreInstance(String value, CoreInstance classifier, int internalSyntheticId)
    {
        return new SimpleStringCoreInstance(value, classifier, internalSyntheticId);
    }
}
