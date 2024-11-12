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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.junit.Test;

public class TestStrictDateParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testDay()
    {
        assertParsesTo("2014-02-07", "%2014-02-07");
        assertParsesTo("2014-02-07", "%2014-2-7");

        assertFailsToParse("%2014-02-1b");
        assertFailsToParse("Invalid Pure Date: '%2014-02-53'", 1, 11, "%2014-02-53");
        assertFailsToParse("Invalid Pure Date: '%2014-02-29'", 1, 11, "%2014-02-29");
        assertFailsToParse("Invalid Pure Date: '%2016-06-31'", 1, 11, "%2016-06-31");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return M3Paths.StrictDate;
    }
}
