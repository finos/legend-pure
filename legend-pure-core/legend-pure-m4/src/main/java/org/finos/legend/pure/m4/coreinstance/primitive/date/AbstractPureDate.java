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

import org.eclipse.collections.impl.block.factory.Comparators;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

abstract class AbstractPureDate implements PureDate, Serializable
{
    private static final long serialVersionUID = -1L;

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PureDate))
        {
            return false;
        }

        PureDate that = (PureDate)other;
        return (this.getYear() == that.getYear()) &&
                (this.getMonth() == that.getMonth()) &&
                (this.getDay() == that.getDay()) &&
                (this.getHour() == that.getHour()) &&
                (this.getMinute() == that.getMinute()) &&
                (this.getSecond() == that.getSecond()) &&
                Comparators.nullSafeEquals(this.getSubsecond(), that.getSubsecond());
    }

    @Override
    public int hashCode()
    {
        int hash = getYear();
        hash = 31 * hash + getMonth();
        hash = 31 * hash + getDay();
        hash = 31 * hash + getHour();
        hash = 31 * hash + getMinute();
        hash = 31 * hash + getSecond();
        hash = 31 * hash + (hasSubsecond() ? getSubsecond().hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(PureDate other)
    {
        if (this == other)
        {
            return 0;
        }

        // Compare year
        int cmp = Integer.compare(getYear(), other.getYear());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare month
        if (!hasMonth())
        {
            return other.hasMonth() ? -1 : 0;
        }
        if (!other.hasMonth())
        {
            return 1;
        }
        cmp = Integer.compare(getMonth(), other.getMonth());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare day
        if (!hasDay())
        {
            return other.hasDay() ? -1 : 0;
        }
        if (!other.hasDay())
        {
            return 1;
        }
        cmp = Integer.compare(getDay(), other.getDay());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare hour
        if (!hasHour())
        {
            return other.hasHour() ? -1 : 0;
        }
        if (!other.hasHour())
        {
            return 1;
        }
        cmp = Integer.compare(getHour(), other.getHour());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare minute
        if (!hasMinute())
        {
            return other.hasMinute() ? -1 : 0;
        }
        if (!other.hasMinute())
        {
            return 1;
        }
        cmp = Integer.compare(getMinute(), other.getMinute());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare second
        if (!hasSecond())
        {
            return other.hasSecond() ? -1 : 0;
        }
        if (!other.hasSecond())
        {
            return 1;
        }
        cmp = Integer.compare(getSecond(), other.getSecond());
        if (cmp != 0)
        {
            return cmp;
        }

        // Compare subsecond
        if (!hasSubsecond())
        {
            return other.hasSubsecond() ? -1 : 0;
        }
        if (!other.hasSubsecond())
        {
            return 1;
        }
        String thisSubsecond = getSubsecond();
        String otherSubsecond = other.getSubsecond();
        int thisLength = thisSubsecond.length();
        int otherLength = otherSubsecond.length();
        int minLength = Math.min(thisLength, otherLength);
        for (int i = 0; i < minLength; i++)
        {
            cmp = Integer.compare(thisSubsecond.charAt(i), otherSubsecond.charAt(i));
            if (cmp != 0)
            {
                return cmp;
            }
        }
        return Integer.compare(thisLength, otherLength);
    }

    @Override
    public PureDate addWeeks(long weeks)
    {
        return addDays(Math.multiplyExact(7L, weeks));
    }

    @Override
    public long dateDifference(PureDate otherDate, String unit)
    {
        return DateFunctions.dateDifference(this, otherDate, unit);
    }

    @Override
    public String format(String formatString)
    {
        StringBuilder builder = new StringBuilder(32);
        format(builder, formatString);
        return builder.toString();
    }

    @Override
    public void format(Appendable appendable, String formatString)
    {
        try
        {
            DateFormat.format(appendable, formatString, this);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(32);
        writeString(builder);
        return builder.toString();
    }

    @Override
    public void writeString(Appendable appendable)
    {
        try
        {
            DateFormat.write(appendable, this);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GregorianCalendar getCalendar()
    {
        GregorianCalendar calendar = new GregorianCalendar(getYear(), hasMonth() ? (getMonth() - 1) : 0, hasDay() ? getDay() : 1);
        calendar.setTimeZone(DateFunctions.GMT_TIME_ZONE);
        if (hasHour())
        {
            calendar.set(Calendar.HOUR_OF_DAY, getHour());
        }
        if (hasMinute())
        {
            calendar.set(Calendar.MINUTE, getMinute());
        }
        if (hasSecond())
        {
            calendar.set(Calendar.SECOND, getSecond());
        }
        if (hasSubsecond())
        {
            String subsecond = getSubsecond();
            String millisecond;
            int length = subsecond.length();
            switch (length)
            {
                case 1:
                {
                    millisecond = subsecond + "00";
                    break;
                }
                case 2:
                {
                    millisecond = subsecond + "0";
                    break;
                }
                case 3:
                {
                    millisecond = subsecond;
                    break;
                }
                default:
                {
                    millisecond = subsecond.substring(0, 3);
                }
            }
            calendar.set(Calendar.MILLISECOND, Integer.valueOf(millisecond));
        }
        return calendar;
    }

    @Override
    public abstract AbstractPureDate clone();
}
