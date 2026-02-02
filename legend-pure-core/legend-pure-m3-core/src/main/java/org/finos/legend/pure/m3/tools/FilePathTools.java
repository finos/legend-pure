// Copyright 2025 Goldman Sachs
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

import org.eclipse.collections.impl.SpreadFunctions;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class FilePathTools
{
    // UTF-16 uses 2 bytes per Java char, possibly plus a byte order marker of 2 bytes
    // To be conservative, file names should be no more than 255 bytes in UTF-16; so 126 is the length limit for names (126 * 2 + 2 = 254)
    private static final int NAME_LEN_LIMIT = 126;
    private static final int SUFFIX_LEN = 13;

    /**
     * Convert the given string to a file path by splitting on the given delimiter and using the given file system
     * separator. If provided, the file extension will be appended at the end. See
     * {@link #appendFilePathName(Appendable, String, int, int, String)} for more details on how file names are
     * processed.
     *
     * @param string      string
     * @param delimiter   delimiter to split string on
     * @param fsSeparator file system separator
     * @param extension   optional file extension
     * @return string converted to file path
     */
    public static String toFilePath(String string, String delimiter, String fsSeparator, String extension)
    {
        return toFilePath(new StringBuilder(string.length() + ((extension == null) ? 0 : extension.length())), string, delimiter, fsSeparator, extension).toString();
    }

    /**
     * Convert the given string to a file path by splitting on the given delimiter and using the given file system
     * separator. If provided, the file extension will be appended at the end. See
     * {@link #appendFilePathName(Appendable, String, int, int, String)} for more details on how file names are
     * processed.
     *
     * @param appendable  appendable
     * @param string      string
     * @param delimiter   delimiter to split string on
     * @param fsSeparator file system separator
     * @param extension   optional file extension
     * @param <T>         appendable type
     * @return supplied appendable
     */
    public static <T extends Appendable> T toFilePath(T appendable, String string, String delimiter, String fsSeparator, String extension)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        int delimiterLen = delimiter.length();
        int start = 0;
        int end;
        while ((end = string.indexOf(delimiter, start)) != -1)
        {
            appendFilePathName(safeAppendable, string, start, end, null).append(fsSeparator);
            start = end + delimiterLen;
        }
        appendFilePathName(safeAppendable, string, start, string.length(), extension);
        return appendable;
    }

    public static String getFilePathName(String name)
    {
        if (name.length() <= NAME_LEN_LIMIT)
        {
            return name;
        }
        return getFilePathForLongName(name, 0, name.length(), null);
    }

    public static String getFilePathName(String name, String extension)
    {
        if (extension == null)
        {
            return getFilePathName(name);
        }
        if ((name.length() + extension.length()) <= NAME_LEN_LIMIT)
        {
            return name + extension;
        }
        return getFilePathForLongName(name, 0, name.length(), extension);
    }

    public static String getFilePathName(String string, int start, int end)
    {
        if ((end - start) <= NAME_LEN_LIMIT)
        {
            return string.substring(start, end);
        }
        return getFilePathForLongName(string, start, end, null);
    }

    public static String getFilePathName(String string, int start, int end, String extension)
    {
        if (extension == null)
        {
            return getFilePathName(string, start, end);
        }
        if (((end - start) + extension.length()) <= NAME_LEN_LIMIT)
        {
            return string.substring(start, end) + extension;
        }
        return getFilePathForLongName(string, start, end, extension);
    }

    private static String getFilePathForLongName(String string, int start, int end, String extension)
    {
        return appendFilePathName(new StringBuilder(NAME_LEN_LIMIT), string, start, end, extension).toString();
    }

    public static <T extends Appendable> T appendFilePathName(T appendable, String name)
    {
        return appendFilePathName(appendable, name, null);
    }

    public static <T extends Appendable> T appendFilePathName(T appendable, String name, String extension)
    {
        return appendFilePathName(appendable, name, 0, name.length(), extension);
    }

    public static <T extends Appendable> T appendFilePathName(T appendable, String string, int start, int end)
    {
        return appendFilePathName(appendable, string, start, end, null);
    }

    /**
     * Append a portion of a string to an {@link Appendable} as a file name. This function attempts to keep the file
     * name within the length limits of most file and operating systems by ensuring the resulting file name will take
     * no more than 255 bytes when encoded using UTF-16. In effect, this means that the file name (including extension)
     * must be no more than 126 characters (since UTF-16 uses 2 bytes per Java character, plus a possible 2 bytes for a
     * byte order marker). If this limit is exceeded, then the file name will be truncated and the overage will be
     * replaced with a base 32 string of a hash computed from the overage. Note that the extension, if provided,
     * will not be truncated. It will be appended after the base file name is truncated. If the extension is itself too
     * long, an {@link IllegalArgumentException} will be thrown.
     *
     * @param appendable target appendable
     * @param string     source string
     * @param start      start index (inclusive)
     * @param end        end index (exclusive)
     * @param extension  optional file extension
     * @param <T>        appendable type
     * @return given appendable
     * @throws IllegalArgumentException if the extension is too long
     */
    public static <T extends Appendable> T appendFilePathName(T appendable, String string, int start, int end, String extension)
    {
        appendFilePathName(SafeAppendable.wrap(appendable), string, start, end, extension);
        return appendable;
    }

    private static SafeAppendable appendFilePathName(SafeAppendable appendable, String string, int start, int end, String extension)
    {
        int extLen = (extension == null) ? 0 : extension.length();
        if (((end - start) + extLen) <= NAME_LEN_LIMIT)
        {
            // append the name, possibly plus extension, as it's below the limit
            appendable.append(string, start, end);
            return (extension == null) ? appendable : appendable.append(extension);
        }

        if (extLen > (NAME_LEN_LIMIT - SUFFIX_LEN))
        {
            throw new IllegalArgumentException("File extension exceeds the limit of " + (NAME_LEN_LIMIT - SUFFIX_LEN) + " characters: " + extension);
        }

        // if the name is too long, append an initial segment plus a fixed length suffix computed from the overage
        int overageStart = start + NAME_LEN_LIMIT - SUFFIX_LEN - extLen;
        if (Character.isLowSurrogate(string.charAt(overageStart)) && Character.isHighSurrogate(string.charAt(overageStart - 1)))
        {
            // avoid splitting the string in the middle of a supplementary pair
            overageStart--;
        }
        appendable.append(string, start, overageStart);
        String suffix = getOverageSuffix(string, overageStart, end);
        for (int i = suffix.length(); i < SUFFIX_LEN; i++)
        {
            appendable.append('0');
        }
        appendable.append(suffix);
        return (extension == null) ? appendable : appendable.append(extension);
    }

    private static String getOverageSuffix(String string, int start, int end)
    {
        return Long.toUnsignedString(hashOverage(string, start, end), 32);
    }

    // exposed for testing
    static long hashOverage(String string, int start, int end)
    {
        long hash = 0;
        for (int i = start, cp; i < end; i += Character.charCount(cp))
        {
            hash = SpreadFunctions.longSpreadOne(hash) + (cp = string.codePointAt(i));
        }
        return hash;
    }
}
