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

public class StrictTimeWithSubsecond extends AbstractStrictTimeWithSubsecond
{

    private StrictTimeWithSubsecond(int hour, int minute, int second, String subsecond)
    {
        super(hour, minute, second, subsecond);
    }

    @Override
    public StrictTimeWithSubsecond clone()
    {
        return new StrictTimeWithSubsecond(getHour(), getMinute(), getSecond(), getSubsecond());
    }

    public static StrictTimeWithSubsecond newStrictTimeWithSubsecond(int hour, int minute, int second, String subsecond)
    {
        StrictTimeFunctions.validateHour(hour);
        StrictTimeFunctions.validateMinute(minute);
        StrictTimeFunctions.validateSecond(second);
        StrictTimeFunctions.validateSubsecond(subsecond);
        return new StrictTimeWithSubsecond(hour, minute, second, subsecond);
    }
}
