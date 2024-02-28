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

package org.finos.legend.pure.m4.serialization.grammar.antlr;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class PureParserException extends PureException
{
    public PureParserException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(sourceInformation, info, cause);
    }

    public PureParserException(SourceInformation sourceInformation, String info)
    {
        this(sourceInformation, info, null);
    }

    public PureParserException(String resource, int offsetLine, int offsetColumn, String message, String text, Throwable cause)
    {
        this(new SourceInformation(resource, extractLineColumn(message).getOne() + offsetLine, extractLineColumn(message).getTwo() + offsetColumn, -1, -1), generateInfo(null, (message == null) ? "null" : (message.contains(".") ? message.substring(message.indexOf(".")) : message), text), cause);
    }

    public PureParserException(String resource, int offsetLine, int offsetColumn, String message, String text)
    {
        this(resource, offsetLine, offsetColumn, message, text, null);
    }

    public PureParserException(String resource, int offsetLine, int offsetColumn, int nextTokenBeginLine, int nextTokenBeginColumn, int nextTokenEndLine, int nextTokenEndColumn, ListIterable<? extends Pair<Integer, String>> tokens, int[][] expectedTokenSequences, String[] tokenImage, Throwable cause)
    {
        this(new SourceInformation(resource, nextTokenBeginLine + offsetLine, nextTokenBeginColumn + offsetColumn, nextTokenEndLine + offsetLine, nextTokenEndColumn + offsetColumn), generateInfo(tokens, expectedTokenSequences, tokenImage), cause);
    }

    public PureParserException(String resource, int offsetLine, int offsetColumn, int nextTokenBeginLine, int nextTokenBeginColumn, int nextTokenEndLine, int nextTokenEndColumn, ListIterable<? extends Pair<Integer, String>> tokens, int[][] expectedTokenSequences, String[] tokenImage)
    {
        this(resource, offsetLine, offsetColumn, nextTokenBeginLine, nextTokenBeginColumn, nextTokenEndLine, nextTokenEndColumn, tokens, expectedTokenSequences, tokenImage, null);
    }

    @Override
    public String getExceptionName()
    {
        return "Parser error";
    }

    @Override
    protected void writeAdditionalMessageInfo(SafeAppendable appendable)
    {
        appendable.append(getInfo());
    }

    private static String generateInfo(ListIterable<? extends Pair<Integer, String>> tokens, int[][] expectedTokenSequences, String[] tokenImage)
    {
        StringBuilder info = new StringBuilder("expected: ");
        int maxSize = 0;
        for (int i = 0; i < expectedTokenSequences.length; i++)
        {
            if (maxSize < expectedTokenSequences[i].length)
            {
                maxSize = expectedTokenSequences[i].length;
            }
            for (int j = 0; j < expectedTokenSequences[i].length; j++)
            {
                info.append(tokenImage[expectedTokenSequences[i][j]]);
                if (j < expectedTokenSequences[i].length - 1)
                {
                    info.append(' ');
                }
            }
            if (i < expectedTokenSequences.length - 1)
            {
                info.append(" or ");
            }
        }

        info.append(" found: ");
        for (int i = 0; i < maxSize; i++)
        {
            Pair<Integer, String> tok = tokens.get(i + 1);
            if (i != 0)
            {
                info.append(" ");
            }
            if (tok.getOne() == 0)
            {
                info.append(tokenImage[0]);
                break;
            }
            info.append("\"");
            info.append(tok.getTwo());
            info.append("\"");
        }

        return info.toString();
    }

    private static String generateInfo(String expected, String found, String text)
    {
        if (expected != null)
        {
            return "expected: " + expected + " found: " + found;
        }
        else if (found != null)
        {
            return "(" + found + ") in\n'" + text + "'";
        }
        else
        {
            return text;
        }
    }

    private static Pair<Integer, Integer> extractLineColumn(String message)
    {
        if ((message != null) && message.startsWith("Lexical error"))
        {
            int line = Integer.parseInt(message.substring("Lexical error at line ".length(), message.indexOf(",")));
            int column = Integer.parseInt(message.substring(message.indexOf("column") + "column".length() + 1, message.indexOf(".")));
            return Tuples.pair(line, column);
        }
        else
        {
            return Tuples.pair(-1, -1);
        }
    }
}
