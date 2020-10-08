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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.EnumerationMappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.EnumerationMappingParserBaseVisitor;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.antlr.v4.runtime.Token;

import static org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils.removeLastCommaCharacterIfPresent;

public class EnumerationMappingGraphBuilder extends EnumerationMappingParserBaseVisitor<String>
{

    private final String classPath;
    private final String classSourceInfo;
    private final String importId;
    private final AntlrSourceInformation sourceInformation;

    private final StringBuilder builder = new StringBuilder();

    public EnumerationMappingGraphBuilder(String classPath, String classSourceInfo, String importId, AntlrSourceInformation sourceInformation){
        this.classPath = classPath;
        this.classSourceInfo = classSourceInfo;
        this.importId = importId;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public String visitMapping(EnumerationMappingParser.MappingContext ctx){
        visitChildren(ctx);
        removeLastCommaCharacterIfPresent(builder);
        return builder.toString();
    }

    @Override
    public String visitEnumSingleEntryMapping(EnumerationMappingParser.EnumSingleEntryMappingContext ctx){
        builder.append("^meta::pure::mapping::EnumValueMapping");
        builder.append(" " + sourceInformation.getPureSourceInformation(ctx.enumName().getStart(), ctx.enumName().getStart(), ctx.enumName().getStop()).toM4String());
        builder.append("(enum=^meta::pure::metamodel::import::EnumStub");
        builder.append(" " + sourceInformation.getPureSourceInformation(ctx.enumName().getStart(), ctx.enumName().getStart(), ctx.enumName().getStop()).toM4String());
        builder.append(" (enumName='");
        builder.append(ctx.enumName().getText());
        builder.append("', ");
        builder.append(" enumeration=");
        builder.append("^meta::pure::metamodel::import::ImportStub ");
        builder.append(classSourceInfo);
        builder.append(" (importGroup=system::imports::");
        builder.append(importId);
        builder.append(", idOrPath='");
        builder.append(classPath);
        builder.append("')), sourceValues=");
        builder.append("[");
        visitChildren(ctx);
        removeLastCommaCharacterIfPresent(builder);
        builder.append("]");
        builder.append(")");
        builder.append(",");

        return null;
    }

    @Override
    public String visitEnumSourceValue(EnumerationMappingParser.EnumSourceValueContext ctx){
        if (ctx.STRING() != null) {
            builder.append(ctx.STRING().getText());
        } else if (ctx.INTEGER() != null)
            builder.append(ctx.INTEGER());
        else
        {
            builder.append("^meta::pure::metamodel::import::EnumStub");
            builder.append(" " + sourceInformation.getPureSourceInformation(ctx.enumReference().identifier().getStart(), ctx.enumReference().identifier().getStart(), ctx.enumReference().identifier().getStop()).toM4String());
            builder.append("(enumName='");
            builder.append(ctx.enumReference().identifier().getText());
            builder.append("', enumeration=^meta::pure::metamodel::import::ImportStub");
            Token startToken = ctx.enumReference().qualifiedName().packagePath() == null ? ctx.enumReference().qualifiedName().identifier().getStart() : ctx.enumReference().qualifiedName().packagePath().getStart();
            builder.append(" "+ sourceInformation.getPureSourceInformation(startToken, startToken, ctx.enumReference().identifier().stop).toM4String());
            builder.append("(importGroup=system::imports::");
            builder.append(importId);
            builder.append(", idOrPath='");
            String idOrPath = ctx.enumReference().qualifiedName().packagePath() == null ? ctx.enumReference().qualifiedName().identifier().getText() : ListAdapter.adapt(ctx.enumReference().qualifiedName().packagePath().identifier()).collect(IDENTIFIER_CONTEXT_STRING_FUNCTION).makeString("::") + "::" + ctx.enumReference().qualifiedName().identifier().getText();
            builder.append(idOrPath);
            builder.append("'))");
        }
        builder.append(",");
        return null;
    }

    private static final Function<EnumerationMappingParser.IdentifierContext, String> IDENTIFIER_CONTEXT_STRING_FUNCTION = new Function<EnumerationMappingParser.IdentifierContext, String>()
    {
        @Override
        public String valueOf(EnumerationMappingParser.IdentifierContext identifierContext)
        {
            return identifierContext.getText();
        }
    };
}
