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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareLexer;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.antlr.AggregationAwareGraphBuilder;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.AggregationAwareProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.AggregationAwareUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.AggregationAwareValidator;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.AggregateSpecificationValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
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

public class AggregationAwareParser implements Parser
{
    private ParserLibrary parserLibrary;

    public AggregationAwareParser()
    {
    }

    public AggregationAwareParser(ParserLibrary parserLibrary)
    {
        this.parserLibrary = parserLibrary;
    }

    @Override
    public String getName()
    {
        return "AggregationAware";
    }

    @Override
    public void parse(String string, String sourceName, boolean addLines, int offset, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String parseMapping(String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        return parseMapping(true, content, id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath, sourceName, offset, importId, repository, context);
    }

    public String parseMapping(boolean useFastParser, String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offset, 0, sourceName, true);
        org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser parser = initAntlrParser(useFastParser, content, sourceInformation);

        try
        {
            AggregationAwareGraphBuilder visitor = new AggregationAwareGraphBuilder(id, setSourceInfo, root, classPath, classSourceInfo, mappingPath, importId, repository, context, parserLibrary, sourceInformation);
            org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser.MappingContext c = parser.mapping();
            return visitor.visitMapping(c);
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
                //System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseMapping(false, content, id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath, sourceName, offset, importId, repository, context);
            }
            throw e;
        }
    }

    @Override
    public Parser newInstance(ParserLibrary library)
    {
        return new AggregationAwareParser(library);
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.with(new AggregationAwareProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.with(new AggregateSpecificationValueSpecificationContextUnloaderWalk());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new AggregationAwareUnbind());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.with(new AggregationAwareValidator());
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
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.with("Pure", "Mapping");
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.with("/platform/pure/mapping.pure");
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.empty();
    }

    private static org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser initAntlrParser(boolean fastParser, String code, AntlrSourceInformation sourceInformation)
    {
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        AggregationAwareLexer lexer = new AggregationAwareLexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser parser = new org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }
}
