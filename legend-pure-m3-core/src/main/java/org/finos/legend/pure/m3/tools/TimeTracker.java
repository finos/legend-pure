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

package org.finos.legend.pure.m3.tools;

public class TimeTracker
{
    private String id;
    private long time;

    public TimeTracker(String id)
    {
        this.id = id;
        this.time = System.nanoTime();
    }

    public String diff(TimeTracker timeTracker)
    {
        if (!this.id.equals(timeTracker.id))
        {
            throw new RuntimeException("Incompatible time trackers!"+this+" "+timeTracker);
        }
        return TimePrinter.makeItHuman(this.time - timeTracker.time);
    }

    public long diffLong(TimeTracker timeTracker)
    {
        return this.time - timeTracker.time;
    }

    public String toString()
    {
        return "["+this.id+" "+this.time+"]";
    }
}
