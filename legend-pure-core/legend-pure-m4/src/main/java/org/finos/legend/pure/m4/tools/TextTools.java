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

import java.util.Locale;
import java.util.function.IntPredicate;

public class TextTools
{
    /**
     * Return the index of the first character found in the given text which satisfies the given predicate, or -1 if no
     * such character is found.
     *
     * @param text      text
     * @param predicate predicate
     * @return index of first satisfying character or -1
     */
    public static int indexOf(String text, IntPredicate predicate)
    {
        return indexOf(text, 0, text.length(), predicate);
    }

    /**
     * Return the index of the first character found in a region of text (starting from start) which satisfies the given
     * predicate, or -1 if no such character is found in that region.
     *
     * @param text      text
     * @param start     start of region (inclusive)
     * @param predicate predicate
     * @return index of first satisfying character or -1
     */
    public static int indexOf(String text, int start, IntPredicate predicate)
    {
        return indexOf(text, start, text.length(), predicate);
    }

    /**
     * Return the index of the first character found in a region of text which satisfies the given predicate, or -1 if
     * no such character is found in that region. The text is traversed by code point, so the predicate is applied to
     * supplementary characters as single code points, and the returned index is that of the first char of the
     * character. Only chars within the region are considered: if a surrogate pair straddles the end of the region, the
     * high surrogate is presented to the predicate as an unpaired surrogate.
     *
     * @param text      text
     * @param start     start of region (inclusive)
     * @param end       end of region (exclusive)
     * @param predicate predicate
     * @return index of first satisfying character or -1
     */
    public static int indexOf(String text, int start, int end, IntPredicate predicate)
    {
        checkRegionBounds(text, start, end);
        int index = start;
        while (index < end)
        {
            int codePoint = text.codePointAt(index);
            int nextIndex = index + Character.charCount(codePoint);
            if (nextIndex > end)
            {
                // a surrogate pair straddles the end of the region: consider only the char within the region
                codePoint = text.charAt(index);
            }
            if (predicate.test(codePoint))
            {
                return index;
            }
            index = nextIndex;
        }
        return -1;
    }

    /**
     * Return the index of the last character found in the given text which satisfies the given predicate, or -1 if no
     * such character is found.
     *
     * @param text      text
     * @param predicate predicate
     * @return index of last satisfying character or -1
     */
    public static int lastIndexOf(String text, IntPredicate predicate)
    {
        return lastIndexOf(text, 0, text.length(), predicate);
    }

    /**
     * Return the index of the last character found in a region of text (starting from start) which satisfies the given
     * predicate, or -1 if no such character is found in that region.
     *
     * @param text      text
     * @param start     start of region (inclusive)
     * @param predicate predicate
     * @return index of last satisfying character or -1
     */
    public static int lastIndexOf(String text, int start, IntPredicate predicate)
    {
        return lastIndexOf(text, start, text.length(), predicate);
    }

    /**
     * Return the index of the last character found in a region of text which satisfies the given predicate, or -1 if
     * no such character is found in that region. The text is traversed backward by code point, so the predicate is
     * applied to supplementary characters as single code points, and the returned index is that of the first char of
     * the character. Only chars within the region are considered: if a surrogate pair straddles the start of the
     * region, the low surrogate is presented to the predicate as an unpaired surrogate.
     *
     * @param text      text
     * @param start     start of region (inclusive)
     * @param end       end of region (exclusive)
     * @param predicate predicate
     * @return index of last satisfying character or -1
     */
    public static int lastIndexOf(String text, int start, int end, IntPredicate predicate)
    {
        checkRegionBounds(text, start, end);
        int afterIndex = end;
        while (afterIndex > start)
        {
            int codePoint = text.codePointBefore(afterIndex);
            int index = afterIndex - Character.charCount(codePoint);
            if (index < start)
            {
                // a surrogate pair straddles the start of the region: consider only the char within the region
                index = start;
                codePoint = text.charAt(start);
            }
            if (predicate.test(codePoint))
            {
                return index;
            }
            afterIndex = index;
        }
        return -1;
    }

