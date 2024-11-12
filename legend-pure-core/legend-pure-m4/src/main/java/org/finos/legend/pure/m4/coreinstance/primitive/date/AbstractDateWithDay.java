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

package org.finos.legend.pure.m4.coreinstance.primitive.date;

import java.time.LocalDate;
import java.util.Objects;

abstract class AbstractDateWithDay extends AbstractPureDate
{
    protected final LocalDate date;

    protected AbstractDateWithDay(LocalDate localDate)
    {
        this.date = Objects.requireNonNull(localDate);
    }

    protected AbstractDateWithDay(int year, int month, int day)
    {
        DateFunctions.validateMonth(month);
        DateFunctions.validateDay(year, month, day);
        this.date = LocalDate.of(year, month, day);
    }

    @Override
    public int getYear()
    {
        return this.date.getYear();
    }

    @Override
    public boolean hasMonth()
    {
        return true;
    }

    @Override
    public int getMonth()
    {
        return this.date.getMonthValue();
    }

    @Override
    public boolean hasDay()
    {
        return true;
    }

    @Override
    public int getDay()
    {
        return this.date.getDayOfMonth();
    }

    @Override
    public PureDate addYears(long years)
    {
        return (years == 0L) ? this : newWith(addYearsToDatePart(years));
    }

    @Override
    public PureDate addMonths(long months)
    {
        return (months == 0L) ? this : newWith(addMonthsToDatePart(months));
    }

    @Override
    public PureDate addWeeks(long weeks)
    {
        if (weeks == 0L)
        {
            return this;
        }

        LocalDate localDate = this.date;
        if (weeks > 0)
        {
            long limit = Long.MAX_VALUE / 7L;
            long limitDays = limit * 7L;
            while (weeks > limit)
            {
                localDate = addDays(localDate, limitDays);
                weeks -= limit;
            }
            localDate = addDays(localDate, weeks * 7L);
        }
        else
        {
            long limit = Long.MIN_VALUE / 7L;
            long limitDays = limit * 7L;
            while (weeks < limit)
            {
                localDate = addDays(localDate, limitDays);
                weeks -= limit;
            }
            localDate = addDays(localDate, weeks * 7L);
        }
        return newWith(localDate);
    }

    @Override
    public PureDate addDays(long days)
    {
        return (days == 0L) ? this : newWith(addDaysToDatePart(days));
    }

    protected LocalDate addYearsToDatePart(long years)
    {
        return addYears(this.date, years);
    }

    protected LocalDate addMonthsToDatePart(long months)
    {
        return addMonths(this.date, months);
    }

    protected LocalDate addDaysToDatePart(long days)
    {
        return addDays(this.date, days);
    }

    protected abstract PureDate newWith(LocalDate newDate);
}
