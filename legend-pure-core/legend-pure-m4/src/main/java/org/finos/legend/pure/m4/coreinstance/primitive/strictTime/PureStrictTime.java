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

public interface PureStrictTime extends Comparable<PureStrictTime>
{
    default boolean hasHour()
    {
        return true;
    }

    int getHour();

    boolean hasMinute();

    int getMinute();

    boolean hasSecond();

    int getSecond();

    boolean hasSubsecond();

    String getSubsecond();

    default <T extends Appendable> T appendString(T appendable)
    {
        return StrictTimeFormat.append(appendable, this);
    }

    default <T extends Appendable> T appendFormat(T appendable, String formatString)
    {
        return StrictTimeFormat.format(appendable, formatString, this);
    }

    default void writeString(Appendable appendable)
    {
        appendString(appendable);
    }

    default String format(String formatString)
    {
        return appendFormat(new StringBuilder(32), formatString).toString();
    }

    default void format(Appendable appendable, String formatString)
    {
        appendFormat(appendable, formatString);
    }

    PureStrictTime addHours(int hours);

    PureStrictTime addMinutes(int minutes);

    PureStrictTime addSeconds(int seconds);

    PureStrictTime addMilliseconds(int milliseconds);

    PureStrictTime addMicroseconds(int microseconds);

    PureStrictTime addNanoseconds(long nanoseconds);

    PureStrictTime addSubseconds(String subseconds);

    PureStrictTime subtractSubseconds(String subseconds);
}
