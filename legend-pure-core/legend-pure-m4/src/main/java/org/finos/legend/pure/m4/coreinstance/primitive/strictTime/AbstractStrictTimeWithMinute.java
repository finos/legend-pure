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

package org.finos.legend.pure.m4.coreinstance.primitive.strictTime;

abstract class AbstractStrictTimeWithMinute extends AbstractPureStrictTime
{
    private int hour;
    private int minute;

    protected AbstractStrictTimeWithMinute(int hour, int minute)
    {
        this.hour = hour;
        this.minute = minute;
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
    public PureStrictTime addHours(int hours)
    {
        if (hours == 0)
        {
            return this;
        }

        AbstractStrictTimeWithMinute copy = clone();
        copy.incrementHour(hours);
        return copy;
    }

    @Override
    public PureStrictTime addMinutes(int minutes)
    {
        if (minutes == 0)
        {
            return this;
        }

        AbstractStrictTimeWithMinute copy = clone();
        copy.incrementMinute(minutes);
        return copy;
    }

    @Override
    public abstract AbstractStrictTimeWithMinute clone();

    void incrementHour(int delta)
    {
        this.hour += (delta % 24);
        if (this.hour < 0)
        {
            this.hour += 24;
        }
        else if (this.hour > 23)
        {
            this.hour -= 24;
        }
    }

    void incrementMinute(int delta)
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
}
