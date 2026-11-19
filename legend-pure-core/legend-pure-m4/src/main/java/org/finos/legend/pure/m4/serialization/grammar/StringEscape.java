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

package org.finos.legend.pure.m4.serialization.grammar;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Escapes and unescapes the text of Pure string literals.
 *
 * <p>Escaping works on each UTF-16 code unit separately, and writes ASCII. It puts a backslash before {@code '} and
 * {@code \}, writes {@code \b}, {@code \t}, {@code \n}, {@code \f}, and {@code \r} for those five control characters,
 * and keeps every other code unit from U+0020 to U+007F. It writes each remaining code unit as a backslash, a
 * {@code u}, and four uppercase hex digits, so a supplementary character takes two such escapes, and an unpaired
 * surrogate one. Reference IDs escape their keys this way, which makes the output part of the PELT format.</p>
 *
 * <p>Unescaping reverses that. It also accepts:</p>
 * <ul>
 *     <li>hex digits in either case;</li>
 *     <li>more than one {@code u}, and a {@code +} after them;</li>
 *     <li>any four characters after them that {@link Integer#parseInt(String, int)} accepts in base 16, including a
 *     sign, and decimal digits outside ASCII;</li>
 *     <li>octal escapes of one to three digits, the third only when the first is 0 to 3;</li>
 *     <li>a backslash before any other character, or at the end, which is dropped.</li>
 * </ul>
 */
public class StringEscape
{
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    public static String escape(String string)
    {
        if (string == null)
        {
            return null;
        }

        int length = string.length();
        int first = 0;
        while ((first < length) && !needsEscape(string.charAt(first)))
        {
            first++;
        }
        return (first == length) ? string : appendEscaped(new StringBuilder(length + 16).append(string, 0, first), string, first).toString();
    }

    public static <T extends Appendable> T escape(T appendable, String string)
    {
        return (string == null) ? appendable : appendEscaped(appendable, string, 0);
    }

    public static String unescape(String string)
    {
        if (string == null)
        {
            return null;
        }

        int first = string.indexOf('\\');
        return (first == -1) ? string : appendUnescaped(new StringBuilder(string.length()).append(string, 0, first), string, first).toString();
    }

    public static <T extends Appendable> T unescape(T appendable, String string)
    {
        return (string == null) ? appendable : appendUnescaped(appendable, string, 0);
    }

    private static boolean needsEscape(char c)
    {
        return (c < ' ') || (c > 0x7F) || (c == '\'') || (c == '\\');
    }

    private static <T extends Appendable> T appendEscaped(T appendable, String string, int start)
    {
        try
        {
            for (int i = start, length = string.length(); i < length; i++)
            {
                char c = string.charAt(i);
                switch (c)
                {
                    case '\'':
                    {
                        appendable.append("\\'");
                        break;
                    }
                    case '\\':
                    {
                        appendable.append("\\\\");
                        break;
                    }
                    case '\b':
                    {
                        appendable.append("\\b");
                        break;
                    }
                    case '\t':
                    {
                        appendable.append("\\t");
                        break;
                    }
                    case '\n':
                    {
                        appendable.append("\\n");
                        break;
                    }
                    case '\f':
                    {
                        appendable.append("\\f");
                        break;
                    }
                    case '\r':
                    {
                        appendable.append("\\r");
                        break;
                    }
                    default:
                    {
                        if ((c < ' ') || (c > 0x7F))
                        {
                            appendable.append('\\').append('u')
                                    .append(HEX_DIGITS[(c >>> 12) & 0xF])
                                    .append(HEX_DIGITS[(c >>> 8) & 0xF])
                                    .append(HEX_DIGITS[(c >>> 4) & 0xF])
                                    .append(HEX_DIGITS[c & 0xF]);
                        }
                        else
                        {
                            appendable.append(c);
                        }
                    }
                }
            }
            return appendable;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static <T extends Appendable> T appendUnescaped(T appendable, String string, int start)
    {
        try
        {
            int length = string.length();
            int i = start;
            while (i < length)
            {
                char c = string.charAt(i);
                if (c == '\\')
                {
                    i = appendUnescapedSequence(appendable, string, i);
                }
                else
                {
                    appendable.append(c);
                    i++;
                }
            }
            return appendable;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Unescape the sequence that starts with the backslash at {@code index}.
     *
     * @return the index after the sequence
     */
    private static int appendUnescapedSequence(Appendable appendable, String string, int index) throws IOException
    {
        int next = index + 1;
        if (next == string.length())
        {
            // a backslash at the end is dropped
            return next;
        }

        char c = string.charAt(next);
        switch (c)
        {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            {
                return appendOctalEscape(appendable, string, index);
            }
            case 'u':
            {
                return appendUnicodeEscape(appendable, string, index);
            }
            case '\\':
            case '\'':
            {
                appendable.append(c);
                return next + 1;
            }
            case 'b':
            {
                appendable.append('\b');
                return next + 1;
            }
            case 't':
            {
                appendable.append('\t');
                return next + 1;
            }
            case 'n':
            {
                appendable.append('\n');
                return next + 1;
            }
            case 'f':
            {
                appendable.append('\f');
                return next + 1;
            }
            case 'r':
            {
                appendable.append('\r');
                return next + 1;
            }
            default:
            {
                // the backslash is dropped, and the character after it is kept
                return next;
            }
        }
    }

    private static int appendOctalEscape(Appendable appendable, String string, int index) throws IOException
    {
        // one to three digits, with a third only when the first is 0 to 3, so that the value fits in 8 bits
        int length = string.length();
        char first = string.charAt(index + 1);
        int value = first - '0';
        int end = index + 2;
        if ((end < length) && isOctalDigit(string.charAt(end)))
        {
            value = (value * 8) + (string.charAt(end++) - '0');
            if ((first <= '3') && (end < length) && isOctalDigit(string.charAt(end)))
            {
                value = (value * 8) + (string.charAt(end++) - '0');
            }
        }
        appendable.append((char) value);
        return end;
    }

    private static boolean isOctalDigit(char c)
    {
        return ('0' <= c) && (c <= '7');
    }

    private static int appendUnicodeEscape(Appendable appendable, String string, int index) throws IOException
    {
        int length = string.length();
        int start = index + 2;
        while ((start < length) && (string.charAt(start) == 'u'))
        {
            start++;
        }
        if ((start < length) && (string.charAt(start) == '+'))
        {
            start++;
        }

        int end = start + 4;
        if (end > length)
        {
            throw new IllegalArgumentException("Less than 4 hex digits in unicode value: '" + string.substring(index) + "' due to end of CharSequence");
        }

        String digits = string.substring(start, end);
        int value;
        try
        {
            value = Integer.parseInt(digits, 16);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Unable to parse unicode value: " + digits, e);
        }
        appendable.append((char) value);
        return end;
    }
}
