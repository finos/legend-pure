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

package org.finos.legend.pure.m3.coreinstance.lazy;

import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;

public interface PrimitiveValueResolver
{
    Object resolveBoolean(boolean value);

    Object resolveByte(byte b);

    Object resolveDate(PureDate date);

    default Object resolveDateTime(PureDate dateTime)
    {
        return resolveDate(dateTime);
    }

    default Object resolveStrictDate(PureDate strictDate)
    {
        return resolveDate(strictDate);
    }

    Object resolveLatestDate();

    Object resolveDecimal(BigDecimal decimal);

    Object resolveFloat(BigDecimal value);

    Object resolveInteger(Number value);

    Object resolveStrictTime(PureStrictTime strictTime);

    Object resolveString(String string);
}
