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

import org.antlr.v4.runtime.Token;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.RelationMappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.RelationMappingParserBaseVisitor;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class RelationMappingGraphBuilder extends RelationMappingParserBaseVisitor<String>
{
    private final String importId;
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;
    private final AntlrSourceInformation sourceInformation;
    private String mappingPath;

    public RelationMappingGraphBuilder(String importId, ModelRepository repository, ProcessorSupport processorSupport, AntlrSourceInformation sourceInformation)
    {
        this.importId = importId;
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.sourceInformation = sourceInformation;
    }

    public String visitMapping(RelationMappingParser.MappingContext ctx, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath)
    {
        this.mappingPath = mappingPath;
        Token functionStartToken = ctx.functionDescriptor() != null ? ctx.functionDescriptor().getStart() : ctx.qualifiedName().getStart();
        Token functionEndToken = ctx.functionDescriptor() != null ? ctx.functionDescriptor().getStop() : ctx.qualifiedName().getStop();
        try
        {
            String functionId = ctx.functionDescriptor() != null ? FunctionDescriptor.functionDescriptorToId(ctx.functionDescriptor().getText()) : ctx.qualifiedName().getText();
            String propertyMappings = ctx.singlePropertyMapping() == null ? "" : ListIterate.collect(ctx.singlePropertyMapping(), c -> visitPropertyMapping(c, id)).makeString(",");

            return "^meta::pure::mapping::relation::RelationFunctionInstanceSetImplementation" + setSourceInfo + "(" +
                    (id == null ? "" : "id = '" + id + "',") +
                    (extendsId == null ? "" : "superSetImplementationId = '" + extendsId + "',") +
                    "root = " + root + "," +
                    "class = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                    "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                    "relationFunction = ^meta::pure::metamodel::import::ImportStub " + this.sourceInformation.getPureSourceInformation(functionStartToken, functionStartToken,  functionEndToken).toM4String() + " (importGroup=system::imports::" + this.importId + ", idOrPath='" + functionId + "')," +
                    "propertyMappings=[" + propertyMappings + "]" +
                    ")";
        }
        catch (InvalidFunctionDescriptorException e)
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(functionStartToken, functionStartToken,  functionEndToken), "Invalid function descriptor specified for relation function mapping!", e);
        }
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
        String columnName = AntlrContextToM3CoreInstance.removeQuotes(ctx.columnName().getText());
        String transformer = ctx.transformer() != null ? visitTransformerBlock(ctx.transformer()) : null;

        return "^meta::pure::mapping::relation::RelationFunctionPropertyMapping" + sourceInfo.toM4String() + "(" +
            "        localMappingProperty = " + (localPropertyType != null) + "," +
            (localPropertyType == null ? "" : "        localMappingPropertyType = " + localPropertyType + ",") +
            (localPropertyMultiplicity == null ? "" : "localMappingPropertyMultiplicity = " + localPropertyMultiplicity + ",") +
            "        property = '" + propertyName + "'," +
            (classMappingId == null ? "" : "        sourceSetImplementationId = '" + classMappingId + "', ") +
            (transformer == null ? "" : "        transformer = " + transformer + ",") +
            "        column=^meta::pure::metamodel::relation::Column(name = '" + columnName + "', nameWildCard = false)" +
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

        return "^meta::pure::mapping::relation::InlineEmbeddedRelationFunctionSetImplementation" + sourceInfo.toM4String() + "(" +
            "id = '" + embeddedId + "'," +
            "root = false," +
            "property = '" + propertyName + "'," +
            "sourceSetImplementationId = '" + parentId + "'," +
            "targetSetImplementationId = '" + embeddedId + "'," +
            "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + this.importId + ", idOrPath='" + mappingPath + "')," +
            "propertyMappings = []," +
            "inlineSetImplementationId = '" + inlineId + "'" +
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

}
