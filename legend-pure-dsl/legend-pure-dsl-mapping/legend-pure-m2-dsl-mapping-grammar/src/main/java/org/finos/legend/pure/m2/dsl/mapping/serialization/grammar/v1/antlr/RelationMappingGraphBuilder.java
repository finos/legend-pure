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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.AntlrContextToM3CoreInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;

public class RelationMappingGraphBuilder extends RelationMappingParserBaseVisitor<String>
{
    private final String importId;
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;
    private final AntlrSourceInformation sourceInformation;

    public RelationMappingGraphBuilder(String importId, ModelRepository repository, ProcessorSupport processorSupport, AntlrSourceInformation sourceInformation)
    {
        this.importId = importId;
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.sourceInformation = sourceInformation;
    }
    
    public String visitMapping(RelationMappingParser.MappingContext ctx, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath)
    {
        Token startToken = ctx.qualifiedName().getStart();
        String functionPath = ctx.qualifiedName().getText();
        String propertyMappings = ctx.singlePropertyMapping() == null ? "" : ListIterate.collect(ctx.singlePropertyMapping(), c -> visitPropertyMapping(c, id)).makeString(",");
        
        return "^meta::pure::mapping::relation::RelationFunctionInstanceSetImplementation" + setSourceInfo + "(" +
                (id == null ? "" : "id = '" + id + "',") +
                (extendsId == null ? "" : "superSetImplementationId = '" + extendsId + "',") +
                "root = " + root + "," +
                "class = ^meta::pure::metamodel::import::ImportStub " + classSourceInfo + " (importGroup=system::imports::" + importId + ", idOrPath='" + classPath + "')," +
                "parent = ^meta::pure::metamodel::import::ImportStub (importGroup=system::imports::" + importId + ", idOrPath='" + mappingPath + "')," +
                "relationFunction = ^meta::pure::metamodel::import::ImportStub " + this.sourceInformation.getPureSourceInformation(startToken, startToken,  ctx.qualifiedName().getStop()).toM4String() + " (importGroup=system::imports::" + this.importId + ", idOrPath='" + functionPath + "')," +
                "propertyMappings=[" + propertyMappings + "]" +
                ")";
    }

    private String visitPropertyMapping(RelationMappingParser.SinglePropertyMappingContext ctx, String id)
    {
        if (ctx.PLUS() != null)
        {
            return visitPropertyMapping(ctx, id, ctx.qualifiedName(1).getText(), buildMultiplicity(ctx.multiplicity().multiplicityArgument()));
        }
        return visitPropertyMapping(ctx, id, null, null);
    }
    
    private String visitPropertyMapping(RelationMappingParser.SinglePropertyMappingContext ctx, String classMappingId, String localPropertyType, String localPropertyMultiplicity)
    {
        SourceInformation sourceInfo = sourceInformation.getPureSourceInformation(ctx.qualifiedName(0).start);
        String columnName = AntlrContextToM3CoreInstance.removeQuotes(ctx.columnName().getText());
        String propertyName = ctx.qualifiedName(0).getText();
        
        return "^meta::pure::mapping::relation::RelationFunctionPropertyMapping" + sourceInfo.toM4String() + "(" +
            "        localMappingProperty = " + (localPropertyType != null) + "," +
            (localPropertyType == null ? "" : "        localMappingPropertyType = " + localPropertyType + ",") +
            (localPropertyMultiplicity == null ? "" : "localMappingPropertyMultiplicity = " + localPropertyMultiplicity + ",") +
            "        property = '" + propertyName + "'," +
            (classMappingId == null ? "" : "        sourceSetImplementationId = '" + classMappingId + "', ") +
            "        column=^meta::pure::metamodel::relation::Column(name = '" + columnName + "', nameWildCard = false)" +
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
