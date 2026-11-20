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

package org.finos.legend.pure.m4.transaction;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class TestPrintStreamTransactionObserver
{
    private static final MutableList<CoreInstance> INSTANCES = Lists.mutable.withNValues(1234, () -> null);

    @Test
    public void testCounts() throws UnsupportedEncodingException
    {
        Assert.assertEquals(expected(), observe());
    }

    @Test
    public void testCountsWithDefaultLocale() throws UnsupportedEncodingException
    {
        // With a default locale such as de-DE, Formatter writes a period between groups of digits; with ar-EG, it
        // writes Arabic-Indic digits
        for (String languageTag : new String[]{"de-DE", "ar-EG"})
        {
            Locale defaultLocale = Locale.getDefault();
            Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
            Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
            Locale.setDefault(Locale.forLanguageTag(languageTag));
            String output;
            try
            {
                output = observe();
            }
            finally
            {
                Locale.setDefault(defaultLocale);
                Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
                Locale.setDefault(Locale.Category.FORMAT, formatLocale);
            }
            Assert.assertEquals(languageTag, expected(), output);
        }
    }

    private static String observe() throws UnsupportedEncodingException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(bytes, true, "UTF-8"))
        {
            PrintStreamTransactionObserver observer = new PrintStreamTransactionObserver(stream, "> ");
            observer.added(INSTANCES);
            observer.modified(INSTANCES);
        }
        return bytes.toString("UTF-8");
    }

    private static String expected()
    {
        return "> New instances: 1,234" + System.lineSeparator() + "> Modified instances: 1,234" + System.lineSeparator();
    }
}
