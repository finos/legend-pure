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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Test;

public class TestStringParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testSimpleString()
    {
        assertParsesTo("the quick brown fox jumps over the lazy dog", "'the quick brown fox jumps over the lazy dog'");
    }

    @Test
    public void testStringWithEscapedQuotes()
    {
        assertParsesTo("the quick brown 'fox' jumps over the lazy 'dog'", "'the quick brown \\'fox\\' jumps over the lazy \\'dog\\''");
    }

    @Test
    public void testStringWithDoubleQuotes()
    {
        assertParsesTo("the quick brown \"fox\" jumps over the lazy \"dog\"", "'the quick brown \"fox\" jumps over the lazy \"dog\"'");
    }

    @Test
    public void testStringWithNewline()
    {
        assertParsesTo("the quick brown fox\njumps over the lazy dog", "'the quick brown fox\\njumps over the lazy dog'");
    }

    @Test
    public void testStringWithEscapedSlash()
    {
        assertParsesTo("the quick brown fox \\ jumps over the lazy dog", "'the quick brown fox \\\\ jumps over the lazy dog'");
    }

    @Test
    public void testEscapedSlashThenQuoteThenTextThenQuote()
    {
        assertFailsToParse("'\\\\'outside quote'"); // '\\'outside quote'
    }

    @Test
    public void testStringWithUnescapedQuote()
    {
        assertFailsToParse("'''");
    }

    @Test
    public void testStringWithOnlyEscapedQuote()
    {
        assertParsesTo("'", "'\\''");
    }

    @Test
    public void testStringWithOnlyEscapedSlash()
    {
        assertParsesTo("\\", "'\\\\'");
    }

    @Test
    public void testStringWithEscapedSlashAndEscapedQuote()
    {
        assertParsesTo("\\'", "'\\\\\\''");
    }

    @Test
    public void testStringWithEscapedSlashAndUnescapedQuote()
    {
        assertFailsToParse("'\\\\''");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.STRING_TYPE_NAME;
    }
}
