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

public class TestStrictTimeParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testHour()
    {
        assertParsesTo("07:04", "%07:04");
        assertParsesTo("18:03", "%18:03");
        assertParsesTo("17:04", "%17:04");

        assertFailsToParse("Parser error at (resource:fromString line:18 column:31), token recognition error at: '%b'","%b:09");
        assertFailsToParse("java.lang.IllegalArgumentException: Invalid Pure StrictTime: '44:09'","%44:09");
    }

    @Test
    public void testMinute()
    {
        assertParsesTo("07:03", "%07:03");
        assertParsesTo("07:03", "%7:3");
        assertParsesTo("07:03", "%7:0000003");

        assertFailsToParse("%07:b");
        assertFailsToParse("java.lang.IllegalArgumentException: Invalid Pure StrictTime: '07:95'","%07:95");
    }

    @Test
    public void testSecond()
    {
        assertParsesTo("07:03:01", "%07:03:01");
        assertParsesTo("07:03:01", "%7:3:1");
        assertParsesTo("07:03:01", "%7:3:000000001");

        assertFailsToParse("Parser error at (resource:fromString line:18 column:37), expected: one of {',', '}'} found: ':'","%07:03:b");
        assertFailsToParse("java.lang.IllegalArgumentException: Invalid Pure StrictTime: '07:03:95'", "%07:03:95");
    }

    @Test
    public void testSubSecond()
    {
        assertParsesTo("07:03:01.0003742635", "%07:03:01.0003742635");
        assertParsesTo("07:03:01.000374263500000000", "%7:3:1.000374263500000000");

        assertFailsToParse("Parser error at (resource:fromString line:18 column:43), expected: one of {',', '}'} found: 'b6'","%07:03:01.95b6");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.STRICT_TIME_TYPE_NAME;
    }
}
