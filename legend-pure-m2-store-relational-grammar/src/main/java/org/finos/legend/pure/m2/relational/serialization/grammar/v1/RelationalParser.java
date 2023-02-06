// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalGraphBuilder;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.EmbeddedRelationalInstanceSetImplementationNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.FilterMappingNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.JoinTreeNodeNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.RelationalAssociationImplementationNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.RelationalPropertyMappingNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.TableAliasColumnNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.navigation.TableAliasNavigationHandler;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.DatabaseProcessor;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.RelationalAssociationImplementationProcessor;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.RelationalInstanceSetImplementationProcessor;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.DatabaseUnloadUnbind;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.DatabaseUnloadWalker;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.FilterMappingUnloadWalker;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.JoinTreeNodeUnloadWalker;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.RelationalAssociationImplementationUnbind;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.RelationalInstanceSetImplementationUnloadUnbind;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.TableAliasColumnUnloadWalker;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.unloader.TableAliasUnloadWalker;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.validator.RelationalAssociationImplementationValidator;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.validator.RelationalInstanceSetImplementationValidator;
import org.finos.legend.pure.m2.relational.serialization.runtime.binary.reference.ColumnReferenceSerializer;
import org.finos.legend.pure.m2.relational.serialization.runtime.binary.reference.FilterReferenceSerializer;
import org.finos.legend.pure.m2.relational.serialization.runtime.binary.reference.JoinReferenceSerializer;
import org.finos.legend.pure.m2.relational.serialization.runtime.binary.reference.TableReferenceSerializer;
import org.finos.legend.pure.m2.relational.serialization.runtime.binary.reference.ViewReferenceSerializer;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.RelationalStoreCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
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

public class RelationalParser implements IRelationalParser
{
    private AntlrSourceInformation sourceInformation;
    private ParserLibrary parserLibrary;

    public RelationalParser()
    {

    }

    public RelationalParser(ParserLibrary parserLibrary)
    {
        this.parserLibrary = parserLibrary;
    }

    @Override
    public String getName()
    {
        return "Relational";
    }