    /**
     * Return the index of the first non-whitespace character found in the given text, or -1 if no non-whitespace
     * character is found.
     *
     * @param text text
     * @return index of first non-whitespace character or -1
     * @see #indexOfWhitespace(String)
     * @see #isBlank(String)
     */
    public static int indexOfNonWhitespace(String text)
    {
        return indexOf(text, TextTools::isNonWhiteSpace);
    }

    /**
     * Return the index of the first non-whitespace character found in a region of text (starting from start), or -1 if
     * no non-whitespace character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @return index of first non-whitespace character or -1
     * @see #indexOfWhitespace(String, int)
     * @see #isBlank(String, int)
     */
    public static int indexOfNonWhitespace(String text, int start)
    {
        return indexOf(text, start, TextTools::isNonWhiteSpace);
    }

    /**
     * Return the index of the first non-whitespace character found in a region of text, or -1 if no non-whitespace
     * character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @param end   end of region (exclusive)
     * @return index of first non-whitespace character or -1
     * @see #indexOfWhitespace(String, int, int)
     * @see #isBlank(String, int, int)
     */
    public static int indexOfNonWhitespace(String text, int start, int end)
    {
        return indexOf(text, start, end, TextTools::isNonWhiteSpace);
    }

    /**
     * Return the index of the last non-whitespace character found in the given text, or -1 if no non-whitespace
     * character is found.
     *
     * @param text text
     * @return index of last non-whitespace character or -1
     * @see #indexOfNonWhitespace(String)
     * @see #isBlank(String)
     */
    public static int lastIndexOfNonWhitespace(String text)
    {
        return lastIndexOf(text, TextTools::isNonWhiteSpace);
    }

    /**
     * Return the index of the last non-whitespace character found in a region of text (starting from start), or -1 if
     * no non-whitespace character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @return index of last non-whitespace character or -1
     * @see #indexOfNonWhitespace(String, int)
     * @see #isBlank(String, int)
     */
    public static int lastIndexOfNonWhitespace(String text, int start)
    {
        return lastIndexOf(text, start, TextTools::isNonWhiteSpace);
    }

    /**
     * Return the index of the last non-whitespace character found in a region of text, or -1 if no non-whitespace
     * character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @param end   end of region (exclusive)
     * @return index of last non-whitespace character or -1
     * @see #indexOfNonWhitespace(String, int, int)
     * @see #isBlank(String, int, int)
     */
    public static int lastIndexOfNonWhitespace(String text, int start, int end)
    {
        return lastIndexOf(text, start, end, TextTools::isNonWhiteSpace);
    }

    private static boolean isNonWhiteSpace(int c)
    {
        return !Character.isWhitespace(c);
    }

    /**
     * Return the index of the first whitespace character found in the given text, or -1 if no whitespace character is
     * found.
     *
     * @param text text
     * @return index of first whitespace character or -1
     * @see #indexOfNonWhitespace(String)
     * @see #isBlank(String)
     */
    public static int indexOfWhitespace(String text)
    {
        return indexOf(text, Character::isWhitespace);
    }

    /**
     * Return the index of the first whitespace character found in a region of text (starting from start), or -1 if no
     * whitespace character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @return index of first whitespace character or -1
     * @see #indexOfNonWhitespace(String, int)
     * @see #isBlank(String, int)
     */
    public static int indexOfWhitespace(String text, int start)
    {
        return indexOf(text, start, Character::isWhitespace);
    }

    /**
     * Return the index of the first whitespace character found in a region of text, or -1 if no whitespace character is
     * found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @param end   end of region (exclusive)
     * @return index of first whitespace character or -1
     * @see #indexOfNonWhitespace(String, int, int)
     * @see #isBlank(String, int, int)
     */
    public static int indexOfWhitespace(String text, int start, int end)
    {
        return indexOf(text, start, end, Character::isWhitespace);
    }

    /**
     * Return whether the given text is blank, meaning it is empty or contains only whitespace.
     *
     * @param text text
     * @return whether the text is blank
     * @see #indexOfNonWhitespace(String)
     */
    public static boolean isBlank(String text)
    {
        return indexOfNonWhitespace(text) == -1;
    }

