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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Locale;
import java.util.Map;

public class TestExecutedTestTracker
{
    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testSummaryOfImpactedTests()
    {
        Assert.assertEquals(expected(), summarize());
    }

    @Test
    public void testSummaryOfImpactedTestsWithArabicDefaultLocale()
    {
        // With a default locale such as ar-EG, NumberFormat writes Arabic-Indic digits and an Arabic percent sign
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        Map<?, ?> summary;
        try
        {
            summary = summarize();
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
        Assert.assertEquals(expected(), summary);
    }

    private Map<?, ?> summarize()
    {
        ExecutedTestTracker tracker = new ExecutedTestTracker(this.tmp.getRoot().toPath().resolve("tests.json"), "userDb");
        tracker.notePassingTests(Sets.mutable.with("test1"));
        tracker.noteFailingTests(Sets.mutable.with("test2"));
        MutableSet<String> impactedTests = Sets.mutable.with("test1", "test2", "test3", "test4");
        return tracker.summaryOfImpactedTests(impactedTests);
    }

    private static Map<String, String> expected()
    {
        MutableMap<String, String> expected = Maps.mutable.empty();
        expected.put("Passed", "25%");
        expected.put("Failed", "25%");
        expected.put("NotYetExecuted", "50%");
        return expected;
    }
}
