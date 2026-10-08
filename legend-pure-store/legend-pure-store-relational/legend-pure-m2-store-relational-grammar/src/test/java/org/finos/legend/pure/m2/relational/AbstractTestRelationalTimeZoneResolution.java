// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.tools.time.TimeZoneResolution;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that the relational natives resolve the time zone a connection names in the way the
 * runtime's options say: strictly unless the option asking for the legacy resolution is set.
 * The legacy resolution reads a name {@link java.util.TimeZone#getTimeZone(String)} does not know
 * as GMT, so a TIMESTAMP read through such a connection is not shifted at all.
 */
public abstract class AbstractTestRelationalTimeZoneResolution extends AbstractPureTestWithCoreCompiled
{
    private static final String SOURCE = "import meta::external::store::relational::runtime::*;\n" +
            "import meta::relational::runtime::*;\n" +
            "import meta::relational::metamodel::execute::*;\n" +
            "\n" +
            "function test::readTimestampInZone(timeZone:String[1]):Any[*]\n" +
            "{\n" +
            "    let db = ^TestDatabaseConnection(type = DatabaseType.H2, timeZone = $timeZone);\n" +
            "    executeInDb('SELECT TIMESTAMP \\'2014-03-10 12:00:00\\'', $db, 0, 1000).rows->map(row | $row.values->at(0));\n" +
            "}\n" +
            "\n" +
            "function test::fetchTablesInZone(timeZone:String[1]):Any[*]\n" +
            "{\n" +
            "    let db = ^TestDatabaseConnection(type = DatabaseType.H2, timeZone = $timeZone);\n" +
            "    executeInDb('CREATE TABLE IF NOT EXISTS TZR_PROBE (a INTEGER)', $db, 0, 1000);\n" +
            "    fetchDbTablesMetaData($db, 'PUBLIC', 'TZR_PROBE').rows->map(row | $row.values->at(2));\n" +
            "}\n" +
            "\n" +
            "function test::readsStrictly():Boolean[1]\n" +
            "{\n" +
            "    assertEquals(%2014-03-10T06:30:00.000000000+0000, test::readTimestampInZone('+0530')->toOne());\n" +
            "    assertEquals(%2014-03-10T16:00:00.000000000+0000, test::readTimestampInZone('America/New_York')->toOne());\n" +
            "    assertError(|test::readTimestampInZone('Not/AZone'), 'Error in executeInDb: Unknown time zone: Not/AZone');\n" +
            "}\n" +
            "\n" +
            "function test::readsTheLegacyWay():Boolean[1]\n" +
            "{\n" +
            "    assertEquals(%2014-03-10T12:00:00.000000000+0000, test::readTimestampInZone('+0530')->toOne());\n" +
            "    assertEquals(%2014-03-10T12:00:00.000000000+0000, test::readTimestampInZone('Not/AZone')->toOne());\n" +
            "    assertEquals(%2014-03-10T12:00:00.000000000+0000, test::readTimestampInZone('\\'US/Arizona\\'')->toOne());\n" +
            "    assertEquals(%2014-03-10T16:00:00.000000000+0000, test::readTimestampInZone('America/New_York')->toOne());\n" +
            "}\n" +
            "\n" +
            "function test::fetchesTablesInAnUnknownZone():Boolean[1]\n" +
            "{\n" +
            "    assertEquals(['TZR_PROBE'], test::fetchTablesInZone('Not/AZone'));\n" +
            "}\n";

    /**
     * Ask the runtime these tests run on for the given time zone resolution.
     *
     * @param timeZoneResolution time zone resolution to ask for
     */
    protected abstract void setTimeZoneResolution(TimeZoneResolution timeZoneResolution);

    @Before
    public void compileSource()
    {
        compileTestSource("fromString.pure", SOURCE);
    }

    @After
    public void cleanRuntime()
    {
        setTimeZoneResolution(TimeZoneResolution.STRICT);
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testStrictByDefault()
    {
        execute("test::readsStrictly():Boolean[1]");
        assertFailsMentioning("Unknown time zone: Not/AZone", "test::fetchesTablesInAnUnknownZone():Boolean[1]");
    }

    @Test
    public void testLegacyWhenTheOptionIsSet()
    {
        setTimeZoneResolution(TimeZoneResolution.LEGACY);
        execute("test::readsTheLegacyWay():Boolean[1]");
        execute("test::fetchesTablesInAnUnknownZone():Boolean[1]");
    }

    private void assertFailsMentioning(String expected, String function)
    {
        Exception e = Assert.assertThrows(Exception.class, () -> execute(function));
        for (Throwable t = e; t != null; t = t.getCause())
        {
            if ((t.getMessage() != null) && t.getMessage().contains(expected))
            {
                return;
            }
        }
        throw new AssertionError("Expected an error mentioning \"" + expected + "\"", e);
    }
}
