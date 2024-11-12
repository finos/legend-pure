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

abstract class AbstractDateWithHour extends AbstractDateWithDay implements DateTime
{
    protected final int hour;

    protected AbstractDateWithHour(LocalDate date, int hour)
    {
        super(date);
        this.hour = hour;
    }

    protected AbstractDateWithHour(int year, int month, int day, int hour)
    {
        super(year, month, day);
        this.hour = hour;
    }

    @Override
    public boolean hasHour()
    {
        return true;
    }

    @Override
    public int getHour()
    {
        return this.hour;
    }

    @Override
    public PureDate addHours(long hours)
    {
        if (hours == 0L)
        {
            return this;
        }

        long daysToAdd = hours / 24L;
        int newHour = this.hour + (int) (hours % 24L);
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

        return newWith(addDaysToDatePart(daysToAdd), newHour);
    }

    @Override
    public StrictDate datePart()
    {
        return StrictDate.fromLocalDate(this.date);
    }

    @Override
    protected PureDate newWith(LocalDate newDate)
    {
        return newWith(newDate, this.hour);
    }

    protected abstract PureDate newWith(LocalDate newDate, int newHour);
}
