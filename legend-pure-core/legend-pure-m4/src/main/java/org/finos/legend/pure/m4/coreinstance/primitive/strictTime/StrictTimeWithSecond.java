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


public class StrictTimeWithSecond extends AbstractStrictTimeWithSecond
{
    private StrictTimeWithSecond(int hour, int minute, int second)
    {
        super(hour, minute, second);
    }

    @Override
    public boolean hasSubsecond()
    {
        return false;
    }

    @Override
    public String getSubsecond()
    {
        return null;
    }

    @Override
    public PureStrictTime addMilliseconds(int milliseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureStrictTime addMicroseconds(int microseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureStrictTime addNanoseconds(long nanoseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureStrictTime addSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureStrictTime subtractSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public StrictTimeWithSecond clone()
    {
        return new StrictTimeWithSecond(getHour(), getMinute(), getSecond());
    }

    public static StrictTimeWithSecond newStrictTimeWithSecond(int hour, int minute, int second)
    {
        StrictTimeFunctions.validateHour(hour);
        StrictTimeFunctions.validateMinute(minute);
        StrictTimeFunctions.validateSecond(second);
        return new StrictTimeWithSecond(hour, minute, second);
    }
}
