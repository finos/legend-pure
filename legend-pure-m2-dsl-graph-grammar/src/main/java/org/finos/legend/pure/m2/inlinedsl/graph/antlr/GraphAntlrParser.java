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

package org.finos.legend.pure.m2.inlinedsl.graph.antlr;

import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphLexer;
import org.finos.legend.pure.m2.inlinedsl.graph.serialization.grammar.GraphParser;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrDescriptiveErrorListener;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureAntlrErrorStrategy;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

public class GraphAntlrParser
{
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetColumn, int offsetLine, ModelRepository repository, Context context) throws PureParserException
    {
        return this.parseDefinition(true, code, importId, fileName, offsetColumn - 1, offsetLine - 1, repository, context);
    }

    private CoreInstance parseDefinition(boolean fastParser, String code, ImportGroup importId, String fileName, int offsetColumn, int offsetLine, ModelRepository repository, Context context) throws PureParserException
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offsetLine, offsetColumn, fileName, true);
        GraphParser graphParser = this.initializeParser(sourceInformation, fastParser, code);
        try
        {
            GraphAntlrTreeWalker treeWalker = new GraphAntlrTreeWalker(sourceInformation, importId);
            String result = treeWalker.visitDefinition(graphParser.definition());
            return new M3AntlrParser(false).parseInstance(fastParser, result, fileName, offsetLine, offsetColumn, importId, repository, context);
        }
        catch (Exception e)
        {
            if (fastParser && e instanceof PureParserException && e.getCause() instanceof RecognitionException)
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseDefinition(false, code, importId, fileName, offsetColumn, offsetLine, repository, context);
            }
            else
            {
                throw e;
            }
        }
    }

    private GraphParser initializeParser(AntlrSourceInformation sourceInformation, boolean fastParser, String code)
    {
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        GraphLexer graphLexer = new GraphLexer(new ANTLRInputStream(code));
        graphLexer.removeErrorListeners();
        graphLexer.addErrorListener(pureErrorListener);

        GraphParser graphParser = new GraphParser(new CommonTokenStream(graphLexer));
        graphParser.removeErrorListeners();
        graphParser.addErrorListener(pureErrorListener);
        graphParser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        graphParser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);

        return graphParser;
    }
}
