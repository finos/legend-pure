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

import org.eclipse.collections.impl.utility.StringIterate;

import javax.lang.model.SourceVersion;

public class JavaTools
{
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
     * @param string candidate Java identifier
     * @param fix    string to use to fix invalid identifiers
     * @return valid Java identifier
     */
    public static String makeValidJavaIdentifier(String string, String fix)
    {
        if ((string == null) || string.isEmpty())
        {
            if (!SourceVersion.isIdentifier(fix) || SourceVersion.isKeyword(fix))
            {
                throw new IllegalStateException("Cannot replace null or empty string with \"" + fix + "\", as it is not a valid Java identifier");
            }
            return fix;
        }

        if (SourceVersion.isKeyword(string))
        {
            if (isValidAtStart(fix))
            {
                return fix + string;
            }
            if (isValidPart(fix))
            {
                return string + fix;
            }
            throw new IllegalStateException("\"" + string + "\" is a Java keyword, but fix (\"" + fix + "\") is not a valid Java identifier part");
        }

        int length = string.length();
        StringBuilder builder = null;
        int codePoint = string.codePointAt(0);
        int codePointLen = Character.charCount(codePoint);
        if (!Character.isJavaIdentifierStart(codePoint))
        {
            if (!isValidAtStart(fix))
            {
                throw new IllegalStateException("First character of \"" + string + "\" needs to be replaced, but replacement (\"" + fix + "\") is not a valid Java identifier start");
            }
            builder = new StringBuilder(length + fix.length()).append(fix);
            if (Character.isJavaIdentifierPart(codePoint))
            {
                builder.append(string, 0, codePointLen);
            }
        }
        for (int index = codePointLen; index < length; index += codePointLen)
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
                    if (!isValidPart(fix))
                    {
                        throw new IllegalStateException("Character " + index + " of \"" + string + "\" needs to be replaced, but replacement (\"" + fix + "\") is not a valid Java identifier part");
                    }
                    builder = new StringBuilder(length + (fix.length() - 1));
                    builder.append(string, 0, index);
                }
                builder.append(fix);
            }
        }

        return (builder == null) ? string : builder.toString();
    }

    private static boolean isValidAtStart(String fix)
    {
        return isValidPart(fix) && Character.isJavaIdentifierStart(fix.codePointAt(0));
    }

    private static boolean isValidPart(String fix)
    {
        return (fix != null) && !fix.isEmpty() && StringIterate.allSatisfyCodePoint(fix, Character::isJavaIdentifierPart);
    }
}
