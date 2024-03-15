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

abstract class AbstractDateWithYear extends AbstractPureDate
{
    private int year;

    protected AbstractDateWithYear(int year)
    {
        this.year = year;
    }

    @Override
    public int getYear()
    {
        return this.year;
    }

    @Override
    public PureDate addYears(long years)
    {
        if (years == 0)
        {
            return this;
        }

        AbstractDateWithYear copy = clone();
        copy.incrementYear(years);
        return copy;
    }

    @Override
    public abstract AbstractDateWithYear clone();

    void incrementYear(long delta)
    {
        long newYear = Math.addExact(this.year, delta);
        if ((newYear > Integer.MAX_VALUE) || (newYear < Integer.MIN_VALUE))
        {
            throw new IllegalStateException("Year incremented beyond supported bounds");
        }
        this.year = (int) newYear;
    }
}
