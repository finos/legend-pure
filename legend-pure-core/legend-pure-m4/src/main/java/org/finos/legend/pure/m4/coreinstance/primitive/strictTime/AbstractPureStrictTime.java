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

import java.io.Serializable;
import java.util.Objects;

abstract class AbstractPureStrictTime implements PureStrictTime, Serializable
{
    protected final int hour;

    protected AbstractPureStrictTime(int hour)
    {
        this.hour = hour;
    }

    @Override
    public int getHour()
    {
        return this.hour;
    }

    @Override
    public PureStrictTime addHours(int hours)
    {
        if (hours == 0)
        {
            return this;
        }

        int newHour = this.hour + (hours % 24);
        if (newHour < 0)
        {
            newHour += 24;
        }
        else if (newHour > 23)
        {
            newHour -= 24;
        }
        return with(newHour);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PureStrictTime))
        {
            return false;
        }

        PureStrictTime that = (PureStrictTime) other;
        return (this.getHour() == that.getHour()) &&
                (this.getMinute() == that.getMinute()) &&
                (this.getSecond() == that.getSecond()) &&
                Objects.equals(this.getSubsecond(), that.getSubsecond());
    }

    @Override
    public int hashCode()
    {
        int hash = getHour();
        hash = 31 * hash + getMinute();
        hash = 31 * hash + getSecond();
        hash = 31 * hash + (hasSubsecond() ? getSubsecond().hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(PureStrictTime other)
    {
        if (this == other)
        {
            return 0;
        }

        int cmp = Integer.compare(getHour(), other.getHour());
        if (cmp != 0)
        {
            return cmp;
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
    public String toString()
    {
        return appendString(new StringBuilder(32)).toString();
    }

    protected abstract AbstractPureStrictTime with(int hour);
}
