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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.BiFunction;

public class TestPureStringFormat
{
    /**
     * Only the paths that need no execution support are exercised here, which is every path that
     * can fail on the format string or on the argument rather than inside a nested Pure call.
     */
    private static final BiFunction<Object, ? super ExecutionSupport, ? extends String> TO_REPRESENTATION = (value, es) ->
    {
        throw new AssertionError("toRepresentation should not have been reached");
    };

    @Test
    public void testFormatWrites()
    {
        Assert.assertEquals("on 2014-03-10", format("on %t{yyyy-MM-dd}", date("2014-03-10T13:07:44.07")));
        Assert.assertEquals("on 070", format("on %t{S3}", date("2014-03-10T13:07:44.07")));
        Assert.assertEquals("00042", format("%05d", 42L));
    }

    /**
     * A date pattern that cannot write the date it was handed is a fault of the pair rather than of
     * the format string, so it is only ever found while writing. It has to reach Pure as an error
     * Pure can catch, since nothing above the native turns a Java exception into one.
     */
    @Test
    public void testDateThatCannotBeWritten()
    {
        assertFormatError(
                "Date has a 2 digit sub-second, but 3 are required: 2014-03-10T13:07:44.07+0000",
                "on %t{S!3}", date("2014-03-10T13:07:44.07"));
        assertFormatError(
                "Date has no hour: 2014-03-10",
                "on %t{HH:mm}", date("2014-03-10"));
    }

    /**
     * A format string that is wrong is caught where it is written when it is a literal, and here
     * when it was computed.
     */
    @Test
    public void testMalformedFormatString()
    {
        assertFormatError("Invalid format control character 'Q' in format string: yyyy-Q", "%t{yyyy-Q}", date("2014-03-10"));
        assertFormatError("Sub-second minimum 5 exceeds maximum 3 in format string: S(5,3)", "%t{S(5,3)}", date("2014-03-10T13:07:44.07"));
        assertFormatError("Invalid format specifier: %q", "%q", "anything");
    }

    @Test
    public void testArgumentOfTheWrongType()
    {
        assertFormatError("Expected Integer, got: not an integer", "%d", "not an integer");
        assertFormatError("Expected Date, got: not a date", "%t{yyyy}", "not a date");
        assertFormatError("Expected Float, got: 3", "%.2f", 3L);
    }

    /**
     * The two argument count errors were already reported as Pure errors, and still are.
     */
    @Test
    public void testArgumentCount()
    {
        Assert.assertEquals(
                "Too few arguments passed to format function. Format expression \"%s %s\", number of arguments [1]",
                Assert.assertThrows(PureExecutionException.class, () -> format("%s %s", "one")).getInfo());
        Assert.assertEquals(
                "Unused format args. [2] arguments provided to expression \"%d\"",
                Assert.assertThrows(PureExecutionException.class, () -> format("%d", 1L, 2L)).getInfo());
    }

    private static void assertFormatError(String expectedMessage, String formatString, Object... formatArgs)
    {
        PureExecutionException e = Assert.assertThrows(formatString, PureExecutionException.class, () -> format(formatString, formatArgs));
        Assert.assertEquals(formatString, expectedMessage, e.getInfo());
        Assert.assertNotNull(formatString, e.getCause());
    }

    private static String format(String formatString, Object... formatArgs)
    {
        return PureStringFormat.format(formatString, Lists.mutable.with(formatArgs), TO_REPRESENTATION, null);
    }

    private static Object date(String date)
    {
        return DateFunctions.parsePureDate(date);
    }
}
