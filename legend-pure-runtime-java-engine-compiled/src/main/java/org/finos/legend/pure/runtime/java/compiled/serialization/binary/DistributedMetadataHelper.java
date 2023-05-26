// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

class DistributedMetadataHelper
{
    private static final String META_DATA_DIRNAME = "metadata/";
    private static final String SPECS_DIRNAME = META_DATA_DIRNAME + "specs/";
    private static final String CLASSIFIERS_DIRNAME = META_DATA_DIRNAME + "classifiers/";
    private static final String STRINGS_DIRNAME = META_DATA_DIRNAME + "strings/";
    private static final String BINARIES_DIRNAME = META_DATA_DIRNAME + "bin/";

    private static final String BIN_FILE_EXTENSION = ".bin";
    private static final String INDEX_FILE_EXTENSION = ".idx";
    private static final String METADATA_SPEC_FILE_EXTENSION = ".json";

    // Metadata name

    static <T extends CharSequence> T validateMetadataName(T charSequence)
    {
        if (!isValidMetadataName(charSequence))
        {
            throw new IllegalArgumentException("Invalid metadata name: " + ((charSequence == null) ? null : ('"' + charSequence.toString() + '"')));
        }
        return charSequence;
    }

    static <T extends CharSequence> T validateMetadataNameIfPresent(T charSequence)
    {
        return (charSequence == null) ? null : validateMetadataName(charSequence);
    }

    /**
     * Return whether charSequence is a valid metadata name. This is true if it consists of one or more ASCII letters,
     * numbers, and underscore (_a-zA-Z0-9).
     *
     * @param charSequence character sequence
     * @return whether charSequence is a valid metadata name
     */
    static boolean isValidMetadataName(CharSequence charSequence)
    {
        return (charSequence != null) && isValidMetadataName_internal(charSequence, 0, charSequence.length());
    }

    /**
     * Return whether a region of charSequence is a valid metadata name. This is true if it consists of one or more
     * ASCII letters, numbers, and underscore (_a-zA-Z0-9).
     *
     * @param charSequence character sequence
     * @param start        start (inclusive)
     * @param end          end (exclusive)
     * @return whether the given region of charSequence is a valid metadata name
     */
    static boolean isValidMetadataName(CharSequence charSequence, int start, int end)
    {
        return (charSequence != null) &&
                (start >= 0) &&
                (end <= charSequence.length()) &&
                isValidMetadataName_internal(charSequence, start, end);
    }

    private static boolean isValidMetadataName_internal(CharSequence charSequence, int start, int end)
    {
        if (end <= start)
        {
            return false;
        }
        for (int i = start; i < end; i++)
        {
            if (!isValidMetadataNameChar(charSequence.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Only ASCII letters, numbers, and underscore are valid (_a-zA-Z0-9).
     *
     * @param c character
     * @return whether it is a valid metadata name code point
     */
    private static boolean isValidMetadataNameChar(char c)
    {
        return (c == '_') ||                  // underscore
                (('0' <= c) && (c <= '9')) || // digit
                (('A' <= c) && (c <= 'Z')) || // uppercase letter
                (('a' <= c) && (c <= 'z'));   // lowercase letter
    }

    static String getMetadataIdPrefix(String metadataName)
    {
        return (metadataName == null) ? null : ('$' + metadataName + '$');
    }

    // Metadata specification paths

    static String getMetadataSpecificationsDirectory()
    {
        return SPECS_DIRNAME;
    }

    static String getMetadataSpecificationFilePath(String metadataName)
    {
        return SPECS_DIRNAME + metadataName + METADATA_SPEC_FILE_EXTENSION;
    }

    static boolean isMetadataSpecificationFilePath(String path)
    {
        return (path != null) &&
                (path.length() > (SPECS_DIRNAME.length() + METADATA_SPEC_FILE_EXTENSION.length())) &&
                path.startsWith(SPECS_DIRNAME) &&
                path.endsWith(METADATA_SPEC_FILE_EXTENSION) &&
                isValidMetadataName(path, SPECS_DIRNAME.length(), path.length() - METADATA_SPEC_FILE_EXTENSION.length());
    }

    static boolean isMetadataSpecificationFileName(String fileName)
    {
        return (fileName != null) &&
                fileName.endsWith(METADATA_SPEC_FILE_EXTENSION) &&
                isValidMetadataName(fileName, 0, fileName.length() - METADATA_SPEC_FILE_EXTENSION.length());
    }

    // Metadata file paths

    static String getMetadataClassifierIndexFilePath(String metadataName, String classifierName)
    {
        return (metadataName == null) ?
                (CLASSIFIERS_DIRNAME + classifierName.replace("::", "/") + INDEX_FILE_EXTENSION) :
                (CLASSIFIERS_DIRNAME + metadataName + "/" + classifierName.replace("::", "/") + INDEX_FILE_EXTENSION);
    }

    static String getMetadataPartitionBinFilePath(String metadataName, int partitionId)
    {
        return (metadataName == null) ?
                (BINARIES_DIRNAME + partitionId + BIN_FILE_EXTENSION) :
                (BINARIES_DIRNAME + metadataName + "/" + partitionId + BIN_FILE_EXTENSION);
    }

    // Strings

    static String getClassifierIdStringsIndexFilePath(String metadataName)
    {
        return (metadataName == null) ?
                (STRINGS_DIRNAME + "classifiers" + INDEX_FILE_EXTENSION) :
                (STRINGS_DIRNAME + metadataName + "/classifiers" + INDEX_FILE_EXTENSION);
    }

    static String getOtherStringsIndexFilePath(String metadataName)
    {
        return (metadataName == null) ?
                (STRINGS_DIRNAME + "other" + INDEX_FILE_EXTENSION) :
                (STRINGS_DIRNAME + metadataName + "/other" + INDEX_FILE_EXTENSION);
    }

    static String getOtherStringsIndexPartitionFilePath(String metadataName, int partitionId)
    {
        return (metadataName == null) ?
                (STRINGS_DIRNAME + "other-" + partitionId + INDEX_FILE_EXTENSION) :
                (STRINGS_DIRNAME + metadataName + "/other-" + partitionId + INDEX_FILE_EXTENSION);
    }
}
