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

import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.AggregationAwareParserBaseVisitor;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.TemporaryPureAggregateSpecification;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

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
        return "^meta::pure::mapping::aggregationAware::AggregationAwareSetImplementation" + this.setSourceInfo + "(" +
                ((this.id == null) ? "" : ("id = '" + this.id + "',")) +
                "root = " + this.root + "," +
                "class = ^meta::pure::metamodel::import::ImportStub " + this.classSourceInfo + " (importGroup=system::imports::" + this.importId + ", idOrPath='" + this.classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + this.importId + ", idOrPath='" + this.mappingPath + "')," +
                "aggregateSetImplementations=[" + this.aggregateSetImplementations + "]," +
                "mainSetImplementation=" + this.mainSetImplementation +
                ")";
    }

    @Override
    public String visitAggregationSpecification(AggregationAwareParser.AggregationSpecificationContext ctx)
    {
        String aggSpecSourceInfo = " " + this.sourceInformation.getPureSourceInformation(ctx.GROUP_OPEN().getSymbol(), ctx.GROUP_OPEN().getSymbol(), ctx.GROUP_CLOSE().getSymbol()).toM4String();
        this.aggregateSetImplementations.append("^meta::pure::mapping::aggregationAware::AggregateSetImplementationContainer ").append(aggSpecSourceInfo).append(" (")
                .append("index=").append(this.index).append(",")
                .append("aggregateSpecification=^meta::pure::mapping::aggregationAware::AggregateSpecification ").append(aggSpecSourceInfo).append(" (");
        visitChildren(ctx);
        return null;
    }

    @Override
    public String visitMainMapping(AggregationAwareParser.MainMappingContext ctx)
    {
        ParsingUtils.removeLastCommaCharacterIfPresent(aggregateSetImplementations);
        mainSetImplementation = parseSingleMapping(ctx.parserName().VALID_STRING().getSymbol(), ctx.CONTENT().getSymbol(), ctx.CURLY_BRACKET_CLOSE().getSymbol());
        return null;
    }

    @Override
    public String visitModelOperation(AggregationAwareParser.ModelOperationContext ctx)
    {
        BooleanObjectPair<Pair<String, String>> aggregateSpecificationParseResult = parseAggregateSpecification(ctx.CONTENT().getText(), ctx.CONTENT().getSymbol().getLine(), this.index);

        this.aggregateSetImplementations.append("canAggregate=").append(aggregateSpecificationParseResult.getOne()).append(",")
                .append("groupByFunctions=[").append(aggregateSpecificationParseResult.getTwo().getOne()).append("],")
                .append("aggregateValues=[").append(aggregateSpecificationParseResult.getTwo().getTwo()).append("]),");
        this.index++;
        return null;
    }

    @Override
    public String visitAggregateMapping(AggregationAwareParser.AggregateMappingContext ctx)
    {
        String subSetImplementation = parseSingleMapping(ctx.parserName().VALID_STRING().getSymbol(), ctx.CONTENT().getSymbol(), ctx.CURLY_BRACKET_CLOSE().getSymbol());
        this.aggregateSetImplementations.append("setImplementation=").append(subSetImplementation).append("),");
        return null;
    }

    final public BooleanObjectPair<Pair<String, String>> parseAggregateSpecification(String content, int beginLine, int index)
    {
        M3AntlrParser parser = new M3AntlrParser();
        M3ProcessorSupport processorSupport = new M3ProcessorSupport(this.context, this.repository);
        String mappingName = this.mappingPath.replace("::", "_");
        String classMappingName = this.classPath.replace("::", "_");
        AntlrContextToM3CoreInstance.LambdaContext lambdaContext = new AntlrContextToM3CoreInstance.LambdaContext(mappingName + '_' + classMappingName + (id == null ? "" : '_' + id) + "_AggregationAware_" + index);
        TemporaryPureAggregateSpecification temporarySpecification = parser.parseAggregateSpecification(content, lambdaContext, this.sourceInformation.getSourceName(), this.sourceInformation.getOffsetLine() + beginLine - 1, this.importId, index, this.repository, processorSupport, this.context);

        MutableList<String> groupByFunctionSpecifications = temporarySpecification.groupByFunctionSpecifications.collect(groupByFunctionSpecification ->
                "^meta::pure::mapping::aggregationAware::GroupByFunctionSpecification " + groupByFunctionSpecification.sourceInformation.toM4String() + " (" +
                        "groupByFn=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + " " + groupByFunctionSpecification.groupByExpression.getSourceInformation().toM4String() + " (" +
                        "classifierGenericType=^meta::pure::metamodel::type::generics::GenericType " + groupByFunctionSpecification.groupByExpression.getSourceInformation().toM4String() + " (rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType " + groupByFunctionSpecification.groupByExpression.getSourceInformation().toM4String() + " (rawType = ^meta::pure::metamodel::type::FunctionType " + groupByFunctionSpecification.groupByExpression.getSourceInformation().toM4String() + " ()))," +
                        "expressionSequence=" + M3AntlrParser.process(groupByFunctionSpecification.groupByExpression, processorSupport) + "))");

        MutableList<String> aggregationFunctionSpecifications = temporarySpecification.aggregationFunctionSpecifications.collect(aggregationFunctionSpecification ->
                "^meta::pure::mapping::aggregationAware::AggregationFunctionSpecification " + aggregationFunctionSpecification.sourceInformation.toM4String() + " (" +
                        "mapFn=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + " " + aggregationFunctionSpecification.mapExpression.getSourceInformation().toM4String() + " (" +
                        "classifierGenericType=^meta::pure::metamodel::type::generics::GenericType " + aggregationFunctionSpecification.mapExpression.getSourceInformation().toM4String()+ " (rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType " + aggregationFunctionSpecification.mapExpression.getSourceInformation().toM4String()+ " (rawType = ^meta::pure::metamodel::type::FunctionType " + aggregationFunctionSpecification.mapExpression.getSourceInformation().toM4String() + " ()))," +
                        "expressionSequence=" + M3AntlrParser.process(aggregationFunctionSpecification.mapExpression, processorSupport) +
                        ")," +
                        "aggregateFn=^meta::pure::metamodel::function::LambdaFunction " + lambdaContext.getLambdaFunctionUniqueName() + " " + aggregationFunctionSpecification.aggregateExpression.getSourceInformation().toM4String() + " (" +
                        "classifierGenericType=^meta::pure::metamodel::type::generics::GenericType " + aggregationFunctionSpecification.aggregateExpression.getSourceInformation().toM4String() + " (rawType=meta::pure::metamodel::function::LambdaFunction, typeArguments=^meta::pure::metamodel::type::generics::GenericType " + aggregationFunctionSpecification.aggregateExpression.getSourceInformation().toM4String() + " (rawType = ^meta::pure::metamodel::type::FunctionType " + aggregationFunctionSpecification.aggregateExpression.getSourceInformation().toM4String() + " ()))," +
                        "expressionSequence=" + M3AntlrParser.process(aggregationFunctionSpecification.aggregateExpression, processorSupport) + "))");

        return PrimitiveTuples.pair(temporarySpecification.canAggregate, Tuples.pair(groupByFunctionSpecifications.makeString(","), aggregationFunctionSpecifications.makeString(",")));
    }

    String parseSingleMapping(Token parserNameTok, Token content, Token end)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(parserNameTok, parserNameTok, end);
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
            return parser.parseMapping(content.getText(), this.id == null ? null : this.id + "_Main", null, newSetSourceInfo, true, this.classPath, this.classSourceInfo, this.mappingPath, this.sourceInformation.getSourceName(), this.sourceInformation.getOffsetLine() + content.getLine() - 1, this.importId, this.repository, this.context);
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
