// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.pct.reports.model;

import java.util.Objects;

public class AdapterReverse extends org.finos.legend.pure.m3.pct.reports.model.Adapter
{
    public String reversesFunction;

    public AdapterReverse()
    {
    }

    public AdapterReverse(String name, String group, String function, String reversesFunction)
    {
        super(name, group, function);
        this.reversesFunction = reversesFunction;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }
        AdapterReverse adapter = (AdapterReverse) o;
        return Objects.equals(reversesFunction, adapter.reversesFunction);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), reversesFunction);
    }
}
