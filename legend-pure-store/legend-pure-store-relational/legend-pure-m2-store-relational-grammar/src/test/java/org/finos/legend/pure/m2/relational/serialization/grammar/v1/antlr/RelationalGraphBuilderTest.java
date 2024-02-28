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

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RelationalGraphBuilderTest
{

    private String importId = "<importId>";
    private SourceInformation fakeSourceInfo = new SourceInformation("sourceId", 1, 1, 1, 1, 1, 1);
    private AntlrSourceInformation sourceInformation = mock(AntlrSourceInformation.class);

    private org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser parser;
    private org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalLexer lexer;
    private RelationalGraphBuilder subject;

    @Before
    public void setUpMocks()
    {
        when(sourceInformation.getOffsetColumn()).thenReturn(1);
        when(sourceInformation.getOffsetLine()).thenReturn(1);
        when(sourceInformation.getPureSourceInformation(any(Token.class))).thenReturn(fakeSourceInfo);
        when(sourceInformation.getPureSourceInformation(any(Token.class), any(Token.class), any(Token.class))).thenReturn(fakeSourceInfo);
        when(sourceInformation.getPureSourceInformation(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(fakeSourceInfo);
        when(sourceInformation.getSourceInformationForUnknownErrorPosition(anyInt(), anyInt())).thenReturn(fakeSourceInfo);
        when(sourceInformation.getSourceInformationForOffendingToken(anyInt(), anyInt(), any(Token.class))).thenReturn(fakeSourceInfo);
    }

    public void setup(String joinDefinition)
    {
        CharStream input = new ANTLRInputStream(joinDefinition);
        lexer = new org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        parser = new org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr.RelationalParser(tokens);
        subject = new RelationalGraphBuilder(importId, sourceInformation, null);
    }

    @Test
    public void testDefaultJoin()
    {
        setup("Join Firm_Person_In0(firmTable.ID = personTable.FIRMID and firmTable.ID = 1 or firmTable.ID = 2 or firmTable.ID = 3)");
        String joinResult = subject.visitJoin(parser.join());

        Assert.assertEquals(
                "^meta::relational::metamodel::join::Join Firm_Person_In0?[sourceId:1,1,1,1,1,1]?(name='Firm_Person_In0', operation=^meta::relational::metamodel::DynaFunction(name = 'and', parameters = [^meta::relational::metamodel::DynaFunction(name = 'equal', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='FIRMID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'personTable'))]), ^meta::relational::metamodel::DynaFunction(name = 'group', parameters = ^meta::relational::metamodel::DynaFunction(name = 'or', parameters = [^meta::relational::metamodel::DynaFunction(name = 'equal', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::Literal(value=1)]), ^meta::relational::metamodel::DynaFunction(name = 'equal', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::Literal(value=2)]), ^meta::relational::metamodel::DynaFunction(name = 'equal', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::Literal(value=3)])]))]))",
                joinResult
        );
    }

    @Test
    public void testJoinWithPrefixInClauseAndNumbers()
    {
        setup("Join Firm_Person_In1_Prefix(firmTable.ID = personTable.FIRMID and in(firmTable.ID, [2,3,4]))");
        String joinResult = subject.visitJoin(parser.join());

        Assert.assertEquals(
                "^meta::relational::metamodel::join::Join Firm_Person_In1_Prefix?[sourceId:1,1,1,1,1,1]?(name='Firm_Person_In1_Prefix', operation=^meta::relational::metamodel::DynaFunction(name = 'and', parameters = [^meta::relational::metamodel::DynaFunction(name = 'equal', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='FIRMID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'personTable'))]), ^meta::relational::metamodel::DynaFunction(name = 'in', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::LiteralList(values=[^meta::relational::metamodel::Literal(value=2),^meta::relational::metamodel::Literal(value=3),^meta::relational::metamodel::Literal(value=4)])])]))",
                joinResult
        );
    }

    @Test
    public void testJoinWithPrefixInClauseAndStrings()
    {
        setup("Join Firm_Person_In2_Prefix(firmTable.ID = personTable.FIRMID and in(firmTable.LEGALNAME, ['Google', 'Apple']))");
        String joinResult = subject.visitJoin(parser.join());

        Assert.assertEquals(
                "^meta::relational::metamodel::join::Join Firm_Person_In2_Prefix?[sourceId:1,1,1,1,1,1]?(name='Firm_Person_In2_Prefix', operation=^meta::relational::metamodel::DynaFunction(name = 'and', parameters = [^meta::relational::metamodel::DynaFunction(name = 'equal', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='ID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='FIRMID',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'personTable'))]), ^meta::relational::metamodel::DynaFunction(name = 'in', parameters=[^meta::relational::metamodel::TableAliasColumn?[sourceId:1,1,1,1,1,1]?(columnName='LEGALNAME',alias=^meta::relational::metamodel::TableAlias?[sourceId:1,1,1,1,1,1]?(name = 'firmTable')),^meta::relational::metamodel::LiteralList(values=[^meta::relational::metamodel::Literal(value='Google'),^meta::relational::metamodel::Literal(value='Apple')])])]))",
                joinResult
        );
    }

}
