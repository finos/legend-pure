// Copyright 2024 Goldman Sachs
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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.RelationMappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.RelationMappingParserBaseVisitor;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class RelationMappingGraphBuilder extends RelationMappingParserBaseVisitor<String>
{
    private final String importId;
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;
    private final AntlrSourceInformation sourceInformation;
    private final Context context;
    private final String sourceName;
    private final int offset;
    private String mappingPath;
    private int lambdaCounter;

    public RelationMappingGraphBuilder(String importId, ModelRepository repository, ProcessorSupport processorSupport, AntlrSourceInformation sourceInformation, Context context, String sourceName, int offset)
    {
        this.importId = importId;
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.sourceInformation = sourceInformation;
        this.context = context;
        this.sourceName = sourceName;
        this.offset = offset;
    }

    public String visitMapping(RelationMappingParser.MappingContext ctx, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath)
    {
        this.mappingPath = mappingPath;
        RelationMappingParser.RelationSourceContext srcCtx = ctx.relationSource();
        try
        {
            String relationFunctionM4 = visitRelationSource(srcCtx, id, classPath);
            String propertyMappings = ctx.singlePropertyMapping() == null ? "" : ListIterate.collect(ctx.singlePropertyMapping(), c -> visitPropertyMapping(c, id)).makeString(",");

            return "^meta::pure::mapping::relation::RelationFunctionInstanceSetImplementation" + setSourceInfo + "(" +
                    (id == null ? "" : "id = '" + id + "',") +
                    (extendsId == null ? "" : "superSetImplementationId = '" + extendsId + "',") +
                    "root = " + root + "," +
                    "class = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                    "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                    "relationFunction = " + relationFunctionM4 + "," +
                    "propertyMappings=[" + propertyMappings + "]" +
                    ")";
        }
        catch (InvalidFunctionDescriptorException e)
        {
            Token startTok = srcCtx.getStart();
            Token stopTok = srcCtx.getStop();
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(startTok, startTok, stopTok), "Invalid function descriptor specified for relation function mapping!", e);
        }
    }

    private String visitRelationSource(RelationMappingParser.RelationSourceContext ctx, String id, String classPath) throws InvalidFunctionDescriptorException
    {
        if (ctx.RELATION_FUNCTION() != null)
        {
            // ~func qualifiedName | functionDescriptor — emit ImportStub as before.
            Token functionStartToken = ctx.functionDescriptor() != null ? ctx.functionDescriptor().getStart() : ctx.qualifiedName().getStart();
            Token functionEndToken = ctx.functionDescriptor() != null ? ctx.functionDescriptor().getStop() : ctx.qualifiedName().getStop();
            String functionId = ctx.functionDescriptor() != null ? FunctionDescriptor.functionDescriptorToId(ctx.functionDescriptor().getText()) : ctx.qualifiedName().getText();
            return "^meta::pure::metamodel::import::ImportStub " + this.sourceInformation.getPureSourceInformation(functionStartToken, functionStartToken, functionEndToken).toM4String() + " (importGroup=system::imports::" + this.importId + ", idOrPath='" + functionId + "')";
        }
        // ~src combinedExpression — wrap the inline expression in a synthetic
        // zero-arg lambda so the rest of the pipeline can treat both forms
        // uniformly with the `~func` ImportStub flow. The user writes a single
        // expression (`~src my::personFunction()`) and the resulting lambda's
        // last-expression generic type drives `$src` binding for property
        // mappings exactly as if it had come from an explicit Pure function.
        String srcLambdaText = "{| " + extractText(ctx.combinedExpression()) + "}";
        return parseLambdaTextToM4(srcLambdaText, lambdaContextId(id, classPath, "src"));
    }

    private String visitPropertyMapping(RelationMappingParser.SinglePropertyMappingContext ctx, String id)
    {
        if (ctx.singleLocalPropertyMapping() != null)
        {
            RelationMappingParser.SingleLocalPropertyMappingContext localCtx = ctx.singleLocalPropertyMapping();
            String localPropertyType = localCtx.qualifiedName(1).getText();
            String localPropertyMultiplicity = buildMultiplicity(localCtx.multiplicity().multiplicityArgument());
            String propertyName = localCtx.qualifiedName(0).getText();
            SourceInformation sourceInfo = sourceInformation.getPureSourceInformation(localCtx.qualifiedName(0).start);
            return buildRelationFunctionPropertyMapping(localCtx.relationFunctionPropertyMapping(), id, propertyName, sourceInfo, localPropertyType, localPropertyMultiplicity);
        }

        RelationMappingParser.SingleNonLocalPropertyMappingContext nonLocalCtx = ctx.singleNonLocalPropertyMapping();
        String propertyName = nonLocalCtx.qualifiedName().getText();
        SourceInformation sourceInfo = sourceInformation.getPureSourceInformation(nonLocalCtx.qualifiedName().start);

        if (nonLocalCtx.relationFunctionEmbeddedPropertyMapping() != null)
        {
            return visitRelationFunctionEmbeddedPropertyMapping(nonLocalCtx.relationFunctionEmbeddedPropertyMapping(), id, propertyName, sourceInfo);
        }
        if (nonLocalCtx.inlineRelationFunctionEmbeddedPropertyMapping() != null)
        {
            return visitInlineRelationFunctionEmbeddedPropertyMapping(nonLocalCtx.inlineRelationFunctionEmbeddedPropertyMapping(), id, propertyName, sourceInfo);
        }
        return buildRelationFunctionPropertyMapping(nonLocalCtx.relationFunctionPropertyMapping(), id, propertyName, sourceInfo, null, null);
    }

    private String buildRelationFunctionPropertyMapping(RelationMappingParser.RelationFunctionPropertyMappingContext ctx, String classMappingId, String propertyName, SourceInformation sourceInfo, String localPropertyType, String localPropertyMultiplicity)
    {
        String lambdaText;
        if (ctx.columnName() != null)
        {
            // Bare-column lowering: `prop: COL` -> `{| $src.COL}`.  Keep the
            // original (possibly quoted) column text so columns with spaces
            // (`'LEGAL NAME'`) still parse correctly as quoted property
            // accessors.  The M3 expression post-processor resolves the
            // accessor once `src`'s generic type is bound to the relation's
            // last-expression type.
            lambdaText = "{| $src." + ctx.columnName().getText() + "}";
        }
        else
        {
            // Expression form: wrap user expression in a zero-param lambda.
            lambdaText = "{| " + extractText(ctx.combinedExpression()) + "}";
        }
        String lambdaM4 = parseLambdaTextToM4(lambdaText, lambdaContextId(classMappingId, propertyName, "valueFn"));

        String transformer = ctx.transformer() != null ? visitTransformerBlock(ctx.transformer()) : null;

        return "^meta::pure::mapping::relation::RelationFunctionPropertyMapping" + sourceInfo.toM4String() + "(" +
            "        localMappingProperty = " + (localPropertyType != null) + "," +
            (localPropertyType == null ? "" : "        localMappingPropertyType = " + localPropertyType + ",") +
            (localPropertyMultiplicity == null ? "" : "localMappingPropertyMultiplicity = " + localPropertyMultiplicity + ",") +
            "        property = '" + propertyName + "'," +
            (classMappingId == null ? "" : "        sourceSetImplementationId = '" + classMappingId + "', ") +
            (transformer == null ? "" : "        transformer = " + transformer + ",") +
            "        valueFn = " + lambdaM4 +
            ")";
    }

    private String visitTransformerBlock(RelationMappingParser.TransformerContext ctx)
    {
        return "^meta::pure::tools::GrammarInfoStub" + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(value='" + mappingPath + "," + ctx.identifier().getText() + "')";
    }

    private String visitRelationFunctionEmbeddedPropertyMapping(RelationMappingParser.RelationFunctionEmbeddedPropertyMappingContext ctx, String parentId, String propertyName, SourceInformation sourceInfo)
    {
        String embeddedId = parentId + "_" + propertyName;
        String subPropertyMappings = ctx.singlePropertyMapping() == null ? "" : ListIterate.collect(ctx.singlePropertyMapping(), c -> visitPropertyMapping(c, embeddedId)).makeString(",");

        return "^meta::pure::mapping::relation::EmbeddedRelationFunctionSetImplementation" + sourceInfo.toM4String() + "(" +
            "id = '" + embeddedId + "'," +
            "root = false," +
            "property = '" + propertyName + "'," +
            "sourceSetImplementationId = '" + parentId + "'," +
            "targetSetImplementationId = '" + embeddedId + "'," +
            "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + this.importId + ", idOrPath='" + mappingPath + "')," +
            "propertyMappings = [" + subPropertyMappings + "]" +
            ")";
    }

    private String visitInlineRelationFunctionEmbeddedPropertyMapping(RelationMappingParser.InlineRelationFunctionEmbeddedPropertyMappingContext ctx, String parentId, String propertyName, SourceInformation sourceInfo)
    {
        String embeddedId = parentId + "_" + propertyName;
        String inlineId = ctx.identifier().getText();

        return "^meta::pure::mapping::relation::EmbeddedRelationFunctionSetImplementation" + sourceInfo.toM4String() + "(" +
            "id = '" + embeddedId + "'," +
            "root = false," +
            "property = '" + propertyName + "'," +
            "sourceSetImplementationId = '" + parentId + "'," +
            "targetSetImplementationId = '" + inlineId + "'," +
            "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + this.importId + ", idOrPath='" + mappingPath + "')," +
            "propertyMappings = []" +
            ")";
    }

    private String buildMultiplicity(RelationMappingParser.MultiplicityArgumentContext ctx)
    {
        String from = ctx.fromMultiplicity() == null ? "*".equals(ctx.toMultiplicity().getText()) ? "0" : ctx.toMultiplicity().getText() : ctx.fromMultiplicity().getText();
        String to = ctx.toMultiplicity().getText();
        return "^meta::pure::metamodel::multiplicity::Multiplicity(" +
                "   lowerBound=^meta::pure::metamodel::multiplicity::MultiplicityValue(value=" + from + ")," +
                "   upperBound=^meta::pure::metamodel::multiplicity::MultiplicityValue(" + (to.equals("*") ? "" : "value=" + to) + ")" +
                ")";
    }

    /**
     * Re-enter the M3 parser to convert a Pure lambda literal text (e.g.
     * {@code "{| $src.X + $src.Y}"}) into its M4 string form, suitable for
     * embedding back into the M4 string this graph builder is assembling.
     * <p>
     * We deliberately do NOT serialize the parsed {@code LambdaFunction} via
     * {@code M3AntlrParser.process} — that walks resolved {@code Function}
     * references inside the body and recurses indefinitely (e.g. for
     * {@code 1->cast(...)}).  Instead we extract the lambda's
     * {@code expressionSequence}, serialize just that, and hand-roll an empty
     * {@code FunctionType} wrapper around it — same shape the existing
     * {@code PureInstanceSetImplementation.parseMapping} produces.  The
     * post-processor injects the {@code src} parameter and runs full type
     * inference at processing time.
     */
    private String parseLambdaTextToM4(String lambdaText, String lambdaContextSuffix)
    {
        AntlrContextToM3CoreInstance.LambdaContext lambdaContext =
                new AntlrContextToM3CoreInstance.LambdaContext(lambdaContextSuffix + "_" + (++lambdaCounter));
        ProcessorSupport ps = (this.processorSupport instanceof M3ProcessorSupport)
                ? this.processorSupport
                : new M3ProcessorSupport(this.context, this.repository);
        CoreInstance parsed = new M3AntlrParser().parseCombinedExpression(
                lambdaText, lambdaContext, this.sourceName, this.offset, this.importId, this.repository, ps, this.context);
        ListIterable<? extends CoreInstance> wrapped = parsed == null
                ? null
                : parsed.getValueForMetaPropertyToMany(M3Properties.values).toList();
        if (wrapped == null || wrapped.size() != 1)
        {
            throw new PureParserException(null, "Expected a lambda function literal, got: " + lambdaText);
        }
        CoreInstance lambda = wrapped.get(0);
        ListIterable<? extends CoreInstance> bodyExpressions =
                lambda.getValueForMetaPropertyToMany(M3Properties.expressionSequence).toList();
        if (bodyExpressions.isEmpty())
        {
            throw new PureParserException(null, "Lambda body is empty in: " + lambdaText);
        }
        String lambdaSrcInfo = lambda.getSourceInformation() == null
                ? ""
                : (" " + lambda.getSourceInformation().toM4String());
        StringBuilder bodyM4 = new StringBuilder();
        bodyM4.append('[');
        for (int i = 0; i < bodyExpressions.size(); i++)
        {
            if (i > 0)
            {
                bodyM4.append(',');
            }
            bodyM4.append(M3AntlrParser.process(bodyExpressions.get(i), ps));
        }
        bodyM4.append(']');
        return "^meta::pure::metamodel::function::LambdaFunction<{->Any[*]}> " + lambdaContext.getLambdaFunctionUniqueName() + lambdaSrcInfo + " (" +
                "  classifierGenericType = ^meta::pure::metamodel::type::generics::GenericType" + lambdaSrcInfo + " (" +
                "    rawType = meta::pure::metamodel::function::LambdaFunction," +
                "    typeArguments = ^meta::pure::metamodel::type::generics::GenericType" + lambdaSrcInfo + " (" +
                "      rawType = ^meta::pure::metamodel::type::FunctionType" + lambdaSrcInfo + " ()" +
                "    )" +
                "  )," +
                "  expressionSequence = " + bodyM4 +
                ")";
    }

    private static String extractText(ParserRuleContext ctx)
    {
        Token start = ctx.getStart();
        Token stop = ctx.getStop();
        return start.getInputStream().getText(Interval.of(start.getStartIndex(), stop.getStopIndex()));
    }

    private String lambdaContextId(String classMappingId, String classOrPropertyName, String suffix)
    {
        String mp = mappingPath == null ? "anon" : mappingPath.replace("::", "_");
        return mp + (classMappingId == null ? "" : "_" + classMappingId) + (classOrPropertyName == null ? "" : "_" + classOrPropertyName.replace("::", "_")) + "_" + suffix;
    }

}


