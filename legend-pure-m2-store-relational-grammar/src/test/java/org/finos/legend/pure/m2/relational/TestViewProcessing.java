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

package org.finos.legend.pure.m2.relational;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestViewProcessing extends AbstractPureRelationalTestWithCoreCompiled
{

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    private RelationalGraphWalker graphWalker;

    @Before
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(this.runtime, this.processorSupport);
    }

    private static String dbWithLongJoinChain = "###Relational\n" +
            "Database with::view::Db\n" +
            "(\n" +
            "   Schema pureTestSchema\n" +
            "   ( \n" +
            "      Table table1( id Integer PRIMARY KEY, t2id Integer)\n" +
            "      Table table2( id Integer PRIMARY KEY, t3id Integer)\n" +
            "      Table table3( id Integer PRIMARY KEY, t4id Integer)\n" +
            "      Table table4( id Integer PRIMARY KEY, t5id Integer)\n" +
            "      Table table5( id Integer PRIMARY KEY, t6id Integer)\n" +
            "      Table table6( id Integer PRIMARY KEY, t7id Integer)\n" +
            "      Table table7( id Integer PRIMARY KEY, t8id Integer)\n" +
            "      Table table8( id Integer PRIMARY KEY, t9id Integer)\n" +
            "      Table table9( id Integer PRIMARY KEY, value DOUBLE)\n" +
            "\n" +
            "       View aView\n" +
            "       (\n" +
            "           id : table1.id PRIMARY KEY,\n" +
            "           value : @t1t2 > @t2t3 > @t3t4 > @t4t5 > @t5t6 > @t6t7 > @t7t8 > @t8t9 | table9.value\n" +
            "       )\n" +
            "    )\n" +
            "\n" +
            "    Join t1t2(pureTestSchema.table1.t2id=pureTestSchema.table2.id)\n" +
            "    Join t2t3(pureTestSchema.table2.t3id=pureTestSchema.table3.id)\n" +
            "    Join t3t4(pureTestSchema.table3.t4id=pureTestSchema.table4.id)\n" +
            "    Join t4t5(pureTestSchema.table4.t5id=pureTestSchema.table5.id)\n" +
            "    Join t5t6(pureTestSchema.table5.t6id=pureTestSchema.table6.id)\n" +
            "    Join t6t7(pureTestSchema.table6.t7id=pureTestSchema.table7.id)\n" +
            "    Join t7t8(pureTestSchema.table7.t8id=pureTestSchema.table8.id)\n" +
            "    Join t8t9(pureTestSchema.table8.t9id=pureTestSchema.table9.id)\n" +
            ")";

    private static String dBWithViewMainTableValidationError = "###Relational\n" +
            "Database with::view::dBWithViewMainTableValidationError\n" +
            "(\n" +
            "      Table orderTable(id Integer PRIMARY KEY, prodFk Integer)\n" +
            "\n" +
            "      Table orderPnlTable( \n" +
            "            ORDER_ID INT PRIMARY KEY, \n" +
            "            pnl FLOAT\n" +
            "      )\n" +
            "\n" +
            "      Table productDataSet(id Integer)\n" +
            "           \n" +
            "      View multipleMainTablesView\n" +
            "       (\n" +
            "           orderId : orderPnlTable.ORDER_ID PRIMARY KEY, \n" +
            "           pnl: orderTable.id,\n" +
            "           productName : @OrderTable_Product | orderTable.id\n" +
            "       )\n" +
            "\n" +
            "    Join OrderTable_Product(orderTable.prodFk=productDataSet.id) \n" +
            ")";

    private static String dBWithFilteredView = "###Relational\n" +
            "Database with::view::filter\n" +
            "(\n" +
            "      Table orderTable(id Integer PRIMARY KEY, prodFk Integer)\n" +
            "\n" +
            "      Table orderPnlTable( \n" +
            "            ORDER_ID INT PRIMARY KEY, \n" +
            "            pnl FLOAT\n" +
            "      )\n" +
            "      View multipleMainTablesView\n" +
            "      (\n" +
            "           ~filter nonZeroPnlFilter" +
            "           pnl : orderPnlTable.pnl, \n" +
            "           orderId: @OrderPnlTable_OrderTable | orderTable.id\n" +
            "      )\n" +
            "      Join OrderPnlTable_OrderTable(orderPnlTable.ORDER_ID=orderTable.id)\n" +
            "      Filter nonZeroPnlFilter(orderPnlTable.pnl != 0) \n" +
            "\n" +
            ")";

    private static String dbWithViewDependentOnTableInOtherDb = "###Relational\n" +
            "Database db\n" +
            "(\n" +
            "      Table orderTable(id Integer PRIMARY KEY, prodFk Integer)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database with::view::db\n" +
            "(\n" +
            "      Table orderPnlTable( \n" +
            "            ORDER_ID INT PRIMARY KEY, \n" +
            "            pnl FLOAT\n" +
            "      )\n" +
            "\n" +
            "      Table productDataSet(id Integer)\n" +
            "           \n" +
            "      View multipleMainTablesView\n" +
            "      (\n" +
            "         orderId : [db]orderTable.id\n" +
            "      )\n" +
            ")";

    private static String dbWithViewDependentOnTableInOtherDbViaIncludes = "###Relational\n" +
            "Database db\n" +
            "(\n" +
            "      Table orderTable(id Integer PRIMARY KEY, prodFk Integer)\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database with::view::db\n" +
            "(\n" +
            "      include db" +
            "      Table orderPnlTable( \n" +
            "            ORDER_ID INT PRIMARY KEY, \n" +
            "            pnl FLOAT\n" +
            "      )\n" +
            "\n" +
            "      Table productDataSet(id Integer)\n" +
            "           \n" +
            "      View multipleMainTablesView\n" +
            "      (\n" +
            "         orderId : [db]orderTable.id\n" +
            "      )\n" +
            ")";

    @Test
    public void testMainTableValidatesWithLongJoinChain()
    {
        Loader.parseM3(dbWithLongJoinChain, this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        CoreInstance db = this.graphWalker.getDbInstance("with::view::Db");
        CoreInstance pureTestSchema = this.graphWalker.getSchema(db, "pureTestSchema");
        CoreInstance view = this.graphWalker.getView(pureTestSchema, "aView");
        assertMainTableAlias(view, "pureTestSchema", "table1");
    }

    @Test
    public void testViewFilter()
    {
        Loader.parseM3(dBWithFilteredView, this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        CoreInstance db = this.graphWalker.getDbInstance("with::view::filter");
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);

        CoreInstance multipleMainTablesView = this.graphWalker.getView(defaultSchema, "multipleMainTablesView");
        CoreInstance filter = multipleMainTablesView.getValueForMetaPropertyToOne(M2RelationalProperties.filter);
        Assert.assertEquals("nonZeroPnlFilter", filter.getValueForMetaPropertyToOne(M2RelationalProperties.filterName).getName());
        Assert.assertNotNull(filter.getValueForMetaPropertyToOne(M2RelationalProperties.filter).getValueForMetaPropertyToOne(M2RelationalProperties.operation));
        Assert.assertEquals(db, Instance.getValueForMetaPropertyToOneResolved(filter, M2RelationalProperties.database, processorSupport));
    }

    @Test
    public void testRelationalViewMainTableValidation()
    {
        this.expectedEx.expect(RuntimeException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:fromString.pure lines:13c12-18c8), \"View: multipleMainTablesView contains multiple main tables: [orderPnlTable,orderTable,productDataSet] there should be only one root Table for Views");

        Loader.parseM3(dBWithViewMainTableValidationError, this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
    }

    @Test
    public void testRelationalViewDisallowOtherDbDependencyValidation()
    {
        this.expectedEx.expect(RuntimeException.class);
        this.expectedEx.expectMessage("All tables referenced in View: multipleMainTablesView should come from the View's owning or included DB: 'with::view::db', table: 'orderTable' does not");

        Loader.parseM3(dbWithViewDependentOnTableInOtherDb, this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
    }

    @Test
    public void testRelationalViewAllowOtherDbDependencyFromIncludesValidation()
    {
        Loader.parseM3(dbWithViewDependentOnTableInOtherDbViaIncludes, this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
    }

    private void assertMainTableAlias(CoreInstance orderPnlView, String expectedSchemaName, String expectedTableName)
    {
        CoreInstance mainTableAlias = orderPnlView.getValueForMetaPropertyToOne(M2RelationalProperties.mainTableAlias);
        CoreInstance mainTable = mainTableAlias.getValueForMetaPropertyToOne(M2RelationalProperties.relationalElement);
        Assert.assertEquals(expectedSchemaName, graphWalker.getName(mainTable.getValueForMetaPropertyToOne(M2RelationalProperties.schema)));
        Assert.assertEquals(expectedTableName, graphWalker.getName(mainTable));
    }

    @Test
    public void testPrimaryKeyCannotBeSpecifiedInMapping()
    {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("'PRIMARY KEY' cannot be specified in mapping");

        Loader.parseM3("Class person::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database db(\n" +
                "   Table employeeTable\n" +
                "   (\n" +
                "    name VARCHAR(200)\n" +
                "   )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    person::Person: Relational\n" +
                "    {\n" +
                "       name : [db]employeeTable.name PRIMARY KEY\n" +
                "    }\n" +
                ")", this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();
    }

    @Test
    public void testViewInSchemaPotentialIssue()
    {
        Loader.parseM3(
                "###Relational\n" +
                        " Database db(\n" +
                        "    Schema ep_Datastore(\n" +
                        "       Table Team( TEAM VARCHAR(200) PRIMARY KEY  )\n" +
                        "    )\n" +
                        "    \n" +
                        "    Schema viewSchema(\n" +
                        "       View TeamDistinct(\n" +
                        "             ~distinct\n" +
                        "              TEAM: ep_Datastore.Team.TEAM PRIMARY KEY \n" +
                        "        ) \n" +
                        "    )\n" +
                        ")"
                , this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("db");
        CoreInstance viewSchema = this.graphWalker.getSchema(db, "viewSchema");
        CoreInstance teamDistinctView = this.graphWalker.getView(viewSchema, "TeamDistinct");
        CoreInstance teamCol = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.columns);
        CoreInstance teamMappingCol = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.columnMappings).getValueForMetaPropertyToOne(M2RelationalProperties.relationalOperationElement).getValueForMetaPropertyToOne(M2RelationalProperties.column);
        CoreInstance viewPkCol = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.primaryKey);
        CoreInstance mainTable = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.mainTableAlias).getValueForMetaPropertyToOne(M2RelationalProperties.relationalElement);
        Assert.assertEquals(mainTable, teamMappingCol.getValueForMetaPropertyToOne(M3Properties.owner));
        Assert.assertEquals(teamDistinctView, teamCol.getValueForMetaPropertyToOne(M3Properties.owner));
        Assert.assertEquals(teamDistinctView, viewPkCol.getValueForMetaPropertyToOne(M3Properties.owner));
    }

    @Test
    public void testViewGroupByCompilation()
    {
        Loader.parseM3(
                "###Relational\n" +
                        " Database db(\n" +
                        "    Schema ep_Datastore(\n" +
                        "       Table Order( ID Integer PRIMARY KEY,\n" +
                        "            ACCOUNT_ID INTEGER,\n" +
                        "            PNL FLOAT\n" +
                        "         )\n" +
                        "    )\n" +
                        "    \n" +
                        "    Schema viewSchema(\n" +
                        "       View TeamDistinct(\n" +
                        "             ~groupBy (ep_Datastore.Order.ACCOUNT_ID)\n" +
                        "              accountId: ep_Datastore.Order.ACCOUNT_ID PRIMARY KEY, \n" +
                        "              summedPnl: sum(ep_Datastore.Order.PNL) \n" +
                        "        ) \n" +
                        "    )\n" +
                        ")"
                , this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("db");
        CoreInstance viewSchema = this.graphWalker.getSchema(db, "viewSchema");
        CoreInstance teamDistinctView = this.graphWalker.getView(viewSchema, "TeamDistinct");

        ListIterable<? extends CoreInstance> viewMappingCols = teamDistinctView.getValueForMetaPropertyToMany(M2RelationalProperties.columnMappings);
        Assert.assertEquals(2, viewMappingCols.size());
        CoreInstance groupByMapping = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.groupBy);
        Assert.assertNotNull(groupByMapping);
        Assert.assertEquals(1, groupByMapping.getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
    }

    @Test
    public void testViewGroupWithJoinCompilation()
    {
        Loader.parseM3(
                "###Relational\n" +
                        " Database db(\n" +
                        "    Schema ep_Datastore(\n" +
                        "       Table Order( ID Integer PRIMARY KEY,\n" +
                        "            ACCOUNT_ID INTEGER,\n" +
                        "            PNL FLOAT\n" +
                        "         )\n" +
                        "       Table orderPnlTable( ORDER_ID INT PRIMARY KEY, pnl FLOAT)" +
                        "    )\n" +
                        "    \n" +
                        "    Schema viewSchema(\n" +
                        "       View TeamDistinct(\n" +
                        "             ~groupBy (ep_Datastore.Order.ACCOUNT_ID)\n" +
                        "              accountId: ep_Datastore.Order.ACCOUNT_ID PRIMARY KEY, \n" +
                        "              orderPnl : sum(@OrderPnlTable_Order | ep_Datastore.orderPnlTable.pnl) \n" +
                        "        ) \n" +
                        "    )\n" +
                        "    Join OrderPnlTable_Order(ep_Datastore.orderPnlTable.ORDER_ID = ep_Datastore.Order.ID)" +
                        ")"
                , this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("db");
        CoreInstance viewSchema = this.graphWalker.getSchema(db, "viewSchema");
        CoreInstance teamDistinctView = this.graphWalker.getView(viewSchema, "TeamDistinct");
        CoreInstance orderPnlColMapping = teamDistinctView.getValueForMetaPropertyToMany(M2RelationalProperties.columnMappings).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M2RelationalProperties.columnName).getName().equals("orderPnl");
            }
        });
        CoreInstance roeWithJoin = Instance.getValueForMetaPropertyToOneResolved(orderPnlColMapping, M2RelationalProperties.relationalOperationElement, M3Properties.parameters, processorSupport);
        CoreInstance join = roeWithJoin.getValueForMetaPropertyToOne(M2RelationalProperties.joinTreeNode);
        CoreInstance alias = join.getValueForMetaPropertyToOne(M2RelationalProperties.alias);
        Assert.assertNotNull(alias);
    }

    @Test
    public void testViewGroupWithIncorrectGroupByTableSpecification()
    {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("View: TeamDistinct has a groupBy which refers to table: 'otherOrder' which is not the mainTable: 'Order'");

        Loader.parseM3(
                "###Relational\n" +
                        " Database db(\n" +
                        "    Schema ep_Datastore(\n" +
                        "       Table Order( ID Integer PRIMARY KEY,\n" +
                        "            ACCOUNT_ID INTEGER,\n" +
                        "            PNL FLOAT\n" +
                        "         )\n" +
                        " Table otherOrder( ID Integer PRIMARY KEY)" +
                        "    )\n" +
                        "    \n" +
                        "    Schema viewSchema(\n" +
                        "       View TeamDistinct(\n" +
                        "             ~groupBy (ep_Datastore.otherOrder.ID)\n" +
                        "              accountId: ep_Datastore.Order.ACCOUNT_ID PRIMARY KEY, \n" +
                        "              summedPnl: sum(ep_Datastore.Order.PNL) \n" +
                        "        ) \n" +
                        "    )\n" +
                        ")"
                , this.repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        this.runtime.compile();
    }


}
