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
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.EnumerationMappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.EnumerationMappingParserBaseVisitor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.ParsingUtils;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;

public class EnumerationMappingGraphBuilder extends EnumerationMappingParserBaseVisitor<String>
{
    private final String classPath;
    private final String classSourceInfo;
    private final String importId;
    private final AntlrSourceInformation sourceInformation;

    private final StringBuilder builder = new StringBuilder();

    public EnumerationMappingGraphBuilder(String classPath, String classSourceInfo, String importId, AntlrSourceInformation sourceInformation)
    {
        this.classPath = classPath;
        this.classSourceInfo = classSourceInfo;
        this.importId = importId;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public String visitMapping(EnumerationMappingParser.MappingContext ctx)
    {
        visitChildren(ctx);
        ParsingUtils.removeLastCommaCharacterIfPresent(this.builder);
        return this.builder.toString();
    }

    @Override
    public String visitEnumSingleEntryMapping(EnumerationMappingParser.EnumSingleEntryMappingContext ctx)
    {
        this.builder.append("^meta::pure::mapping::EnumValueMapping ").append(this.sourceInformation.getPureSourceInformation(ctx.enumName().getStart(), ctx.enumName().getStart(), ctx.enumName().getStop()).toM4String());
        this.builder.append("(enum=^meta::pure::metamodel::import::EnumStub ").append(this.sourceInformation.getPureSourceInformation(ctx.enumName().getStart(), ctx.enumName().getStart(), ctx.enumName().getStop()).toM4String());
        this.builder.append(" (enumName='").append(ctx.enumName().getText()).append("', ");
        this.builder.append("enumeration=^meta::pure::metamodel::import::ImportStub ").append(this.classSourceInfo);
        this.builder.append(" (importGroup=system::imports::").append(this.importId).append(", idOrPath='").append(this.classPath).append("')), ");
        this.builder.append("sourceValues=[");
        visitChildren(ctx);
        ParsingUtils.removeLastCommaCharacterIfPresent(this.builder);
        this.builder.append("]),");
        return null;
    }

    @Override
    public String visitEnumSourceValue(EnumerationMappingParser.EnumSourceValueContext ctx)
    {
        if (ctx.STRING() != null)
        {
            this.builder.append(ctx.STRING().getText());
        }
        else if (ctx.INTEGER() != null)
        {
            this.builder.append(ctx.INTEGER());
        }
        else
        {
            this.builder.append("^meta::pure::metamodel::import::EnumStub ").append(this.sourceInformation.getPureSourceInformation(ctx.enumReference().identifier().getStart(), ctx.enumReference().identifier().getStart(), ctx.enumReference().identifier().getStop()).toM4String());
            this.builder.append(" (enumName='").append(ctx.enumReference().identifier().getText()).append("', ");
            this.builder.append("enumeration=^meta::pure::metamodel::import::ImportStub ");
            Token startToken = ctx.enumReference().qualifiedName().packagePath() == null ? ctx.enumReference().qualifiedName().identifier().getStart() : ctx.enumReference().qualifiedName().packagePath().getStart();
            this.builder.append(this.sourceInformation.getPureSourceInformation(startToken, startToken, ctx.enumReference().identifier().stop).toM4String());
            this.builder.append("(importGroup=system::imports::").append(this.importId).append(", ");
            this.builder.append("idOrPath='");
            if (ctx.enumReference().qualifiedName().packagePath() != null)
            {
                ctx.enumReference().qualifiedName().packagePath().identifier().forEach(pkg -> this.builder.append(pkg.getText()).append("::"));
            }
            this.builder.append(ctx.enumReference().qualifiedName().identifier().getText()).append("'))");
        }
        this.builder.append(",");
        return null;
    }
}
