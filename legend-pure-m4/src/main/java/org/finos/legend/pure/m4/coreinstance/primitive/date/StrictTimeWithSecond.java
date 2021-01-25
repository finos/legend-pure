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

import java.io.IOException;

public class StrictTimeWithSecond extends AbstractDateWithSecond implements StrictTime
{
    private StrictTimeWithSecond(int hour, int minute, int second)
    {
        super(-1,-1,-1,hour, minute, second);
    }

    @Override
    public  boolean hasDay(){
        return false;
    }

    @Override
    public int getDay(){
        return -1;
    }

    @Override
    public  boolean hasMonth(){
        return false;
    }

    @Override
    public int getMonth(){
        return -1;
    }

    @Override
    public int getYear(){
        return -1;
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
    public PureDate addMilliseconds(int milliseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addMicroseconds(int microseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addNanoseconds(long nanoseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate addSubseconds(String subseconds)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PureDate subtractSubseconds(String subseconds)
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
        DateFunctions.validateHour(hour);
        DateFunctions.validateMinute(minute);
        DateFunctions.validateSecond(second);
        return new StrictTimeWithSecond(hour, minute, second);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        write(builder, this.getHour());
        builder.append(":");
        write(builder, this.getMinute());
        builder.append(":");
        write(builder, this.getSecond());
        return builder.toString();
    }
    private static void write(Appendable appendable, Integer val)
    {
        try
        {
            DateFormat.appendTwoDigitInt(appendable,val);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
