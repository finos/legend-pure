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

package org.finos.legend.pure.runtime.java.compiled.testHelper;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TestIgnoreUnsupportedApiPureTestSuiteRunner
{
    @Test
    public void testUnsupportedApiInUpperCaseWithTurkishDefaultLocale() throws Throwable
    {
        // In Turkish, "I".toLowerCase() is a dotless i (U+0131), so recognizing the marker must not depend on the
        // default locale
        List<Failure> assumptionFailures = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(new RunListener()
        {
            @Override
            public void testAssumptionFailure(Failure failure)
            {
                assumptionFailures.add(failure);
            }

            @Override
            public void testFailure(Failure failure)
            {
                failures.add(failure);
            }
        });

        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try
        {
            new IgnoreUnsupportedApiPureTestSuiteRunner(UnsupportedApiSuite.class).run(notifier);
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }

        Assert.assertEquals(new ArrayList<>(), failures);
        Assert.assertEquals(1, assumptionFailures.size());
    }

    public static class UnsupportedApiSuite
    {
        public static junit.framework.Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest(new TestCase("unsupportedApi")
            {
                @Override
                protected void runTest()
                {
                    throw new PureExecutionException("[UNSUPPORTED-API] not supported here");
                }
            });
            return suite;
        }
    }
}
