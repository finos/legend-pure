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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr;

import org.finos.legend.pure.m2.relational.serialization.grammar.v1.IRelationalParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.AssociationMappingContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.BusinessMilestoningFromContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.BussinessSnapshotDateContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ClassMappingContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ColWithDbOrConstantContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ColumnDefinitionContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ColumnDefinitionsContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ConstantContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.DatabaseContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.DefinitionContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.FilterContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.FilterMappingBlockContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.FilterMappingJoinSequenceContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.FilterViewBlockContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.FunctionArgumentContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.IncludeContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.JoinColWithDbOrConstantContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.JoinColWithDbOrConstantsContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.JoinContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.JoinSequenceContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.LocalMappingPropertyContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MainTableBlockContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MappingBlockContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MappingBlockGroupByContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MappingContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MappingElementContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MappingElementsContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MilestoneSpecContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MilestoningDefinitionContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MilestoningDefinitionsContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MultiGrainFilterContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.NonePlusSingleMappingLineContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.OneJoinContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.OneJoinRightContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_atomicOperationContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_boolean_operation_rightContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_boolean_operatorContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_columnContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_functionContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_groupOperationContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_operationContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.Op_operatorContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.OtherwiseJoinContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.OtherwisePropertyMappingContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.OtherwisePropertyMappingsContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.PlusSingleMappingLineContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.PrimaryKeyContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.PropertyMappingsContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.RelationalMappingContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.SchemaContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ScopeContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ScopeInfoContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.SimpleScopeInfoContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.SingleMappingLineContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.SingleMappingLinesContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.TableAliasColumnContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.TableAliasColumnWithScopeInfoContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.TableContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.TransformerContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ViewColumnMappingLineContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ViewColumnMappingLinesContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ViewContext;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.ColumnDataTypeFactory;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.ColumnDataTypeFactory.ColumnDataTypeException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.util.List;

public class RelationalGraphBuilder extends org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParserBaseVisitor<String>
{
    private final String importId;

    private final AntlrSourceInformation sourceInformation;

    private String dbImport;

    private ParserLibrary parserLibrary;

    public RelationalGraphBuilder(String importId, AntlrSourceInformation sourceInformation, ParserLibrary parserLibrary)
    {
        this.importId = importId;
        this.sourceInformation = sourceInformation;
        this.parserLibrary = parserLibrary;
    }


    public String visitMappingBlock(MappingContext ctx, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath)
    {
        if (ctx.classMapping() != null)
        {
            return visitClassMappingBlock(ctx.classMapping(), id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath);
        }
        return visitAssociationMappingBlock(ctx.associationMapping(), id, extendsId, setSourceInfo, root, classPath, classSourceInfo, mappingPath);
    }

    private String visitAssociationMappingBlock(AssociationMappingContext ctx, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath)
    {
        if (extendsId != null)
        {
            throw new PureParserException(sourceInformation.getPureSourceInformation(ctx.ASSOCIATION_MAPPING().getSymbol()),
                    "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Association Mappings.");
        }

        String associationPropertyMappings = visitPropertyMappings(ctx.propertyMappings(), mappingPath);
        return "^meta::relational::mapping::RelationalAssociationImplementation" + setSourceInfo + "(" +
                (id == null ? "" : "id = '" + id + "',") +
                "association = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                "propertyMappings=[" + associationPropertyMappings + "]" +
                ")";
    }

    private String visitPropertyMappings(PropertyMappingsContext ctx, String mappingPath)
    {
        return visitMappingElementsBlock(ctx.mappingElements(), null, mappingPath);
    }

    private String visitClassMappingBlock(ClassMappingContext ctx, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath)
    {
        String block = visitMappingBlockBlock(ctx.mappingBlock());
        String mappingElements = "";
        if (ctx.mappingElements() != null)
        {
            mappingElements = visitMappingElementsBlock(ctx.mappingElements(), id, mappingPath);
        }
        return "^meta::relational::mapping::RootRelationalInstanceSetImplementation" + setSourceInfo + "(" +
                (id == null ? "" : "id = '" + id + "',") +
                (extendsId == null ? "" : "superSetImplementationId = '" + extendsId + "',") +
                "root = " + root + "," +
                "class = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                block +
                "propertyMappings=[" + mappingElements + "]" +
                ")";
    }

