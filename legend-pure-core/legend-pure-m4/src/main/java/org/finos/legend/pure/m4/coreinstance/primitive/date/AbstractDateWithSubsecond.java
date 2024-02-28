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

abstract class AbstractDateWithSubsecond extends AbstractDateWithSecond
{
    private String subsecond;

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
        if (milliseconds == 0)
        {
            return this;
        }

        String subsecond = getSubsecond();
        if (subsecond.length() < 3)
        {
            throw new UnsupportedOperationException("Cannot add milliseconds to a date that does not have milliseconds: " + this);
        }

        AbstractDateWithSubsecond copy = clone();
        long seconds = milliseconds / 1000;
        if (seconds != 0)
        {
            copy.incrementSecond(seconds);
            milliseconds %= 1000;
        }
        if (milliseconds < 0)
        {
            copy.decrementSubsecond(String.format("%03d", -milliseconds), 0, 3);
        }
        else if (milliseconds != 0)
        {
            copy.incrementSubsecond(String.format("%03d", milliseconds), 0, 3);
        }
        return copy;
    }

    @Override
    public PureDate addMicroseconds(long microseconds)
    {
        if (microseconds == 0)
        {
            return this;
        }

        String subsecond = getSubsecond();
        if (subsecond.length() < 6)
        {
            throw new UnsupportedOperationException("Cannot add microseconds to a date that does not have microseconds: " + this);
        }

        AbstractDateWithSubsecond copy = clone();
        long seconds = microseconds / 1_000_000;
        if (seconds != 0)
        {
            copy.incrementSecond(seconds);
            microseconds %= 1_000_000;
        }
        if (microseconds < 0)
        {
            copy.decrementSubsecond(String.format("%06d", -microseconds), 0, 6);
        }
        else if (microseconds != 0)
        {
            copy.incrementSubsecond(String.format("%06d", microseconds), 0, 6);
        }
        return copy;
    }

    @Override
    public PureDate addNanoseconds(long nanoseconds)
    {
        if (nanoseconds == 0)
        {
            return this;
        }

        String subsecond = getSubsecond();
        if (subsecond.length() < 9)
        {
            throw new UnsupportedOperationException("Cannot add nanoseconds to a date that does not have nanoseconds: " + this);
        }

        AbstractDateWithSubsecond copy = clone();
        long seconds = nanoseconds / 1_000_000_000;
        if (seconds != 0)
        {
            if ((seconds > Integer.MAX_VALUE) || (seconds < Integer.MIN_VALUE))
            {
                long days = seconds / 86_400;
                if ((days > Integer.MAX_VALUE) || (days < Integer.MIN_VALUE))
                {
                    throw new IllegalArgumentException(String.format("Cannot add %,d nanoseconds: too large", nanoseconds));
                }
                copy.incrementDay((int)days);
                seconds %= 86_400;
            }
            copy.incrementSecond((int)seconds);
            nanoseconds %= 1_000_000_000;
        }
        if (nanoseconds < 0)
        {
            copy.decrementSubsecond(String.format("%09d", -nanoseconds), 0, 9);
        }
        else if (nanoseconds != 0)
        {
            copy.incrementSubsecond(String.format("%09d", nanoseconds), 0, 9);
        }
        return copy;
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
            // adjusting by zero seconds, nothing to change
            return this;
        }

        if ((end - start) > this.subsecond.length())
        {
            throw new UnsupportedOperationException("Cannot " + (add ? "add" : "subtract") + " subseconds with " + (end - start) + " digits of precision " + (add ? "to" : "from") + " a date that has subseconds to only " + this.subsecond.length() + " digits of precision");
        }

        AbstractDateWithSubsecond copy = clone();
        if (add)
        {
            copy.incrementSubsecond(subseconds, start, end);
        }
        else
        {
            copy.decrementSubsecond(subseconds, start, end);
        }
        return copy;
    }

    @Override
    public abstract AbstractDateWithSubsecond clone();

    private void incrementSubsecond(String delta, int start, int end)
    {
        char[] digits = this.subsecond.toCharArray();
        boolean carry = false;
        for (int i = (end - start) - 1; i >= 0; i--)
        {
            int sum = (int)digits[i] + (int)delta.charAt(i + start) - 96;
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
            digits[i] = (char)(sum + 48);
        }
        if (carry)
        {
            incrementSecond(1);
        }
        this.subsecond = new String(digits);
    }

    private void decrementSubsecond(String delta, int start, int end)
    {
        char[] digits = this.subsecond.toCharArray();
        boolean carry = false;
        for (int i = (end - start) - 1; i >= 0; i--)
        {
            int difference = (int)digits[i] - (int)delta.charAt(i + start);
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
            digits[i] = (char)(difference + 48);
        }
        if (carry)
        {
            incrementSecond(-1);
        }
        this.subsecond = new String(digits);
    }
}
