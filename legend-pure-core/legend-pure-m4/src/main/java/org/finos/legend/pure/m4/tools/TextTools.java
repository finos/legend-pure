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

public class TextTools
{
    /**
     * Return the index of the first non-whitespace character found in the given text, or -1 if no non-whitespace
     * character is found.
     *
     * @param text text
     * @return index of first non-whitespace character or -1
     * @see #indexOfWhitespace(String)
     * @see #isBlank
     */
    public static int indexOfNonWhitespace(String text)
    {
        return indexOfNonWhitespace(text, 0);
    }

    /**
     * Return the index of the first non-whitespace character found in a region of text (starting from start), or -1 if
     * no non-whitespace character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @return index of first non-whitespace character or -1
     * @see #indexOfWhitespace(String, int)
     * @see #isBlank
     */
    public static int indexOfNonWhitespace(String text, int start)
    {
        return indexOfNonWhitespace(text, start, text.length());
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
     * @see #isBlank
     */
    public static int indexOfNonWhitespace(String text, int start, int end)
    {
        checkRegionBounds(text, start, end);
        int codePoint;
        for (int i = start; i < end; i += Character.charCount(codePoint))
        {
            codePoint = text.codePointAt(i);
            if (!Character.isWhitespace(codePoint))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the index of the first whitespace character found in the given text, or -1 if no whitespace character is
     * found.
     *
     * @param text text
     * @return index of first whitespace character or -1
     * @see #indexOfNonWhitespace(String)
     * @see #isBlank
     */
    public static int indexOfWhitespace(String text)
    {
        return indexOfWhitespace(text, 0);
    }

    /**
     * Return the index of the first whitespace character found in a region of text (starting from start), or -1 if no
     * whitespace character is found.
     *
     * @param text  text
     * @param start start of region (inclusive)
     * @return index of first whitespace character or -1
     * @see #indexOfNonWhitespace(String, int)
     * @see #isBlank
     */
    public static int indexOfWhitespace(String text, int start)
    {
        return indexOfWhitespace(text, start, text.length());
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
     * @see #isBlank
     */
    public static int indexOfWhitespace(String text, int start, int end)
    {
        checkRegionBounds(text, start, end);
        int codePoint;
        for (int i = start; i < end; i += Character.charCount(codePoint))
        {
            codePoint = text.codePointAt(i);
            if (Character.isWhitespace(codePoint))
            {
                return i;
            }
        }
        return -1;
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
        checkRegionBounds(text, start, end);
        return indexOfNonWhitespace(text, start, end) == -1;
    }

    /**
     * Convert a code point of a string to lower case using the rules of the default locale.
     *
     * @param string string
     * @param index  code point index
     * @return converted string
     */
    public static String toLowerCase(String string, int index)
    {
        return toLowerCase(string, index, Locale.getDefault());
    }

    /**
     * Convert a code point of a string to lower case using the rules of the given locale.
     *
     * @param string string
     * @param index  code point index
     * @param locale locale for case transformation rules
     * @return converted string
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
     * @return converted string
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
     * @return converted string
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
     * @param index  code point index
     * @return converted string
     */
    public static String toUpperCase(String string, int index)
    {
        return toUpperCase(string, index, Locale.getDefault());
    }

    /**
     * Convert a code point of a string to upper case using the rules of the given locale.
     *
     * @param string string
     * @param index  code point index
     * @param locale locale for case transformation rules
     * @return converted string
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
     * @return converted string
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
     * @return converted string
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
