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

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestTimePrinter
{
    private static final long DELTA = 3_723_004_005_006L;

    @Test
    public void testMakeItHuman()
    {
        Assert.assertEquals("0:00:00.000.000.000", TimePrinter.makeItHuman(0L));
        Assert.assertEquals("1:02:03.004.005.006", TimePrinter.makeItHuman(DELTA));
    }

    @Test
    public void testMakeItHumanWithArabicDefaultLocale()
    {
        // With a default locale such as ar-EG, String.format writes Arabic-Indic digits
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        String result;
        try
        {
            result = TimePrinter.makeItHuman(DELTA);
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
        Assert.assertEquals("1:02:03.004.005.006", result);
    }
}
