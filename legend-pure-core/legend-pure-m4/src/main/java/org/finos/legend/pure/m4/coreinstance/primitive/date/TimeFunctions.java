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

import org.eclipse.collections.impl.utility.StringIterate;

public abstract class TimeFunctions
{
    public static void validateHour(int hour)
    {
        if ((hour < 0) || (hour > 23))
        {
            throw new IllegalArgumentException("Invalid hour: " + hour);
        }
    }

    public static void validateMinute(int minute)
    {
        if ((minute < 0) || (minute > 59))
        {
            throw new IllegalArgumentException("Invalid minute: " + minute);
        }
    }

    public static void validateSecond(int second)
    {
        if ((second < 0) || (second > 59))
        {
            throw new IllegalArgumentException("Invalid second: " + second);
        }
    }

    public static void validateSubsecond(String subsecond)
    {
        if (subsecond == null)
        {
            throw new IllegalArgumentException("Invalid subsecond value: null");
        }
        if (subsecond.isEmpty() || !StringIterate.isNumber(subsecond))
        {
            throw new IllegalArgumentException("Invalid subsecond value: \"" + subsecond + "\"");
        }
    }
}
