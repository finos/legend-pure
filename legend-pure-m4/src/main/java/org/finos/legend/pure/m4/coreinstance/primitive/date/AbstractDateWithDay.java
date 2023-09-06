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

abstract class AbstractDateWithDay extends AbstractDateWithMonth
{
    private int day;

    protected AbstractDateWithDay(int year, int month, int day)
    {
        super(year, month);
        this.day = day;
    }

    @Override
    public boolean hasDay()
    {
        return true;
    }

    @Override
    public int getDay()
    {
        return this.day;
    }

    @Override
    public PureDate addYears(long years)
    {
        PureDate result = super.addYears(years);
        if ((result.getMonth() == 2) && (result.getDay() == 29) && !DateFunctions.isLeapYear(result.getYear()))
        {
            ((AbstractDateWithDay)result).day = 28;
        }
        return result;
    }

    @Override
    public PureDate addMonths(long months)
    {
        PureDate result = super.addMonths(months);
        if (result != this)
        {
            int daysInMonth = DateFunctions.getDaysInMonth(result.getYear(), result.getMonth());
            AbstractDateWithDay resultWithDay = (AbstractDateWithDay)result;
            if (resultWithDay.day > daysInMonth)
            {
                resultWithDay.day = daysInMonth;
            }
        }
        return result;
    }

    @Override
    public PureDate addDays(long days)
    {
        if (days == 0)
        {
            return this;
        }

        AbstractDateWithDay copy = clone();
        copy.incrementDay(days);
        return copy;
    }

    @Override
    public abstract AbstractDateWithDay clone();

    void incrementDay(long delta)
    {
        long remDelta = Math.addExact(this.day, delta);
        if (delta < 0)
        {
            while (remDelta < 1)
            {
                incrementMonth(-1);
                remDelta += DateFunctions.getDaysInMonth(this.getYear(), this.getMonth());
            }
        }
        else if (delta > 0)
        {
            for (int maxDay = DateFunctions.getDaysInMonth(getYear(), getMonth()); remDelta > maxDay; maxDay = DateFunctions.getDaysInMonth(getYear(), getMonth()))
            {
                remDelta -= maxDay;
                incrementMonth(1);
            }
        }
        this.day = (int) remDelta;
    }
}
