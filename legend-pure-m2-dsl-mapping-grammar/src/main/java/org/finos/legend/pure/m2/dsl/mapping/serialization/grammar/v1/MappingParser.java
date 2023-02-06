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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingLexer;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.antlr.MappingGraphBuilder;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.AssociationImplementationProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.MappingProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.PropertyOwnerImplementationProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.PureInstanceSetImplementationProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.SetImplementationProcessor;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.AssociationImplementationUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.InstanceSetImplementationUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.MappingUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.PropertyMappingsImplementationUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.PropertyOwnerImplementationUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.PureInstanceSetImplementationUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader.SetImplementationUnbind;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.MappingValidator;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.PureInstanceSetImplementationValidator;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.StoreValidator;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.MappingIncludeUnloaderWalk;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.MappingUnloaderWalk;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.PropertyMappingUnloaderWalk;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.PropertyMappingValueSpecificationContextUnloaderWalk;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.PropertyOwnerImplementationWalker;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.walker.SubstituteStoreUnloaderWalk;
import org.finos.legend.pure.m2.dsl.mapping.serialization.runtime.binary.reference.EnumerationMappingReferenceSerializer;
import org.finos.legend.pure.m2.dsl.mapping.serialization.runtime.binary.reference.SetImplementationReferenceSerializer;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.MappingCoreInstanceFactoryRegistry;
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

public class MappingParser implements Parser
{
    private ParserLibrary parserLibrary;

    public MappingParser(){}

    public MappingParser(ParserLibrary parserLibrary)
    {
        this.parserLibrary = parserLibrary;
    }

    @Override
    public String getName()
    {
        return "Mapping";
    }

    @Override
    public void parse(String string, String sourceName, boolean addLines, int offset, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        String result = parseDefinition(true, string, sourceName, addLines, offset, repository, listener, context, count);
        new M3AntlrParser(false).parse(result, sourceName, false, offset, repository, coreInstancesResult, listener, context, count, null);

    }

    private String parseDefinition(boolean useFastParser, String code, String sourceName, boolean addLines, int offset, ModelRepository repository, M3M4StateListener listener, Context context, int count)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offset, 0, sourceName, addLines);
        org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParser parser = this.initAntlrParser(useFastParser, code, sourceInformation);
        try
        {
            MappingGraphBuilder visitor = new MappingGraphBuilder(repository, context, count, this.parserLibrary, sourceInformation);
            org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParser.DefinitionContext c = parser.definition();
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
    public String parseMapping(String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        throw new RuntimeException("Not Supported");
    }

    @Override
    public Parser newInstance(ParserLibrary parserLibrary)
    {
        return new MappingParser(parserLibrary);
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.<MatchRunner>with(new MappingProcessor(), new PropertyOwnerImplementationProcessor(),new SetImplementationProcessor(),new AssociationImplementationProcessor(), new PureInstanceSetImplementationProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.<MatchRunner>with(new MappingUnloaderWalk(), new MappingIncludeUnloaderWalk(), new SubstituteStoreUnloaderWalk(),
                new PropertyOwnerImplementationWalker(),new PropertyMappingUnloaderWalk(), new PropertyMappingValueSpecificationContextUnloaderWalk());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.<MatchRunner>with(new PropertyMappingsImplementationUnbind(), new MappingUnbind(), new PropertyOwnerImplementationUnbind(), new InstanceSetImplementationUnbind(), new SetImplementationUnbind(), new AssociationImplementationUnbind(), new PureInstanceSetImplementationUnbind());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.<MatchRunner>with(new MappingValidator(), new PureInstanceSetImplementationValidator(), new StoreValidator());
    }

    @Override
    public RichIterable<NavigationHandler> getNavigationHandlers()
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers()
    {
        return Lists.immutable.<ExternalReferenceSerializer>with(new EnumerationMappingReferenceSerializer(), new SetImplementationReferenceSerializer());
    }

    @Override
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.with("Pure");
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.with("/platform_dsl_mapping/grammar/mapping.pure");
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(MappingCoreInstanceFactoryRegistry.REGISTRY);
    }

    private static org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParser initAntlrParser(boolean fastParser, String code, AntlrSourceInformation sourceInformation)
    {
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(sourceInformation);

        MappingLexer lexer = new MappingLexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParser parser = new org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.MappingParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }

}
