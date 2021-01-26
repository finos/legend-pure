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
import java.util.Calendar;
import java.util.GregorianCalendar;

public class StrictTimeWithSubsecond extends AbstractDateWithSubsecond implements StrictTime
{
    private StrictTimeWithSubsecond(int hour, int minute, int second, String subsecond)
    {
        super(-1, -1, -1, hour, minute, second, subsecond);
    }

    @Override
    public  boolean hasDay(){
        return true;
    }

    @Override
    public int getDay(){
        return -1;
    }

    @Override
    public  boolean hasMonth(){
        return true;
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
    public StrictTimeWithSubsecond clone()
    {
        return new StrictTimeWithSubsecond(getHour(), getMinute(), getSecond(), getSubsecond());
    }

    public static StrictTimeWithSubsecond newStrictTimeWithSubsecond(int hour, int minute, int second, String subsecond)
    {
        DateFunctions.validateHour(hour);
        DateFunctions.validateMinute(minute);
        DateFunctions.validateSecond(second);
        DateFunctions.validateSubsecond(subsecond);
        return new StrictTimeWithSubsecond(hour, minute, second, subsecond);
    }

    public static StrictTimeWithSubsecond fromSQLTimestamp(java.sql.Timestamp timestamp)
    {
        GregorianCalendar calendar = new GregorianCalendar(DateFunctions.GMT_TIME_ZONE);
        calendar.setTime(timestamp);
        String subsecond = String.format("%09d", timestamp.getNanos());
        return new StrictTimeWithSubsecond(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), subsecond);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        write(builder, this.getHour());
        builder.append(":");
        write(builder, this.getMinute());
        builder.append(":");
        write(builder, this.getSecond());
        builder.append(".");
        builder.append((this.getSubsecond()));
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
