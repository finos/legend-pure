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
    private int second;

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

        AbstractStrictTimeWithSecond copy = clone();
        copy.incrementSecond(seconds);
        return copy;
    }

    @Override
    public abstract AbstractStrictTimeWithSecond clone();

    void incrementSecond(int delta)
    {
        incrementMinute(delta / 60);
        this.second += (delta % 60);
        if (this.second < 0)
        {
            incrementMinute(-1);
            this.second += 60;
        }
        else if (this.second > 59)
        {
            incrementMinute(1);
            this.second -= 60;
        }
    }
}