    /**
     * Return whether a region of text (starting from start) is blank, meaning it is empty or contains only whitespace.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @return whether the region of text is blank
     * @see #indexOfNonWhitespace(String, int)
     */
    public static boolean isBlank(String text, int start)
    {
        return indexOfNonWhitespace(text, start) == -1;
    }

    /**
     * Return whether a region of text is blank, meaning it is empty or contains only whitespace.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @param end   end of region (exclusive)
     * @return whether the region of text is blank
     * @see #indexOfNonWhitespace(String, int, int)
     */
    public static boolean isBlank(String text, int start, int end)
    {
        return indexOfNonWhitespace(text, start, end) == -1;
    }

    /**
     * Convert a code point of a string to lower case using the rules of the default locale.
     *
     * @param string string
     * @param index  index of the code point (a char index, not a count of code points)
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toLowerCase(String string, int index)
    {
        return toLowerCase(string, index, Locale.getDefault());
    }

    /**
     * Convert a code point of a string to lower case using the rules of the given locale.
     *
     * @param string string
     * @param index  index of the code point (a char index, not a count of code points)
     * @param locale locale for case transformation rules
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toLowerCase(String string, int index, Locale locale)
    {
        return toLowerCase(string, index, index + Character.charCount(string.codePointAt(index)), locale);
    }

    /**
     * Convert a region of a string to lower case using the rules of the default locale.
     *
     * @param string string
     * @param start  region start (inclusive)
     * @param end    region end (exclusive)
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toLowerCase(String string, int start, int end)
    {
        return toLowerCase(string, start, end, Locale.getDefault());
    }

    /**
     * Convert a region of a string to lower case using the rules of the given locale.
     *
     * @param string string
     * @param start  region start (inclusive)
     * @param end    region end (exclusive)
     * @param locale locale for case transformation rules
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toLowerCase(String string, int start, int end, Locale locale)
    {
        String substring = string.substring(start, end);
        String replacement = substring.toLowerCase(locale);
        return substring.equals(replacement) ?
               string :
               new StringBuilder(string)
                       .replace(start, end, replacement)
                       .toString();
    }

    /**
     * Convert a code point of a string to upper case using the rules of the default locale.
     *
     * @param string string
     * @param index  index of the code point (a char index, not a count of code points)
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toUpperCase(String string, int index)
    {
        return toUpperCase(string, index, Locale.getDefault());
    }

    /**
     * Convert a code point of a string to upper case using the rules of the given locale.
     *
     * @param string string
     * @param index  index of the code point (a char index, not a count of code points)
     * @param locale locale for case transformation rules
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toUpperCase(String string, int index, Locale locale)
    {
        return toUpperCase(string, index, index + Character.charCount(string.codePointAt(index)), locale);
    }

    /**
     * Convert a region of a string to upper case using the rules of the default locale.
     *
     * @param string string
     * @param start  region start (inclusive)
     * @param end    region end (exclusive)
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toUpperCase(String string, int start, int end)
    {
        return toUpperCase(string, start, end, Locale.getDefault());
    }

    /**
     * Convert a region of a string to upper case using the rules of the given locale.
     *
     * @param string string
     * @param start  region start (inclusive)
     * @param end    region end (exclusive)
     * @param locale locale for case transformation rules
     * @return converted string, or the given string itself if the conversion changes nothing
     */
    public static String toUpperCase(String string, int start, int end, Locale locale)
    {
        String substring = string.substring(start, end);
        String replacement = substring.toUpperCase(locale);
        return substring.equals(replacement) ?
               string :
               new StringBuilder(string)
                       .replace(start, end, replacement)
                       .toString();
    }

    private static void checkRegionBounds(String text, int start, int end)
    {
        if ((start < 0) || (start > end) || (end > text.length()))
        {
            throw new StringIndexOutOfBoundsException("start " + start + ", end " + end + ", length " + text.length());
        }
    }
}
