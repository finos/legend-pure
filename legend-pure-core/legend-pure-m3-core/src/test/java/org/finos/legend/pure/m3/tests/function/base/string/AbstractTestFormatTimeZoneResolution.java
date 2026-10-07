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

package org.finos.legend.pure.m3.tests.function.base.string;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.tools.time.TimeZoneResolution;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that {@code format} resolves the time zone a date pattern names in the way the runtime's
 * options say: strictly unless the option asking for the legacy resolution is set.
 *
 * <p>The format strings reach {@code format} through a parameter rather than as literals, since a
 * literal one is checked when it is compiled, strictly, and a zone that only the legacy resolution
 * takes would not compile.
 */
public abstract class AbstractTestFormatTimeZoneResolution extends AbstractPureTestWithCoreCompiled
{
    private static final String SOURCE = "function test::fmt(formatString:String[1], date:Date[1]):String[1]\n" +
            "{\n" +
            "    format($formatString, $date)\n" +
            "}\n" +
            "\n" +
            "function test::formatsStrictly():Boolean[1]\n" +
            "{\n" +
            "    assertEquals('2014-03-10 21:42:35 +0530', test::fmt('%t{[+0530]yyyy-MM-dd HH:mm:ss Z}', %2014-03-10T16:12:35));\n" +
            "    assertEquals('2014-03-10 09:12:35 -0700', test::fmt('%t{[US/Arizona]yyyy-MM-dd HH:mm:ss Z}', %2014-03-10T16:12:35));\n" +
            "}\n" +
            "\n" +
            "function test::formatsTheLegacyWay():Boolean[1]\n" +
            "{\n" +
            "    assertEquals('2014-03-10 16:12:35 +0000 +0530', test::fmt('%t{[+0530]yyyy-MM-dd HH:mm:ss Z z}', %2014-03-10T16:12:35));\n" +
            "    assertEquals('2014-03-10 16:12:35 +0000', test::fmt('%t{[\\'US/Arizona\\']yyyy-MM-dd HH:mm:ss Z}', %2014-03-10T16:12:35));\n" +
            "    assertEquals('2014-03-10 16:12:35 +0000', test::fmt('%t{[America/NewYork]yyyy-MM-dd HH:mm:ss Z}', %2014-03-10T16:12:35));\n" +
            "    assertEquals('2014-03-10 09:12:35 -0700', test::fmt('%t{[US/Arizona]yyyy-MM-dd HH:mm:ss Z}', %2014-03-10T16:12:35));\n" +
            "}\n" +
            "\n" +
            "function test::formatsAQuotedZone():String[1]\n" +
            "{\n" +
            "    test::fmt('%t{[\\'US/Arizona\\']yyyy-MM-dd HH:mm:ss Z}', %2014-03-10T16:12:35)\n" +
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
        execute("test::formatsStrictly():Boolean[1]");
        Exception e = Assert.assertThrows(Exception.class, () -> execute("test::formatsAQuotedZone():String[1]"));
        assertOriginatingPureException(PureExecutionException.class, "Unknown time zone: 'US/Arizona'", e);
    }

    @Test
    public void testLegacyWhenTheOptionIsSet()
    {
        setTimeZoneResolution(TimeZoneResolution.LEGACY);
        execute("test::formatsTheLegacyWay():Boolean[1]");
    }

    @Test
    public void testTheOptionIsReadAtEachCall()
    {
        execute("test::formatsStrictly():Boolean[1]");
        setTimeZoneResolution(TimeZoneResolution.LEGACY);
        execute("test::formatsTheLegacyWay():Boolean[1]");
        setTimeZoneResolution(TimeZoneResolution.STRICT);
        execute("test::formatsStrictly():Boolean[1]");
    }
}
