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

import org.eclipse.collections.impl.block.factory.Comparators;

import java.io.IOException;
import java.io.Serializable;

abstract class AbstractPureStrictTime implements PureStrictTime, Serializable
{
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

        PureStrictTime that = (PureStrictTime)other;
        return
                (this.getHour() == that.getHour()) &&
                (this.getMinute() == that.getMinute()) &&
                (this.getSecond() == that.getSecond()) &&
                Comparators.nullSafeEquals(this.getSubsecond(), that.getSubsecond());
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
            StrictTimeFormat.format(appendable, formatString, this);
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
            StrictTimeFormat.write(appendable, this);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public abstract AbstractPureStrictTime clone();
}
