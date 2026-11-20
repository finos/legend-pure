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

package org.finos.legend.pure.m4.logs;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestAbstractPureLogger
{
    @Test
    public void testFormattedMessage()
    {
        TestLogger logger = new TestLogger();
        logger.log("Loaded %,d files in %.3fs", 1234567, 1.5);
        logger.log(new RuntimeException(), "Failed after %d attempts", 3);
        Assert.assertEquals(Lists.mutable.with("Loaded 1,234,567 files in 1.500s", "Failed after 3 attempts"), logger.messages);
    }

    @Test
    public void testFormattedMessageWithDefaultLocale()
    {
        // With a default locale such as de-DE, String.format writes a comma as the decimal separator and a period
        // between groups of digits; with ar-EG, it writes Arabic-Indic digits
        for (String languageTag : new String[]{"de-DE", "ar-EG"})
        {
            TestLogger logger = new TestLogger();
            Locale defaultLocale = Locale.getDefault();
            Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
            Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
            Locale.setDefault(Locale.forLanguageTag(languageTag));
            try
            {
                logger.log("Loaded %,d files in %.3fs", 1234567, 1.5);
                logger.log(new RuntimeException(), "Failed after %d attempts", 3);
            }
            finally
            {
                Locale.setDefault(defaultLocale);
                Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
                Locale.setDefault(Locale.Category.FORMAT, formatLocale);
            }
            Assert.assertEquals(languageTag, Lists.mutable.with("Loaded 1,234,567 files in 1.500s", "Failed after 3 attempts"), logger.messages);
        }
    }

    private static class TestLogger extends AbstractPureLogger
    {
        private final MutableList<String> messages = Lists.mutable.empty();

        @Override
        public void log(String message)
        {
            this.messages.add(message);
        }

        @Override
        public void log(Throwable t, String message)
        {
            this.messages.add(message);
        }
    }
}
