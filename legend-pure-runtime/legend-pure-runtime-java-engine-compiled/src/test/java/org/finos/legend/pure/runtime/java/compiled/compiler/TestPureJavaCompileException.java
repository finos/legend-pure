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

package org.finos.legend.pure.runtime.java.compiled.compiler;

import org.eclipse.collections.api.factory.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class TestPureJavaCompileException
{
    @Test
    public void testSourceLines()
    {
        assertSourceLines(createException());
    }

    @Test
    public void testSourceLinesWithArabicDefaultLocale()
    {
        // With a default locale such as ar-EG, String.format writes Arabic-Indic digits
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        PureJavaCompileException e;
        try
        {
            e = createException();
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
        assertSourceLines(e);
    }

    private static PureJavaCompileException createException()
    {
        StringBuilder code = new StringBuilder("package test.locale;\n");
        for (int i = 2; i <= 40; i++)
        {
            code.append("// line ").append(i).append('\n');
        }
        StringJavaSource source = StringJavaSource.newStringJavaSource("test.locale", "Example", code.toString(), false);
        return new PureJavaCompileException(Lists.mutable.<Diagnostic<? extends JavaFileObject>>with(new TestDiagnostic(source, 30)));
    }

    private static void assertSourceLines(PureJavaCompileException e)
    {
        String message = e.getMessage();
        // the lines before the context are elided, and the context is numbered
        assertContains("\n0001:0009 ...\n", message);
        assertContains("\n0010 // line 10\n", message);
        assertContains("\n0030 // line 30\n", message);
    }

    private static void assertContains(String expected, String actual)
    {
        if ((actual == null) || !actual.contains(expected))
        {
            Assert.fail("Expected to find \"" + expected + "\" in: " + actual);
        }
    }

    private static class TestDiagnostic implements Diagnostic<JavaFileObject>
    {
        private final JavaFileObject source;
        private final long lineNumber;

        private TestDiagnostic(JavaFileObject source, long lineNumber)
        {
            this.source = source;
            this.lineNumber = lineNumber;
        }

        @Override
        public Kind getKind()
        {
            return Kind.ERROR;
        }

        @Override
        public JavaFileObject getSource()
        {
            return this.source;
        }

        @Override
        public long getPosition()
        {
            return NOPOS;
        }

        @Override
        public long getStartPosition()
        {
            return NOPOS;
        }

        @Override
        public long getEndPosition()
        {
            return NOPOS;
        }

        @Override
        public long getLineNumber()
        {
            return this.lineNumber;
        }

        @Override
        public long getColumnNumber()
        {
            return 1;
        }

        @Override
        public String getCode()
        {
            return "test.error";
        }

        @Override
        public String getMessage(Locale locale)
        {
            return "test error";
        }

        @Override
        public String toString()
        {
            return this.source.getName() + ":" + this.lineNumber + ": error: test error";
        }
    }
}
