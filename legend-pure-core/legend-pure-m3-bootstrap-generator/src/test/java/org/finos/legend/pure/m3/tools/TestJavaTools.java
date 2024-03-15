// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

public class TestJavaTools
{
    @Test
    public void testMakeValidJavaIdentifier_emptyStrings()
    {
        ImmutableList<String> validIdentifiers = Lists.immutable.with("$", "_ab", "__", "abc");
        ImmutableList<String> invalidIdentifiers = Lists.immutable.with("123", "%#@", "$#", "assert", "public", "class");

        for (String empty : new String[]{null, ""})
        {
            for (String validIdentifier : validIdentifiers)
            {
                Assert.assertEquals(empty, validIdentifier, JavaTools.makeValidJavaIdentifier(empty, validIdentifier));
            }
            for (String invalidIdentifier : invalidIdentifiers)
            {
                IllegalStateException e = Assert.assertThrows(empty, IllegalStateException.class, () -> JavaTools.makeValidJavaIdentifier(empty, invalidIdentifier));
                Assert.assertEquals(empty, "Cannot replace null or empty string with \"" + invalidIdentifier + "\", as it is not a valid Java identifier", e.getMessage());
            }
        }
    }

    @Test
    public void testMakeValidJavaIdentifier_validIdentifiers()
    {
        ImmutableList<String> fixStrings = Lists.immutable.with(
                "$", "_", "_ab", "abc", "$$$", "X", // valid start
                "123", "1", "7_8_9_a", // valid part
                null, "", ":##", "_%", "@", // invalid part
                "1", "1a", "99_100" // invalid start
        );

        for (String string : new String[]{"$", "_ab", "__", "abc"})
        {
            for (String fix : fixStrings)
            {
                Assert.assertSame(string + " / " + fix, string, JavaTools.makeValidJavaIdentifier(string, fix));
            }
        }
    }

    @Test
    public void testMakeValidJavaIdentifier_keywords()
    {
        for (String keyword : new String[] {"assert", "class", "public", "private"})
        {
            Assert.assertEquals("$" + keyword, JavaTools.makeValidJavaIdentifier(keyword, "$"));
            Assert.assertEquals(keyword + "1", JavaTools.makeValidJavaIdentifier(keyword, "1"));

            IllegalStateException e = Assert.assertThrows(IllegalStateException.class, () -> JavaTools.makeValidJavaIdentifier(keyword, "%%%"));
            Assert.assertEquals("\"" + keyword + "\" is a Java keyword, but fix (\"%%%\") is not a valid Java identifier part", e.getMessage());
        }
    }

    @Test
    public void testMakeValidJavaIdentifier_nonKeywordInvalidIdentifiers()
    {
        Assert.assertEquals("_123", JavaTools.makeValidJavaIdentifier("123", "_"));
        Assert.assertEquals("$$1_a", JavaTools.makeValidJavaIdentifier("1_a", "$$"));

        Assert.assertEquals("abc$def", JavaTools.makeValidJavaIdentifier("abc%def", "$"));
        Assert.assertEquals("abc_X_def", JavaTools.makeValidJavaIdentifier("abc%def", "_X_"));
        Assert.assertEquals("abc$_Y_$def$_Y_$ghi", JavaTools.makeValidJavaIdentifier("abc%def%ghi", "$_Y_$"));

        Assert.assertEquals("abc$", JavaTools.makeValidJavaIdentifier("abc%", "$"));
        Assert.assertEquals("a35____", JavaTools.makeValidJavaIdentifier("a35%%", "__"));

        IllegalStateException e1 = Assert.assertThrows(IllegalStateException.class, () -> JavaTools.makeValidJavaIdentifier("123", "9"));
        Assert.assertEquals("First character of \"123\" needs to be replaced, but replacement (\"9\") is not a valid Java identifier start", e1.getMessage());
        IllegalStateException e2 = Assert.assertThrows(IllegalStateException.class, () -> JavaTools.makeValidJavaIdentifier("ab%", "##"));
        Assert.assertEquals("Character 2 of \"ab%\" needs to be replaced, but replacement (\"##\") is not a valid Java identifier part", e2.getMessage());
    }
}
