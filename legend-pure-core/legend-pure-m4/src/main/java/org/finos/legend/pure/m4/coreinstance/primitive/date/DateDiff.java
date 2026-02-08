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

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

class DateDiff
{
    private DateDiff()
    {
    }

    static long getDiffYears(PureDate from, PureDate to)
    {
        return Math.abs(from.getYear() - to.getYear());
    }

    static long getDiffMonths(PureDate from, PureDate to)
    {
        int thisMonthTotal = (from.getYear() * 12) + from.getMonth();
        int otherMonthTotal = (to.getYear() * 12) + to.getMonth();
        return Math.abs(thisMonthTotal - otherMonthTotal);
    }

    static long getDateDiffWeeks(PureDate from, PureDate to)
    {
        long absDateDiffDays = Math.abs(getDiffDays(from, to));
        int noDaysTillSunday = daysUntilSunday(from.getCalendar(), to.getCalendar());

        if (noDaysTillSunday > absDateDiffDays)
        {
            return 0;
        }
        else
        {
            long fullWeeks = (absDateDiffDays - noDaysTillSunday) / 7;
            boolean partialWeek = noDaysTillSunday > 0;
            return fullWeeks + (partialWeek ? 1 : 0);
        }
    }

    static long getDiffDays(PureDate first, PureDate second)
    {
        Pair<GregorianCalendar, PureDate> thisCalPair = Tuples.pair(first.getCalendar(), first);
        Pair<GregorianCalendar, PureDate> otherCalPair = Tuples.pair(second.getCalendar(), second);
        Pair<Pair<GregorianCalendar, PureDate>, Pair<GregorianCalendar, PureDate>> earlierLaterPair = thisCalPair.getOne().before(otherCalPair.getOne()) ? Tuples.pair(thisCalPair, otherCalPair) : Tuples.pair(otherCalPair, thisCalPair);
        long result = 0;
        if (first.getYear() != second.getYear())
        {
            int fromYear = earlierLaterPair.getOne().getTwo().getYear();
            int toYear = earlierLaterPair.getTwo().getTwo().getYear();
            result += DateFunctions.getYearDays(fromYear) - earlierLaterPair.getOne().getOne().get(Calendar.DAY_OF_YEAR);
            int nextYear = fromYear + 1;
            for (; nextYear != toYear; nextYear++)
            {
                result += DateFunctions.getYearDays(nextYear);
            }
            result += earlierLaterPair.getTwo().getOne().get(Calendar.DAY_OF_YEAR);
        }
        else
        {
            result = (long)earlierLaterPair.getTwo().getOne().get(Calendar.DAY_OF_YEAR) - earlierLaterPair.getOne().getOne().get(Calendar.DAY_OF_YEAR);
        }
        return result;
    }

    static long getDiffHours(PureDate first, PureDate second)
    {
        long msDiff = getDiffInMilliseconds(first, second);
        return TimeUnit.MILLISECONDS.toHours(msDiff);
    }

    static long getDiffMinutes(PureDate first, PureDate second)
    {
        long msDiff = getDiffInMilliseconds(first, second);
        return TimeUnit.MILLISECONDS.toMinutes(msDiff);
    }

    static long getDiffSeconds(PureDate first, PureDate second)
    {
        long msDiff = getDiffInMilliseconds(first, second);
        return TimeUnit.MILLISECONDS.toSeconds(msDiff);
    }

    static long getDiffInMilliseconds(PureDate date1, PureDate date2)
    {
        long time1 = date1.getCalendar().getTimeInMillis();
        long time2 = date2.getCalendar().getTimeInMillis();
        return Math.abs(time1 - time2);
    }

    static long getDiffInMicroseconds(PureDate thisDate, PureDate otherDate)
    {
        return getDiffInNanoseconds(thisDate, otherDate) / 1000L;
    }

    static long getDiffInNanoseconds(PureDate thisDate, PureDate otherDate)
    {
        long totalNanos1 = getTotalNanos(thisDate);
        long totalNanos2 = getTotalNanos(otherDate);
        return Math.abs(totalNanos1 - totalNanos2);
    }

    private static long getTotalNanos(PureDate date)
    {
        long totalNanos = date.getCalendar().getTimeInMillis() * 1_000_000L;

        if (date.hasSubsecond())
        {
            String subsecond = date.getSubsecond();
            if (subsecond.length() > 3)
            {
                String subMilliPart = subsecond.substring(3);
                if (subMilliPart.length() > 6)
                {
                    subMilliPart = subMilliPart.substring(0, 6);
                }
                else
                {
                    subMilliPart = subMilliPart + "000000".substring(subMilliPart.length());
                }
                totalNanos += Long.parseLong(subMilliPart);
            }
        }
        return totalNanos;
    }

    private static int daysUntilSunday(Calendar start, Calendar end)
    {
        if (start.before(end))
        {
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            return 7 - (dayOfWeek - 1);
        }
        else
        {
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            return dayOfWeek - 1;
        }
    }
}
