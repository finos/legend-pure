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

import java.util.GregorianCalendar;

public interface PureDate extends Comparable<PureDate>
{
    int getYear();

    boolean hasMonth();

    int getMonth();

    boolean hasDay();

    int getDay();

    boolean hasHour();

    int getHour();

    boolean hasMinute();

    int getMinute();

    boolean hasSecond();

    int getSecond();

    boolean hasSubsecond();

    String getSubsecond();

    String format(String formatString);

    void format(Appendable appendable, String formatString);

    PureDate addYears(int years);

    PureDate addMonths(int months);

    PureDate addWeeks(int weeks);

    PureDate addDays(int days);

    PureDate addHours(int hours);

    PureDate addMinutes(int minutes);

    PureDate addSeconds(int seconds);

    PureDate addMilliseconds(int milliseconds);

    PureDate addMicroseconds(long microseconds);

    PureDate addNanoseconds(long nanoseconds);

    PureDate addSubseconds(String subseconds);

    PureDate subtractSubseconds(String subseconds);

    /**
     * Get a Gregorian calendar representation of this Pure date.
     * Note that precision may be lost if the Pure date has
     * precision greater than millisecond.
     *
     * @return Gregorian calendar for Pure date
     */
    GregorianCalendar getCalendar();

    void writeString(Appendable appendable);

    long dateDifference(PureDate otherDate, String unit);

    int compareTo(PureDate pureDate);

    PureDate clone();
}
