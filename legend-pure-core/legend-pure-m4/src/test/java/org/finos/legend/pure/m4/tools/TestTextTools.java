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

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.junit.Assert;
import org.junit.Test;

import javax.lang.model.SourceVersion;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntPredicate;

public class TestTextTools
{
    // Supplementary (non-BMP) characters. Each is two chars wide, so char indices and code point counts diverge.
    private static final String AVESTAN_A = "\uD802\uDF00";      // U+10B00 AVESTAN LETTER A
    private static final String AVESTAN_AA = "\uD802\uDF01";     // U+10B01 AVESTAN LETTER AA
    private static final String LINEAR_A_AB001 = "\uD801\uDE00"; // U+10600 LINEAR A SIGN AB001
    private static final String LINEAR_B_A = "\uD800\uDC00";     // U+10000 LINEAR B SYLLABLE B008 A
    private static final String LINEAR_B_E = "\uD800\uDC01";     // U+10001 LINEAR B SYLLABLE B038 E

    private static final int AVESTAN_A_CP = 0x10B00;
    private static final int AVESTAN_AA_CP = 0x10B01;
    private static final int LINEAR_A_AB001_CP = 0x10600;
    private static final int LINEAR_B_A_CP = 0x10000;

    // The two halves of AVESTAN_A, for checking regions that split it
    private static final char AVESTAN_A_HIGH = '\uD802';
    private static final char AVESTAN_A_LOW = '\uDF00';

    // Avestan, Linear A and Linear B are caseless, so the case conversion tests use Deseret, one of the few
    // supplementary scripts that does have case mappings.
    private static final String DESERET_LONG_I_UPPER = "\uD801\uDC00"; // U+10400 DESERET CAPITAL LETTER LONG I
    private static final String DESERET_LONG_I_LOWER = "\uD801\uDC28"; // U+10428 DESERET SMALL LETTER LONG I

    // Twelve chars, covering the space, separator and control cases that Character.isWhitespace recognises.
    private static final String ALL_WHITESPACE = " \u2028\u2029\t\n\u000B\f\r\u001C\u001D\u001E\u001F";

    // "start of whitespace{"(0-19) ALL_WHITESPACE(20-31) "}end of whitespace"(32-49)
    private static final String WS_TEXT = "start of whitespace{" + ALL_WHITESPACE + "}end of whitespace";

    // AVESTAN_A(0-1) SPACE(2) LINEAR_B_A(3-4) TAB(5) LINEAR_A_AB001(6-7)
    private static final String SUPP_WS_TEXT = AVESTAN_A + " " + LINEAR_B_A + "\t" + LINEAR_A_AB001;

    // 'a'(0) AVESTAN_A(1-2) 'b'(3): a region boundary at index 2 splits the surrogate pair
    private static final String SPLIT_TEXT = "a" + AVESTAN_A + "b";

    // 'x'(0) lone-high(1) 'y'(2) lone-low(3) 'z'(4) AVESTAN_A(5-6)
    private static final String UNPAIRED_TEXT = "x" + AVESTAN_A_HIGH + "y" + AVESTAN_A_LOW + "z" + AVESTAN_A;

    // 'a'(0) AVESTAN_A(1-2) SPACE(3) LINEAR_B_A(4-5) TAB(6) 'z'(7) LINEAR_A_AB001(8-9)
    private static final String MIXED_TEXT = "a" + AVESTAN_A + " " + LINEAR_B_A + "\tz" + LINEAR_A_AB001;

    private static final String QBF = "the quick\tbrown\nfox\rjumps over the lazy dog";

    private static final List<String> TRAVERSAL_SAMPLES = Arrays.asList(
            "",
            "a",
            AVESTAN_A,
            "\uD802",
            "\uDF00",
            "\uDF00\uD802",
            SPLIT_TEXT,
            UNPAIRED_TEXT,
            MIXED_TEXT,
            SUPP_WS_TEXT,
            WS_TEXT);

    private static final IntPredicate NEVER = cp -> false;
    private static final IntPredicate ALWAYS = cp -> true;

    // indexOf / lastIndexOf with an arbitrary predicate

