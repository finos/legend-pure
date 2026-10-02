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

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

abstract class AbstractPureDate implements PureDate, Serializable
{
    private static final long serialVersionUID = -1L;

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PureDate))
        {
            return false;
        }

        PureDate that = (PureDate)other;
        return (this.getYear() == that.getYear()) &&
                (this.getMonth() == that.getMonth()) &&
                (this.getDay() == that.getDay()) &&
                (this.getHour() == that.getHour()) &&
                (this.getMinute() == that.getMinute()) &&
                (this.getSecond() == that.getSecond()) &&
                Objects.equals(this.getSubsecond(), that.getSubsecond());
    }

    @Override
    public int hashCode()
    {
        int hash = getYear();
        hash = 31 * hash + getMonth();
        hash = 31 * hash + getDay();
        hash = 31 * hash + getHour();
        hash = 31 * hash + getMinute();
        hash = 31 * hash + getSecond();
        hash = 31 * hash + Objects.hashCode(getSubsecond());
        return hash;
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder(32)).toString();
    }

    @Override
    public GregorianCalendar getCalendar()
    {
        GregorianCalendar calendar = new GregorianCalendar(getYear(), hasMonth() ? (getMonth() - 1) : 0, hasDay() ? getDay() : 1);
        calendar.setTimeZone(DateFunctions.GMT_TIME_ZONE);
        if (hasHour())
        {
            calendar.set(Calendar.HOUR_OF_DAY, getHour());
        }
        if (hasMinute())
        {
            calendar.set(Calendar.MINUTE, getMinute());
        }
        if (hasSecond())
        {
            calendar.set(Calendar.SECOND, getSecond());
        }
        if (hasSubsecond())
        {
            String subsecond = getSubsecond();
            int millisecond;
            switch (subsecond.length())
            {
                case 1:
                {
                    millisecond = 100 * Integer.parseInt(subsecond);
                    break;
                }
                case 2:
                {
                    millisecond = 10 * Integer.parseInt(subsecond);
                    break;
                }
                case 3:
                {
                    millisecond = Integer.parseInt(subsecond);
                    break;
                }
                default:
                {
                    millisecond = Integer.parseInt(subsecond.substring(0, 3));
                }
            }
            calendar.set(Calendar.MILLISECOND, millisecond);
        }
        return calendar;
    }

    protected static Year addYears(Year year, long years)
    {
        try
        {
            return year.plusYears(years);
        }
        catch (DateTimeException e)
        {
            throw handleDateTimeException(e);
        }
    }

    protected static YearMonth addYears(YearMonth yearMonth, long years)
    {
        try
        {
            return yearMonth.plusYears(years);
        }
        catch (DateTimeException e)
        {
            throw handleDateTimeException(e);
        }
    }

    protected static LocalDate addYears(LocalDate date, long years)
    {
        try
        {
            return date.plusYears(years);
        }
        catch (DateTimeException e)
        {
            throw handleDateTimeException(e);
        }
    }

    protected static YearMonth addMonths(YearMonth yearMonth, long months)
    {
        try
        {
            return yearMonth.plusMonths(months);
        }
        catch (DateTimeException e)
        {
            throw handleDateTimeException(e);
        }
    }

    protected static LocalDate addMonths(LocalDate date, long months)
    {
        try
        {
            return date.plusMonths(months);
        }
        catch (DateTimeException e)
        {
            throw handleDateTimeException(e);
        }
    }

    protected static LocalDate addDays(LocalDate date, long days)
    {
        try
        {
            return date.plusDays(days);
        }
        catch (DateTimeException e)
        {
            throw handleDateTimeException(e);
        }
    }

    private static IllegalStateException handleDateTimeException(DateTimeException e)
    {
        return new IllegalStateException("Date incremented beyond supported bounds", e);
    }
}
