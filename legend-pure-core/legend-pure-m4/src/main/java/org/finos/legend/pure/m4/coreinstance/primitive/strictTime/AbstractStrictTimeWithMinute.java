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
    protected final int minute;

    protected AbstractStrictTimeWithMinute(int hour, int minute)
    {
        super(hour);
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
    public PureStrictTime addMinutes(int minutes)
    {
        if (minutes == 0)
        {
            return this;
        }

        int hoursToAdd = minutes / 60;
        int newMinute = this.minute + (minutes % 60);
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

        int newHour = this.hour + (hoursToAdd % 24);
        if (newHour < 0)
        {
            newHour += 24;
        }
        else if (newHour > 23)
        {
            newHour -= 24;
        }
        return with(newHour, newMinute);
    }

    @Override
    protected AbstractStrictTimeWithMinute with(int hour)
    {
        return with(hour, this.minute);
    }

    protected abstract AbstractStrictTimeWithMinute with(int hour, int minute);
}
