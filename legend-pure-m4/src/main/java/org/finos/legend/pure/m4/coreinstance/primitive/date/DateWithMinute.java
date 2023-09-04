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

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class DateWithMinute extends AbstractDateWithMinute
{
    private DateWithMinute(int year, int month, int day, int hour, int minute)
    {
        super(year, month, day, hour, minute);
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
    public DateWithMinute clone()
    {
        return new DateWithMinute(getYear(), getMonth(), getDay(), getHour(), getMinute());
    }

    public static DateWithMinute newDateWithMinute(int year, int month, int day, int hour, int minute)
    {
        DateFunctions.validateMonth(month);
        DateFunctions.validateDay(year, month, day);
        DateFunctions.validateHour(hour);
        DateFunctions.validateMinute(minute);
        return new DateWithMinute(year, month, day, hour, minute);
    }

    public static DateWithMinute fromCalendar(GregorianCalendar calendar)
    {
        return new DateWithMinute(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }
}
