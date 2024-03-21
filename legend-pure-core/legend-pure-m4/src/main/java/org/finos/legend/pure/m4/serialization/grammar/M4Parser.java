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

package org.finos.legend.pure.m4.serialization.grammar;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrDescriptiveErrorListener;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureAntlrErrorStrategy;
import org.finos.legend.pure.m4.statelistener.M4StateListener;

public class M4Parser
{
    public MutableList<CoreInstance> parse(String pureCode, ModelRepository repository, M4StateListener stateListener)
    {
        stateListener.startParsingM4(pureCode);
        MutableList<CoreInstance> results = parseDefinition(true, pureCode, "fromString", repository, false);
        stateListener.finishedParsingM4(pureCode);
        return results;
    }

    public MutableList<CoreInstance> parse(String expression, String fileName, ModelRepository repository, M4StateListener stateListener)
    {
        stateListener.startParsingM4(fileName);
        MutableList<CoreInstance> results = parseDefinition(true, expression, fileName, repository, true);
        stateListener.finishedParsingM4(fileName);
        return results;
    }

    private MutableList<CoreInstance> parseDefinition(boolean useFastParser, String code, String sourceName, ModelRepository repository, boolean addLines)
    {
        M4AntlrParser parser = this.initAntlrParser(useFastParser, code, sourceName);

        M4GraphBuilder visitor = new M4GraphBuilder(sourceName, repository, addLines);
        M4AntlrParser.DefinitionContext c = parser.definition();

        return visitor.visitDefinition(c);
    }

    private M4AntlrParser initAntlrParser(boolean fastParser, String code, String sourceName)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(0, 0, sourceName);
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        M4AntlrLexer lexer = new M4AntlrLexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        M4AntlrParser parser = new M4AntlrParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }
}
