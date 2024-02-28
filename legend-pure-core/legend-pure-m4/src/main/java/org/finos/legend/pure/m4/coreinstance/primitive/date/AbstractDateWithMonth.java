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

abstract class AbstractDateWithMonth extends AbstractDateWithYear
{
    private int month;

    protected AbstractDateWithMonth(int year, int month)
    {
        super(year);
        this.month = month;
    }

    @Override
    public boolean hasMonth()
    {
        return true;
    }

    @Override
    public int getMonth()
    {
        return this.month;
    }

    @Override
    public PureDate addMonths(long months)
    {
        if (months == 0)
        {
            return this;
        }

        AbstractDateWithMonth copy = clone();
        copy.incrementMonth(months);
        return copy;
    }

    @Override
    public abstract AbstractDateWithMonth clone();

    void incrementMonth(long delta)
    {
        incrementYear(delta / 12);
        this.month += (delta % 12);
        if (this.month < 1)
        {
            incrementYear(-1);
            this.month += 12;
        }
        else if (this.month > 12)
        {
            incrementYear(1);
            this.month -= 12;
        }
    }
}
