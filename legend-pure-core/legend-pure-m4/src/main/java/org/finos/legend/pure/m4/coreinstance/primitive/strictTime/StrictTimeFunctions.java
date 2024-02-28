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

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.TimeFunctions;

public class StrictTimeFunctions extends TimeFunctions
{
    public static StrictTimeWithMinute newPureStrictTime(int hour, int minute)
    {
        return StrictTimeWithMinute.newStrictTimeWithMinute(hour, minute);
    }

    public static StrictTimeWithSecond newPureStrictTime(int hour, int minute, int second)
    {
        return StrictTimeWithSecond.newStrictTimeWithSecond(hour, minute, second);
    }

    public static StrictTimeWithSubsecond newPureStrictTime(int hour, int minute, int second, String subsecond)
    {
        return StrictTimeWithSubsecond.newStrictTimeWithSubsecond(hour, minute, second, subsecond);
    }

    public static String strictTimePrimitiveType(PureStrictTime pureStrictTime)
    {
        return ModelRepository.STRICT_TIME_TYPE_NAME;
    }

    public static PureStrictTime parsePureStrictTime(String string)
    {
        return StrictTimeFormat.parseStrictTime(string);
    }
}
