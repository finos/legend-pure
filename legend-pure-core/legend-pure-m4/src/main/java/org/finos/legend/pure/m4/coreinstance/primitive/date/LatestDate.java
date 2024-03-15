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

import java.util.GregorianCalendar;

public class LatestDate implements PureDate
{
    private static final String latestDateConstant = "%latest";
    public static final LatestDate instance = new LatestDate();

    private LatestDate()
    {
    }

    @Override
    public int getYear()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public boolean hasMonth()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public int getMonth()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public boolean hasDay()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public int getDay()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public boolean hasHour()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public int getHour()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public boolean hasMinute()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public int getMinute()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public boolean hasSecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public int getSecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public boolean hasSubsecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public String getSubsecond()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public String format(String formatString)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public void format(Appendable appendable, String formatString)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addYears(long years)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addMonths(long months)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addWeeks(long weeks)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addDays(long days)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addHours(long hours)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addMinutes(long minutes)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addSeconds(long seconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addMilliseconds(long milliseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addMicroseconds(long microseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addNanoseconds(long nanoseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate addSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public PureDate subtractSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public GregorianCalendar getCalendar()
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public void writeString(Appendable appendable)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public long dateDifference(PureDate otherDate, String unit)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public int compareTo(PureDate pureDate)
    {
        throw new UnsupportedOperationException("Invalid operation for LatestDate");
    }

    @Override
    public String toString()
    {
        return latestDateConstant;
    }

    @Override
    public LatestDate clone()
    {
        return this;
    }

    public static boolean isLatestDate(PureDate date)
    {
        return date == instance;
    }

    public static boolean isLatestDateString(String string)
    {
        return latestDateConstant.equals(string);
    }
}