    @Test
    public void testIndexOfPredicate()
    {
        Assert.assertEquals(-1, TextTools.indexOf("", ALWAYS));
        Assert.assertEquals(-1, TextTools.indexOf("abc", NEVER));
        Assert.assertEquals(0, TextTools.indexOf("abc", ALWAYS));

        String text = "abcabc";
        Assert.assertEquals(0, TextTools.indexOf(text, cp -> cp == 'a'));
        Assert.assertEquals(1, TextTools.indexOf(text, cp -> cp == 'b'));
        Assert.assertEquals(2, TextTools.indexOf(text, cp -> cp == 'c'));
        Assert.assertEquals(-1, TextTools.indexOf(text, cp -> cp == 'd'));

        // two arg overload
        Assert.assertEquals(3, TextTools.indexOf(text, 1, cp -> cp == 'a'));
        Assert.assertEquals(4, TextTools.indexOf(text, 2, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.indexOf(text, 5, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.indexOf(text, text.length(), ALWAYS));

        // three arg overload
        Assert.assertEquals(1, TextTools.indexOf(text, 0, 3, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.indexOf(text, 0, 1, cp -> cp == 'b'));
        Assert.assertEquals(4, TextTools.indexOf(text, 3, 6, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.indexOf(text, 2, 4, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.indexOf(text, 3, 3, ALWAYS));
    }

    @Test
    public void testLastIndexOfPredicate()
    {
        Assert.assertEquals(-1, TextTools.lastIndexOf("", ALWAYS));
        Assert.assertEquals(-1, TextTools.lastIndexOf("abc", NEVER));
        Assert.assertEquals(2, TextTools.lastIndexOf("abc", ALWAYS));

        // the index of the matching character is returned, not the index just past it
        Assert.assertEquals(2, TextTools.lastIndexOf("abc", cp -> cp == 'c'));

        String text = "abcabc";
        Assert.assertEquals(3, TextTools.lastIndexOf(text, cp -> cp == 'a'));
        Assert.assertEquals(4, TextTools.lastIndexOf(text, cp -> cp == 'b'));
        Assert.assertEquals(5, TextTools.lastIndexOf(text, cp -> cp == 'c'));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, cp -> cp == 'd'));

        // two arg overload
        Assert.assertEquals(3, TextTools.lastIndexOf(text, 3, cp -> cp == 'a'));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, 4, cp -> cp == 'a'));
        Assert.assertEquals(4, TextTools.lastIndexOf(text, 4, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, text.length(), ALWAYS));

        // three arg overload
        Assert.assertEquals(1, TextTools.lastIndexOf(text, 0, 3, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, 0, 1, cp -> cp == 'b'));
        Assert.assertEquals(4, TextTools.lastIndexOf(text, 3, 6, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, 2, 4, cp -> cp == 'b'));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, 3, 3, ALWAYS));
    }

    @Test
    public void testIndexOfPredicateSupplementary()
    {
        // whole supplementary characters are presented to the predicate, and the index of the first char is returned
        Assert.assertEquals(0, TextTools.indexOf(SUPP_WS_TEXT, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(3, TextTools.indexOf(SUPP_WS_TEXT, cp -> cp == LINEAR_B_A_CP));
        Assert.assertEquals(6, TextTools.indexOf(SUPP_WS_TEXT, cp -> cp == LINEAR_A_AB001_CP));
        Assert.assertEquals(0, TextTools.indexOf(SUPP_WS_TEXT, Character::isLetter));
        Assert.assertEquals(1, TextTools.indexOf(MIXED_TEXT, cp -> cp > Character.MAX_VALUE));
        Assert.assertEquals(8, TextTools.indexOf(MIXED_TEXT, cp -> cp == LINEAR_A_AB001_CP));

        // halves of a well formed surrogate pair are never presented to the predicate
        Assert.assertEquals(-1, TextTools.indexOf(SUPP_WS_TEXT, TestTextTools::isSurrogate));
        Assert.assertEquals(-1, TextTools.lastIndexOf(SUPP_WS_TEXT, TestTextTools::isSurrogate));
        Assert.assertEquals(-1, TextTools.indexOf(MIXED_TEXT, TestTextTools::isSurrogate));
        Assert.assertEquals(-1, TextTools.lastIndexOf(MIXED_TEXT, TestTextTools::isSurrogate));

        // starting or ending mid character
        Assert.assertEquals(-1, TextTools.indexOf(SUPP_WS_TEXT, 1, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(1, TextTools.indexOf(SUPP_WS_TEXT, 1, TestTextTools::isSurrogate));
        Assert.assertEquals(-1, TextTools.indexOf(SUPP_WS_TEXT, 0, 1, cp -> cp == AVESTAN_A_CP));
    }

    @Test
    public void testLastIndexOfPredicateSupplementary()
    {
        Assert.assertEquals(0, TextTools.lastIndexOf(SUPP_WS_TEXT, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(3, TextTools.lastIndexOf(SUPP_WS_TEXT, cp -> cp == LINEAR_B_A_CP));
        Assert.assertEquals(6, TextTools.lastIndexOf(SUPP_WS_TEXT, cp -> cp == LINEAR_A_AB001_CP));
        if (SourceVersion.latest().compareTo(SourceVersion.RELEASE_8) > 0)
        {
            // Java 8 does not properly handle supplemental characters in Character.isLetter
            Assert.assertEquals(6, TextTools.lastIndexOf(SUPP_WS_TEXT, Character::isLetter));
        }
        Assert.assertEquals(8, TextTools.lastIndexOf(MIXED_TEXT, cp -> cp > Character.MAX_VALUE));

        // the index of the first char of the character is returned, not the index just past its last char
        String doubled = AVESTAN_A + AVESTAN_A;
        Assert.assertEquals(0, TextTools.indexOf(doubled, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(2, TextTools.lastIndexOf(doubled, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(0, TextTools.lastIndexOf(doubled, 0, 2, cp -> cp == AVESTAN_A_CP));

        // adjacent supplementary characters are told apart
        String adjacent = AVESTAN_A + AVESTAN_AA;
        Assert.assertEquals(0, TextTools.indexOf(adjacent, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(2, TextTools.indexOf(adjacent, cp -> cp == AVESTAN_AA_CP));
        Assert.assertEquals(0, TextTools.lastIndexOf(adjacent, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(2, TextTools.lastIndexOf(adjacent, cp -> cp == AVESTAN_AA_CP));

        // ending mid character
        Assert.assertEquals(-1, TextTools.lastIndexOf(SUPP_WS_TEXT, 0, 7, cp -> cp == LINEAR_A_AB001_CP));
        Assert.assertEquals(3, TextTools.lastIndexOf(SUPP_WS_TEXT, 0, 7, cp -> cp == LINEAR_B_A_CP));
    }

    @Test
    public void testSurrogatePairStraddlingRegionStartOrEnd()
    {
        // SPLIT_TEXT is 'a' AVESTAN_A 'b'; the pair occupies indices 1 and 2

        // scanned as one character when the region contains both chars
        Assert.assertEquals(1, TextTools.indexOf(SPLIT_TEXT, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(1, TextTools.lastIndexOf(SPLIT_TEXT, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(1, TextTools.indexOf(SPLIT_TEXT, 1, 3, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(1, TextTools.lastIndexOf(SPLIT_TEXT, 1, 3, cp -> cp == AVESTAN_A_CP));

        // a pair straddling the end of the region: only the high surrogate is in the region, so that is what the
        // predicate sees, and the char at index 2 is not read
        Assert.assertEquals(-1, TextTools.indexOf(SPLIT_TEXT, 0, 2, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(-1, TextTools.lastIndexOf(SPLIT_TEXT, 0, 2, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(1, TextTools.indexOf(SPLIT_TEXT, 0, 2, cp -> cp == AVESTAN_A_HIGH));
        Assert.assertEquals(1, TextTools.lastIndexOf(SPLIT_TEXT, 0, 2, cp -> cp == AVESTAN_A_HIGH));

        // a pair straddling the start of the region: only the low surrogate is in the region, and the char at
        // index 1 is not read
        Assert.assertEquals(-1, TextTools.indexOf(SPLIT_TEXT, 2, 4, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(-1, TextTools.lastIndexOf(SPLIT_TEXT, 2, 4, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(2, TextTools.indexOf(SPLIT_TEXT, 2, 4, cp -> cp == AVESTAN_A_LOW));
        Assert.assertEquals(2, TextTools.lastIndexOf(SPLIT_TEXT, 2, 4, cp -> cp == AVESTAN_A_LOW));

        // consequently a character is never found twice when a string is scanned in two halves
        for (int split = 0; split <= SPLIT_TEXT.length(); split++)
        {
            boolean inFirst = TextTools.indexOf(SPLIT_TEXT, 0, split, cp -> cp == AVESTAN_A_CP) != -1;
            boolean inSecond = TextTools.indexOf(SPLIT_TEXT, split, SPLIT_TEXT.length(), cp -> cp == AVESTAN_A_CP) != -1;
            Assert.assertFalse("split " + split, inFirst && inSecond);
        }
    }

    @Test
    public void testUnpairedSurrogates()
    {
        // UNPAIRED_TEXT is 'x' lone-high 'y' lone-low 'z' AVESTAN_A
        Assert.assertEquals(1, TextTools.indexOf(UNPAIRED_TEXT, TestTextTools::isSurrogate));
        Assert.assertEquals(3, TextTools.lastIndexOf(UNPAIRED_TEXT, TestTextTools::isSurrogate));
        Assert.assertEquals(1, TextTools.indexOf(UNPAIRED_TEXT, cp -> cp == AVESTAN_A_HIGH));
        Assert.assertEquals(3, TextTools.indexOf(UNPAIRED_TEXT, cp -> cp == AVESTAN_A_LOW));
        Assert.assertEquals(5, TextTools.indexOf(UNPAIRED_TEXT, cp -> cp == AVESTAN_A_CP));
        Assert.assertEquals(5, TextTools.lastIndexOf(UNPAIRED_TEXT, cp -> cp == AVESTAN_A_CP));

        // a low surrogate immediately followed by a high surrogate is two unpaired surrogates, not a character
        String reversedPair = "\uDF00\uD802";
        Assert.assertEquals(0, TextTools.indexOf(reversedPair, TestTextTools::isSurrogate));
        Assert.assertEquals(1, TextTools.lastIndexOf(reversedPair, TestTextTools::isSurrogate));
        Assert.assertEquals(-1, TextTools.indexOf(reversedPair, cp -> cp > Character.MAX_VALUE));
    }

    /**
     * Check that both scan directions partition every region of every sample string into exactly the same sequence of
     * code points, in ascending and descending order respectively, and that no char outside the region is ever read.
     */
    @Test
    public void testTraversalPartitionsEveryRegion()
    {
        TRAVERSAL_SAMPLES.forEach(text ->
        {
            for (int start = 0; start <= text.length(); start++)
            {
                for (int end = start; end <= text.length(); end++)
                {
                    String message = "\"" + escape(text) + "\" [" + start + ", " + end + ")";
                    MutableIntList expected = expectedCodePoints(text, start, end);

                    Assert.assertEquals(message, expected, collect(text, start, end, true));
                    Assert.assertEquals(message, expected.toReversed(), collect(text, start, end, false));

                    // the code points visited reconstruct the region exactly: nothing is skipped, duplicated, or
                    // read from outside the region
                    StringBuilder builder = new StringBuilder(end - start);
                    expected.each(builder::appendCodePoint);
                    Assert.assertEquals(message, text.substring(start, end), builder.toString());

                    // the index returned is the index of the first char of the matching character
                    assertMatchIndices(message, text, start, end, expected);
                }
            }
        });
    }

    @Test
    public void testIndexOfStopsAtFirstMatch()
    {
        int[] calls = {0};
        Assert.assertEquals(1, TextTools.indexOf("abcabc", cp ->
        {
            calls[0]++;
            return cp == 'b';
        }));
        Assert.assertEquals(2, calls[0]);

        calls[0] = 0;
        Assert.assertEquals(4, TextTools.lastIndexOf("abcabc", cp ->
        {
            calls[0]++;
            return cp == 'b';
        }));
        Assert.assertEquals(2, calls[0]);
    }

    @Test
    public void testEmptyRegionNeverTestsPredicate()
    {
        IntPredicate boom = cp ->
        {
            throw new AssertionError("predicate applied to " + cp);
        };

        Assert.assertEquals(-1, TextTools.indexOf("", boom));
        Assert.assertEquals(-1, TextTools.lastIndexOf("", boom));
        Assert.assertEquals(-1, TextTools.indexOf("abc", 3, boom));
        Assert.assertEquals(-1, TextTools.lastIndexOf("abc", 3, boom));
        for (int i = 0; i <= 3; i++)
        {
            Assert.assertEquals(-1, TextTools.indexOf("abc", i, i, boom));
            Assert.assertEquals(-1, TextTools.lastIndexOf("abc", i, i, boom));
        }
    }

    @Test
    public void testRegionBounds()
    {
        String text = "abc";
        int length = text.length();

        assertBadRegion(text, -1, 0);
        assertBadRegion(text, -1, length);
        assertBadRegion(text, 0, -1);
        assertBadRegion(text, 0, length + 1);
        assertBadRegion(text, length + 1, length + 1);
        assertBadRegion(text, 2, 1);
        assertBadRegion("", 0, 1);
        assertBadRegion("", 1, 1);

        // start == end == length is a valid empty region
        Assert.assertEquals(-1, TextTools.indexOf(text, length, length, ALWAYS));
        Assert.assertEquals(-1, TextTools.lastIndexOf(text, length, length, ALWAYS));
        Assert.assertTrue(TextTools.isBlank(text, length, length));
    }

    // indexOfWhitespace

    @Test
    public void testIndexOfWhitespace()
    {
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(""));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace("no_whitespace_here"));
        Assert.assertEquals(0, TextTools.indexOfWhitespace(ALL_WHITESPACE));

        String text = WS_TEXT;
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
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(text, text.length()));

        // three arg overload
        Assert.assertEquals(5, TextTools.indexOfWhitespace(text, 0, text.length()));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(text, 0, 5));
        Assert.assertEquals(5, TextTools.indexOfWhitespace(text, 0, 6));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(text, 21, 21));
    }

    @Test
    public void testIndexOfWhitespaceSupplementary()
    {
        // no supplementary code point is whitespace, so supplementary characters are simply skipped over
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(AVESTAN_A));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(LINEAR_A_AB001 + LINEAR_B_A + AVESTAN_AA));
        Assert.assertEquals(2, TextTools.indexOfWhitespace(SUPP_WS_TEXT));
        Assert.assertEquals(2, TextTools.indexOfWhitespace(SUPP_WS_TEXT, 1));
        Assert.assertEquals(5, TextTools.indexOfWhitespace(SUPP_WS_TEXT, 3));
        Assert.assertEquals(5, TextTools.indexOfWhitespace(SUPP_WS_TEXT, 4));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(SUPP_WS_TEXT, 6));
        Assert.assertEquals(2, TextTools.indexOfWhitespace(SUPP_WS_TEXT, 0, 3));
        Assert.assertEquals(-1, TextTools.indexOfWhitespace(SUPP_WS_TEXT, 0, 2));
    }

    // indexOfNonWhitespace

    @Test
    public void testIndexOfNonWhitespace()
    {
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(""));
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(ALL_WHITESPACE));
        Assert.assertEquals(0, TextTools.indexOfNonWhitespace("no_whitespace_here"));

        String text = WS_TEXT;
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
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(text, text.length()));

        // three arg overload
        Assert.assertEquals(0, TextTools.indexOfNonWhitespace(text, 0, text.length()));
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(text, 20, 32));
        Assert.assertEquals(32, TextTools.indexOfNonWhitespace(text, 20, 33));
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(text, 5, 5));
    }

    @Test
    public void testIndexOfNonWhitespaceSupplementary()
    {
        Assert.assertEquals(0, TextTools.indexOfNonWhitespace(AVESTAN_A));
        Assert.assertEquals(0, TextTools.indexOfNonWhitespace(SUPP_WS_TEXT));
        Assert.assertEquals(3, TextTools.indexOfNonWhitespace(SUPP_WS_TEXT, 2));
        Assert.assertEquals(6, TextTools.indexOfNonWhitespace(SUPP_WS_TEXT, 5));
        Assert.assertEquals(-1, TextTools.indexOfNonWhitespace(SUPP_WS_TEXT, 2, 3));

        // an unpaired half of a supplementary character is itself not whitespace
        Assert.assertEquals(2, TextTools.indexOfNonWhitespace(" " + AVESTAN_A, 2, 3));
    }

    // lastIndexOfNonWhitespace

    @Test
    public void testLastIndexOfNonWhitespace()
    {
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(""));
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(ALL_WHITESPACE));
        Assert.assertEquals(17, TextTools.lastIndexOfNonWhitespace("no_whitespace_here"));

        // the index returned is that of the character itself, so it can be used to trim trailing whitespace
        String trailing = "abc   ";
        Assert.assertEquals(2, TextTools.lastIndexOfNonWhitespace(trailing));
        Assert.assertEquals("abc", trailing.substring(0, TextTools.lastIndexOfNonWhitespace(trailing) + 1));

        String text = WS_TEXT;
        Assert.assertEquals(text.length() - 1, TextTools.lastIndexOfNonWhitespace(text));

        // two arg overload
        Assert.assertEquals(text.length() - 1, TextTools.lastIndexOfNonWhitespace(text, 0));
        Assert.assertEquals(text.length() - 1, TextTools.lastIndexOfNonWhitespace(text, 32));
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(text, text.length()));

        // three arg overload: [20, 32) is the run of whitespace inside the braces
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(text, 20, 32));
        Assert.assertEquals(19, TextTools.lastIndexOfNonWhitespace(text, 0, 32));
        Assert.assertEquals(19, TextTools.lastIndexOfNonWhitespace(text, 19, 32));
        Assert.assertEquals(32, TextTools.lastIndexOfNonWhitespace(text, 20, 33));
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(text, 5, 5));

        Assert.assertEquals(QBF.length() - 1, TextTools.lastIndexOfNonWhitespace(QBF));
        Assert.assertEquals(2, TextTools.lastIndexOfNonWhitespace(QBF, 0, 4));
        Assert.assertEquals(8, TextTools.lastIndexOfNonWhitespace(QBF, 0, 10));
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(QBF, 9, 10));
    }

    @Test
    public void testLastIndexOfNonWhitespaceSupplementary()
    {
        Assert.assertEquals(0, TextTools.lastIndexOfNonWhitespace(AVESTAN_A));

        // the last non-whitespace character is LINEAR_A_AB001, occupying indices 6 and 7: index 6 is returned
        Assert.assertEquals(6, TextTools.lastIndexOfNonWhitespace(SUPP_WS_TEXT));
        Assert.assertEquals(6, TextTools.lastIndexOfNonWhitespace(SUPP_WS_TEXT, 6));
        Assert.assertEquals(3, TextTools.lastIndexOfNonWhitespace(SUPP_WS_TEXT, 0, 6));
        Assert.assertEquals(0, TextTools.lastIndexOfNonWhitespace(SUPP_WS_TEXT, 0, 3));
        Assert.assertEquals(-1, TextTools.lastIndexOfNonWhitespace(SUPP_WS_TEXT, 2, 3));

        // trailing whitespace can be trimmed off without splitting the last character
        String padded = "  " + LINEAR_B_A + " \t ";
        int last = TextTools.lastIndexOfNonWhitespace(padded);
        Assert.assertEquals(2, last);
        Assert.assertEquals("  " + LINEAR_B_A, padded.substring(0, last + Character.charCount(padded.codePointAt(last))));
    }

    // isBlank

    @Test
    public void testIsBlank()
    {
        Assert.assertTrue(TextTools.isBlank(""));
        Assert.assertTrue(TextTools.isBlank(" "));
        Assert.assertTrue(TextTools.isBlank(ALL_WHITESPACE));
        Assert.assertFalse(TextTools.isBlank("a"));
        Assert.assertFalse(TextTools.isBlank(ALL_WHITESPACE + "a"));
        Assert.assertFalse(TextTools.isBlank("a" + ALL_WHITESPACE));

        assertIsBlank("", 0, 0);

        String text = WS_TEXT;
        assertIsNotBlank(text, 0, text.length());
        assertIsNotBlank(text, 0, 20);
        assertIsNotBlank(text, 25, text.length());
        assertIsBlank(text, 5, 6);
        assertIsBlank(text, 20, 32);

        // two arg overload
        Assert.assertFalse(TextTools.isBlank(text, 0));
        Assert.assertFalse(TextTools.isBlank(text, 20));
        Assert.assertTrue(TextTools.isBlank(text, text.length()));
        Assert.assertTrue(TextTools.isBlank(ALL_WHITESPACE, 0));
        Assert.assertTrue(TextTools.isBlank(ALL_WHITESPACE, 5));

        for (int i = 0; i < QBF.length(); i++)
        {
            assertIsBlank(QBF, i, i);
        }
        assertIsNotBlank(QBF, 0, QBF.length());
        assertIsNotBlank(QBF, 0, 4);
        assertIsNotBlank(QBF, 2, 4);
        assertIsBlank(QBF, 3, 4);
        assertIsBlank(QBF, 9, 10);
        assertIsBlank(QBF, 15, 16);
        assertIsBlank(QBF, 19, 20);
    }

    @Test
    public void testIsBlankSupplementary()
    {
        // no supplementary code point is whitespace
        Assert.assertFalse(TextTools.isBlank(AVESTAN_A));
        Assert.assertFalse(TextTools.isBlank(LINEAR_A_AB001));
        Assert.assertFalse(TextTools.isBlank(LINEAR_B_A));
        Assert.assertFalse(TextTools.isBlank(LINEAR_B_E));
        Assert.assertFalse(TextTools.isBlank(" " + AVESTAN_A + " "));
        Assert.assertFalse(TextTools.isBlank(SUPP_WS_TEXT));

        Assert.assertTrue(TextTools.isBlank(SUPP_WS_TEXT, 2, 3));
        Assert.assertTrue(TextTools.isBlank(SUPP_WS_TEXT, 5, 6));
        Assert.assertFalse(TextTools.isBlank(SUPP_WS_TEXT, 5, 7));

        // a region holding only half of a supplementary character is not blank: an unpaired surrogate is not whitespace
        String padded = " " + AVESTAN_A + " ";
        Assert.assertFalse(TextTools.isBlank(padded, 0, 2));
        Assert.assertFalse(TextTools.isBlank(padded, 2, 4));
        Assert.assertTrue(TextTools.isBlank(padded, 0, 1));
        Assert.assertTrue(TextTools.isBlank(padded, 3, 4));
    }

    /**
     * Check that the whitespace helpers agree with the predicate methods they delegate to, and that the one and two
     * argument overloads agree with the three argument overload, over every region of every sample string.
     */
    @Test
    public void testWhitespaceHelpersAgreeWithPredicateMethods()
    {
        IntPredicate whitespace = Character::isWhitespace;
        IntPredicate nonWhitespace = cp -> !Character.isWhitespace(cp);
        TRAVERSAL_SAMPLES.forEach(text ->
        {
            int length = text.length();
            String prefix = "\"" + escape(text) + "\" ";

            Assert.assertEquals(prefix, TextTools.indexOf(text, 0, length, whitespace), TextTools.indexOfWhitespace(text));
            Assert.assertEquals(prefix, TextTools.indexOf(text, 0, length, nonWhitespace), TextTools.indexOfNonWhitespace(text));
            Assert.assertEquals(prefix, TextTools.lastIndexOf(text, 0, length, nonWhitespace), TextTools.lastIndexOfNonWhitespace(text));
            Assert.assertEquals(prefix, TextTools.indexOfNonWhitespace(text) == -1, TextTools.isBlank(text));

            for (int start = 0; start <= length; start++)
            {
                String startMessage = prefix + "[" + start + ", " + length + ")";
                Assert.assertEquals(startMessage, TextTools.indexOfWhitespace(text, start, length), TextTools.indexOfWhitespace(text, start));
                Assert.assertEquals(startMessage, TextTools.indexOfNonWhitespace(text, start, length), TextTools.indexOfNonWhitespace(text, start));
                Assert.assertEquals(startMessage, TextTools.lastIndexOfNonWhitespace(text, start, length), TextTools.lastIndexOfNonWhitespace(text, start));
                Assert.assertEquals(startMessage, TextTools.isBlank(text, start, length), TextTools.isBlank(text, start));
                Assert.assertEquals(startMessage, TextTools.indexOf(text, start, length, whitespace), TextTools.indexOf(text, start, whitespace));
                Assert.assertEquals(startMessage, TextTools.lastIndexOf(text, start, length, whitespace), TextTools.lastIndexOf(text, start, whitespace));

                for (int end = start; end <= length; end++)
                {
                    String message = prefix + "[" + start + ", " + end + ")";
                    Assert.assertEquals(message, TextTools.indexOf(text, start, end, whitespace), TextTools.indexOfWhitespace(text, start, end));
                    Assert.assertEquals(message, TextTools.indexOf(text, start, end, nonWhitespace), TextTools.indexOfNonWhitespace(text, start, end));
                    Assert.assertEquals(message, TextTools.lastIndexOf(text, start, end, nonWhitespace), TextTools.lastIndexOfNonWhitespace(text, start, end));
                    Assert.assertEquals(message, TextTools.indexOfNonWhitespace(text, start, end) == -1, TextTools.isBlank(text, start, end));
                }
            }
        });
    }

    // toLowerCase / toUpperCase

    @Test
    public void testToLowerCase()
    {
        Assert.assertEquals("tHE QUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, Locale.ROOT));
        Assert.assertEquals("the QUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, 3, Locale.ROOT));
        Assert.assertEquals("the QUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, 4, Locale.ROOT));
        Assert.assertEquals("the qUICK BROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 0, 5, Locale.ROOT));

        Assert.assertEquals("THE QUICK bROWN FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 10, Locale.ROOT));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 3, 15, Locale.ROOT));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 3, 16, Locale.ROOT));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 4, 15, Locale.ROOT));
        Assert.assertEquals("THE quick brown FOX", TextTools.toLowerCase("THE QUICK BROWN FOX", 4, 16, Locale.ROOT));

        // an empty region converts nothing
        Assert.assertEquals("ABC", TextTools.toLowerCase("ABC", 0, 0, Locale.ROOT));
        Assert.assertEquals("ABC", TextTools.toLowerCase("ABC", 3, 3, Locale.ROOT));

        // the string itself is returned when the conversion changes nothing
        Arrays.asList("no caps", "12345", "    ", "\n").forEach(s ->
        {
            Assert.assertSame(s, TextTools.toLowerCase(s, 0));
            Assert.assertSame(s, TextTools.toLowerCase(s, 0, s.length()));
            Assert.assertSame(s, TextTools.toLowerCase(s, 0, Locale.ROOT));
            Assert.assertSame(s, TextTools.toLowerCase(s, 0, s.length(), Locale.ROOT));
        });

        // the default locale overloads delegate to the explicit locale overloads
        String text = "THE QUICK BROWN FOX";
        Assert.assertEquals(TextTools.toLowerCase(text, 0, Locale.getDefault()), TextTools.toLowerCase(text, 0));
        Assert.assertEquals(TextTools.toLowerCase(text, 3, 15, Locale.getDefault()), TextTools.toLowerCase(text, 3, 15));
    }

    @Test
    public void testToUpperCase()
    {
        Assert.assertEquals("The quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, Locale.ROOT));
        Assert.assertEquals("THE quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, 3, Locale.ROOT));
        Assert.assertEquals("THE quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, 4, Locale.ROOT));
        Assert.assertEquals("THE Quick brown fox", TextTools.toUpperCase("the quick brown fox", 0, 5, Locale.ROOT));

        Assert.assertEquals("the quick Brown fox", TextTools.toUpperCase("the quick brown fox", 10, Locale.ROOT));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 3, 15, Locale.ROOT));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 3, 16, Locale.ROOT));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 4, 15, Locale.ROOT));
        Assert.assertEquals("the QUICK BROWN fox", TextTools.toUpperCase("the quick brown fox", 4, 16, Locale.ROOT));

        Assert.assertEquals("abc", TextTools.toUpperCase("abc", 0, 0, Locale.ROOT));
        Assert.assertEquals("abc", TextTools.toUpperCase("abc", 3, 3, Locale.ROOT));

        Arrays.asList("NO LOWER", "12345", "    ", "\n\t\b\r\n").forEach(s ->
        {
            Assert.assertSame(s, TextTools.toUpperCase(s, 0));
            Assert.assertSame(s, TextTools.toUpperCase(s, 0, s.length()));
            Assert.assertSame(s, TextTools.toUpperCase(s, 0, Locale.ROOT));
            Assert.assertSame(s, TextTools.toUpperCase(s, 0, s.length(), Locale.ROOT));
        });

        String text = "the quick brown fox";
        Assert.assertEquals(TextTools.toUpperCase(text, 0, Locale.getDefault()), TextTools.toUpperCase(text, 0));
        Assert.assertEquals(TextTools.toUpperCase(text, 3, 15, Locale.getDefault()), TextTools.toUpperCase(text, 3, 15));
    }

    @Test
    public void testCaseConversionSupplementary()
    {
        // the index overloads must convert the whole surrogate pair, not just the char at the index
        Assert.assertEquals(DESERET_LONG_I_LOWER, TextTools.toLowerCase(DESERET_LONG_I_UPPER, 0, Locale.ROOT));
        Assert.assertEquals(DESERET_LONG_I_UPPER, TextTools.toUpperCase(DESERET_LONG_I_LOWER, 0, Locale.ROOT));

        String upper = "x" + DESERET_LONG_I_UPPER + "y";
        String lower = "x" + DESERET_LONG_I_LOWER + "y";
        Assert.assertEquals(lower, TextTools.toLowerCase(upper, 1, Locale.ROOT));
        Assert.assertEquals(upper, TextTools.toUpperCase(lower, 1, Locale.ROOT));
        Assert.assertEquals(lower, TextTools.toLowerCase(upper, 1, 3, Locale.ROOT));
        Assert.assertEquals(upper, TextTools.toUpperCase(lower, 1, 3, Locale.ROOT));

        // a caseless supplementary character is left alone
        Arrays.asList(AVESTAN_A, AVESTAN_AA, LINEAR_A_AB001, LINEAR_B_A, LINEAR_B_E).forEach(s ->
        {
            Assert.assertSame(s, TextTools.toLowerCase(s, 0, Locale.ROOT));
            Assert.assertSame(s, TextTools.toUpperCase(s, 0, Locale.ROOT));
        });

        // an index pointing at the low surrogate covers only that char, which has no case mapping
        Assert.assertSame(upper, TextTools.toLowerCase(upper, 2, Locale.ROOT));

        // likewise a region that splits the pair covers only the char in the region
        Assert.assertSame(upper, TextTools.toLowerCase(upper, 1, 2, Locale.ROOT));
        Assert.assertSame(upper, TextTools.toLowerCase(upper, 2, 3, Locale.ROOT));
    }

    @Test
    public void testCaseConversionChangingLength()
    {
        // the replacement may be longer than the region it replaces: LATIN SMALL LETTER SHARP S upper cases to "SS"
        Assert.assertEquals("STRASSE", TextTools.toUpperCase("stra\u00DFe", 0, 6, Locale.ROOT));
        Assert.assertEquals("aSSb", TextTools.toUpperCase("a\u00DFb", 1, 2, Locale.ROOT));
        Assert.assertEquals("aSSb", TextTools.toUpperCase("a\u00DFb", 1, Locale.ROOT));
    }

    @Test
    public void testCaseConversionLocaleSensitive()
    {
        Locale turkish = Locale.forLanguageTag("tr");

        // U+0131 LATIN SMALL LETTER DOTLESS I and U+0130 LATIN CAPITAL LETTER I WITH DOT ABOVE
        Assert.assertEquals("\u0131", TextTools.toLowerCase("I", 0, turkish));
        Assert.assertEquals("i", TextTools.toLowerCase("I", 0, Locale.ROOT));
        Assert.assertEquals("\u0130", TextTools.toUpperCase("i", 0, turkish));
        Assert.assertEquals("I", TextTools.toUpperCase("i", 0, Locale.ROOT));

        Assert.assertEquals("TITLE", TextTools.toUpperCase("title", 0, 5, Locale.ROOT));
        Assert.assertEquals("T\u0130TLE", TextTools.toUpperCase("title", 0, 5, turkish));
    }

    @Test
    public void testCaseConversionBounds()
    {
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toLowerCase("abc", 3, Locale.ROOT));
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toLowerCase("abc", -1, Locale.ROOT));
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toLowerCase("abc", 0, 4, Locale.ROOT));
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toLowerCase("abc", 2, 1, Locale.ROOT));

        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toUpperCase("abc", 3, Locale.ROOT));
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toUpperCase("abc", -1, Locale.ROOT));
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toUpperCase("abc", 0, 4, Locale.ROOT));
        Assert.assertThrows(StringIndexOutOfBoundsException.class, () -> TextTools.toUpperCase("abc", 2, 1, Locale.ROOT));
    }

    private static boolean isSurrogate(int codePoint)
    {
        return (codePoint >= Character.MIN_SURROGATE) && (codePoint <= Character.MAX_SURROGATE);
    }

    /**
     * Collect, in traversal order, the code points that indexOf or lastIndexOf applies its predicate to over a region.
     *
     * @param text    text
     * @param start   start of region (inclusive)
     * @param end     end of region (exclusive)
     * @param forward whether to traverse with indexOf rather than lastIndexOf
     * @return code points visited, in traversal order
     */
    private static MutableIntList collect(String text, int start, int end, boolean forward)
    {
        MutableIntList visited = IntLists.mutable.empty();
        IntPredicate collector = codePoint ->
        {
            visited.add(codePoint);
            return false;
        };
        int result = forward ?
                     TextTools.indexOf(text, start, end, collector) :
                     TextTools.lastIndexOf(text, start, end, collector);
        Assert.assertEquals(-1, result);
        return visited;
    }

    /**
     * Independently compute the code points of a region, treating a surrogate pair that straddles either region
     * boundary as an unpaired surrogate.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @param end   end of region (exclusive)
     * @return code points of the region, in ascending index order
     */
    private static MutableIntList expectedCodePoints(String text, int start, int end)
    {
        MutableIntList codePoints = IntLists.mutable.empty();
        int i = start;
        while (i < end)
        {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c) && ((i + 1) < end) && Character.isLowSurrogate(text.charAt(i + 1)))
            {
                codePoints.add(Character.toCodePoint(c, text.charAt(i + 1)));
                i += 2;
            }
            else
            {
                codePoints.add(c);
                i++;
            }
        }
        return codePoints;
    }

    /**
     * Check that indexOf and lastIndexOf return the index of the first char of the matching character, by matching
     * each code point of the region in turn from each end.
     *
     * @param message    assertion message
     * @param text       text
     * @param start      start of region (inclusive)
     * @param end        end of region (exclusive)
     * @param codePoints expected code points of the region, in ascending index order
     */
    private static void assertMatchIndices(String message, String text, int start, int end, MutableIntList codePoints)
    {
        int index = start;
        for (int n = 0; n < codePoints.size(); n++)
        {
            int forwardTarget = n;
            int[] forwardCounter = {0};
            Assert.assertEquals(message + " code point " + n, index, TextTools.indexOf(text, start, end, cp -> forwardCounter[0]++ == forwardTarget));

            int backwardTarget = codePoints.size() - 1 - n;
            int[] backwardCounter = {0};
            Assert.assertEquals(message + " code point " + n, index, TextTools.lastIndexOf(text, start, end, cp -> backwardCounter[0]++ == backwardTarget));

            index += Character.charCount(codePoints.get(n));
        }
        Assert.assertEquals(message, end, index);
    }

    private static String escape(String string)
    {
        StringBuilder builder = new StringBuilder(string.length());
        string.chars().forEach(c ->
        {
            if ((c >= 0x20) && (c < 0x7F))
            {
                builder.append((char) c);
            }
            else
            {
                builder.append(String.format("\\u%04X", c));
            }
        });
        return builder.toString();
    }

    private static void assertBadRegion(String text, int start, int end)
    {
        String message = "\"" + escape(text) + "\" [" + start + ", " + end + ")";
        Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.indexOf(text, start, end, ALWAYS));
        Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.lastIndexOf(text, start, end, ALWAYS));
        Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.indexOfWhitespace(text, start, end));
        Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.indexOfNonWhitespace(text, start, end));
        Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.lastIndexOfNonWhitespace(text, start, end));
        Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.isBlank(text, start, end));

        if (end == text.length())
        {
            // the two argument overloads default end to the length of the text
            Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.indexOf(text, start, ALWAYS));
            Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.lastIndexOf(text, start, ALWAYS));
            Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.indexOfWhitespace(text, start));
            Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.indexOfNonWhitespace(text, start));
            Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.lastIndexOfNonWhitespace(text, start));
            Assert.assertThrows(message, StringIndexOutOfBoundsException.class, () -> TextTools.isBlank(text, start));
        }
    }

    private void assertIsNotBlank(String string, int start, int end)
    {
        Assert.assertFalse(TextTools.isBlank(string, start, end));
    }

    private void assertIsBlank(String string, int start, int end)
    {
        if (!TextTools.isBlank(string, start, end))
        {
            Assert.fail("isBlank(\"" + escape(string) + "\", " + start + ", " + end + "); non-whitespace index: " + TextTools.indexOfNonWhitespace(string, start, end));
        }
    }
}
