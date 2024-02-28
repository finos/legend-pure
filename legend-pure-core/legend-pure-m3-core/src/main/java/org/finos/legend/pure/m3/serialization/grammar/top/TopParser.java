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

package org.finos.legend.pure.m3.serialization.grammar.top;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.top.antlr.TopAntlrLexer;
import org.finos.legend.pure.m3.serialization.grammar.top.antlr.TopAntlrParser;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrDescriptiveErrorListener;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureAntlrErrorStrategy;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class TopParser
{
    public ListMultimap<Parser, CoreInstance> parse(String code, String sourceName, ModelRepository repository, ParserLibrary parserLibrary, M3M4StateListener listener, Context context, SourceState oldState) throws PureParserException
    {
        return parseDefinition(true, code, sourceName, repository, parserLibrary, listener, context, oldState);
    }

    private MutableListMultimap<Parser, CoreInstance> parseDefinition(boolean useFastParser, String code, String sourceName, ModelRepository repository, ParserLibrary parserLibrary, M3M4StateListener listener, Context context, SourceState oldState)
    {
        TopAntlrParser parser = this.initAntlrParser(useFastParser, "\u005cn###Pure\u005cn" + code, sourceName);

        TopGraphBuilder visitor = new TopGraphBuilder(sourceName, repository, listener, context, parserLibrary, oldState);
        TopAntlrParser.DefinitionContext c = parser.definition();
        return visitor.visitDefinition(c);
    }

    private TopAntlrParser initAntlrParser(boolean fastParser, String code, String sourceName)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(0, 0, sourceName);
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        TopAntlrLexer lexer = new TopAntlrLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        TopAntlrParser parser = new TopAntlrParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }
}
