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

package org.finos.legend.pure.lsp;

import org.eclipse.lsp4j.CompletionItem;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Tests completion using a real PureRuntime.
 */
public class CompletionProviderTest
{
    private static LegendPureSession session;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        LegendPureSession.CompileResult result = session.modifyAndCompile(
                "completion_test_class.pure",
                "Class test::completion::Invoice\n{\n  number: String[1];\n}\n"
        );
        Assert.assertTrue("Invoice class should compile: " +
                (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    // In Turkish, "I".toLowerCase() is a dotless i (U+0131), so matching a prefix must not depend on the default locale

    @Test
    public void packagePathCompletion_withTurkishDefaultLocale()
    {
        String content = "test::completion::inv";
        List<String> labels = withTurkishDefaultLocale(() -> getCompletionLabels(content, 1, content.length()));
        Assert.assertEquals(Collections.singletonList("Invoice"), labels);
    }

    @Test
    public void identifierCompletion_withTurkishDefaultLocale()
    {
        String content = "import test::completion::*;\ninv";
        List<String> labels = withTurkishDefaultLocale(() -> getCompletionLabels(content, 2, 3));
        Assert.assertTrue("Should complete Invoice, found " + labels, labels.contains("Invoice"));
    }

    private static List<String> getCompletionLabels(String content, int line, int column)
    {
        return CompletionProvider.getCompletions(session.getPureRuntime(), "completion_test_user.pure", content, line, column)
                .stream()
                .map(CompletionItem::getLabel)
                .collect(Collectors.toList());
    }

    private static <T> T withTurkishDefaultLocale(Supplier<T> supplier)
    {
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try
        {
            return supplier.get();
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
    }
}