    private String visitMappingElementsBlock(MappingElementsContext mappingElements, String id, String mappingPath)
    {
        StringBuilder sb = new StringBuilder();
        if (mappingElements.mappingElement() != null)
        {
            for (MappingElementContext me : mappingElements.mappingElement())
            {
                sb.append(visitMappingElementBlock(me, id, mappingPath));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    private String visitMappingElementBlock(MappingElementContext ctx, String id, String mappingPath)
    {
        if (ctx.singleMappingLine() != null)
        {
            return visitSingleMappingLineBlock(ctx.singleMappingLine(), null, id, mappingPath);
        }
        return visitScopeBlock(ctx.scope(), id, mappingPath);
    }

    private String visitScopeBlock(ScopeContext ctx, String id, String mappingPath)
    {
        ScopeInfo scope = null;
        String db = visitDatabase(ctx.database());
        if (ctx.simpleScopeInfo() != null)
        {
            scope = visitSimpleScopeInfoBlock(ctx.simpleScopeInfo(), db, false);
        }
        scope = scope == null ? new ScopeInfo(db, null, null, null) : scope;
        scope.setDatabase(db);
        return visitSingleMappingLinesBlock(ctx.singleMappingLines(), scope, id, mappingPath);
    }

    private String visitSingleMappingLineBlock(SingleMappingLineContext ctx, ScopeInfo scopeInfo, String id, String mappingPath)
    {
        if (ctx.plusSingleMappingLine() != null)
        {
            return visitPlusSingleMappingLineBlock(ctx.plusSingleMappingLine(), scopeInfo, id, mappingPath, null);
        }
        return visitNonePlusSingleMappingLineBlock(ctx.nonePlusSingleMappingLine(), scopeInfo, id, mappingPath);
    }

    private String visitPlusSingleMappingLineBlock(PlusSingleMappingLineContext ctx, ScopeInfo scopeInfo, String id, String mappingPath, Token targetId)
    {
        LocalMappingPropertyParseResult r = visitLocalMappingPropertyBlock(ctx.localMappingProperty());
        return visitRelationalMappingBlock(ctx.relationalMapping(), ctx.identifier().getStart(), true, r.getLocalMappingPropertyType(), r.getLocalMappingPropertyFirstMul(), r.getLocalMappingPropertySecondMul(), scopeInfo, id, mappingPath, targetId);
    }

    private String visitRelationalMappingBlock(RelationalMappingContext ctx, Token propertyName, boolean localMappingProperty, String localMappingPropertyType, Token localMappingPropertyFirstMul, Token localMappingPropertySecondMul, ScopeInfo scopeInfo, String id, String mappingPath, Token targetId)
    {
        String tx = null;
        if (ctx.transformer() != null)
        {
            tx = visitTransformerBlock(ctx.transformer(), mappingPath);
        }
        String relationalOperation = visitJoinColWithDbOrConstantBlock(ctx.joinColWithDbOrConstant(), "", FastList.<String>newList(), scopeInfo);
        return "^meta::relational::mapping::RelationalPropertyMapping" + sourceInformation.getPureSourceInformation(propertyName).toM4String() + "(" +
                "        localMappingProperty = " + localMappingProperty + "," +
                (localMappingPropertyType == null ? "" : "        localMappingPropertyType = " + localMappingPropertyType + ",") +
                buildMul(localMappingPropertyFirstMul, localMappingPropertySecondMul) +
                "        property = '" + propertyName.getText() + "'," +
                (id == null ? "" : "        sourceSetImplementationId = '" + id + "', ") +
                (targetId == null ? "" : "        targetSetImplementationId = '" + targetId.getText() + "', ") +
                (tx == null ? "" : "        transformer = " + tx + ",") +
                "        relationalOperationElement = " + relationalOperation +
                ")";
    }

    private String buildMul(Token localMappingPropertyFirstMul, Token localMappingPropertySecondMul)
    {
        Token secondOne = localMappingPropertySecondMul == null ? localMappingPropertyFirstMul : localMappingPropertySecondMul;
        return localMappingPropertyFirstMul == null && localMappingPropertySecondMul == null ?
                "" :
                "localMappingPropertyMultiplicity = ^meta::pure::metamodel::multiplicity::Multiplicity(" +
                        "   lowerBound=^meta::pure::metamodel::multiplicity::MultiplicityValue(value=" + (localMappingPropertySecondMul == null || localMappingPropertyFirstMul == null? "0" : localMappingPropertyFirstMul.getText()) + ")," +
                        "   upperBound=^meta::pure::metamodel::multiplicity::MultiplicityValue(" + (secondOne.getText().equals("*") ? "" : "value=" + secondOne.getText()) + ")" +
                        "),";
    }

    private String visitTransformerBlock(TransformerContext ctx, String mappingPath)
    {
        return "^meta::pure::tools::GrammarInfoStub" + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(value='" + mappingPath + "," + ctx.identifier().getText() + "')";
    }

    private LocalMappingPropertyParseResult visitLocalMappingPropertyBlock(LocalMappingPropertyContext ctx)
    {
        return new LocalMappingPropertyParseResult(ctx.qualifiedName().getText(), ctx.localMappingPropertyFirstMul().INTEGER() != null ? ctx.localMappingPropertyFirstMul().INTEGER().getSymbol() : ctx.localMappingPropertyFirstMul().STAR().getSymbol(), ctx.localMappingPropertySecondMul() != null ?
                ctx.localMappingPropertySecondMul().INTEGER() != null ? ctx.localMappingPropertySecondMul().INTEGER().getSymbol() : ctx.localMappingPropertySecondMul().STAR().getSymbol() : null);
    }

    private String visitNonePlusSingleMappingLineBlock(NonePlusSingleMappingLineContext ctx, ScopeInfo scopeInfo, String id, String mappingPath)
    {
        StringBuffer buffer = new StringBuffer();
        Token propertyName = ctx.identifier().getStart();
        String primaryKeyBlock = null;
        String embeddedResult = "";
        String otherwisePropertyMappings = "";
        Token inlineSetId = null;

        Token sourceId = null;
        Token targetId = null;

        if (ctx.sourceAndTargetMappingId() != null)
        {
            sourceId = ctx.sourceAndTargetMappingId().targetId() == null ? null : ctx.sourceAndTargetMappingId().sourceId().getStart();
            targetId = ctx.sourceAndTargetMappingId().targetId() == null ? ctx.sourceAndTargetMappingId().sourceId().getStart() : ctx.sourceAndTargetMappingId().targetId().getStart();
        }
        String sourceIdString = sourceId != null ? sourceId.getText() : id;

        if (ctx.embeddedMapping() != null)
        {
            if (ctx.embeddedMapping().primaryKey() != null)
            {
                primaryKeyBlock = visitPrimaryKeyBlock(ctx.embeddedMapping().primaryKey(), "");
            }
            if (ctx.embeddedMapping().singleMappingLines() != null)
            {
                embeddedResult = visitSingleMappingLinesBlock(ctx.embeddedMapping().singleMappingLines(), scopeInfo, id, mappingPath);
            }
            if (ctx.embeddedMapping().otherwiseEmbeddedMapping() != null)
            {
                otherwisePropertyMappings = visitOtherwisePropertyMappingsBlock(ctx.embeddedMapping().otherwiseEmbeddedMapping().otherwisePropertyMappings(), propertyName, scopeInfo);
            }
            if (ctx.embeddedMapping().inline() != null)
            {
                inlineSetId = ctx.embeddedMapping().inline().identifier().getStart();
            }
            buffer.append(otherwisePropertyMappings.isEmpty() ?
                    embeddedResult.isEmpty() ?
                            "^meta::relational::mapping::InlineEmbeddedRelationalInstanceSetImplementation" :
                            "^meta::relational::mapping::EmbeddedRelationalInstanceSetImplementation" :
                    "^meta::relational::mapping::OtherwiseEmbeddedRelationalInstanceSetImplementation")
                    .append(sourceInformation.getPureSourceInformation(propertyName).toM4String())
                    .append("(root = false,")
                    .append(targetId == null ? "" : "id = '" + targetId.getText() + "',")
                    .append(targetId == null ? "" : "targetSetImplementationId = '" + targetId.getText() + "',")
                    .append("parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "'),")
                    .append(primaryKeyBlock == null ? "" : "primaryKey=" + primaryKeyBlock + ",")
                    .append("property = '" + propertyName.getText() + "'")
                    .append(embeddedResult.isEmpty() ? "" : ",propertyMappings = [" + embeddedResult + "]");
            if (otherwisePropertyMappings.isEmpty())
            {
                buffer.append(embeddedResult.isEmpty() ? ",inlineSetImplementationId = '" + (inlineSetId == null ? "" : inlineSetId.getText()) + "')" : ")");
            }
            else
            {
                buffer.append(", otherwisePropertyMapping = " + otherwisePropertyMappings + ")");
            }
            return buffer.toString();
        }

        return visitRelationalMappingBlock(ctx.relationalMapping(), propertyName, false, null, null, null, scopeInfo, sourceIdString, mappingPath, targetId);
    }

    private String visitOtherwisePropertyMappingsBlock(OtherwisePropertyMappingsContext ctx, Token propertyName, ScopeInfo scopeInfo)
    {
        StringBuilder sb = new StringBuilder();
        if (ctx.otherwisePropertyMapping() != null)
        {
            for (OtherwisePropertyMappingContext opm : ctx.otherwisePropertyMapping())
            {
                sb.append(visitOtherwisePropertyMapping(opm, propertyName, scopeInfo));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    private String visitOtherwisePropertyMapping(OtherwisePropertyMappingContext ctx, Token propertyName, ScopeInfo scopeInfo)
    {
        Token targetId = ctx.identifier().getStart();
        String relationalOperation = visitOtherwiseJoinBlock(ctx.otherwiseJoin(), "", FastList.<String>newList(), scopeInfo);

        return "^meta::relational::mapping::RelationalPropertyMapping" + sourceInformation.getPureSourceInformation(propertyName).toM4String() + "(" +
                "        property = '" + propertyName.getText() + "'," +
                (targetId == null ? "" : "        targetSetImplementationId = '" + targetId.getText() + "', ") +
                "        relationalOperationElement = " + relationalOperation +
                ")";
    }

    private String visitOtherwiseJoinBlock(OtherwiseJoinContext otherwiseJoinContext, String database, FastList<String> strings, ScopeInfo scopeInfo)
    {
        String db = "";
        String joins;
        if (otherwiseJoinContext.database() != null)
        {
            db = visitDatabase(otherwiseJoinContext.database());
        }
        joins = visitJoinSequenceBlock(otherwiseJoinContext.joinSequence(), scopeInfo, db);

        return "^meta::relational::metamodel::RelationalOperationElementWithJoin(" +
                "    joinTreeNode = " + joins +
                ")";
    }

    private String visitSingleMappingLinesBlock(SingleMappingLinesContext ctx, ScopeInfo scopeInfo, String id, String mappingPath)
    {
        StringBuilder sb = new StringBuilder();
        if (ctx.singleMappingLine() != null)
        {
            for (org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.SingleMappingLineContext smlc : ctx.singleMappingLine())
            {
                sb.append(visitSingleMappingLineBlock(smlc, scopeInfo, id, mappingPath));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    private String visitMappingBlockBlock(MappingBlockContext ctx)
    {
        String db = null;
        String groupByBlock = null;
        String primaryKeyBlock = null;
        ScopeInfo mainTable = null;
        FilterMappingBlockParseResult filterBlock = null;

        if (ctx.filterMappingBlock() != null)
        {
            filterBlock = visitFilterMappingBlockBlock(ctx.filterMappingBlock());
            db = filterBlock.getDb();
        }

        boolean distinct = ctx.DISTINCTCMD() != null;
        groupByBlock = visitMappingBlockGroupByBlock(ctx.mappingBlockGroupBy(), db);
        primaryKeyBlock = visitPrimaryKeyBlock(ctx.primaryKey(), db);
        mainTable = visitMainTableBlockBlock(ctx.mainTableBlock());

        return (mainTable == null ? "" : "mainTableAlias=" + generateTableAlias(mainTable) + ",") +
                (primaryKeyBlock == null ? "userDefinedPrimaryKey=false," : "userDefinedPrimaryKey=true, primaryKey=" + primaryKeyBlock + ",") +
                (filterBlock == null ? "" : "filter=" + filterBlock.getBlock() + ",") +
                "distinct=" + distinct + "," +
                (groupByBlock == null ? "" : "groupBy=" + groupByBlock + ",");
    }

    private ScopeInfo visitMainTableBlockBlock(MainTableBlockContext ctx)
    {
        if (ctx != null)
        {
            String db = visitDatabase(ctx.database());

            return visitSimpleScopeInfoBlock(ctx.simpleScopeInfo(), db, false);
        }
        return null;
    }

    private ScopeInfo visitSimpleScopeInfoBlock(SimpleScopeInfoContext ctx, String db, boolean rightLeft)
    {
        ScopeInfo res;
        if (rightLeft)
        {
            res = new ScopeInfo(db, null, null, ctx.relationalIdentifier().getStart());
        }
        else
        {
            res = new ScopeInfo(db, ctx.relationalIdentifier().getStart(), null, null);
        }
        if (ctx.scopeInfo() != null)
        {
            return visitScopeInfoBlock(ctx.scopeInfo(), ctx.relationalIdentifier().getStart(), db, rightLeft);
        }

        return res;
    }

    private String visitPrimaryKeyBlock(PrimaryKeyContext ctx, String db)
    {
        if (ctx != null)
        {
            return "[" + visitJoinColWithDbOrConstantsBlock(ctx.joinColWithDbOrConstants(), db, FastList.<String>newList(), new ScopeInfo(db, null, null, null)) + "]";
        }
        return null;
    }

    private FilterMappingBlockParseResult visitFilterMappingBlockBlock(FilterMappingBlockContext ctx)
    {
        String db = visitDatabase(ctx.database(0));
        String joins = null;
        if (ctx.filterMappingJoinSequence() != null)
        {
            joins = visitFilterMappingJoinSequenceBlock(ctx.filterMappingJoinSequence(), new ScopeInfo(db, null, null, null), db);
            db = visitDatabase(ctx.database(1));
        }
        String block = "^meta::relational::mapping::FilterMapping " + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(" +
                (joins == null ? "" : "        joinTreeNode = " + joins + ",") +
                "        database = " + db + "," +
                "        filterName = '" + ctx.identifier().getText() + "'" +
                ")";

        return new FilterMappingBlockParseResult(db, block);

    }

    @Override
    public String visitDefinition(DefinitionContext ctx)
    {
        dbImport = "^meta::pure::metamodel::import::ImportStub(importGroup=system::imports::" + importId + ", idOrPath='" + ctx.qualifiedName().getText() + "')";
        String includes = visitIncludesBlock(ctx.include());
        String schemas = visitSchemasBlock(ctx.schema());
        String tables = visitTablesBlock(ctx.table());
        String filters = visitFiltersBlock(ctx.filter());
        String joins = visitJoinsBlock(ctx.join());
        String multiGrainFilter = visitMultiGrainFiltersBlock(ctx.multiGrainFilter());
        String views = visitViewsBlock(ctx.view(), new ScopeInfo(dbImport, null, null, null, true));
        if (!tables.isEmpty())
        {
            schemas = schemas + (schemas.isEmpty() ? "" : ",") + ("^meta::relational::metamodel::Schema " + sourceInformation.getPureSourceInformation(ctx.DATABASE().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.GROUP_CLOSE().getSymbol()).toM4String() +
                    " (name='default', tables=[" + tables + "], views=[" + views + "])");
        }
        return "^meta::relational::metamodel::Database " + ctx.qualifiedName().identifier().getText() + sourceInformation.getPureSourceInformation(ctx.DATABASE().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.GROUP_CLOSE().getSymbol()).toM4String() +
                (ctx.qualifiedName().packagePath() != null ? "@" + ctx.qualifiedName().packagePath().getText().substring(0, ctx.qualifiedName().packagePath().getText().length() - 2) : "") + "(name='" + ctx.qualifiedName().identifier().getText() +
                "', package=" + (ctx.qualifiedName().packagePath() == null ? "::" : ctx.qualifiedName().packagePath().getText().substring(0, ctx.qualifiedName().packagePath().getText().length() - 2)) + ", includes = [" + includes + "], schemas=[" + schemas + "],joins=[" + joins + "],filters=[" + (filters.isEmpty() ? multiGrainFilter : !multiGrainFilter.isEmpty() ? filters + ", " + multiGrainFilter : filters) + "])";
    }

    private String visitMultiGrainFiltersBlock(List<MultiGrainFilterContext> multiGrainFilters)
    {
        StringBuilder sb = new StringBuilder();
        if (multiGrainFilters != null)
        {
            for (MultiGrainFilterContext mgf : multiGrainFilters)
            {
                sb.append(visitMultiGrainFilter(mgf));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    @Override
    public String visitMultiGrainFilter(MultiGrainFilterContext ctx)
    {
        String operation = visitOp_operationBlock(ctx.op_operation(), null, null);
        return "^meta::relational::metamodel::MultiGrainFilter " + ctx.identifier().getText() + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(name='" + ctx.identifier().getText() + "', operation=" + operation + ")";
    }

    private String visitJoinsBlock(List<JoinContext> joins)
    {
        StringBuilder sb = new StringBuilder();
        if (joins != null)
        {
            for (JoinContext jc : joins)
            {
                sb.append(visitJoin(jc));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();

    }

    @Override
    public String visitJoin(JoinContext ctx)
    {
        String operation = visitOp_operationBlock(ctx.op_operation(), null, null);
        return "^meta::relational::metamodel::join::Join " + ctx.identifier().getText() + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(name='" + ctx.identifier().getText() + "', operation=" + operation + ")";
    }

    private String visitFiltersBlock(List<FilterContext> filters)
    {
        StringBuilder sb = new StringBuilder();
        if (filters != null)
        {
            for (FilterContext fc : filters)
            {
                sb.append(visitFilter(fc));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    private String visitSchemasBlock(List<SchemaContext> schemas)
    {
        StringBuilder sb = new StringBuilder();
        if (schemas != null)
        {
            for (SchemaContext sc : schemas)
            {
                sb.append(visitSchema(sc));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    private String visitIncludesBlock(List<IncludeContext> includes)
    {
        StringBuilder sb = new StringBuilder();
        if (includes != null)
        {
            for (IncludeContext ic : includes)
            {
                sb.append(visitInclude(ic));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }

        return sb.toString();
    }

    @Override
    public String visitInclude(IncludeContext ctx)
    {
        return "^meta::pure::metamodel::import::ImportStub " + sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()).toM4String() + " (importGroup=system::imports::" + importId + ", idOrPath='" + ctx.qualifiedName().getText() + "')";
    }

    @Override
    public String visitSchema(SchemaContext ctx)
    {
        String tables = visitTablesBlock(ctx.table());

        ScopeInfo viewSchemaScopeInfo = new ScopeInfo(dbImport, ctx.identifier().getStart(), null, null, true);
        String views = visitViewsBlock(ctx.view(), viewSchemaScopeInfo);

        return "^meta::relational::metamodel::Schema " + sourceInformation.getPureSourceInformation(ctx.identifier().getStart(), ctx.identifier().getStart(), ctx.GROUP_CLOSE().getSymbol()).toM4String() + " (name='" + ctx.identifier().getText() + "', tables=[" + tables + "], views=[" + views + "])";
    }

    private String visitTablesBlock(List<TableContext> tables)
    {
        StringBuilder sb = new StringBuilder();

        if (tables != null)
        {
            for (TableContext tc : tables)
            {
                MutableList<String> pk = FastList.newList();
                sb.append(visitTableBlock(tc, pk));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }


    private String visitViewsBlock(List<ViewContext> views, ScopeInfo viewSchemaScopeInfo)
    {
        StringBuilder sb = new StringBuilder();
        if (views != null)
        {
            for (ViewContext vc : views)
            {
                sb.append(visitViewBlock(vc, viewSchemaScopeInfo));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    public String visitViewBlock(ViewContext ctx, ScopeInfo viewSchemaScopeInfo)
    {
        MutableList<String> pks = FastList.newList();
        String filterBlock = visitFilterViewBlock(ctx.filterViewBlock(), viewSchemaScopeInfo.getDatabase());
        String groupByBlock = visitMappingBlockGroupByBlock(ctx.mappingBlockGroupBy(), viewSchemaScopeInfo.getDatabase());
        boolean distinct = ctx.DISTINCTCMD() != null;
        Pair<String, String> result = visitViewColumnMappingLinesBlock(ctx.viewColumnMappingLines(), viewSchemaScopeInfo, pks);

        return "^meta::relational::metamodel::relation::View" + sourceInformation.getPureSourceInformation(ctx.VIEW().getSymbol(), ctx.relationalIdentifier().getStart(), ctx.GROUP_CLOSE().getSymbol()).toM4String() + "(" +
                "name = '" + ctx.relationalIdentifier().getText() + "'," +
                "columns = [" + result.getOne() + "]," +
                (filterBlock == null ? "" : "filter=" + filterBlock + ",") +
                "distinct=" + distinct + "," +
                "columnMappings=[" + result.getTwo() + "]," +
                "primaryKey = [" + pks.makeString(",") + "]" +
                (groupByBlock == null ? "" : ",groupBy=" + groupByBlock) + ")";
    }

    @Override
    public String visitFilter(FilterContext ctx)
    {
        String operation = visitOp_operationBlock(ctx.op_operation(), null, null);

        return "^meta::relational::metamodel::Filter " + ctx.identifier().getText() + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(name='" + ctx.identifier().getText() + "', operation=" + operation + ")";
    }

    private String visitOp_operationBlock(Op_operationContext ctx, String database, ScopeInfo scopeInfo)
    {
        String left = ctx.op_groupOperation() != null ? visitOp_groupOperationBlock(ctx.op_groupOperation(), database, scopeInfo) : visitOp_atomicOperationBlock(ctx.op_atomicOperation(), scopeInfo);
        if (ctx.op_boolean_operation_right() == null)
        {
            return left;
        }
        return visitOp_boolean_operation_rightBlock(ctx.op_boolean_operation_right(), database, scopeInfo, left);

    }

    private String visitOp_boolean_operation_rightBlock(Op_boolean_operation_rightContext ctx, String database, ScopeInfo scopeInfo, String left)
    {

        String booleanOp = visitOp_boolean_operator(ctx.op_boolean_operator());
        String right = visitOp_operationBlock(ctx.op_operation(), database, scopeInfo);
        String possiblyModifiedParams = null;
        String oppositeBooleanOperator = "andor".replace(booleanOp, "");
        //we are trying to be clever here and extract params of right DynaFunction if it's same type boolean operation (and/or) to avoid nested DynaFunctions of same operation
        if (right.startsWith("^meta::relational::metamodel::DynaFunction(name = '" + booleanOp + "'"))
        {
            int leftSquareBracket = right.indexOf('[');
            int rightSquareBracket = right.lastIndexOf(']');
            possiblyModifiedParams = right.substring(leftSquareBracket + 1, rightSquareBracket);
        }
        //if root level operator on right hand side is opposite boolean operation wrap in group to enforce precedence
        else if (right.startsWith("^meta::relational::metamodel::DynaFunction(name = '" + oppositeBooleanOperator + "'"))
        {
            possiblyModifiedParams = "^meta::relational::metamodel::DynaFunction(name = 'group', parameters = " + right + ")";
        }
        String params = possiblyModifiedParams == null ? left + ", " + right : left + ", " + possiblyModifiedParams;
        return "^meta::relational::metamodel::DynaFunction(name = '" + booleanOp + "', parameters = [" + params + "])";
    }

    @Override
    public String visitOp_boolean_operator(Op_boolean_operatorContext ctx)
    {
        if (ctx.AND() != null)
        {
            return "and";
        }
        return "or";
    }

    public String visitOp_function(Op_functionContext ctx, ScopeInfo scopeInfo)
    {
        String functionName = ctx.functionName().getText();
        List<FunctionArgumentContext> argCtxs = ctx.functionArgument();
        StringBuilder sb = new StringBuilder();
        for (FunctionArgumentContext argCtx : argCtxs)
        {
            sb.append(visitFunctionArgument(argCtx, scopeInfo));
            sb.append(",");
        }
        ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        return "^meta::relational::metamodel::DynaFunction(name = '" + functionName + "', parameters=[" + sb.toString() + "])";
    }

    public String visitFunctionArgument(FunctionArgumentContext ctx, ScopeInfo scopeInfo)
    {
        if (ctx.colWithDbOrConstant() != null)
        {
            return visitColWithDbOrConstantBlock(ctx.colWithDbOrConstant(), scopeInfo);
        }
        List<FunctionArgumentContext> argCtxs = ctx.arrayOfFunctionArguments().functionArgument();
        StringBuilder sb = new StringBuilder();
        for (FunctionArgumentContext argCtx : argCtxs)
        {
            sb.append(visitFunctionArgument(argCtx, scopeInfo));
            sb.append(",");
        }
        ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        return "^meta::relational::metamodel::LiteralList(values=[" + sb.toString() + "])";
    }

    private String visitOp_atomicOperationBlock(Op_atomicOperationContext ctx, ScopeInfo scopeInfo)
    {
        if (ctx.op_function() != null)
        {
            return visitOp_function(ctx.op_function(), scopeInfo);
        }

        String operation;
        String right;
        String left = visitColWithDbOrConstantBlock(ctx.colWithDbOrConstant(0), scopeInfo);
        if (ctx.op_operator() != null)
        {
            operation = visitOp_operator(ctx.op_operator());
            right = visitColWithDbOrConstantBlock(ctx.colWithDbOrConstant(1), scopeInfo);
            return "^meta::relational::metamodel::DynaFunction(name = '" + operation + "', parameters=[" + left + "," + right + "])";
        }
        if (ctx.ISNULL() != null)
        {
            return "^meta::relational::metamodel::DynaFunction(name = 'isNull', parameters=[" + left + "])";
        }
        if (ctx.ISNOTNULL() != null)
        {
            return "^meta::relational::metamodel::DynaFunction(name = 'isNotNull', parameters=[" + left + "])";
        }
        return null;
    }

    @Override
    public String visitOp_operator(Op_operatorContext ctx)
    {
        if (ctx.EQUAL() != null)
        {
            return "equal";
        }
        if (ctx.GREATERTHAN() != null)
        {
            return "greaterThan";
        }
        if (ctx.LESSTHAN() != null)
        {
            return "lessThan";
        }
        if (ctx.GREATERTHANEQUAL() != null)
        {
            return "greaterThanEqual";
        }
        if (ctx.LESSTHANEQUAL() != null)
        {
            return "lessThanEqual";
        }
        if (ctx.TEST_NOT_EQUAL() != null)
        {
            return "notEqual";
        }
        if (ctx.NOT_EQUAL_2() != null)
        {
            return "notEqualAnsi";
        }
        return null;
    }

    private String visitColWithDbOrConstantBlock(ColWithDbOrConstantContext ctx, ScopeInfo scopeInfo)
    {
        String db = ctx.database() != null ? visitDatabase(ctx.database()) : "";
        return ctx.op_column() != null ? visitOp_columnBlock(ctx.op_column(), db, FastList.<String>newList(), scopeInfo) : visitConstant(ctx.constant());
    }

    private String visitOp_groupOperationBlock(Op_groupOperationContext ctx, String database, ScopeInfo scopeInfo)
    {
        String result = visitOp_operationBlock(ctx.op_operation(), database, scopeInfo);
        return "^meta::relational::metamodel::DynaFunction(name='group',parameters=" + result + ")";
    }


    private Pair<String, String> visitViewColumnMappingLinesBlock(ViewColumnMappingLinesContext ctx, ScopeInfo scopeInfo, MutableList<String> pks)
    {
        StringBuilder buffer = new StringBuilder();
        StringBuilder columnBuilder = new StringBuilder();
        for (ViewColumnMappingLineContext vcmp : ctx.viewColumnMappingLine())
        {
            Pair<String, String> pair = visitViewColumnMappingLine(vcmp, scopeInfo, pks);
            columnBuilder.append(pair.getOne() + ",");
            buffer.append(pair.getTwo() + ",");
        }
        ParsingUtils.removeLastCommaCharacterIfPresent(columnBuilder);
        ParsingUtils.removeLastCommaCharacterIfPresent(buffer);

        return Tuples.pair(columnBuilder.toString(), buffer.toString());
    }

    public Pair<String, String> visitViewColumnMappingLine(ViewColumnMappingLineContext ctx, ScopeInfo scopeInfo, MutableList<String> pks)
    {
        MutableList<String> tacPks = FastList.newList();
        String relationalOperation = visitJoinColWithDbOrConstantBlock(ctx.joinColWithDbOrConstant(), scopeInfo.getDatabase(), tacPks, scopeInfo);
        String column = "^meta::relational::metamodel::Column" + sourceInformation.getPureSourceInformation(ctx.identifier(0).getStart()).toM4String() + "(name='" + ctx.identifier(0).getText() + "')";
        String buffer = "^meta::relational::mapping::ColumnMapping" + sourceInformation.getPureSourceInformation(ctx.identifier(0).getStart()).toM4String() + "(" +
                "        columnName = '" + ctx.identifier(0).getText() + "', " + (ctx.identifier(1) == null ? "" : "targetSetImplementationId = '" + ctx.identifier(1).getText() + "', ") +
                "        relationalOperationElement = " + relationalOperation + ")";
        if (tacPks.notEmpty())
        {
            pks.add(column);
        }

        return Tuples.pair(column, buffer);
    }

    private String visitMappingBlockGroupByBlock(MappingBlockGroupByContext ctx, String db)
    {
        if (ctx != null)
        {
            String columns = visitJoinColWithDbOrConstantsBlock(ctx.joinColWithDbOrConstants(), db, FastList.<String>newList(), new ScopeInfo(db, null, null, null));
            return "^meta::relational::mapping::GroupByMapping " + sourceInformation.getPureSourceInformation(ctx.GROUP_OPEN().getSymbol()).toM4String() + "(columns = [" + columns + "])";
        }
        return null;
    }


    private String visitJoinColWithDbOrConstantsBlock(JoinColWithDbOrConstantsContext ctx, String database, MutableList<String> pks, ScopeInfo scopeInfo)
    {
        StringBuilder sb = new StringBuilder();
        for (JoinColWithDbOrConstantContext jcwdc : ctx.joinColWithDbOrConstant())
        {
            sb.append(visitJoinColWithDbOrConstantBlock(jcwdc, database, pks, scopeInfo));
            sb.append(",");
        }

        ParsingUtils.removeLastCommaCharacterIfPresent(sb);

        return sb.toString();
    }

    private String visitJoinColWithDbOrConstantBlock(JoinColWithDbOrConstantContext ctx, String database, MutableList<String> pks, ScopeInfo scopeInfo)
    {
        if (ctx.constant() != null)
        {
            return visitConstant(ctx.constant());
        }
        else
        {
            String db = "";
            db = ctx.database() != null ? visitDatabase(ctx.database()) : database;
            if (ctx.joinSequence() != null)
            {
                String joins = visitJoinSequenceBlock(ctx.joinSequence(), scopeInfo, db);
                if (ctx.op_column() != null)
                {
                    String relationalOperationElement = visitOp_columnBlock(ctx.op_column(), db, pks, scopeInfo);
                    return "^meta::relational::metamodel::RelationalOperationElementWithJoin(" + "    relationalOperationElement = " + relationalOperationElement + "," + "    joinTreeNode = " + joins + ")";
                }
                return "^meta::relational::metamodel::RelationalOperationElementWithJoin(" + "    joinTreeNode = " + joins + ")";
            }
            return visitOp_columnBlock(ctx.op_column(), db, pks, scopeInfo);
        }
    }

    private String visitOp_columnBlock(Op_columnContext ctx, String database, MutableList<String> pks, ScopeInfo scopeInfo)
    {
        if (ctx.tableAliasColumn() != null)
        {
            return visitTableAliasColumnBlock(ctx.tableAliasColumn(), database, pks, scopeInfo);
        }
        return visitTableAliasColumnWithScopeInfoBlock(ctx.tableAliasColumnWithScopeInfo(), database, pks, scopeInfo);
    }

    private String visitTableAliasColumnWithScopeInfoBlock(TableAliasColumnWithScopeInfoContext ctx, String database, MutableList<String> pks, ScopeInfo scopeInfo)
    {
        Token first = ctx.relationalIdentifier() != null ? ctx.relationalIdentifier().getStart() : ctx.AND() != null ? ctx.AND().getSymbol() : ctx.OR().getSymbol();
        if (ctx.scopeInfo() != null)
        {
            ScopeInfo info = visitScopeInfoBlock(ctx.scopeInfo(), first, database, true);
            processScopeInfo(scopeInfo, info);

            String tableAliasColumn = generateTableAliasColumn(info);
            if (scopeInfo != null && scopeInfo.isParseView() && ctx.PRIMARYKEY() != null)
            {
                pks.add(tableAliasColumn);
            }
            else if (scopeInfo == null && ctx.PRIMARYKEY() != null)
            {
                throw new PureParserException(sourceInformation.getPureSourceInformation(ctx.PRIMARYKEY().getSymbol()), "'PRIMARY KEY' cannot be specified in mapping");
            }
            return tableAliasColumn;
        }
        if (ctx.GROUP_OPEN() != null)
        {
            String params = visitJoinColWithDbOrConstantsBlock(ctx.joinColWithDbOrConstants(), database, pks, scopeInfo);
            return "^meta::relational::metamodel::DynaFunction(name='" + first.getText() + "', parameters=[" + params + "])";
        }

        ScopeInfo info = new ScopeInfo(database, null, null, first);
        processScopeInfo(scopeInfo, info);

        return generateTableAliasColumn(info);
    }

    private void processScopeInfo(ScopeInfo scopeInfo, ScopeInfo info)
    {
        if (scopeInfo != null)
        {
            info.setDatabase("".equals(info.getDatabase()) ? scopeInfo.getDatabase() : info.getDatabase());
            info.setSchema(info.getSchema() == null ? scopeInfo.getSchema() : info.getSchema());
            info.setTableAlias(info.getTableAlias() == null ? scopeInfo.getTableAlias() : info.getTableAlias());
        }
    }

    private ScopeInfo visitScopeInfoBlock(ScopeInfoContext ctx, Token first, String database, boolean rightLeft)
    {
        Token second = ctx.scopeInfoPart(0).relationalIdentifier().getStart();
        Token third = ctx.scopeInfoPart().size() > 1 ? ctx.scopeInfoPart(1).relationalIdentifier().getStart() : null;
        Token schema = null;
        Token alias = null;
        Token column = null;

        if (rightLeft)
        {
            if (second == null && third == null)
            {
                column = first;
            }
            else if (third == null)
            {
                alias = first;
                column = second;
            }
            else
            {
                schema = first;
                alias = second;
                column = third;
            }
        }
        else
        {
            if (second == null && third == null)
            {
                schema = first;
            }
            else if (third == null)
            {
                schema = first;
                alias = second;
            }
            else
            {
                schema = first;
                alias = second;
                column = third;
            }
        }

        return new ScopeInfo(database, schema, alias, column);
    }

    private String visitTableAliasColumnBlock(TableAliasColumnContext ctx, String database, MutableList<String> pks, ScopeInfo scopeInfo)
    {
        String tableAliasColumn = "^meta::relational::metamodel::TableAliasColumn" + sourceInformation.getPureSourceInformation(ctx.relationalIdentifier().getStart()).toM4String() + "(" +
                "columnName='" + (ctx.relationalIdentifier().QUOTED_STRING() == null ? ctx.relationalIdentifier().identifier().getText() : ctx.relationalIdentifier().QUOTED_STRING().getText()) + "'," + "alias=" + generateTableAlias(new ScopeInfo(database, null, ctx.TARGET().getSymbol(), ctx.relationalIdentifier().getStart())) + ")";
        if (scopeInfo != null && scopeInfo.isParseView() && ctx.PRIMARYKEY() != null)
        {
            pks.add(tableAliasColumn);
        }
        else if (scopeInfo == null && ctx.PRIMARYKEY() != null)
        {
            throw new PureParserException(sourceInformation.getPureSourceInformation(ctx.PRIMARYKEY().getSymbol()), "'PRIMARY KEY' cannot be specified in mapping");
        }

        return tableAliasColumn;
    }

    private String generateTableAliasColumn(ScopeInfo info) throws PureParserException
    {
        Token column = info.getColumn();

        return "^meta::relational::metamodel::TableAliasColumn" + sourceInformation.getPureSourceInformation(column).toM4String() + "(" +
                "columnName='" + column.getText() + "'," +
                "alias=" + generateTableAlias(info) +
                ")";
    }

    private String generateTableAlias(ScopeInfo info) throws PureParserException
    {
        Token schema = info.getSchema();
        Token alias = info.getTableAlias();

        if (alias == null)
        {
            if (schema == null)
            {
                if (info.getColumn() == null)
                {
                    throw new RuntimeException("Missing table or alias");
                }
                else
                {
                    throw new PureParserException(sourceInformation.getPureSourceInformation(info.getColumn()), "Missing table or alias for column: " + info.getColumn().getText());
                }
            }
            alias = schema;
            schema = null;
        }

        return "^meta::relational::metamodel::TableAlias" + (schema == null ? sourceInformation.getPureSourceInformation(alias).toM4String() : sourceInformation.getPureSourceInformation(schema, schema, alias).toM4String()) + "(name = '" + alias.getText() + "'" +
                (schema != null ? ",schema='" + schema.getText() + "'" : "") +
                ("".equals(info.getDatabase()) ? "" : ",database=" + info.getDatabase()) +
                ")";
    }


    @Override
    public String visitConstant(ConstantContext ctx)
    {
        if (ctx.STRING() != null)
        {
            return "^meta::relational::metamodel::Literal(value=" + ctx.STRING().getText() + ")";
        }

        if (ctx.INTEGER() != null)
        {
            return "^meta::relational::metamodel::Literal(value=" + ctx.INTEGER().getText() + ")";
        }
        if (ctx.FLOAT() != null)
        {
            return "^meta::relational::metamodel::Literal(value=" + ctx.FLOAT().getText() + ")";
        }
        return null;
    }

    public String visitFilterViewBlock(FilterViewBlockContext ctx, String viewDb)
    {
        if (ctx == null)
        {
            return null;
        }
        String db = null;
        String joins = null;
        if (ctx.database() != null)
        {
            db = visitDatabase(ctx.database(0));
            if (ctx.joinSequence() != null)
            {
                joins = visitJoinSequenceBlock(ctx.joinSequence(), new ScopeInfo(db, null, null, null), db == null ? viewDb : db);
            }
            db = visitDatabase(ctx.database(1));

        }
        return "^meta::relational::mapping::FilterMapping " + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(" +
                (joins == null ? "" : "        joinTreeNode = " + joins + ",") +
                "        database = " + (db == null ? viewDb : db) + "," +
                "        filterName = '" + ctx.identifier().getText() + "'" +
                ")";
    }

    private String visitJoinSequenceBlock(JoinSequenceContext ctx, ScopeInfo scopeInfo, String db)
    {
        AsciiNodeBuilder root;
        AsciiNodeBuilder parentNode;
        AsciiNodeBuilder currentNode;
        Token joinType = null;

        currentNode = visitOneJoinBlock(ctx.oneJoin(), null, scopeInfo, db);
        root = currentNode;
        parentNode = currentNode;

        if (ctx.oneJoinRight() != null)
        {
            for (OneJoinRightContext ojrc : ctx.oneJoinRight())
            {
                joinType = ojrc.identifier() != null ? ojrc.identifier().getStart() : null;
                db = ojrc.database() != null ? visitDatabase(ojrc.database()) : db;
                currentNode = visitOneJoinBlock(ojrc.oneJoin(), joinType, scopeInfo, db);

                parentNode.add(currentNode);
                parentNode = currentNode;
            }
        }

        return root.build();
    }

    private String visitFilterMappingJoinSequenceBlock(FilterMappingJoinSequenceContext ctx, ScopeInfo scopeInfo, String db)
    {
        AsciiNodeBuilder root;
        AsciiNodeBuilder parentNode;
        AsciiNodeBuilder currentNode;
        Token joinType = null;

        joinType = ctx.identifier() != null ? ctx.identifier().getStart() : null;

        currentNode = visitOneJoinBlock(ctx.oneJoin(), joinType, scopeInfo, db);
        root = currentNode;
        parentNode = currentNode;

        if (ctx.oneJoinRight() != null)
        {
            for (OneJoinRightContext ojrc : ctx.oneJoinRight())
            {
                joinType = ojrc.identifier() != null ? ojrc.identifier().getStart() : null;
                db = ojrc.database() != null ? visitDatabase(ojrc.database()) : db;
                currentNode = visitOneJoinBlock(ojrc.oneJoin(), joinType, scopeInfo, db);

                parentNode.add(currentNode);
                parentNode = currentNode;
            }
        }

        return root.build();
    }

    private AsciiNodeBuilder visitOneJoinBlock(OneJoinContext ctx, Token joinType, ScopeInfo scope, String db)
    {
        MutableList<String> values = FastList.newListWith("INNER", "OUTER");
        if (joinType != null && !values.contains(joinType.getText().toUpperCase()))
        {
            throw new PureParserException(sourceInformation.getPureSourceInformation(joinType), "The joinType is not recognized. Valid join types are: " + values);
        }
        return new AsciiNodeBuilder(
                "^meta::relational::metamodel::join::JoinTreeNode" + sourceInformation.getPureSourceInformation(ctx.identifier().getStart()).toM4String() + "(" +
                        (joinType == null ? "" : "           joinType='" + joinType.getText() + "',") +
                        "           joinName='" + ctx.identifier().getText() + "'" +
                        ("".equals(db) ? scope == null || scope.getDatabase().equals("") ? "" : ", database=" + scope.getDatabase() : ", database=" + db));
    }

    @Override
    public String visitDatabase(DatabaseContext ctx)
    {
        if (ctx == null)
        {
            return null;
        }
        return "^meta::pure::metamodel::import::ImportStub " + sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()).toM4String() + " (importGroup=system::imports::" + importId + ", idOrPath='" + ctx.qualifiedName().getText() + "')";
    }

    private String visitTableBlock(TableContext ctx, MutableList<String> pk)
    {
        String milestoneSpec = visitMilestoneSpec(ctx.milestoneSpec());
        String columns = visitColumnDefinitionsBlock(ctx.columnDefinitions(), pk);
        return "^meta::relational::metamodel::relation::Table " + sourceInformation.getPureSourceInformation(ctx.relationalIdentifier().getStart()).toM4String() + "(name='" + (ctx.relationalIdentifier().QUOTED_STRING() == null ? ctx.relationalIdentifier().identifier().getText() : ctx.relationalIdentifier().QUOTED_STRING().getText()) + "',  columns = [" + columns + "], primaryKey = [" + pk.makeString(",") + "], " +
                "temporaryTable = false, milestoning=[" + milestoneSpec + "])";
    }

    @Override
    public String visitMilestoneSpec(MilestoneSpecContext ctx)
    {
        if (ctx == null)
        {
            return "";
        }
        return visitMilestoningDefinitions(ctx.milestoningDefinitions());
    }

    @Override
    public String visitMilestoningDefinitions(MilestoningDefinitionsContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        if (ctx != null && ctx.milestoningDefinition() != null)
        {
            for (MilestoningDefinitionContext mdc : ctx.milestoningDefinition())
            {
                sb.append(visitMilestoningDefinition(mdc));
                sb.append(",");
            }
            ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        }
        return sb.toString();
    }

    private String visitColumnDefinitionsBlock(ColumnDefinitionsContext ctx, MutableList<String> pk)
    {
        StringBuilder sb = new StringBuilder();
        for (ColumnDefinitionContext cdc : ctx.columnDefinition())
        {
            sb.append(visitColumnDefinitionBlock(cdc, pk));
            sb.append(",");
        }
        ParsingUtils.removeLastCommaCharacterIfPresent(sb);
        return sb.toString();
    }

    private String visitColumnDefinitionBlock(ColumnDefinitionContext ctx, MutableList<String> pk)
    {
        boolean nullable = true;
        if (ctx.PRIMARYKEY() != null)
        {
            nullable = false;
            pk.add("'" + ctx.relationalIdentifier().getText() + "'");
        }
        else if (ctx.NOT_NULL() != null)
        {
            nullable = false;
        }
        String pureType;
        try
        {
            if (ctx.INTEGER() != null && ctx.INTEGER().size() == 1)
            {
                pureType = ColumnDataTypeFactory.pureDataTypeConstructorString(ctx.identifier().getText(), ctx.INTEGER(0).getText());
            }
            else if (ctx.INTEGER() != null && ctx.INTEGER().size() == 2)
            {
                pureType = ColumnDataTypeFactory.pureDataTypeConstructorString(ctx.identifier().getText(), ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            }
            else
            {
                pureType = ColumnDataTypeFactory.pureDataTypeConstructorString(ctx.identifier().getText());
            }

        }
        catch (ColumnDataTypeException exp)
        {
            throw new PureParserException(sourceInformation.getPureSourceInformation(ctx.identifier().getStart()), exp.getMessage());
        }

        return "^meta::relational::metamodel::Column " + sourceInformation.getPureSourceInformation(ctx.relationalIdentifier().getStart()).toM4String() + " (name='" + ctx.relationalIdentifier().getText() + "', type=" + pureType + ", nullable= " + nullable + ")";

    }

    @Override
    public String visitMilestoningDefinition(MilestoningDefinitionContext ctx)
    {
        RichIterable<Parser> relationalParsers = this.parserLibrary.getParsers().select(parser -> parser instanceof IRelationalParser);

        SourceInformation srcInfo = this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart());
        String type = ctx.identifier().getText();
        org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.MilestoningContentContext contentCtx = ctx.milestoningContent();
        String content = contentCtx.start.getInputStream().getText(Interval.of(contentCtx.start.getStartIndex(), contentCtx.stop.getStopIndex()));
        List<String> results = relationalParsers.collect(relationalParser -> ((IRelationalParser) relationalParser).parseMilestoningDefinition(type, content, sourceInformation.getSourceName(), srcInfo.getLine() - 1, srcInfo.getEndColumn() + 1, this.importId)).reject(x -> x == null).toList();
        if (results.size() == 1)
        {
            return results.get(0);
        }
        else if (results.isEmpty())
        {
            throw new PureParserException(srcInfo, "Milestoning type : " + type + " not supported!!");
        }
        throw new PureParserException(srcInfo, "Found multiple processors for milestoning type : " + type);
    }

    @Override
    public String visitBusinessMilestoningInnerDefinition(org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.BusinessMilestoningInnerDefinitionContext ctx)
    {
        if (ctx.businessMilestoningFrom() != null)
        {
            return visitBusinessMilestoningFrom(ctx.businessMilestoningFrom());
        }
        return visitBussinessSnapshotDate(ctx.bussinessSnapshotDate());
    }

    @Override
    public String visitBusinessMilestoningFrom(BusinessMilestoningFromContext ctx)
    {
        return "^meta::relational::metamodel::relation::BusinessMilestoning(from='" + ctx.identifier(0).getText() + "', thru='" + ctx.identifier(1).getText() + "', thruIsInclusive = " + (ctx.THRU_IS_INCLUSIVE() == null ? false : ctx.BOOLEAN().getText()) + (ctx.INFINITY_DATE() == null ? "" : ", infinityDate=" + ctx.DATE().getText()) + ")";
    }

    @Override
    public String visitBussinessSnapshotDate(BussinessSnapshotDateContext ctx)
    {
        return "^meta::relational::metamodel::relation::BusinessSnapshotMilestoning(snapshotDate = '" + ctx.identifier().getText() + "')";
    }

    @Override
    public String visitProcessingMilestoningInnerDefinition(org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser.ProcessingMilestoningInnerDefinitionContext ctx)
    {
        return "^meta::relational::metamodel::relation::ProcessingMilestoning(in='" + ctx.identifier(0).getText() + "', out='" + ctx.identifier(1).getText() + "', outIsInclusive = " + (ctx.OUT_IS_INCLUSIVE() == null ? false : ctx.BOOLEAN().getText()) + (ctx.INFINITY_DATE() == null ? "" : ", infinityDate=" + ctx.DATE().getText()) + ")";
    }
}