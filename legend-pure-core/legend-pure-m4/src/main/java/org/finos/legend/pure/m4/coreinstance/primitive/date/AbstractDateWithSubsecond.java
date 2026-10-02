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

abstract class AbstractDateWithSubsecond extends AbstractDateWithSecond
{
    private final String subsecond;

    protected AbstractDateWithSubsecond(LocalDate date, int hour, int minute, int second, String subsecond)
    {
        super(date, hour, minute, second);
        this.subsecond = subsecond;
    }

    protected AbstractDateWithSubsecond(int year, int month, int day, int hour, int minute, int second, String subsecond)
    {
        super(year, month, day, hour, minute, second);
        this.subsecond = subsecond;
    }

    @Override
    public boolean hasSubsecond()
    {
        return true;
    }

    @Override
    public String getSubsecond()
    {
        return this.subsecond;
    }

    @Override
    public PureDate addSubseconds(String subseconds)
    {
        return adjustSubseconds(subseconds, true);
    }

    @Override
    public PureDate subtractSubseconds(String subseconds)
    {
        return adjustSubseconds(subseconds, false);
    }

    @Override
    public PureDate addMilliseconds(long milliseconds)
    {
        if (milliseconds == 0L)
        {
            return this;
        }

        if (this.subsecond.length() < 3)
        {
            throw new UnsupportedOperationException(appendString(new StringBuilder("Cannot add milliseconds to a date that does not have milliseconds: ")).toString());
        }

        long secondsToAdd = milliseconds / 1000L;
        String subseconds = String.format("%03d", Math.abs(milliseconds % 1000L));
        return adjustSubseconds(subseconds, milliseconds > 0).addSeconds(secondsToAdd);
    }

    @Override
    public PureDate addMicroseconds(long microseconds)
    {
        if (microseconds == 0L)
        {
            return this;
        }

        if (this.subsecond.length() < 6)
        {
            throw new UnsupportedOperationException(appendString(new StringBuilder("Cannot add microseconds to a date that does not have microseconds: ")).toString());
        }

        long secondsToAdd = microseconds / 1_000_000L;
        String subseconds = String.format("%06d", Math.abs(microseconds % 1_000_000L));
        return adjustSubseconds(subseconds, microseconds > 0).addSeconds(secondsToAdd);
    }

    @Override
    public PureDate addNanoseconds(long nanoseconds)
    {
        if (nanoseconds == 0L)
        {
            return this;
        }

        if (this.subsecond.length() < 9)
        {
            throw new UnsupportedOperationException(appendString(new StringBuilder("Cannot add nanoseconds to a date that does not have nanoseconds: ")).toString());
        }

        long secondsToAdd = nanoseconds / 1_000_000_000L;
        String subseconds = String.format("%09d", Math.abs(nanoseconds % 1_000_000_000L));
        return adjustSubseconds(subseconds, nanoseconds > 0).addSeconds(secondsToAdd);
    }

    private PureDate adjustSubseconds(String subseconds, boolean add)
    {
        DateFunctions.validateSubsecond(subseconds);

        int start = 0;
        int end = subseconds.length();
        // ignore trailing zeros
        while ((end > start) && (subseconds.charAt(end - 1) == '0'))
        {
            end--;
        }

        if (start == end)
        {
            // adjusting by zero, nothing to change
            return this;
        }

        if ((end - start) > this.subsecond.length())
        {
            throw new UnsupportedOperationException("Cannot " + (add ? "add" : "subtract") + " subseconds with " + (end - start) + " digits of precision " + (add ? "to" : "from") + " a date that has subseconds to only " + this.subsecond.length() + " digits of precision");
        }

        char[] digits = this.subsecond.toCharArray();
        int newSecond = this.second;
        if (add)
        {
            boolean carry = false;
            for (int i = (end - start) - 1; i >= 0; i--)
            {
                int sum = (int) digits[i] + (int) subseconds.charAt(i + start) - 96;
                if (carry)
                {
                    sum += 1;
                }
                if (sum >= 10)
                {
                    carry = true;
                    sum -= 10;
                }
                else
                {
                    carry = false;
                }
                digits[i] = (char) (sum + 48);
            }
            if (carry)
            {
                newSecond += 1;
            }
        }
        else
        {
            boolean carry = false;
            for (int i = (end - start) - 1; i >= 0; i--)
            {
                int difference = (int) digits[i] - (int) subseconds.charAt(i + start);
                if (carry)
                {
                    difference -= 1;
                }
                if (difference < 0)
                {
                    carry = true;
                    difference = 10 + difference;
                }
                else
                {
                    carry = false;
                }
                digits[i] = (char) (difference + 48);
            }
            if (carry)
            {
                newSecond -= 1;
            }
        }

        String newSubsecond = new String(digits);
        int newMinute = this.minute;
        int newHour = this.hour;
        int daysToAdd = 0;
        if (newSecond < 0)
        {
            newMinute -= 1;
            newSecond += 60;
            if (newMinute < 0)
            {
                newHour -= 1;
                newMinute += 60;
                if (newHour < 0)
                {
                    daysToAdd -= 1;
                    newHour += 24;
                }
            }
        }
        else if (newSecond > 59)
        {
            newMinute += 1;
            newSecond -= 60;
            if (newMinute > 59)
            {
                newHour += 1;
                newMinute -= 60;
                if (newHour > 23)
                {
                    daysToAdd += 1;
                    newHour -= 24;
                }
            }
        }

        return newWith(addDaysToDatePart(daysToAdd), newHour, newMinute, newSecond, newSubsecond);
    }

    @Override
    protected PureDate newWith(LocalDate date, int hour, int minute, int second)
    {
        return newWith(date, hour, minute, second, this.subsecond);
    }

    protected abstract PureDate newWith(LocalDate date, int hour, int minute, int second, String subsecond);
}
