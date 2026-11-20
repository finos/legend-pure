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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

/**
 * Tests that {@code toUpper} and {@code toLower} do not depend on the JVM's default locale. In Turkish and
 * Azerbaijani, {@code i} uppercases to a dotted capital I (U+0130), and {@code I} lowercases to a dotless i
 * (U+0131).
 *
 * <p>The strings reach {@code toUpper} and {@code toLower} through parameters, so that they are converted when the
 * functions run, under the default locale that the test sets.</p>
 */
public abstract class AbstractTestCaseConversionLocale extends AbstractPureTestWithCoreCompiled
{
    private static final String SOURCE = "function test::upper(string:String[1]):String[1]\n" +
            "{\n" +
            "    $string->toUpper()\n" +
            "}\n" +
            "\n" +
            "function test::lower(string:String[1]):String[1]\n" +
            "{\n" +
            "    $string->toLower()\n" +
            "}\n" +
            "\n" +
            "function test::convertsCase():Boolean[1]\n" +
            "{\n" +
            "    assertEquals('LIST INDEX', test::upper('list index'));\n" +
            "    assertEquals('list index', test::lower('LIST INDEX'));\n" +
            "}\n";

    @Before
    public void compileSource()
    {
        compileTestSource("fromString.pure", SOURCE);
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testCaseConversion()
    {
        execute("test::convertsCase():Boolean[1]");
    }

    @Test
    public void testCaseConversionWithTurkishDefaultLocale()
    {
        executeWithDefaultLocale("tr-TR", "test::convertsCase():Boolean[1]");
    }

    @Test
    public void testCaseConversionWithAzerbaijaniDefaultLocale()
    {
        executeWithDefaultLocale("az-AZ", "test::convertsCase():Boolean[1]");
    }

    private void executeWithDefaultLocale(String languageTag, String functionId)
    {
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag(languageTag));
        try
        {
            execute(functionId);
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
    }
}
