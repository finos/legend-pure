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
import java.util.Calendar;
import java.util.GregorianCalendar;

public final class StrictDate extends AbstractDateWithDay
{
    private StrictDate(int year, int month, int day)
    {
        super(year, month, day);
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

    @Override
    public StrictDate clone()
    {
        return new StrictDate(getYear(), getMonth(), getDay());
    }

    public static StrictDate newStrictDate(int year, int month, int day)
    {
        DateFunctions.validateMonth(month);
        DateFunctions.validateDay(year, month, day);
        return new StrictDate(year, month, day);
    }

    public static StrictDate fromSQLDate(java.sql.Date date)
    {
        GregorianCalendar calendar = new GregorianCalendar(DateFunctions.GMT_TIME_ZONE);
        calendar.setTime(date);
        return fromCalendar(calendar);
    }

    public static StrictDate fromCalendar(GregorianCalendar calendar)
    {
        return new StrictDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    static StrictDate fromLocalDate(LocalDate date)
    {
        return new StrictDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
