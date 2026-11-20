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

package org.finos.legend.pure.runtime.java.shared.http;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestHttpRawHelper
{
    @Test
    public void testInvalidUrl()
    {
        // a space is not allowed in a host name
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> HttpRawHelper.executeHttpService(URLScheme.http, "bad host", 8080, "/path", HttpMethod.GET, "text/plain", null));
        Assert.assertEquals("Cannot build URI from url (bad host, 8080, /path)", e.getMessage());
    }

    @Test
    public void testInvalidUrlWithArabicDefaultLocale()
    {
        // With a default locale such as ar-EG, String.format writes Arabic-Indic digits
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        RuntimeException e;
        try
        {
            e = Assert.assertThrows(RuntimeException.class, () -> HttpRawHelper.executeHttpService(URLScheme.http, "bad host", 8080, "/path", HttpMethod.GET, "text/plain", null));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
        Assert.assertEquals("Cannot build URI from url (bad host, 8080, /path)", e.getMessage());
    }
}
