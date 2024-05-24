// Copyright 2024 Goldman Sachs
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

public class Adapter
{
    public String name;
    public String function;

    public Adapter()
    {
    }

    public Adapter(String name, String function)
    {
        this.name = name;
        this.function = function;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Adapter adapter = (Adapter) o;
        return Objects.equals(name, adapter.name) && Objects.equals(function, adapter.function);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, function);
    }
}
