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

package org.finos.legend.pure.m3.pct.functions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.factory.Lists;

import java.util.List;

public class FunctionDefinition
{
    public String _package;
    public String name;
    public String sourceId;

    public List<Signature> signatures = Lists.mutable.empty();

    /**
     * Every test exercising this function, PCT and non-PCT alike, documented or not. This is the
     * sole representation of a function's tests; the counts below are derived from it on demand
     * rather than stored, so they cannot drift out of step with the list.
     */
    public List<TestDefinition> tests = Lists.mutable.empty();

    public FunctionDefinition()
    {
    }

    public FunctionDefinition(String sourceId)
    {
        this.sourceId = sourceId;
    }

    /**
     * The number of non-PCT ({@code <<test.Test>>}) tests. PCT tests are deliberately excluded,
     * preserving the meaning this count has always had for downstream consumers.
     * <p>
     * Not serialized: {@link #tests} is what the report carries, and a derived value written into
     * it could not be read back without a field to hold it.
     */
    @JsonIgnore
    public int getTestCount()
    {
        return count(false);
    }

    /**
     * The number of {@code <<PCT.test>>} tests. Not serialized, for the same reason as
     * {@link #getTestCount()}.
     */
    @JsonIgnore
    public int getPctTestCount()
    {
        return count(true);
    }

    private int count(boolean pct)
    {
        return (int) this.tests.stream().filter(t -> t.pct == pct).count();
    }
}
