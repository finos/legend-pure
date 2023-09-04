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

public class DateWithSecond extends AbstractDateWithSecond
{
    private DateWithSecond(int year, int month, int day, int hour, int minute, int second)
    {
        super(year, month, day, hour, minute, second);
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
    public DateWithSecond clone()
    {
        return new DateWithSecond(getYear(), getMonth(), getDay(), getHour(), getMinute(), getSecond());
    }

    public static DateWithSecond newDateWithSecond(int year, int month, int day, int hour, int minute, int second)
    {
        DateFunctions.validateMonth(month);
        DateFunctions.validateDay(year, month, day);
        DateFunctions.validateHour(hour);
        DateFunctions.validateMinute(minute);
        DateFunctions.validateSecond(second);
        return new DateWithSecond(year, month, day, hour, minute, second);
    }
}
