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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.antlr;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParserBaseVisitor;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPureAggregateSpecification;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPureAggregationFunctionSpecification;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPureGroupByFunctionSpecification;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.antlr.v4.runtime.Token;

import static org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils.removeLastCommaCharacterIfPresent;

public class AggregationAwareGraphBuilder extends AggregationAwareParserBaseVisitor<String>
{
    private final String id;
    private final String setSourceInfo;
    private final boolean root;
    private final String classPath;
    private final String classSourceInfo;
    private final String mappingPath;
    private final String importId;
    private final ModelRepository repository;
    private final Context context;
    private final ParserLibrary parserLibrary;

    private final StringBuilder aggregateSetImplementations = new StringBuilder();
    private String mainSetImplementation;
    private int index = 0;
    private final AntlrSourceInformation sourceInformation;

    public AggregationAwareGraphBuilder(String id, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String importId, ModelRepository repository, Context context, ParserLibrary parserLibrary, AntlrSourceInformation sourceInformation)
    {
        this.id = id;
        this.setSourceInfo = setSourceInfo;
        this.root = root;
        this.classPath = classPath;
        this.classSourceInfo = classSourceInfo;
        this.mappingPath = mappingPath;
        this.importId = importId;
        this.repository = repository;
        this.context = context;
        this.parserLibrary = parserLibrary;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public String visitMapping(AggregationAwareParser.MappingContext ctx)
    {
        visitChildren(ctx);
        return "^meta::pure::mapping::aggregationAware::AggregationAwareSetImplementation" + setSourceInfo + "(" +
                ((id == null) ? "" : ("id = '" + id + "',")) +
                "root = " + root + "," +
                "class = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                "aggregateSetImplementations=[" + aggregateSetImplementations.toString() + "]," +
                "mainSetImplementation=" + mainSetImplementation +
                ")";
    }

    @Override
    public String visitAggregationSpecification(AggregationAwareParser.AggregationSpecificationContext ctx)
    {
        String aggSpecSourceInfo = " " + sourceInformation.getPureSourceInformation(ctx.GROUP_OPEN().getSymbol(), ctx.GROUP_OPEN().getSymbol(), ctx.GROUP_CLOSE().getSymbol()).toM4String();
        aggregateSetImplementations.append("^meta::pure::mapping::aggregationAware::AggregateSetImplementationContainer " + aggSpecSourceInfo + " (" + "index=" + index + "," + "aggregateSpecification=^meta::pure::mapping::aggregationAware::AggregateSpecification " + aggSpecSourceInfo + " (");

        visitChildren(ctx);
        return null;
    }

    @Override
    public String visitMainMapping(AggregationAwareParser.MainMappingContext ctx)
    {
        removeLastCommaCharacterIfPresent(aggregateSetImplementations);
        mainSetImplementation = parseSingleMapping(ctx.parserName().VALID_STRING().getSymbol(), ctx.CONTENT().getSymbol(), ctx.CURLY_BRACKET_CLOSE().getSymbol());
        return null;
    }

    @Override
    public String visitModelOperation(AggregationAwareParser.ModelOperationContext ctx)
    {
        Pair<Boolean, Pair<String, String>> aggregateSpecificationParseResult = parseAggregateSpecification(ctx.CONTENT().getText(), ctx.CONTENT().getSymbol().getLine(), index);

        aggregateSetImplementations.append(

                "canAggregate=" + (aggregateSpecificationParseResult.getOne() ? "true" : "false") + "," +
                        "groupByFunctions=[" + aggregateSpecificationParseResult.getTwo().getOne() + "]," +
                        "aggregateValues=[" + aggregateSpecificationParseResult.getTwo().getTwo() + "]),");
        index++;
        return null;
    }

    @Override
    public String visitAggregateMapping(AggregationAwareParser.AggregateMappingContext ctx)
    {
        String subSetImplementation = parseSingleMapping(ctx.parserName().VALID_STRING().getSymbol(), ctx.CONTENT().getSymbol(), ctx.CURLY_BRACKET_CLOSE().getSymbol());
        aggregateSetImplementations.append("setImplementation=" + subSetImplementation + ")");
        aggregateSetImplementations.append(",");
        return null;
    }

    final public Pair<Boolean, Pair<String, String>> parseAggregateSpecification(String content, int beginLine, int index)
    {
        final M3AntlrParser parser = new M3AntlrParser();
        final M3ProcessorSupport processorSupport = new M3ProcessorSupport(context, repository);
        String mappingName = mappingPath.replace("::", "_");
        String classMappingName = classPath.replace("::", "_");
        final AntlrContextToM3CoreInstance.LambdaContext lambdaContext = new AntlrContextToM3CoreInstance.LambdaContext(mappingName + '_' + classMappingName + (id == null ? "" : '_' + id) + "_AggregationAware_" + index);
        TemporaryPureAggregateSpecification temporarySpecification = parser.parseAggregateSpecification(content, lambdaContext, sourceInformation.getSourceName(), sourceInformation.getOffsetLine() + beginLine - 1, importId, index, repository, processorSupport, context);

        final MutableList<String> groupByFunctionSpecifications = Lists.mutable.with();
        temporarySpecification.groupByFunctionSpecifications.forEach(new Procedure<TemporaryPureGroupByFunctionSpecification>()
        {
            @Override
            public void value(TemporaryPureGroupByFunctionSpecification groupByFunctionSpecification)
            {
                groupByFunctionSpecifications.add(
                        "^meta::pure::mapping::aggregationAware::GroupByFunctionSpecification " + groupByFunctionSpecification.sourceInformation.toM4String() + " (" +
                                "groupByFn=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + " " + groupByFunctionSpecification.groupByExpression.getSourceInformation().toM4String() + " (" +
                                "classifierGenericType=^meta::pure::metamodel::type::generics::GenericType(rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType(rawType = ^meta::pure::metamodel::type::FunctionType()))," +
                                "expressionSequence=" + parser.process(groupByFunctionSpecification.groupByExpression, context, processorSupport) +
                                ")" +
                                ")"
                );
            }
        });

        final MutableList<String> aggregationFunctionSpecifications = Lists.mutable.with();
        temporarySpecification.aggregationFunctionSpecifications.forEach(new Procedure<TemporaryPureAggregationFunctionSpecification>()
        {
            @Override
            public void value(TemporaryPureAggregationFunctionSpecification aggregationFunctionSpecification)
            {
                aggregationFunctionSpecifications.add(
                        "^meta::pure::mapping::aggregationAware::AggregationFunctionSpecification " + aggregationFunctionSpecification.sourceInformation.toM4String() + " (" +
                                "mapFn=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + " " + aggregationFunctionSpecification.mapExpression.getSourceInformation().toM4String() + " (" +
                                "classifierGenericType=^meta::pure::metamodel::type::generics::GenericType(rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType(rawType = ^meta::pure::metamodel::type::FunctionType()))," +
                                "expressionSequence=" + parser.process(aggregationFunctionSpecification.mapExpression, context, processorSupport) +
                                ")," +
                                "aggregateFn=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + " " + aggregationFunctionSpecification.aggregateExpression.getSourceInformation().toM4String() + " (" +
                                "classifierGenericType=^meta::pure::metamodel::type::generics::GenericType(rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType(rawType = ^meta::pure::metamodel::type::FunctionType()))," +
                                "expressionSequence=" + parser.process(aggregationFunctionSpecification.aggregateExpression, context, processorSupport) +
                                ")" +
                                ")"
                );
            }
        });

        return Tuples.pair(temporarySpecification.canAggregate, Tuples.pair(groupByFunctionSpecifications.makeString(","), aggregationFunctionSpecifications.makeString(",")));
    }

    String parseSingleMapping(Token parserNameTok, Token content, Token end)
    {
        SourceInformation sourceInfo = sourceInformation.getPureSourceInformation(parserNameTok, parserNameTok, end);
        String newSetSourceInfo = " " + sourceInfo.toM4String();
        String parserName = parserNameTok.getText();
        Parser parser;
        try
        {
            parser = this.parserLibrary.getParser(parserName);
        }
        catch (RuntimeException e)
        {
            throw new PureParserException(sourceInfo, e.getMessage(), e);
        }
        try
        {
            return parser.parseMapping(content.getText(), id == null ? null : id.toString() + "_Main", null, newSetSourceInfo, true, classPath, classSourceInfo, mappingPath, sourceInformation.getSourceName(), sourceInformation.getOffsetLine() + content.getLine() - 1, importId, repository, context);
        }
        catch (PureException e)
        {
            if (e.getSourceInformation() != null)
            {
                throw e;
            }
            throw new PureParserException(sourceInfo, e.getInfo(), e);
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            if (pe != null && pe.getSourceInformation() != null)
            {
                throw e;
            }
            throw new PureParserException(sourceInfo, e.getMessage(), e);
        }

    }

}
