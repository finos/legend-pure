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

package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A date pattern written into a literal format string is checked where it is written, so a pattern
 * that could never write any date is a compilation error rather than something waiting for the line
 * to be executed.
 */
public class TestFormat extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testFunc.pure");
        runtime.compile();
    }

    /**
     * A specifier that does not exist, or one that is not written the way that specifier is written,
     * is caught where it is written rather than where it runs.
     */
    @Test
    public void testSpecifierThatIsNotOne()
    {
        assertFormatFailsToCompile("Invalid format specifier: %q", "'%q'");
        assertFormatFailsToCompile("Invalid format specifier: %S", "'a %S b'");
        assertFormatFailsToCompile("Format string ends with '%': ends with %", "'ends with %'");
        assertFormatFailsToCompile("Invalid format specifier: %05x", "'%05x'");
        assertFormatFailsToCompile("Invalid format specifier: %0d", "'%0d'");
        assertFormatFailsToCompile("Invalid format specifier: %.f", "'%.f'");
    }

    /**
     * How many arguments a format string wants, and of what kind, stays a run-time question. The
     * arguments are an Any[*] that is often computed, so a format string has to be free to be right
     * for arguments the compiler cannot see.
     */
    @Test
    public void testArgumentsAreNotTheCompilersBusiness()
    {
        assertFormatCompiles("'%s %s %s'");
        assertFormatCompiles("'no specifiers at all'");
        assertFormatCompiles("'%d'");
    }

    @Test
    public void testPatternThatMeansNothing()
    {
        assertFormatFailsToCompile(
                "Invalid format control character 'Q' in format string: yyyy-Q",
                "'%t{yyyy-Q}'");
        assertFormatFailsToCompile(
                "Unknown time zone: Europe/Lissabon",
                "'%t{[Europe/Lissabon]yyyy}'");
        assertFormatFailsToCompile(
                "Time zone can only be set at the beginning of the format string",
                "'%t{yyyy[EST]}'");
    }

    @Test
    public void testSubsecondFieldThatMeansNothing()
    {
        assertFormatFailsToCompile(
                "Sub-second minimum 5 exceeds maximum 3 in format string: S(5,3)",
                "'%t{S(5,3)}'");
        assertFormatFailsToCompile(
                "Sub-second maximum must be at least 1: 0 in format string: S<0",
                "'%t{S<0}'");
        assertFormatFailsToCompile(
                "Expected a digit count after 'S<' in format string: S<",
                "'%t{S<}'");
        assertFormatFailsToCompile(
                "Missing closing parenthesis in format string: S(3,3",
                "'%t{S(3,3}'");
    }

    @Test
    public void testOptionalSectionThatMeansNothing()
    {
        assertFormatFailsToCompile(
                "Missing closing bracket for optional section in format string: ?[HH:mm",
                "'%t{?[HH:mm}'");
        assertFormatFailsToCompile(
                "Unmatched ']' in format string: HH]",
                "'%t{HH]}'");
        assertFormatFailsToCompile(
                "'|' outside an optional section in format string: HH|mm",
                "'%t{HH|mm}'");
        assertFormatFailsToCompile(
                "Empty alternative in optional section in format string: ?[|HH]",
                "'%t{?[|HH]}'");
    }

    /**
     * A brace that has been opened and not closed is the format string's fault rather than the
     * pattern's, and is reported where the pattern would have been.
     *
     * <p>An unterminated quote inside a pattern arrives the same way, and can only arrive that way:
     * the scan that finds the closing brace tracks quotes as the pattern parser does, so a quote
     * left open swallows the brace that would have ended the pattern. A pattern reached through
     * {@code %t} therefore never reports the pattern parser's own missing-quote error, and one that
     * closes its quotes has no missing quote left to report.
     */
    @Test
    public void testPatternWithNoEnd()
    {
        assertFormatFailsToCompile(
                "Could not find end of date format starting at index 2 of: %t{yyyy-MM-dd",
                "'%t{yyyy-MM-dd'");
        assertFormatFailsToCompile(
                "Could not find end of date format starting at index 2 of: %t{yyyy \"abc}",
                "'%t{yyyy \"abc}'");

        // and where the quotes do close, the brace outside them ends the pattern as it should
        assertFormatCompiles("'%t{yyyy \"abc}\"}'");
    }

    /**
     * Every date pattern in the string is checked, not only the first.
     */
    @Test
    public void testEveryPatternInTheString()
    {
        assertFormatFailsToCompile(
                "Invalid format control character 'Q' in format string: yyyy-Q",
                "'%t{yyyy-MM-dd} to %t{yyyy-Q}'");
    }

    /**
     * What is valid compiles, including the forms this work added and the ones it did not touch.
     */
    @Test
    public void testPatternsThatMeanSomething()
    {
        assertFormatCompiles("'%t{yyyy-MM-dd}'");
        assertFormatCompiles("'%t{[America/New_York]yyyy-MM-dd\"T\"HH:mm:ssX}'");
        assertFormatCompiles("'%t{SSSS}'");
        assertFormatCompiles("'%t{S<6}'");
        assertFormatCompiles("'%t{S(3!,9,\"_\")}'");
        assertFormatCompiles("'%t{yyyy?[-MM?[-dd?[\"T\"HH:mm:ss?[.S*]]]]}'");
        assertFormatCompiles("'%t{?[HH:mm:ss|HH:mm|\"--\"]}'");

        // a bare %t writes the date in its canonical form, and has no pattern to be wrong
        assertFormatCompiles("'%t'");
        assertFormatCompiles("'%t and %t'");

        // and the second % of %% cannot open a pattern
        assertFormatCompiles("'100%%t{not a pattern}'");
    }

    /**
     * Only a literal format string can be checked here. One that is computed reaches the validator
     * as something other than a literal, and is left to the run-time check, which is why that check
     * stays.
     */
    @Test
    public void testAComputedFormatStringIsLeftAlone()
    {
        compileTestSource("testFunc.pure",
                "function testFunc(zone:String[1]):String[1]\n" +
                        "{\n" +
                        "    format('%t{[' + $zone + ']yyyy-Q}', %2014-03-10);\n" +
                        "}");
    }

    /**
     * The failure names the line and column the call was written at, since a model with more than
     * one format string in it is otherwise a search.
     */
    @Test
    public void testTheFailureSaysWhereItIs()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():String[1]\n" +
                        "{\n" +
                        "    let a = 'x';\n" +
                        "    format('%t{yyyy-Q}', %2014-03-10);\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "Invalid format control character 'Q' in format string: yyyy-Q",
                "testFunc.pure",
                4,
                5,
                e);
    }

    private static void assertFormatCompiles(String formatString)
    {
        compileTestSource("testFunc.pure", functionUsing(formatString));
        discardTestSource();
    }

    private void assertFormatFailsToCompile(String expectedMessage, String formatString)
    {
        PureCompilationException e = Assert.assertThrows(
                formatString,
                PureCompilationException.class,
                () -> compileTestSource("testFunc.pure", functionUsing(formatString)));
        assertPureException(PureCompilationException.class, expectedMessage, e);
        discardTestSource();
    }

    /**
     * Take the source back out of the runtime. A compile that failed leaves it registered all the
     * same, so without this the next one in the same test is refused for the name rather than for
     * the format string it was written to check.
     */
    private static void discardTestSource()
    {
        runtime.delete("testFunc.pure");
        runtime.compile();
    }

    private static String functionUsing(String formatString)
    {
        return "function testFunc():String[1]\n" +
                "{\n" +
                "    format(" + formatString + ", %2014-03-10T13:07:44.070004235);\n" +
                "}";
    }
}
