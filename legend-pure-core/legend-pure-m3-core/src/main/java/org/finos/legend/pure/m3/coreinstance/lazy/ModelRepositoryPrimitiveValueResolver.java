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

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class ModelRepositoryPrimitiveValueResolver implements PrimitiveValueResolver
{
    private final ModelRepository repository;

    public ModelRepositoryPrimitiveValueResolver(ModelRepository repository)
    {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Object resolveBoolean(boolean value)
    {
        return this.repository.newBooleanCoreInstance(value);
    }

    @Override
    public Object resolveByte(byte b)
    {
        return this.repository.newByteCoreInstance(b);
    }

    @Override
    public Object resolveDate(PureDate date)
    {
        return this.repository.newDateCoreInstance(date);
    }

    @Override
    public Object resolveDateTime(PureDate dateTime)
    {
        return this.repository.newDateTimeCoreInstance(dateTime);
    }

    @Override
    public Object resolveStrictDate(PureDate strictDate)
    {
        return this.repository.newStrictDateCoreInstance(strictDate);
    }

    @Override
    public Object resolveLatestDate()
    {
        return this.repository.newLatestDateCoreInstance();
    }

    @Override
    public Object resolveDecimal(BigDecimal decimal)
    {
        return this.repository.newDecimalCoreInstance(decimal);
    }

    @Override
    public Object resolveFloat(BigDecimal value)
    {
        return this.repository.newFloatCoreInstance(value);
    }

    @Override
    public Object resolveInteger(Number value)
    {
        return (value instanceof BigInteger) ?
               this.repository.newIntegerCoreInstance((BigInteger) value) :
               this.repository.newIntegerCoreInstance(value.longValue());
    }

    @Override
    public Object resolveStrictTime(PureStrictTime strictTime)
    {
        return this.repository.newStrictTimeCoreInstance(strictTime);
    }

    @Override
    public Object resolveString(String string)
    {
        return this.repository.newStringCoreInstance_cached(string);
    }
}
