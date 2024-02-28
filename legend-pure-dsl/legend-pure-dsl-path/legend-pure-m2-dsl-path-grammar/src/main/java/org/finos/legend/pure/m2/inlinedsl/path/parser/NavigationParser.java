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

package org.finos.legend.pure.m2.inlinedsl.path.parser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationLexer;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrDescriptiveErrorListener;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureAntlrErrorStrategy;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class NavigationParser
{
    private AntlrSourceInformation sourceInformation;

    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetColumn, int offsetLine, ModelRepository repository, Context context) throws PureParserException
    {
        return parseDefinition(true, code, fileName, true, offsetLine - 1, offsetColumn - 1, repository, context, importId);
    }

    private CoreInstance parseDefinition(boolean useFastParser, String code, String sourceName, boolean addLines, int offsetLine, int offsetColumn, ModelRepository repository, Context context, ImportGroup importId)
    {
        org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser parser = this.initAntlrParser(useFastParser, code, sourceName, offsetLine, offsetColumn, addLines);
        try
        {
            NavigationGraphBuilder visitor = new NavigationGraphBuilder(importId, this.sourceInformation, repository, context);
            return visitor.visitDefinition(parser.definition());
        }
        catch (Exception e)
        {
            if (ParsingUtils.isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                return this.parseDefinition(false, code, sourceName, false, offsetLine, offsetColumn, repository, context, importId);
            }
            throw e;
        }
    }

    private org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser initAntlrParser(boolean fastParser, String code, String sourceName, int offsetLine, int offsetColumn, boolean addLines)
    {
        this.sourceInformation = new AntlrSourceInformation(offsetLine, offsetColumn, sourceName, addLines);
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(this.sourceInformation);

        NavigationLexer lexer = new NavigationLexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser parser = new org.finos.legend.pure.m2.inlinedsl.path.serialization.grammar.NavigationParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(this.sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }
}