    public void parse(String code, String sourceName, boolean addLines, int offset, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException
    {
        String importId = AntlrContextToM3CoreInstance.createImportGroupId(sourceName, count);
        String result = parseDefinition(true, code, sourceName, addLines, offset, repository, listener, context, count, importId);
        new M3AntlrParser(false).parse(result, sourceName, false, offset, repository, coreInstancesResult, listener, context, count, null);
    }

    private String parseDefinition(boolean useFastParser, String code, String sourceName, boolean addLines, int offset, ModelRepository repository, M3M4StateListener listener, Context context, int count, String importId)
    {
        org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser parser = this.initAntlrParser(useFastParser, code, sourceName, offset, 0, addLines);
        try
        {
            RelationalGraphBuilder visitor = new RelationalGraphBuilder(importId, this.sourceInformation, this.parserLibrary);
            return visitor.visitDefinition(parser.definition());
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
//                System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseDefinition(false, code, sourceName, addLines, offset, repository, listener, context, count, importId);
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
        return parseMapping(true, content, id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath, sourceName, offset, importId, repository, context);
    }

    private String parseMapping(boolean useFastParser, String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException
    {
        org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser parser = this.initAntlrParser(useFastParser, content, sourceName, offset, 0, true);

        try
        {
            RelationalGraphBuilder visitor = new RelationalGraphBuilder(importId, this.sourceInformation, this.parserLibrary);
            return visitor.visitMappingBlock(parser.mapping(), id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath);
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
//                System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseMapping(false, content, id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath, sourceName, offset, importId, repository, context);
            }
            else
            {
                throw e;
            }
        }
    }

    public String parseMilestoningDefinition(String type, String content, String sourceName, int rowOffset, int colOffset, String importId)
    {
        return parseMilestoningDefinition(true, type, content, sourceName, rowOffset, colOffset, importId);
    }

    private String parseMilestoningDefinition(Boolean useFastParser, String type, String content, String sourceName, int rowOffset, int colOffset, String importId)
    {
        org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser parser = this.initAntlrParser(useFastParser, content, sourceName, rowOffset, colOffset, true);

        try
        {
            if ("business".equals(type))
            {
                RelationalGraphBuilder visitor = new RelationalGraphBuilder(importId, this.sourceInformation, this.parserLibrary);
                return visitor.visitBusinessMilestoningInnerDefinition(parser.businessMilestoningInnerDefinition());
            }
            if ("processing".equals(type))
            {
                RelationalGraphBuilder visitor = new RelationalGraphBuilder(importId, this.sourceInformation, this.parserLibrary);
                return visitor.visitProcessingMilestoningInnerDefinition(parser.processingMilestoningInnerDefinition());
            }
            return null;
        }
        catch (Exception e)
        {
            if (isAntlrRecognitionExceptionUsingFastParser(useFastParser, e))
            {
//                System.err.println("Error using fast Antlr Parser: " + ExceptionUtils.getStackTrace(e));
                return this.parseMilestoningDefinition(false, type, content, sourceName, rowOffset, colOffset, importId);
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
        return Lists.immutable.with(new DatabaseProcessor(), new RelationalInstanceSetImplementationProcessor(), new RelationalAssociationImplementationProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.with(new DatabaseUnloadWalker(), new TableAliasUnloadWalker(), new TableAliasColumnUnloadWalker(), new JoinTreeNodeUnloadWalker(), new FilterMappingUnloadWalker());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new DatabaseUnloadUnbind(), new RelationalInstanceSetImplementationUnloadUnbind(), new RelationalAssociationImplementationUnbind());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.with(new RelationalInstanceSetImplementationValidator(), new RelationalAssociationImplementationValidator());
    }

    @Override
    public RichIterable<NavigationHandler> getNavigationHandlers()
    {
        return Lists.immutable.with(new TableAliasNavigationHandler(), new JoinTreeNodeNavigationHandler(), new TableAliasColumnNavigationHandler(), new FilterMappingNavigationHandler(), new RelationalAssociationImplementationNavigationHandler(), new RelationalPropertyMappingNavigationHandler(), new EmbeddedRelationalInstanceSetImplementationNavigationHandler());
    }

    @Override
    public RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers()
    {
        return Lists.immutable.with(new TableReferenceSerializer(), new ViewReferenceSerializer(), new ColumnReferenceSerializer(), new JoinReferenceSerializer(), new FilterReferenceSerializer());
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(RelationalStoreCoreInstanceFactoryRegistry.REGISTRY);
    }

    @Override
    public Parser newInstance(ParserLibrary library)
    {
        return new RelationalParser(library);
    }

    @Override
    public SetIterable<String> getRequiredParsers()
    {
        return Sets.immutable.with("Pure", "Mapping");
    }

    @Override
    public ListIterable<String> getRequiredFiles()
    {
        return Lists.immutable.with("/platform_store_relational/grammar/relational.pure",
                "/platform_store_relational/grammar/relationalMapping.pure",
                "/platform_store_relational/relationalRuntime.pure",
                "/platform_store_relational/relationalLogging.pure"
                );
    }

    private org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser initAntlrParser(boolean fastParser, String code, String sourceName, int offsetLine, int offsetColumn, boolean addLines)
    {
        this.sourceInformation = new AntlrSourceInformation(offsetLine, offsetColumn, sourceName, addLines);
        AntlrDescriptiveErrorListener pureErrorListener = new AntlrDescriptiveErrorListener(this.sourceInformation);

        org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalLexer lexer = new org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalLexer(new ANTLRInputStream(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(pureErrorListener);

        org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser parser = new org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(pureErrorListener);
        parser.setErrorHandler(new PureAntlrErrorStrategy(this.sourceInformation));
        parser.getInterpreter().setPredictionMode(fastParser ? PredictionMode.SLL : PredictionMode.LL);
        return parser;
    }

}
