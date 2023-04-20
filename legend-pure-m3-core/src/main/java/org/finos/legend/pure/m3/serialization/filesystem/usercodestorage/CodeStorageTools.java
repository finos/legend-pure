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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage;

import java.util.regex.Pattern;

public class CodeStorageTools
{
    private static final Pattern VALID_NON_ROOT_PATH_PATTERN = Pattern.compile("^/?+\\w++(/\\w++)*+(/|(\\.\\w++))?+$");
    private static final Pattern VALID_NON_ROOT_FOLDER_PATTERN = Pattern.compile("^(\\w++)?(/\\w++)++/?$");
    private static final Pattern VALID_FILE_PATTERN = Pattern.compile("^/?+(\\w++/)*+\\w++\\.\\w++$");
    private static final Pattern PURE_FILE_PATTERN = Pattern.compile("^/?+(\\w++/)*+\\w++\\.(?i)pure$");
    private static final Pattern CANONICAL_NON_ROOT_PATH_PATTERN = Pattern.compile("^(/\\w++)++(\\.\\w++)?$");

    private CodeStorageTools()
    {
        // Utility class
    }

    public static boolean isRootPath(String path)
    {
        return RepositoryCodeStorage.ROOT_PATH.equals(path);
    }

    public static boolean isValidPath(String path)
    {
        return (path != null) && (isRootPath(path) || VALID_NON_ROOT_PATH_PATTERN.matcher(path).matches());
    }

    public static boolean isValidFilePath(String path)
    {
        return (path != null) && VALID_FILE_PATTERN.matcher(path).matches();
    }

    public static boolean isValidFolderPath(String path)
    {
        return (path != null) && (isRootPath(path) || VALID_NON_ROOT_FOLDER_PATTERN.matcher(path).matches());
    }

    public static boolean isPureFilePath(String path)
    {
        return (path != null) && PURE_FILE_PATTERN.matcher(path).matches();
    }

    public static boolean hasPureFileExtension(String path)
    {
        if (path == null)
        {
            return false;
        }
        int extLen = RepositoryCodeStorage.PURE_FILE_EXTENSION.length();
        int offset = path.length() - extLen;
        return (offset >= 0) && path.regionMatches(true, offset, RepositoryCodeStorage.PURE_FILE_EXTENSION, 0, extLen);
    }

    public static String getInitialPathElement(String path)
    {
        if ((path == null) || path.isEmpty())
        {
            return null;
        }

        int start = (path.charAt(0) == '/') ? 1 : 0;
        int end = path.indexOf('/', start);
        String value = (end == -1) ? path.substring(start) : path.substring(start, end);
        return value.endsWith(".pure") ? null : value;
    }

    public static String canonicalizePath(String path)
    {
        if (path == null)
        {
            return RepositoryCodeStorage.ROOT_PATH;
        }
        String canonicalPath = path.trim();
        if (canonicalPath.isEmpty())
        {
            return RepositoryCodeStorage.ROOT_PATH;
        }
        if (canonicalPath.endsWith(RepositoryCodeStorage.PATH_SEPARATOR))
        {
            if (canonicalPath.startsWith(RepositoryCodeStorage.ROOT_PATH))
            {
                return canonicalPath;
            }
            return RepositoryCodeStorage.ROOT_PATH + canonicalPath.substring(0, canonicalPath.length() - RepositoryCodeStorage.PATH_SEPARATOR.length());
        }
        return canonicalPath.startsWith(RepositoryCodeStorage.ROOT_PATH) ? canonicalPath : (RepositoryCodeStorage.ROOT_PATH + canonicalPath);
    }

    public static boolean isCanonicalPath(String path)
    {
        return (path != null) && (isRootPath(path) || CANONICAL_NON_ROOT_PATH_PATTERN.matcher(path).matches());
    }

    public static String joinPaths(String... paths)
    {
        if (paths == null)
        {
            return "";
        }
        int pathCount = paths.length;
        if (pathCount == 0)
        {
            return "";
        }
        if (pathCount == 1)
        {
            return paths[0];
        }

        String separator = RepositoryCodeStorage.PATH_SEPARATOR;
        int separatorLength = separator.length();

        int lastPathIndex = pathCount - 1;
        int maxLength = paths[0].length();
        for (int i = 1; i < pathCount; i++)
        {
            maxLength += separatorLength;
            maxLength += paths[i].length();
        }
        char[] chars = new char[maxLength];

        // first path
        String firstPath = paths[0];
        int length = firstPath.endsWith(separator) ? (firstPath.length() - separatorLength) : firstPath.length();
        firstPath.getChars(0, length, chars, 0);

        // middle paths
        for (int i = 1; i < lastPathIndex; i++)
        {
            String path = paths[i];
            if (!path.startsWith(separator))
            {
                separator.getChars(0, separatorLength, chars, length);
                length += separatorLength;
            }
            int end = path.endsWith(separator) ? (path.length() - separatorLength) : path.length();
            path.getChars(0, end, chars, length);
            length += end;
        }

        // last path
        String lastPath = paths[lastPathIndex];
        if (!lastPath.startsWith(separator))
        {
            separator.getChars(0, separatorLength, chars, length);
            length += separatorLength;
        }
        lastPath.getChars(0, lastPath.length(), chars, length);
        length += lastPath.length();

        return new String(chars, 0, length);
    }
}
