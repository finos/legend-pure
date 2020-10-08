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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;

import static org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.DefinitionContext;

public class M3AntlrTreeWalker extends org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3ParserBaseVisitor<CoreInstance>
{

    private final ModelRepository repository;

    private final M3M4StateListener listener;

    private final ImportGroup importId;

    private final AntlrContextToM3CoreInstance antlrContextToM3Builder;

    private final boolean useImportStubsInInstanceParser;

    private final String classPathForMapping;


    public M3AntlrTreeWalker(String classPathForMapping, AntlrSourceInformation antlrSourceInformation, InlineDSLLibrary inlineDSLLibrary, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, ImportGroup importId, int count, SourceState oldState)
    {
        this(classPathForMapping, antlrSourceInformation, inlineDSLLibrary, repository, coreInstancesResult, listener, context, importId, count, true, true, oldState);
    }

    public M3AntlrTreeWalker(AntlrSourceInformation antlrSourceInformation, InlineDSLLibrary inlineDSLLibrary, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, ImportGroup importId, int count, SourceState oldState)
    {
        this("", antlrSourceInformation, inlineDSLLibrary, repository, coreInstancesResult, listener, context, importId, count, true, true, oldState);
    }

    public M3AntlrTreeWalker(AntlrSourceInformation antlrSourceInformation, InlineDSLLibrary inlineDSLLibrary, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, ImportGroup importId, int count, boolean useImportStubsInInstanceParser, boolean addLines, SourceState oldState)
    {
        this("", antlrSourceInformation, inlineDSLLibrary, repository, coreInstancesResult, listener, context, importId, count, useImportStubsInInstanceParser, addLines, oldState);
    }

    public M3AntlrTreeWalker(String classPathForMapping, AntlrSourceInformation antlrSourceInformation, InlineDSLLibrary inlineDSLLibrary, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, ImportGroup importId, int count, boolean useImportStubsInInstanceParser, boolean addLines, SourceState oldState)
    {
        this.repository = repository;
        this.listener = listener;
        this.importId = importId;
        this.antlrContextToM3Builder = new AntlrContextToM3CoreInstance(context, repository, new M3ProcessorSupport(context, repository), antlrSourceInformation, inlineDSLLibrary, coreInstancesResult, count, addLines, oldState);
        this.useImportStubsInInstanceParser = useImportStubsInInstanceParser;
        this.classPathForMapping = classPathForMapping;
   }

    @Override
    public CoreInstance visitDefinition(DefinitionContext ctx)
    {
        this.listener.startParsingM3(ctx.getText());
        CoreInstance coreInstance = this.antlrContextToM3Builder.definition(ctx, this.useImportStubsInInstanceParser);
        this.listener.finishedParsingM3(ctx.getText());
        return coreInstance;
    }

    @Override
    public CoreInstance visitInstance(InstanceContext ctx)
    {
        return this.antlrContextToM3Builder.instanceParser(ctx, true, this.importId, false, "", false, false);
    }

    @Override
    public CoreInstance visitTreePath(TreePathContext ctx)
    {
        return this.antlrContextToM3Builder.treePath(ctx, this.importId);
    }

    @Override
    public CoreInstance visitType(TypeContext ctx)
    {
        return this.antlrContextToM3Builder.type(ctx, Lists.mutable.<String>empty(), "", this.importId, true);
    }

    TemporaryPureSetImplementation walkMapping(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MappingContext ctx, AntlrContextToM3CoreInstance.LambdaContext lambdaContext)
    {
        return this.antlrContextToM3Builder.mapping(ctx, this.classPathForMapping, lambdaContext, this.importId);
    }

    TemporaryPureAggregateSpecification walkAggregateSpecification(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AggregateSpecificationContext ctx, AntlrContextToM3CoreInstance.LambdaContext lambdaContext, int index)
    {
        return this.antlrContextToM3Builder.aggregateSpecification(ctx, this.importId, lambdaContext, index);
    }

    public TemporaryPurePropertyMapping walkCombinedExpression(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.CombinedExpressionContext ctx, String property, AntlrContextToM3CoreInstance.LambdaContext lambdaContext)
    {
        return this.antlrContextToM3Builder.combinedExpression(ctx, property, lambdaContext, this.importId);
    }

    public CoreInstance walkCombinedExpression(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.CombinedExpressionContext ctx, AntlrContextToM3CoreInstance.LambdaContext lambdaContext)
    {
        return this.antlrContextToM3Builder.combinedExpression(ctx, "", FastList.<String>newList(), lambdaContext, "", true, importId, true);
    }
}
