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

import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;

public class DirectPrimitiveValueResolver implements PrimitiveValueResolver
{
    @Override
    public Object resolveBoolean(boolean value)
    {
        return value;
    }

    @Override
    public Object resolveByte(byte b)
    {
        return b;
    }

    @Override
    public Object resolveDate(PureDate date)
    {
        return date;
    }

    @Override
    public Object resolveLatestDate()
    {
        return LatestDate.instance;
    }

    @Override
    public Object resolveDecimal(BigDecimal decimal)
    {
        return decimal;
    }

    @Override
    public Object resolveFloat(BigDecimal value)
    {
        return value;
    }

    @Override
    public Object resolveInteger(Number value)
    {
        return value;
    }

    @Override
    public Object resolveStrictTime(PureStrictTime strictTime)
    {
        return strictTime;
    }

    @Override
    public Object resolveString(String string)
    {
        return string;
    }
}
