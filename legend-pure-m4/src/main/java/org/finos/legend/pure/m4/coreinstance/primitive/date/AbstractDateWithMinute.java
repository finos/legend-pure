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

abstract class AbstractDateWithMinute extends AbstractDateWithHour
{
    private static final char ISO_UTC = 'Z';

    private int minute;

    protected AbstractDateWithMinute(int year, int month, int day, int hour, int minute)
    {
        super(year, month, day, hour);
        this.minute = minute;
    }

    @Override
    public boolean hasMinute()
    {
        return true;
    }

    @Override
    public int getMinute()
    {
        return this.minute;
    }

    @Override
    public PureDate addMinutes(long minutes)
    {
        if (minutes == 0)
        {
            return this;
        }

        AbstractDateWithMinute copy = clone();
        copy.incrementMinute(minutes);
        return copy;
    }

    @Override
    public abstract AbstractDateWithMinute clone();

    void incrementMinute(long delta)
    {
        incrementHour(delta / 60);
        this.minute += (delta % 60);
        if (this.minute < 0)
        {
            incrementHour(-1);
            this.minute += 60;
        }
        else if (this.minute > 59)
        {
            incrementHour(1);
            this.minute -= 60;
        }
    }

    void setTimeZone(String string, int start, int end)
    {
        char first = string.charAt(start++);
        if ((first == ISO_UTC) && (start == end))
        {
            // time zone = Z, which means UTC: no adjustment necessary
            return;
        }

        boolean negative;
        switch (first)
        {
            case '+':
            {
                negative = false;
                break;
            }
            case '-':
            {
                negative = true;
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Invalid time zone: " + string.substring(start - 1, end));
            }
        }
        if (end - start != 4)
        {
            throw new IllegalArgumentException("Invalid time zone: " + string.substring(start - 1, end));
        }

        int hourOffset = Integer.parseInt(string.substring(start, start + 2));
        int minuteOffset = Integer.parseInt(string.substring(start + 2, end));

        if ((hourOffset != 0) || (minuteOffset != 0))
        {
            // Adjust to UTC
            if (!negative)
            {
                // Offset is from UTC, so we need to reverse the direction
                hourOffset = -hourOffset;
                minuteOffset = -minuteOffset;
            }
            incrementHour(hourOffset);
            incrementMinute(minuteOffset);
        }
    }
}
