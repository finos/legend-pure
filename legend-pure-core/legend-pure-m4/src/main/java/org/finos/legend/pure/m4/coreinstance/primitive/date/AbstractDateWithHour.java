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

abstract class AbstractDateWithHour extends AbstractDateWithDay implements DateTime
{
    private int hour;

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
        if (hours == 0)
        {
            return this;
        }

        AbstractDateWithHour copy = clone();
        copy.incrementHour(hours);
        return copy;
    }

    @Override
    public StrictDate datePart()
    {
        return StrictDate.newStrictDate(getYear(), getMonth(), getDay());
    }

    @Override
    public abstract AbstractDateWithHour clone();

    void incrementHour(long delta)
    {
        incrementDay(delta / 24);
        this.hour += (delta % 24);
        if (this.hour < 0)
        {
            incrementDay(-1);
            this.hour += 24;
        }
        else if (this.hour > 23)
        {
            incrementDay(1);
            this.hour -= 24;
        }
    }
}
