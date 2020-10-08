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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;

import javax.lang.model.SourceVersion;

public class JavaTools
{
    public static final ImmutableSet<String> JAVA_KEYWORDS = Sets.immutable.with("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while");

    public static final Function2<String, String, String> MAKE_VALID_JAVA_IDENTIFIER = new Function2<String, String, String>()
    {
        @Override
        public String value(String string, String fixChar)
        {
            return makeValidJavaIdentifier(string, fixChar);
        }
    };

    private JavaTools()
    {
        // Utility class
    }

    /**
     * Make string into a valid Java identifier.
     *
     * @param string candidate Java identifier
     * @return valid Java identifier
     */
    public static String makeValidJavaIdentifier(String string)
    {
        return makeValidJavaIdentifier(string, "$");
    }

    /**
     * Make string into a valid Java identifier.
     *
     * @param string  candidate Java identifier
     * @param fixChar string to use to fix invalid identifiers
     * @return valid Java identifier
     */
    public static String makeValidJavaIdentifier(String string, String fixChar)
    {
        if (string == null)
        {
            return fixChar;
        }

        int length = string.length();
        if (length == 0)
        {
            return fixChar;
        }

        if (JAVA_KEYWORDS.contains(string))
        {
            return fixChar + string;
        }

        StringBuilder builder = null;
        int codePoint = string.codePointAt(0);
        int codePointLen = Character.charCount(codePoint);
        if (!Character.isJavaIdentifierStart(codePoint))
        {
            builder = new StringBuilder(length + 1);
            builder.append(fixChar);
            if (Character.isJavaIdentifierPart(codePoint))
            {
                builder.append(string, 0, codePointLen);
            }
        }
        int index = codePointLen;
        while (index < length)
        {
            codePoint = string.codePointAt(index);
            codePointLen = Character.charCount(codePoint);
            if (Character.isJavaIdentifierPart(codePoint))
            {
                if (builder != null)
                {
                    builder.append(string, index, index + codePointLen);
                }
            }
            else
            {
                if (builder == null)
                {
                    builder = new StringBuilder(length);
                    builder.append(string, 0, index);
                }
                builder.append(fixChar);
            }
            index += codePointLen;
        }

        return (builder == null) ? string : builder.toString();
    }
}
