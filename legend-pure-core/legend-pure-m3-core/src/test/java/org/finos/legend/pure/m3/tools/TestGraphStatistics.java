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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Bags;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestGraphStatistics
{
    @Test
    public void testInstanceCountDeltas()
    {
        Assert.assertEquals(expected(), writeDeltas());
    }

    @Test
    public void testInstanceCountDeltasWithDefaultLocale()
    {
        // With a default locale such as de-DE, Formatter writes a period between groups of digits; with ar-EG, it
        // writes Arabic-Indic digits
        for (String languageTag : new String[]{"de-DE", "ar-EG"})
        {
            Locale defaultLocale = Locale.getDefault();
            Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
            Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
            Locale.setDefault(Locale.forLanguageTag(languageTag));
            String result;
            try
            {
                result = writeDeltas();
            }
            finally
            {
                Locale.setDefault(defaultLocale);
                Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
                Locale.setDefault(Locale.Category.FORMAT, formatLocale);
            }
            Assert.assertEquals(languageTag, expected(), result);
        }
    }

    private static String writeDeltas()
    {
        MutableBag<String> before = Bags.mutable.empty();
        before.addOccurrences("test::Class1", 1000);
        before.addOccurrences("test::Class2", 7);
        MutableBag<String> after = Bags.mutable.empty();
        after.addOccurrences("test::Class1", 2500);
        after.addOccurrences("test::Class2", 7);
        StringBuilder builder = new StringBuilder();
        GraphStatistics.writeInstanceCountsByClassifierPathDeltas(builder, "  ", "before", before, "after", after);
        return builder.toString();
    }

    private static String expected()
    {
        return "  test::Class1 - before: 1,000; after: 2,500; delta: 1,500" + System.lineSeparator();
    }
}
