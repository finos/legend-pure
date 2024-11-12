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

public final class YearMonth extends AbstractPureDate
{
    private final java.time.YearMonth yearMonth;

    private YearMonth(java.time.YearMonth yearMonth)
    {
        this.yearMonth = yearMonth;
    }

    private YearMonth(int year, int month)
    {
        this(java.time.YearMonth.of(year, month));
    }

    @Override
    public int getYear()
    {
        return this.yearMonth.getYear();
    }

    @Override
    public boolean hasMonth()
    {
        return true;
    }

    @Override
    public int getMonth()
    {
        return this.yearMonth.getMonthValue();
    }

    @Override
    public boolean hasDay()
    {
        return false;
    }

    @Override
    public int getDay()
    {
        return -1;
    }

    @Override
    public boolean hasHour()
    {
        return false;
    }

    @Override
    public int getHour()
    {
        return -1;
    }

    @Override
    public boolean hasMinute()
    {
        return false;
    }

    @Override
    public int getMinute()
    {
        return -1;
    }

    @Override
    public boolean hasSecond()
    {
        return false;
    }

    @Override
    public int getSecond()
    {
        return -1;
    }

    @Override
    public boolean hasSubsecond()
    {
        return false;
    }

    @Override
    public String getSubsecond()
    {
        return null;
    }

    @Override
    public PureDate addYears(long years)
    {
        return (years == 0L) ? this : new YearMonth(addYears(this.yearMonth, years));
    }

    @Override
    public PureDate addMonths(long months)
    {
        return (months == 0L) ? this : new YearMonth(addMonths(this.yearMonth, months));
    }

    @Override
    public PureDate addWeeks(long weeks)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addDays(long days)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addHours(long hours)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addMinutes(long minutes)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addSeconds(long seconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addMilliseconds(long milliseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addMicroseconds(long microseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addNanoseconds(long nanoseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate subtractSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException();
    }

    public static YearMonth newYearMonth(int year, int month)
    {
        DateFunctions.validateYear(year);
        DateFunctions.validateMonth(month);
        return new YearMonth(year, month);
    }
}
