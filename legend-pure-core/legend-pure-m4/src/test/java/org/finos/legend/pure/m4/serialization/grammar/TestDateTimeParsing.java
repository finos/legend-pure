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

public class TestDateTimeParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testHour()
    {
        assertParsesTo("2014-02-07T07", "%2014-02-07T07");
        assertParsesTo("2014-02-07T07", "%2014-2-7T7");
        assertParsesTo("2014-02-07T07", "%2014-2-7T000007");

        assertFailsToParse("%2014-02-07Tb");
        assertFailsToParse("%2014-02-07T55");
    }

    @Test
    public void testMinute()
    {
        assertParsesTo("2014-02-07T07:03+0000", "%2014-02-07T07:03");
        assertParsesTo("2014-02-07T07:03+0000", "%2014-2-7T7:3");
        assertParsesTo("2014-02-07T07:03+0000", "%2014-2-7T7:0000003");

        assertFailsToParse("%2014-02-07T07:b");
        assertFailsToParse("%2014-02-07T07:95");
    }

    @Test
    public void testSecond()
    {
        assertParsesTo("2014-02-07T07:03:01+0000", "%2014-02-07T07:03:01");
        assertParsesTo("2014-02-07T07:03:01+0000", "%2014-2-7T7:3:1");
        assertParsesTo("2014-02-07T07:03:01+0000", "%2014-2-7T7:3:000000001");

        assertFailsToParse("%2014-02-07T07:03:b");
        assertFailsToParse("%2014-02-07T07:03:95");
    }

    @Test
    public void testSubSecond()
    {
        assertParsesTo("2014-02-07T07:03:01.0003742635+0000", "%2014-02-07T07:03:01.0003742635");
        assertParsesTo("2014-02-07T07:03:01.000374263500000000+0000", "%2014-2-7T7:3:1.000374263500000000");

        assertFailsToParse("%2014-02-07T07:03:01.b");
        assertFailsToParse("%2014-02-07T07:03:01.95b");
    }

    @Test
    public void testTimeZone()
    {
        assertParsesTo("2014-02-07T07:03:01.0003742635+0000", "%2014-02-07T07:03:01.0003742635+0000");
        assertParsesTo("2014-02-07T12:03:01.0003742635+0000", "%2014-02-07T07:03:01.0003742635-0500");
        assertParsesTo("2014-03-01T01:48:01.0003742635+0000", "%2014-02-28T20:03:01.0003742635-0545");
        assertParsesTo("2012-02-29T01:48:01.0003742635+0000", "%2012-02-28T20:03:01.0003742635-0545");

        assertFailsToParse("%2014-02-07T07:03:01.0132-43252342");
        assertFailsToParse("%2014-02-07T07:03:01.95+1");
        assertFailsToParse("%2014-02-07T07:03:01.95+00b0");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.DATETIME_TYPE_NAME;
    }
}
