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

abstract class AbstractStrictTimeWithSecond extends AbstractStrictTimeWithMinute
{
    protected final int second;

    protected AbstractStrictTimeWithSecond(int hour, int minute, int second)
    {
        super(hour, minute);
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
    public PureStrictTime addSeconds(int seconds)
    {
        if (seconds == 0)
        {
            return this;
        }

        int minutesToAdd = seconds / 60;
        int newSecond = this.second + (seconds % 60);
        if (newSecond < 0)
        {
            minutesToAdd -= 1;
            newSecond += 60;
        }
        else if (newSecond > 59)
        {
            minutesToAdd += 1;
            newSecond -= 60;
        }

        int hoursToAdd = minutesToAdd / 60;
        int newMinute = this.minute + (minutesToAdd % 60);
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

        return with(newHour, newMinute, newSecond);
    }

    @Override
    protected AbstractStrictTimeWithMinute with(int hour, int minute)
    {
        return with(hour, minute, this.second);
    }

    protected abstract AbstractStrictTimeWithMinute with(int hour, int minute, int second);
}
