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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateWithSubsecond extends AbstractDateWithSubsecond
{
    private DateWithSubsecond(LocalDate date, int hour, int minute, int second, String subsecond)
    {
        super(date, hour, minute, second, subsecond);
    }

    private DateWithSubsecond(int year, int month, int day, int hour, int minute, int second, String subsecond)
    {
        super(year, month, day, hour, minute, second, subsecond);
    }

    @Override
    protected PureDate newWith(LocalDate date, int hour, int minute, int second, String subsecond)
    {
        return new DateWithSubsecond(date, hour, minute, second, subsecond);
    }

    public static DateWithSubsecond newDateWithSubsecond(int year, int month, int day, int hour, int minute, int second, String subsecond)
    {
        DateFunctions.validateMonth(month);
        DateFunctions.validateDay(year, month, day);
        DateFunctions.validateHour(hour);
        DateFunctions.validateMinute(minute);
        DateFunctions.validateSecond(second);
        DateFunctions.validateSubsecond(subsecond);
        return new DateWithSubsecond(year, month, day, hour, minute, second, subsecond);
    }

    public static DateTime fromSQLTimestamp(java.sql.Timestamp timestamp)
    {
        GregorianCalendar calendar = new GregorianCalendar(DateFunctions.GMT_TIME_ZONE);
        calendar.setTime(timestamp);
        String subsecond = String.format("%09d", timestamp.getNanos());
        return new DateWithSubsecond(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), subsecond);
    }

    public static DateTime fromInstant(Instant instant, int subsecondPrecision)
    {
        return fromLocalDateTime(LocalDateTime.from(instant.atZone(ZoneId.of("UTC"))), subsecondPrecision);
    }

    static DateTime fromLocalDateTime(LocalDateTime time, int subsecondPrecision)
    {
        return new DateWithSubsecond(time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), time.getMinute(), time.getSecond(), getSubsecond(time, subsecondPrecision));
    }

    private static String getSubsecond(LocalDateTime time, int subsecondPrecision)
    {
        if ((subsecondPrecision < 0) || (subsecondPrecision > 9))
        {
            throw new IllegalArgumentException("Invalid subsecond precision: " + subsecondPrecision);
        }
        if (subsecondPrecision == 0)
        {
            return null;
        }
        String string = String.format("%09d", time.getNano());
        return (subsecondPrecision == 9) ? string : string.substring(0, subsecondPrecision);
    }
}
