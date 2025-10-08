// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m4.tools;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestTextTools
{
    @Test
    public void testIndexOfWhitespace()
    {
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(""));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace("no_whitespace_here"));

        String text = "start of whitespace{ \u2028\u2029\t\n\u000B\f\r\u001C\u001D\u001E\u001F}end of whitespace";
        Assert.assertEquals(5, TextTools.indexOfWhitespace(text));
        Assert.assertEquals(8, TextTools.indexOfWhitespace(text, 6));
        for (int i = 9; i < 20; i++)
        {
            Assert.assertEquals(Integer.toString(i), 20, TextTools.indexOfWhitespace(text, i));
        }
        for (int i = 20; i < 32; i++)
        {
            Assert.assertEquals(Integer.toString(i), i, TextTools.indexOfWhitespace(text, i));
        }
        Assert.assertEquals(36, TextTools.indexOfWhitespace(text, 32));
        Assert.assertEquals(39, TextTools.indexOfWhitespace(text, 37));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(text, 40));
    }

    @Test
    public void testIndexOfNonWhitespace()
    {
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(""));
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(" \u2028\u2029\t\n\u000B\f\r\u001C\u001D\u001E\u001F"));

        String text = "start of whitespace{ \u2028\u2029\t\n\u000B\f\r\u001C\u001D\u001E\u001F}end of whitespace";
        for (int i = 0; i < 5; i++)
        {
            Assert.assertEquals(Integer.toString(i), i, TextTools.indexOfNonWhitespace(text, i));
        }
        Assert.assertEquals(6, TextTools.indexOfNonWhitespace(text, 5));
        for (int i = 6; i < 8; i++)
        {
            Assert.assertEquals(Integer.toString(i), i, TextTools.indexOfNonWhitespace(text, i));
        }
        Assert.assertEquals(9, TextTools.indexOfNonWhitespace(text, 8));
        for (int i = 9; i < 20; i++)
        {
            Assert.assertEquals(Integer.toString(i), i, TextTools.indexOfNonWhitespace(text, i));
        }
        Assert.assertEquals(32, TextTools.indexOfNonWhitespace(text, 20));
    }

    @Test
    public void testIsBlank()
    {
        assertIsBlank("", 0, 0);

        String text = "start of whitespace{ \u2028\u2029\t\n\u000B\f\r\u001C\u001D\u001E\u001F}end of whitespace";
        assertIsNotBlank(text, 0, text.length());
        assertIsNotBlank(text, 0, 20);
        assertIsNotBlank(text, 25, text.length());
        assertIsBlank(text, 5, 6);
        assertIsBlank(text, 20, 32);

        String qbf = "the quick\tbrown\nfox\rjumps over the lazy dog";
        for (int i = 0; i < qbf.length(); i++)
        {
            assertIsBlank(qbf, i, i);
        }
        assertIsNotBlank(qbf, 0, qbf.length());
        assertIsNotBlank(qbf, 0, 4);
        assertIsNotBlank(qbf, 2, 4);
        assertIsBlank(qbf, 3, 4);
        assertIsBlank(qbf, 9, 10);
        assertIsBlank(qbf, 15, 16);
        assertIsBlank(qbf, 19, 20);
    }

    @Test
    public void testToLowerCase()
    {
        Assert.assertEquals("tHE QUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0));
        Assert.assertEquals("the QUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, 3));
        Assert.assertEquals("the QUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, 4));
        Assert.assertEquals("the qUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, 5));

        Assert.assertEquals("THE QUICK bROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 10));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 3, 15));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 3, 16));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 4, 15));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 4, 16));

        Arrays.asList("no caps", "12345", "    ", "\n").forEach(s ->
        {
            Assert.assertSame(s, TextTools.toLowerCase(s, 0));
            Assert.assertSame(s, TextTools.toLowerCase(s, 0, s.length()));
        });
    }

    @Test
    public void testToUpperCase()
    {
        Assert.assertEquals("The quick brown fox", TextTools.toUpperCase("the quick brown fox", 0));
        Assert.assertEquals("THE quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, 3));
        Assert.assertEquals("THE quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, 4));
        Assert.assertEquals("THE Quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, 5));

        Assert.assertEquals("the quick Brown fox", TextTools.toUpperCase("the quick brown fox", 10));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 3, 15));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 3, 16));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 4, 15));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 4, 16));

        Arrays.asList("NO LOWER", "12345", "    ", "\n\t\b\r\n").forEach(s ->
        {
            Assert.assertSame(s, TextTools.toUpperCase(s, 0));
            Assert.assertSame(s, TextTools.toUpperCase(s, 0, s.length()));
        });
    }

    private void assertIsNotBlank(String string, int start, int end)
    {
        Assert.assertFalse(TextTools.isBlank(string, start, end));
    }

    private void assertIsBlank(String string, int start, int end)
    {
        if (!TextTools.isBlank(string, start, end))
        {
            Assert.fail("isBlank(\"" + string + "\", " + start + ", " + end + "); non-whitespace index: " + TextTools.indexOfNonWhitespace(string, start, end));
        }
    }
}
