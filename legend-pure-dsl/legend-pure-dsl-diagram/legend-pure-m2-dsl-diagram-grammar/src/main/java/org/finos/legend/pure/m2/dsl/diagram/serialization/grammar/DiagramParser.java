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

package org.finos.legend.pure.m2.dsl.diagram.serialization.grammar;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.antlr.DiagramStoreGraphBuilder;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.processor.DiagramProcessor;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.unloader.AssociationViewUnloaderWalk;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.unloader.DiagramUnbind;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.validation.DiagramValidator;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.walker.DiagramUnloaderWalk;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.walker.GeneralizationViewUnloaderWalk;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.walker.PropertyViewUnloaderWalk;
import org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.walker.TypeViewUnloaderWalk;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.DiagramCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrDescriptiveErrorListener;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureAntlrErrorStrategy;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import static org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils.isAntlrRecognitionExceptionUsingFastParser;

public class DiagramParser implements Parser
{
    @Override
    public String getName()
    {
        return "Diagram";
    }

    @Override
    public void parse(String code, String sourceName, boolean addLines, int offset, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        String result = this.parseDefinition(true, code, sourceName, addLines, offset, repository, listener, context, count);
        new M3AntlrParser(false).parse(result, sourceName, false, offset, repository, coreInstancesResult, listener, context, count, null);
    }

    @Override
    public String parseMapping(String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        return null;
    }

    private String parseDefinition(boolean useFastParser, String code, String sourceName, boolean addLines, int offset, ModelRepository repository, M3M4StateListener listener, Context context, int count)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offset, 0, sourceName, addLines);
        DiagramAntlrParser parser = initAntlrParser(useFastParser, code, sourceInformation);
        try
        {
            DiagramStoreGraphBuilder visitor = new DiagramStoreGraphBuilder(repository, count, sourceInformation);
            DiagramAntlrParser.DefinitionContext c = parser.definition();
            DiagramAntlrParser.ImportsContext imports = parser.imports();
            return visitor.visitDefinition(c);
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseDefinition(false, code, sourceName, addLines, offset, repository, listener, context, count);
            }
            else
            {
                throw e;
            }
        }

    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.<MatchRunner>with(new DiagramProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.<MatchRunner>with(new DiagramUnloaderWalk(), new TypeViewUnloaderWalk(), new AssociationViewUnloaderWalk(), new PropertyViewUnloaderWalk(), new GeneralizationViewUnloaderWalk());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.<MatchRunner>with(new DiagramUnbind());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.<MatchRunner>with(new DiagramValidator());
    }

    @Override
    public RichIterable<NavigationHandler> getNavigationHandlers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(DiagramCoreInstanceFactoryRegistry.REGISTRY);
    }

    @Override
    public Parser newInstance(ParserLibrary library)
    {
        return new DiagramParser();
    }

    @Override
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.with("Pure");
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.with("/platform_dsl_diagram/diagram.pure");
    }

    private static DiagramAntlrParser initAntlrParser(boolean fastParser, String code, AntlrSourceInformation sourceInformation)
    {
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        DiagramAntlrLexer lexer = new DiagramAntlrLexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        DiagramAntlrParser parser = new DiagramAntlrParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }
}
