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

abstract class AbstractDateWithSecond extends AbstractDateWithMinute
{
    protected final int second;

    protected AbstractDateWithSecond(LocalDate date, int hour, int minute, int second)
    {
        super(date, hour, minute);
        this.second = second;
    }

    protected AbstractDateWithSecond(int year, int month, int day, int hour, int minute, int second)
    {
        super(year, month, day, hour, minute);
        this.second = second;
    }

    @Override
    public boolean hasSecond()
    {
        return true;
    }

    @Override
    public int getSecond()
    {
        return this.second;
    }

    @Override
    public PureDate addSeconds(long seconds)
    {
        if (seconds == 0L)
        {
            return this;
        }

        long minutesToAdd = seconds / 60L;
        int newSecond = this.second + (int) (seconds % 60L);
        if (newSecond < 0)
        {
            minutesToAdd -= 1L;
            newSecond += 60;
        }
        else if (newSecond > 59)
        {
            minutesToAdd += 1L;
            newSecond -= 60;
        }

        long hoursToAdd = minutesToAdd / 60L;
        int newMinute = this.minute + (int) (minutesToAdd % 60L);
        if (newMinute < 0)
        {
            hoursToAdd -= 1;
            newMinute += 60;
        }
        else if (newMinute > 59)
        {
            hoursToAdd += 1;
            newMinute -= 60;
        }

        long daysToAdd = hoursToAdd / 24L;
        int newHour = this.hour + (int) (hoursToAdd % 24L);
        if (newHour < 0)
        {
            daysToAdd -= 1;
            newHour += 24;
        }
        else if (newHour > 23)
        {
            daysToAdd += 1;
            newHour -= 24;
        }

        return newWith(addDaysToDatePart(daysToAdd), newHour, newMinute, newSecond);
    }

    @Override
    protected PureDate newWith(LocalDate date, int hour, int minute)
    {
        return newWith(date, hour, minute, this.second);
    }

    protected abstract PureDate newWith(LocalDate date, int hour, int minute, int second);
}
