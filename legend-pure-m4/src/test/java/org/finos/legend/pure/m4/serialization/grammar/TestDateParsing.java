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

package org.finos.legend.pure.m4.serialization.grammar;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Test;

public class TestDateParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testYear()
    {
        assertParsesTo("2014", "%2014");
        assertParsesTo("-2014", "%-2014");
        assertParsesTo("0", "%0");
        assertParsesTo("0", "%-0");

        assertFailsToParse("%abc");
    }

    @Test
    public void testMonth()
    {
        assertParsesTo("2014-02", "%2014-02");
        assertParsesTo("2014-02", "%2014-2");
        assertParsesTo("0-01", "%0000-01");

        assertFailsToParse("%2014-b");
        assertFailsToParse("%2014-34");
        assertFailsToParse("%2014-00");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.DATE_TYPE_NAME;
    }
}
