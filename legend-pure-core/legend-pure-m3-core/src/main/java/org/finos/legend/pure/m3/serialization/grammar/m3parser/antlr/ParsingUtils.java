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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.antlr.v4.runtime.RecognitionException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public final class ParsingUtils
{

    public static StringBuilder removeLastCommaCharacterIfPresent(StringBuilder sb)
    {
        return sb.length() > 0 && sb.charAt(sb.length() - 1) == ',' ? sb.deleteCharAt(sb.length() - 1) : sb;
    }

    public static boolean isAntlrRecognitionExceptionUsingFastParser(boolean parseFast, Exception e)
    {
        return parseFast && e instanceof PureParserException && e.getCause() instanceof RecognitionException;
    }
}
