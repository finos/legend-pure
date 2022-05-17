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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.EnumerationMappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.BusinessSnapshotMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestSimpleGrammar extends AbstractPureRelationalTestWithCoreCompiled
{
    private RelationalGraphWalker graphWalker;

    @Before
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(runtime, processorSupport);
    }

    @Test
    public void testTable()
    {
        Loader.parseM3("Class Employee\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database pack::myDatabase (\n" +
                "/* Comment */\n" +
                "Table employeeTable\n" +
                "(\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200)\n" +
                ")\n" +
                "Table firmTable\n" +
                "(\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200)\n" +
                "))\n" +
                "###Pure\n" +
                "import pack::*;\n" +
                "function test():Boolean[1]\n" +
                "{\n" +
                "    let et = myDatabase.schema('default').table('employeeTable');\n" +
                "    assert('employeeTable' == $et.name, |'');\n" +
                "    assert($et.columns->size() == 2, |'');\n" +
                "    assert(['id', 'name'] == $et.columns->cast(@meta::relational::metamodel::Column)->map(c | $c.name), |'');\n" +
                "    let ft = myDatabase.schema('default').table('firmTable');\n" +
                "    assert('firmTable' == $ft.name, |'');\n" +
                "    assert(['id', 'name'] == $ft.columns->cast(@meta::relational::metamodel::Column)->map(c | $c.name), |'');\n" +
                "}\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("pack::myDatabase");
        Assert.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assert.assertNotNull(defaultSchema);

        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "firmTable"));

        CoreInstance employeeTable = this.graphWalker.getTable(defaultSchema, "employeeTable");
        CoreInstance firmTable = this.graphWalker.getTable(defaultSchema, "firmTable");
        ListIterable<? extends CoreInstance> employeeTableColumns = this.graphWalker.getColumns(employeeTable);
        ListIterable<? extends CoreInstance> firmTableColumns = this.graphWalker.getColumns(firmTable);
        Assert.assertEquals(2, employeeTableColumns.size());
        Assert.assertEquals(2, firmTableColumns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(employeeTable, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(employeeTable, "name"));

        Assert.assertNotNull(this.graphWalker.getColumn(firmTable, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(firmTable, "name"));
    }

    @Test
    public void testTableWithBusinessSnapshotMilestoning()
    {
        runtime.createInMemorySource("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(BUS_SNAPSHOT_DATE = snapshotDate)\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Date\n" +
                "   )\n" +
                ")\n");
        runtime.compile();

        Database productDatabase = (Database) runtime.getCoreInstance("pack::ProductDatabase");
        RichIterable<? extends Milestoning> milestonings = productDatabase._schemas().getFirst()._tables().getFirst()._milestoning().selectInstancesOf(BusinessSnapshotMilestoning.class);
        Assert.assertEquals(1, milestonings.size());
        Assert.assertTrue(milestonings.getFirst() instanceof BusinessSnapshotMilestoning);
        BusinessSnapshotMilestoning businessSnapshotMilestoning = (BusinessSnapshotMilestoning) milestonings.getFirst();
        Assert.assertEquals("snapshotDate", businessSnapshotMilestoning._snapshotDate()._name());
        Assert.assertNull(businessSnapshotMilestoning._infinityDate());
    }

    @Test
    public void testSnapshotDateColumnType()
    {
        runtime.createInMemorySource("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(BUS_SNAPSHOT_DATE = snapshotDate)\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Timestamp\n" +
                "   )\n" +
                ")\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:test.pure line:9 column:8), \"Column set as BUS_SNAPSHOT_DATE can only be of type : [Date]\"", e.getMessage());
    }

    @Test
    public void testTableWithMilestoningInformationWithIsThruInclusive()
    {
        runtime.createInMemorySource("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=false, INFINITY_DATE=%9999-12-30T19:00:00.0000),\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=false, INFINITY_DATE=%9999-12-30T19:00:00.0000)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                "   Table ProductTable2(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=true, INFINITY_DATE=%9999-12-30T19:00:00.0000),\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=true, INFINITY_DATE=%9999-12-30T19:00:00.0000)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                "   Table ProductTable3(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=false),\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=true)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                "   Table ProductTable4(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=true),\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("pack::ProductDatabase");
        Assert.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assert.assertNotNull(defaultSchema);
        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "ProductTable3"));

        CoreInstance ProductTable1 = this.graphWalker.getTable(defaultSchema, "ProductTable1");
        ListIterable<? extends CoreInstance> ProductTable1Columns = this.graphWalker.getColumns(ProductTable1);
        Assert.assertEquals(6, ProductTable1Columns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable1, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable1, "name"));
        Assert.assertNotNull(this.graphWalker.getMany(ProductTable1, "milestoning"));
        CoreInstance processingMilestoning1 = this.graphWalker.getMany(ProductTable1, "milestoning").get(0);
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "in"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "out"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "outIsInclusive"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "infinityDate"));
        CoreInstance businessMilestoning1 = this.graphWalker.getMany(ProductTable1, "milestoning").get(1);
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "from"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "thru"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "thruIsInclusive"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "infinityDate"));

        CoreInstance ProductTable2 = this.graphWalker.getTable(defaultSchema, "ProductTable2");
        ListIterable<? extends CoreInstance> ProductTable2Columns = this.graphWalker.getColumns(ProductTable2);
        Assert.assertEquals(6, ProductTable2Columns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable2, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable2, "name"));
        Assert.assertNotNull(this.graphWalker.getMany(ProductTable2, "milestoning"));
        CoreInstance processingMilestoning2 = this.graphWalker.getMany(ProductTable2, "milestoning").get(0);
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "in"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "out"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "outIsInclusive"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "infinityDate"));

        CoreInstance businessMilestoning2 = this.graphWalker.getMany(ProductTable2, "milestoning").get(1);
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "from"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "thru"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "thruIsInclusive"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "infinityDate"));


        CoreInstance ProductTable3 = this.graphWalker.getTable(defaultSchema, "ProductTable3");
        ListIterable<? extends CoreInstance> ProductTable3Columns = this.graphWalker.getColumns(ProductTable3);
        Assert.assertEquals(6, ProductTable3Columns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable3, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable3, "name"));
        Assert.assertNotNull(this.graphWalker.getMany(ProductTable3, "milestoning"));
        CoreInstance processingMilestoning3 = this.graphWalker.getMany(ProductTable3, "milestoning").get(0);
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning3, "in"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning3, "out"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning3, "outIsInclusive"));
        CoreInstance businessMilestoning3 = this.graphWalker.getMany(ProductTable3, "milestoning").get(1);
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning3, "from"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning3, "thru"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning3, "thruIsInclusive"));

        CoreInstance ProductTable4 = this.graphWalker.getTable(defaultSchema, "ProductTable4");
        ListIterable<? extends CoreInstance> ProductTable4Columns = this.graphWalker.getColumns(ProductTable4);
        Assert.assertEquals(6, ProductTable4Columns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable4, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(ProductTable4, "name"));
        Assert.assertNotNull(this.graphWalker.getMany(ProductTable4, "milestoning"));
        CoreInstance processingMilestoning4 = this.graphWalker.getMany(ProductTable4, "milestoning").get(0);
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning4, "in"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning4, "out"));
        Assert.assertNotNull(this.graphWalker.getOne(processingMilestoning4, "outIsInclusive"));

        CoreInstance businessMilestoning4 = this.graphWalker.getMany(ProductTable4, "milestoning").get(1);
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning4, "from"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning4, "thru"));
        Assert.assertNotNull(this.graphWalker.getOne(businessMilestoning4, "thruIsInclusive"));
    }

    @Test
    public void testOutIsInclusiveSyntax()
    {
        runtime.createInMemorySource("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z OUT_IS_INCLUSIVE=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e1 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        assertPureException(PureParserException.class, "expected: ')' found: 'OUT_IS_INCLUSIVE'", 7, 68, e1);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z,, OUT_IS_INCLUSIVE=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e2 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'OUT_IS_INCLUSIVE', 'INFINITY_DATE'} found: ','", 7, 68, e2);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=false, )\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e3 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'INFINITY_DATE' found: '<EOF>'", 7, 92, e3);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, OUT_IS_INCLUSIVE=false, PROCESSING_OUT=out_z)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e4 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'PROCESSING_OUT' found: 'OUT_IS_INCLUSIVE'", 7, 47, e4);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, Out_Is_Inclusive=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            in_z DATE, \n" +
                "            out_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e5 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'OUT_IS_INCLUSIVE', 'INFINITY_DATE'} found: 'Out_Is_Inclusive'", 7, 69, e5);
    }

    @Test
    public void testThruIsInclusiveSyntax()
    {
        runtime.createInMemorySource("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z THRU_IS_INCLUSIVE=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e1 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        assertPureException(PureParserException.class, "expected: ')' found: 'THRU_IS_INCLUSIVE'", 7, 58, e1);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z,, THRU_IS_INCLUSIVE=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e2 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'THRU_IS_INCLUSIVE', 'INFINITY_DATE'} found: ','", 7, 58, e2);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=false, )\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e3 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'INFINITY_DATE' found: '<EOF>'", 7, 83, e3);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               business(BUS_FROM=from_z, THRU_IS_INCLUSIVE=false, BUS_THRU=thru_z)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e4 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'BUS_THRU' found: 'THRU_IS_INCLUSIVE'", 7, 42, e4);

        runtime.modify("database.pure", "###Relational\n" +
                "\n" +
                "Database pack::ProductDatabase\n" +
                "(\n" +
                "   Table ProductTable1(\n" +
                "            milestoning(\n" +
                "               business(BUS_FROM=from_z, BUS_THRU=thru_z, Thru_Is_Inclusive=false)\n" +
                "            )\n" +
                "            id Integer PRIMARY KEY, \n" +
                "            name VARCHAR(200) PRIMARY KEY,\n" +
                "            from_z DATE, \n" +
                "            thru_z DATE\n" +
                "   ) \n" +
                "   \n" +
                ")\n");
        PureParserException e5 = Assert.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'THRU_IS_INCLUSIVE', 'INFINITY_DATE'} found: 'Thru_Is_Inclusive'", 7, 59, e5);
    }

    @Test
    public void testBusinessSnapshotMilestoningSyntax()
    {
        runtime.createInMemorySource("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(INFINITY_DATE=%9999-12-31, BUS_SNAPSHOT_DATE = snapshotDate)\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Date\n" +
                "   )\n" +
                ")\n");
        PureParserException e1 = Assert.assertThrows(PureParserException.class, runtime::compile);
        Assert.assertEquals("Parser error at (resource:test.pure line:6 column:20), expected: one of {'BUS_FROM', 'BUS_SNAPSHOT_DATE', 'processing'} found: 'INFINITY_DATE'", e1.getMessage());

        runtime.modify("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(BUS_SNAPSHOT_DATE = snapshotDate, INFINITY_DATE=%9999-12-31, )\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Date\n" +
                "   )\n" +
                ")\n");
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e2 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        Assert.assertEquals("expected: ')' found: ','", e2.getMessage());

        runtime.modify("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(BUS_SNAPSHOT_DATE = snapshotDate, )\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Date\n" +
                "   )\n" +
                ")\n");
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e3 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        Assert.assertEquals("expected: \")\" found: \",\"", e3.getMessage());

        runtime.modify("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(, BUS_SNAPSHOT_DATE = snapshotDate)\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Date\n" +
                "   )\n" +
                ")\n");
        PureParserException e4 = Assert.assertThrows(PureParserException.class, runtime::compile);
        Assert.assertEquals("Parser error at (resource:test.pure line:6 column:20), expected: one of {'BUS_FROM', 'BUS_SNAPSHOT_DATE', 'processing'} found: ','", e4.getMessage());

        runtime.modify("test.pure", "###Relational\n" +
                "Database pack::ProductDatabase (\n" +
                "   Table ProductTable\n" +
                "   (\n" +
                "       milestoning( \n" +
                "          business(bus_snapshot_date = snapshotDate)\n" +
                "       )" +
                "       id INT PRIMARY KEY,\n" +
                "       name VARCHAR(200),\n" +
                "       snapshotDate Date\n" +
                "   )\n" +
                ")\n");
        PureParserException e5 = Assert.assertThrows(PureParserException.class, runtime::compile);
        Assert.assertEquals("Parser error at (resource:test.pure line:6 column:20), expected: one of {'BUS_FROM', 'BUS_SNAPSHOT_DATE', 'processing'} found: 'bus_snapshot_date'", e5.getMessage());
    }

    @Test
    public void testJoin()
    {
        Loader.parseM3("###Relational\n" +
                "Database myDB ( Table employeeTable\n" +
                "(\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT\n" +
                ")\n" +
                "Table firmTable\n" +
                "(\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200)\n" +
                ")\n" +
                "\n" +
                "Join Employee_Firm\n" +
                "(\n" +
                "    employeeTable.firmId = firmTable.id\n" +
                "))\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("myDB");
        Assert.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assert.assertNotNull(defaultSchema);

        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "firmTable"));

        CoreInstance employeeTable = this.graphWalker.getTable(defaultSchema, "employeeTable");
        CoreInstance firmTable = this.graphWalker.getTable(defaultSchema, "firmTable");
        ListIterable<? extends CoreInstance> employeeTableColumns = this.graphWalker.getColumns(employeeTable);
        Assert.assertEquals(3, employeeTableColumns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(employeeTable, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(employeeTable, "name"));

        Assert.assertNotNull(this.graphWalker.getColumn(firmTable, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(firmTable, "name"));

        ListIterable<? extends CoreInstance> joins = this.graphWalker.getJoins(db);
        Assert.assertEquals(1, joins.size());
        final CoreInstance employeeFirmJoin = this.graphWalker.getJoin(db, "Employee_Firm");
        Assert.assertNotNull(employeeFirmJoin);

        ListIterable<? extends CoreInstance> aliases = this.graphWalker.getJoinAliases(employeeFirmJoin);
        Assert.assertEquals(2, aliases.size());
        CoreInstance firstAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getFirst());

        Assert.assertEquals("employeeTable", this.graphWalker.getName(firstAliasFirst));

        CoreInstance firstAliasSecond = this.graphWalker.getJoinAliasSecond(aliases.getFirst());

        Assert.assertEquals("firmTable", this.graphWalker.getName(firstAliasSecond));

        CoreInstance secondAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getLast());

        Assert.assertEquals("firmTable", this.graphWalker.getName(secondAliasFirst));

        CoreInstance secondAliasSecond = this.graphWalker.getJoinAliasSecond(aliases.getLast());

        Assert.assertEquals("employeeTable", this.graphWalker.getName(secondAliasSecond));

        CoreInstance joinOperation = this.graphWalker.getJoinOperation(employeeFirmJoin);
        ListIterable<? extends CoreInstance> operationParameters = this.graphWalker.getJoinOperationParameters(joinOperation);
        CoreInstance operationLeft = operationParameters.get(0);
        CoreInstance operationRight = operationParameters.get(1);

        CoreInstance operationLeftAlias = this.graphWalker.getJoinOperationAlias(operationLeft);
        CoreInstance operationRightAlias = this.graphWalker.getJoinOperationAlias(operationRight);

        Assert.assertEquals("employeeTable", this.graphWalker.getName(operationLeftAlias));
        Assert.assertEquals("firmTable", this.graphWalker.getName(operationRightAlias));

        CoreInstance operationLeftColumn = this.graphWalker.getJoinOperationRelationalElement(operationLeft);

        CoreInstance operationRightColumn = this.graphWalker.getJoinOperationRelationalElement(operationRight);

        Assert.assertEquals("firmId", this.graphWalker.getName(operationLeftColumn));
        Assert.assertEquals("id", this.graphWalker.getName(operationRightColumn));
    }


    @Test
    public void testJoinTableError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200)\n" +
                        ")" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTableErr.id\n" +
                        ")\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "The table 'firmTableErr' can't be found in the schema 'default' in the database 'db'", "testStore.pure", 17, 28, e);
    }

    @Test
    public void testJoinColumnError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                "###Relational\n" +
                        "Database db(Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200)\n" +
                        ")\n" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.idErr\n" +
                        "))\n"));
        assertPureException(PureCompilationException.class, "The column 'idErr' can't be found in the table 'firmTable'", "testStore.pure", 16, 38, e);
    }

    @Test
    public void testSelfJoinError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStoreWithError.pure",
                "###Relational\n" +
                        "Database TestDB\n" +
                        "(\n" +
                        "  Schema TestSchema\n" +
                        "  (\n" +
                        "    Table TestTable\n" +
                        "    (\n" +
                        "      id1 INT PRIMARY KEY,\n" +
                        "      id2 INT,\n" +
                        "      name VARCHAR(128)\n" +
                        "    )\n" +
                        "  )\n" +
                        "\n" +
                        "  Schema TestSchema2\n" +
                        "  (\n" +
                        "    Table TestTable\n" +
                        "    (\n" +
                        "      id1 INT PRIMARY KEY,\n" +
                        "      id2 INT,\n" +
                        "      name VARCHAR(128)\n" +
                        "    )\n" +
                        "  )\n" +
                        "\n" +
                        "  Join TestJoin\n" +
                        "  (\n" +
                        "    TestSchema.TestTable.id1 = {target}.id1 and\n" +
                        "    TestSchema2.TestTable.id2 = {target}.id2" +
                        "  )\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "A self join can only contain 1 table, found 2", "testStoreWithError.pure", 24, 8, e);
    }

    @Test
    public void testDuplicateTablesCauseError()
    {
        PureCompilationException e1 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore1.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table employeeTable\n" +
                        "   (\n" +
                        "       id INT PRIMARY KEY\n" +
                        "   )\n" +
                        "   Table employeeTable\n" +
                        "   (\n" +
                        "       id INT PRIMARY KEY\n" +
                        "   )\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "More than one Table found with the name 'employeeTable': Table names must be unique within a database", "testStore1.pure", 8, 10, e1);

        PureCompilationException e2 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore2.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Schema hr\n" +
                        "   (\n" +
                        "      Table employeeTable\n" +
                        "      (\n" +
                        "          id INT PRIMARY KEY\n" +
                        "      )\n" +
                        "      Table employeeTable\n" +
                        "      (\n" +
                        "          id INT PRIMARY KEY\n" +
                        "      )\n" +
                        "   )\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "More than one Table found with the name 'employeeTable': Table names must be unique within a schema", "testStore2.pure", 10, 13, e2);
    }

    @Test
    public void testDuplicateJoinsCauseError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200)\n" +
                        ")\n" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.id\n" +
                        ")\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.id\n" +
                        ")\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "More than one Join found with the name 'Employee_Firm': Join names must be unique within a database", "testStore.pure", 20, 6, e);
    }

    @Test
    public void testDuplicateFiltersCauseError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Filter myFilter(employeeTable.firmId = 2)\n" +
                        "Filter myFilter(employeeTable.firmId = 3)\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "More than one Filter found with the name 'myFilter': Filter names must be unique within a database", "testStore.pure", 11, 8, e);
    }

    @Test
    public void testMappingScope()
    {
        Loader.parseM3("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name : String[1];\n" +
                "    id : Integer[1];" +
                "    other : String[1];" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::pack::db" +
                "(" +
                "    Table employeeTable\n" +
                "    (\n" +
                "        id INT PRIMARY KEY,\n" +
                "        name VARCHAR(200),\n" +
                "        other VARCHAR(200),\n" +
                "        firmId INT\n" +
                "    )" +
                ")\n" +
                "###Mapping\n" +
                "import mapping::pack::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                " /* comment */\n" +
                "    other::Person: Relational\n" +
                "            {" +
                "                scope([db])" +
                "                (" +
                "                    name : employeeTable.name\n" +
                "                )," +
                "                scope([db]default.employeeTable)" +
                "                (" +
                "                    id : id" +
                "                )," +
                "                scope([db]employeeTable)" +
                "                (" +
                "                    other : other" +
                "                )" +
                "            }\n" +
                ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = runtime.getCoreInstance("mappingPackage::myMapping");
        CoreInstance personClassMappingImplementation = Instance.getValueForMetaPropertyToManyResolved(mapping, "classMappings", processorSupport).getFirst();
        ListIterable<? extends CoreInstance> personClassMappingImplementationPropertyMappings = Instance.getValueForMetaPropertyToManyResolved(personClassMappingImplementation, "propertyMappings", processorSupport);
        final StringBuilder sb = new StringBuilder("[\n");
        personClassMappingImplementationPropertyMappings.forEach(each ->
        {
            CoreInstance relationalOperationElement = Instance.getValueForMetaPropertyToOneResolved(each, "relationalOperationElement", TestSimpleGrammar.processorSupport);
            Printer.print(sb, relationalOperationElement, 3);
            sb.append("\n");
        });
        String mappingGraphDump = sb.append("]").toString();
        Assert.assertEquals("[\n" +
                "Anonymous_StripedId instance TableAliasColumn\n" +
                "    alias(Property):\n" +
                "        Anonymous_StripedId instance TableAlias\n" +
                "            database(Property):\n" +
                "                [~>] db instance Database\n" +
                "            name(Property):\n" +
                "                employeeTable instance String\n" +
                "            relationalElement(Property):\n" +
                "                Anonymous_StripedId instance Table\n" +
                "                    columns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    milestoning(Property):\n" +
                "                    name(Property):\n" +
                "                        employeeTable instance String\n" +
                "                    primaryKey(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    schema(Property):\n" +
                "                        Anonymous_StripedId instance Schema\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3] default instance String\n" +
                "                            relations(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            tables(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            views(Property):\n" +
                "                    setColumns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    temporaryTable(Property):\n" +
                "                        false instance Boolean\n" +
                "    column(Property):\n" +
                "        Anonymous_StripedId instance Column\n" +
                "            name(Property):\n" +
                "                name instance String\n" +
                "            nullable(Property):\n" +
                "                true instance Boolean\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance Table\n" +
                "                    columns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    milestoning(Property):\n" +
                "                    name(Property):\n" +
                "                        employeeTable instance String\n" +
                "                    primaryKey(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    schema(Property):\n" +
                "                        Anonymous_StripedId instance Schema\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3] default instance String\n" +
                "                            relations(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            tables(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            views(Property):\n" +
                "                    setColumns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    temporaryTable(Property):\n" +
                "                        false instance Boolean\n" +
                "            type(Property):\n" +
                "                Anonymous_StripedId instance Varchar\n" +
                "                    size(Property):\n" +
                "                        200 instance Integer\n" +
                "    columnName(Property):\n" +
                "        name instance String\n" +
                "    setMappingOwner(Property):\n" +
                "        Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "            class(Property):\n" +
                "                [~>] other::Person instance Class\n" +
                "            distinct(Property):\n" +
                "                false instance Boolean\n" +
                "            id(Property):\n" +
                "                other_Person instance String\n" +
                "            mainTableAlias(Property):\n" +
                "                Anonymous_StripedId instance TableAlias\n" +
                "                    database(Property):\n" +
                "                        [X] db instance Database\n" +
                "                    name(Property):\n" +
                "                         instance String\n" +
                "                    relationalElement(Property):\n" +
                "                        Anonymous_StripedId instance Table\n" +
                "                            columns(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            milestoning(Property):\n" +
                "                            name(Property):\n" +
                "                                [>3] employeeTable instance String\n" +
                "                            primaryKey(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            schema(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Schema\n" +
                "                            setColumns(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            temporaryTable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "            parent(Property):\n" +
                "                [~>] mappingPackage::myMapping instance Mapping\n" +
                "            primaryKey(Property):\n" +
                "                Anonymous_StripedId instance TableAliasColumn\n" +
                "                    alias(Property):\n" +
                "                        Anonymous_StripedId instance TableAlias\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3]  instance String\n" +
                "                            relationalElement(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                    column(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "            propertyMappings(Property):\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        name instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        [_] Anonymous_StripedId instance TableAliasColumn\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        id instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        Anonymous_StripedId instance TableAliasColumn\n" +
                "                            alias(Property):\n" +
                "                                [>3] Anonymous_StripedId instance TableAlias\n" +
                "                            column(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            columnName(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            setMappingOwner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        other instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        Anonymous_StripedId instance TableAliasColumn\n" +
                "                            alias(Property):\n" +
                "                                [>3] Anonymous_StripedId instance TableAlias\n" +
                "                            column(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            columnName(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            setMappingOwner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "            root(Property):\n" +
                "                true instance Boolean\n" +
                "            stores(Property):\n" +
                "                [X] db instance Database\n" +
                "            userDefinedPrimaryKey(Property):\n" +
                "                false instance Boolean\n" +
                "Anonymous_StripedId instance TableAliasColumn\n" +
                "    alias(Property):\n" +
                "        Anonymous_StripedId instance TableAlias\n" +
                "            database(Property):\n" +
                "                [~>] db instance Database\n" +
                "            name(Property):\n" +
                "                employeeTable instance String\n" +
                "            relationalElement(Property):\n" +
                "                Anonymous_StripedId instance Table\n" +
                "                    columns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    milestoning(Property):\n" +
                "                    name(Property):\n" +
                "                        employeeTable instance String\n" +
                "                    primaryKey(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    schema(Property):\n" +
                "                        Anonymous_StripedId instance Schema\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3] default instance String\n" +
                "                            relations(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            tables(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            views(Property):\n" +
                "                    setColumns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    temporaryTable(Property):\n" +
                "                        false instance Boolean\n" +
                "            schema(Property):\n" +
                "                default instance String\n" +
                "    column(Property):\n" +
                "        Anonymous_StripedId instance Column\n" +
                "            name(Property):\n" +
                "                id instance String\n" +
                "            nullable(Property):\n" +
                "                false instance Boolean\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance Table\n" +
                "                    columns(Property):\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    milestoning(Property):\n" +
                "                    name(Property):\n" +
                "                        employeeTable instance String\n" +
                "                    primaryKey(Property):\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                    schema(Property):\n" +
                "                        Anonymous_StripedId instance Schema\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3] default instance String\n" +
                "                            relations(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            tables(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            views(Property):\n" +
                "                    setColumns(Property):\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    temporaryTable(Property):\n" +
                "                        false instance Boolean\n" +
                "            type(Property):\n" +
                "                Anonymous_StripedId instance Integer\n" +
                "    columnName(Property):\n" +
                "        id instance String\n" +
                "    setMappingOwner(Property):\n" +
                "        Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "            class(Property):\n" +
                "                [~>] other::Person instance Class\n" +
                "            distinct(Property):\n" +
                "                false instance Boolean\n" +
                "            id(Property):\n" +
                "                other_Person instance String\n" +
                "            mainTableAlias(Property):\n" +
                "                Anonymous_StripedId instance TableAlias\n" +
                "                    database(Property):\n" +
                "                        [X] db instance Database\n" +
                "                    name(Property):\n" +
                "                         instance String\n" +
                "                    relationalElement(Property):\n" +
                "                        Anonymous_StripedId instance Table\n" +
                "                            columns(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            milestoning(Property):\n" +
                "                            name(Property):\n" +
                "                                [>3] employeeTable instance String\n" +
                "                            primaryKey(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            schema(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Schema\n" +
                "                            setColumns(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            temporaryTable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "            parent(Property):\n" +
                "                [~>] mappingPackage::myMapping instance Mapping\n" +
                "            primaryKey(Property):\n" +
                "                Anonymous_StripedId instance TableAliasColumn\n" +
                "                    alias(Property):\n" +
                "                        Anonymous_StripedId instance TableAlias\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3]  instance String\n" +
                "                            relationalElement(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                    column(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "            propertyMappings(Property):\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        name instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        Anonymous_StripedId instance TableAliasColumn\n" +
                "                            alias(Property):\n" +
                "                                [>3] Anonymous_StripedId instance TableAlias\n" +
                "                            column(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            columnName(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            setMappingOwner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        id instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        [_] Anonymous_StripedId instance TableAliasColumn\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        other instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        Anonymous_StripedId instance TableAliasColumn\n" +
                "                            alias(Property):\n" +
                "                                [>3] Anonymous_StripedId instance TableAlias\n" +
                "                            column(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            columnName(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            setMappingOwner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "            root(Property):\n" +
                "                true instance Boolean\n" +
                "            stores(Property):\n" +
                "                [X] db instance Database\n" +
                "            userDefinedPrimaryKey(Property):\n" +
                "                false instance Boolean\n" +
                "Anonymous_StripedId instance TableAliasColumn\n" +
                "    alias(Property):\n" +
                "        Anonymous_StripedId instance TableAlias\n" +
                "            database(Property):\n" +
                "                [~>] db instance Database\n" +
                "            name(Property):\n" +
                "                employeeTable instance String\n" +
                "            relationalElement(Property):\n" +
                "                Anonymous_StripedId instance Table\n" +
                "                    columns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    milestoning(Property):\n" +
                "                    name(Property):\n" +
                "                        employeeTable instance String\n" +
                "                    primaryKey(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    schema(Property):\n" +
                "                        Anonymous_StripedId instance Schema\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3] default instance String\n" +
                "                            relations(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            tables(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            views(Property):\n" +
                "                    setColumns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    temporaryTable(Property):\n" +
                "                        false instance Boolean\n" +
                "    column(Property):\n" +
                "        Anonymous_StripedId instance Column\n" +
                "            name(Property):\n" +
                "                other instance String\n" +
                "            nullable(Property):\n" +
                "                true instance Boolean\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance Table\n" +
                "                    columns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    milestoning(Property):\n" +
                "                    name(Property):\n" +
                "                        employeeTable instance String\n" +
                "                    primaryKey(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    schema(Property):\n" +
                "                        Anonymous_StripedId instance Schema\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3] default instance String\n" +
                "                            relations(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            tables(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            views(Property):\n" +
                "                    setColumns(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Varchar\n" +
                "                        [_] Anonymous_StripedId instance Column\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] firmId instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] true instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "                    temporaryTable(Property):\n" +
                "                        false instance Boolean\n" +
                "            type(Property):\n" +
                "                Anonymous_StripedId instance Varchar\n" +
                "                    size(Property):\n" +
                "                        200 instance Integer\n" +
                "    columnName(Property):\n" +
                "        other instance String\n" +
                "    setMappingOwner(Property):\n" +
                "        Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "            class(Property):\n" +
                "                [~>] other::Person instance Class\n" +
                "            distinct(Property):\n" +
                "                false instance Boolean\n" +
                "            id(Property):\n" +
                "                other_Person instance String\n" +
                "            mainTableAlias(Property):\n" +
                "                Anonymous_StripedId instance TableAlias\n" +
                "                    database(Property):\n" +
                "                        [X] db instance Database\n" +
                "                    name(Property):\n" +
                "                         instance String\n" +
                "                    relationalElement(Property):\n" +
                "                        Anonymous_StripedId instance Table\n" +
                "                            columns(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            milestoning(Property):\n" +
                "                            name(Property):\n" +
                "                                [>3] employeeTable instance String\n" +
                "                            primaryKey(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            schema(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Schema\n" +
                "                            setColumns(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            temporaryTable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "            parent(Property):\n" +
                "                [~>] mappingPackage::myMapping instance Mapping\n" +
                "            primaryKey(Property):\n" +
                "                Anonymous_StripedId instance TableAliasColumn\n" +
                "                    alias(Property):\n" +
                "                        Anonymous_StripedId instance TableAlias\n" +
                "                            database(Property):\n" +
                "                                [X] db instance Database\n" +
                "                            name(Property):\n" +
                "                                [>3]  instance String\n" +
                "                            relationalElement(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                    column(Property):\n" +
                "                        Anonymous_StripedId instance Column\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            nullable(Property):\n" +
                "                                [>3] false instance Boolean\n" +
                "                            owner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Table\n" +
                "                            type(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Integer\n" +
                "            propertyMappings(Property):\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        name instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        Anonymous_StripedId instance TableAliasColumn\n" +
                "                            alias(Property):\n" +
                "                                [>3] Anonymous_StripedId instance TableAlias\n" +
                "                            column(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            columnName(Property):\n" +
                "                                [>3] name instance String\n" +
                "                            setMappingOwner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        id instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        Anonymous_StripedId instance TableAliasColumn\n" +
                "                            alias(Property):\n" +
                "                                [>3] Anonymous_StripedId instance TableAlias\n" +
                "                            column(Property):\n" +
                "                                [>3] Anonymous_StripedId instance Column\n" +
                "                            columnName(Property):\n" +
                "                                [>3] id instance String\n" +
                "                            setMappingOwner(Property):\n" +
                "                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "                Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    localMappingProperty(Property):\n" +
                "                        false instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    property(Property):\n" +
                "                        other instance Property\n" +
                "                            aggregation(Property):\n" +
                "                                [>3] None instance AggregationKind\n" +
                "                            classifierGenericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            genericType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                            multiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            name(Property):\n" +
                "                                [>3] other instance String\n" +
                "                            owner(Property):\n" +
                "                                [X] Person instance Class\n" +
                "                            referenceUsages(Property):\n" +
                "                                [>3] Anonymous_StripedId instance ReferenceUsage\n" +
                "                    relationalOperationElement(Property):\n" +
                "                        [_] Anonymous_StripedId instance TableAliasColumn\n" +
                "                    sourceSetImplementationId(Property):\n" +
                "                        other_Person instance String\n" +
                "                    targetSetImplementationId(Property):\n" +
                "                         instance String\n" +
                "            root(Property):\n" +
                "                true instance Boolean\n" +
                "            stores(Property):\n" +
                "                [X] db instance Database\n" +
                "            userDefinedPrimaryKey(Property):\n" +
                "                false instance Boolean\n" +
                "]", mappingGraphDump);
    }

    @Test
    public void testMapping()
    {
        Loader.parseM3("import other::deep::*;\n" +
                "import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::deep::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    employees:Person[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::pack::db(Table employeeTable\n" +
                "(\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT\n" +
                ")\n" +
                "Table firmTable\n" +
                "(\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200)\n" +
                ")\n" +
                "\n" +
                "Join Employee_Firm\n" +
                "(\n" +
                "    employeeTable.firmId = [mapping::pack::db]firmTable.id\n" +
                "))\n" +
                "###Mapping\n" +
                "import other::deep::*;\n" +
                "import mapping::pack::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    other::Person: Relational\n" +
                "            {\n" +
                "                name : [db]employeeTable.name,\n" +
                "                firm : [db]@Employee_Firm\n" +
                "            }\n" +
                "    Firm : Relational\n" +
                "           {\n" +
                "                legalName: [db]firmTable.name,\n" +
                "                employees: [db]@Employee_Firm\n" +
                "           }\n" +
                ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("mapping::pack::db");
        Assert.assertNotNull(db);
        ListIterable<? extends CoreInstance> schemas = this.graphWalker.getSchemas(db);
        Assert.assertEquals(1, schemas.size());
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assert.assertNotNull(defaultSchema);
        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "firmTable"));

        CoreInstance employeeTable = this.graphWalker.getTable(defaultSchema, "employeeTable");
        CoreInstance firmTable = this.graphWalker.getTable(defaultSchema, "firmTable");
        ListIterable<? extends CoreInstance> employeeTableColumns = this.graphWalker.getColumns(employeeTable);
        ListIterable<? extends CoreInstance> firmTableColumns = this.graphWalker.getColumns(firmTable);
        Assert.assertEquals(3, employeeTableColumns.size());
        Assert.assertEquals(2, firmTableColumns.size());

        Assert.assertNotNull(this.graphWalker.getColumn(employeeTable, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(employeeTable, "name"));

        Assert.assertNotNull(this.graphWalker.getColumn(firmTable, "id"));
        Assert.assertNotNull(this.graphWalker.getColumn(firmTable, "name"));
        Assert.assertEquals(200, this.graphWalker.getColumnSize(this.graphWalker.getColumn(firmTable, "name")));
        Assert.assertEquals("Varchar", this.graphWalker.getClassifier(this.graphWalker.getColumnType(this.graphWalker.getColumn(firmTable, "name"))));

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        Assert.assertEquals(2, this.graphWalker.getClassMappings(mapping).size());
        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assert.assertNotNull(personMapping);
        CoreInstance firmMapping = this.graphWalker.getClassMapping(mapping, "Firm");
        Assert.assertNotNull(firmMapping);


        Assert.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assert.assertEquals("firmTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(firmMapping)));
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());
        Assert.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(firmMapping).size());

        CoreInstance namePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "name");
        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assert.assertNotNull(firmPropMapping);
        CoreInstance nameColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(namePropMapping);

        Assert.assertEquals("employeeTable", this.graphWalker.getTableAliasColumnAliasName(nameColumnAlias));
        Assert.assertEquals("name", this.graphWalker.getTableAliasColumnColumnName(nameColumnAlias));

        CoreInstance firmJoinNode = this.graphWalker.getRelationalOperationElementJoinTreeNode(this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(firmPropMapping));
        Assert.assertEquals("Employee_Firm", this.graphWalker.getRelationalOperationElementJoinTreeNodeJoinName(firmJoinNode));

        CoreInstance legalNamePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmMapping, "legalName");
        CoreInstance legalNameColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(legalNamePropMapping);
        Assert.assertEquals("firmTable", this.graphWalker.getTableAliasColumnAliasName(legalNameColumnAlias));
        Assert.assertEquals("name", this.graphWalker.getTableAliasColumnColumnName(legalNameColumnAlias));

        CoreInstance employeesPropMapping = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(firmPropMapping);
        CoreInstance employeesJoinNode = this.graphWalker.getRelationalOperationElementJoinTreeNode(employeesPropMapping);
        Assert.assertEquals("Employee_Firm", this.graphWalker.getRelationalOperationElementJoinTreeNodeJoinName(employeesJoinNode));
    }

    @Test
    public void testMappingErrorClass()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "###Pure\n" +
                        "Class Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database db (Table employeeTable\n" +
                        "(\n" +
                        "    id INT,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT,\n" +
                        "    name VARCHAR(200)\n" +
                        ")\n" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.id\n" +
                        "))\n" +
                        "###Mapping\n" +
                        "Mapping myMapping\n" +
                        "(\n" +
                        "    PersonErr: Relational\n" +
                        "            {\n" +
                        "                name : employeeTable.name,\n" +
                        "                firm : @Employee_Firm\n" +
                        "            }\n" +
                        "    Firm : Relational\n" +
                        "           {\n" +
                        "                legalName: firmTable.name,\n" +
                        "                employees: @Employee_Firm\n" +
                        "           }\n" +
                        ")\n" +
                        "###Pure\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(myMapping);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "PersonErr has not been defined!", "testSource.pure", 32, 5, e);
    }


    @Test
    public void testMappingErrorProperty()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "###Pure\n" +
                        "Class Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database db(Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200)\n" +
                        ")\n" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.id\n" +
                        "))\n" +
                        "###Mapping\n" +
                        "Mapping myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "            {\n" +
                        "                name : [db]employeeTable.name,\n" +
                        "                firmErr : [db]@Employee_Firm\n" +
                        "            }\n" +
                        "    Firm : Relational\n" +
                        "           {\n" +
                        "                legalName: [db]firmTable.name,\n" +
                        "                employees: [db]@Employee_Firm\n" +
                        "           }\n" +
                        ")\n" +
                        "###Pure\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(myMapping);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "The property 'firmErr' is unknown in the Element 'Person'", "testSource.pure", 35, 17, e);
    }

    @Test
    public void testMappingErrorColumn()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "###Pure\n" +
                        "Class Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database db (Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200)\n" +
                        ")\n" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.id\n" +
                        "))\n" +
                        "###Mapping\n" +
                        "Mapping myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "            {\n" +
                        "                name : [db]employeeTable.nameErr,\n" +
                        "                firm : [db]@Employee_Firm\n" +
                        "            }\n" +
                        "    Firm : Relational\n" +
                        "           {\n" +
                        "                legalName: [db]firmTable.name,\n" +
                        "                employees: [db]@Employee_Firm\n" +
                        "           }\n" +
                        ")\n" +
                        "###Pure\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(myMapping);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "The column 'nameErr' can't be found in the table 'employeeTable'", "testSource.pure", 34, 42, e);
    }


    @Test
    public void testMappingErrorJoin()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "###Pure\n" +
                        "Class Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database db(Table employeeTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT\n" +
                        ")\n" +
                        "Table firmTable\n" +
                        "(\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200)\n" +
                        ")\n" +
                        "\n" +
                        "Join Employee_Firm\n" +
                        "(\n" +
                        "    employeeTable.firmId = firmTable.id\n" +
                        "))\n" +
                        "###Mapping\n" +
                        "Mapping myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "            {\n" +
                        "                name : [db]employeeTable.name,\n" +
                        "                firm : [db]@Employee_Firm\n" +
                        "            }\n" +
                        "    Firm : Relational\n" +
                        "           {\n" +
                        "                legalName: [db]firmTable.name,\n" +
                        "                employees: [db]@Employee_FirmErr\n" +
                        "           }\n" +
                        ")\n" +
                        "###Pure\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(myMapping);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "The join 'Employee_FirmErr' has not been found in the database 'db'", "testSource.pure", 40, 33, e);
    }


    @Test
    public void testSelfJoin()
    {
        Loader.parseM3("import other::*;\n" +
                "###Relational\n" +
                "Database mapping::pack::db" +
                "(" +
                "    Table employeeTable\n" +
                "    (\n" +
                "        id INT PRIMARY KEY,\n" +
                "        name VARCHAR(200)," +
                "        manager_id INT\n" +
                "    )" +
                "    Join Employee_Manager\n" +
                "    (\n" +
                "        {target}.id = employeeTable.manager_id" +
                "    )" +
                ")", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("mapping::pack::db");
        Assert.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assert.assertNotNull(defaultSchema);
        Assert.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        ListIterable<? extends CoreInstance> joins = this.graphWalker.getJoins(db);
        Assert.assertEquals(1, joins.size());
        final CoreInstance employeeManagerJoin = this.graphWalker.getJoin(db, "Employee_Manager");
        Assert.assertNotNull(employeeManagerJoin);

        ListIterable<? extends CoreInstance> aliases = this.graphWalker.getJoinAliases(employeeManagerJoin);
        Assert.assertEquals(2, aliases.size());
        CoreInstance firstAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getFirst());
        Assert.assertEquals("employeeTable", this.graphWalker.getName(firstAliasFirst));
        CoreInstance firstAliasSecond = this.graphWalker.getJoinAliasSecond(aliases.getFirst());
        Assert.assertEquals("t_employeeTable", this.graphWalker.getName(firstAliasSecond));
        CoreInstance secondAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getLast());
        Assert.assertEquals("t_employeeTable", this.graphWalker.getName(secondAliasFirst));
    }

    @Test
    public void testGroupBy()
    {
        Loader.parseM3("Class mapping::groupby::model::domain::IncomeFunction\n" +
                "{\n" +
                "   code:Integer[1];\n" +
                "   name:String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database mapping::groupby::model::store::myDB\n" +
                "(\n" +
                "    Table ACCOUNT_INFO\n" +
                "    (\n" +
                "        id INT PRIMARY KEY,\n" +
                "        ACC_NUM INT,\n" +
                "        IF_CODE INT,\n" +
                "        IF_NAME VARCHAR(200)\n" +
                "    )\n" +
                "\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import mapping::groupby::model::domain::*;\n" +
                "import mapping::groupby::model::store::*;\n" +
                "\n" +
                "Mapping mapping::testMapping\n" +
                "(\n" +
                "    IncomeFunction: Relational\n" +
                "    {\n" +
                "       ~groupBy([myDB]ACCOUNT_INFO.IF_CODE, [myDB]ACCOUNT_INFO.IF_NAME)\n" +
                "       scope([myDB]ACCOUNT_INFO)\n" +
                "       (\n" +
                "          code: [myDB]IF_CODE,\n" +
                "          name : [myDB]IF_NAME\n" +
                "       )\n" +
                "       \n" +
                "    }\n" +
                ")\n" +
                "###Pure\n" +
                "import other::*;\n" +
                "import meta::relational::metamodel::*;\n" +
                "import meta::relational::metamodel::relation::*;\n" +
                "import mapping::groupby::model::domain::*;\n" +
                "import meta::relational::mapping::*;\n" +
                "\n" +
                "function test():Boolean[1]\n" +
                "{" +
                "   let groupBy = mapping::testMapping.classMappingByClass(IncomeFunction)->cast(@RootRelationalInstanceSetImplementation).groupBy;\n" +
                "   print($groupBy, 2);\n" +
                "   assert(2 == $groupBy.columns->size(), |'');\n" +
                "}\n" +
                "\n" +
                "", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = runtime.getCoreInstance("mapping::testMapping");
        CoreInstance classMapping = this.graphWalker.getMany(mapping, "classMappings").getFirst();
        CoreInstance groupBy = this.graphWalker.getOne(classMapping, "groupBy");
        Assert.assertEquals("Anonymous_StripedId instance GroupByMapping\n" +
                "    columns(Property):\n" +
                "        Anonymous_StripedId instance TableAliasColumn\n" +
                "            alias(Property):\n" +
                "                Anonymous_StripedId instance TableAlias\n" +
                "                    database(Property):\n" +
                "                        [~>] myDB instance Database\n" +
                "                    name(Property):\n" +
                "                        [>2] ACCOUNT_INFO instance String\n" +
                "                    relationalElement(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Table\n" +
                "            column(Property):\n" +
                "                Anonymous_StripedId instance Column\n" +
                "                    name(Property):\n" +
                "                        [>2] IF_CODE instance String\n" +
                "                    nullable(Property):\n" +
                "                        [>2] true instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Table\n" +
                "                    type(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Integer\n" +
                "            columnName(Property):\n" +
                "                IF_CODE instance String\n" +
                "            setMappingOwner(Property):\n" +
                "                Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    class(Property):\n" +
                "                        [~>] IncomeFunction instance Class\n" +
                "                    distinct(Property):\n" +
                "                        [>2] false instance Boolean\n" +
                "                    groupBy(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GroupByMapping\n" +
                "                    id(Property):\n" +
                "                        [>2] mapping_groupby_model_domain_IncomeFunction instance String\n" +
                "                    mainTableAlias(Property):\n" +
                "                        [>2] Anonymous_StripedId instance TableAlias\n" +
                "                    parent(Property):\n" +
                "                        [~>] mapping::testMapping instance Mapping\n" +
                "                    primaryKey(Property):\n" +
                "                        [>2] Anonymous_StripedId instance TableAliasColumn\n" +
                "                        [>2] Anonymous_StripedId instance TableAliasColumn\n" +
                "                    propertyMappings(Property):\n" +
                "                        [>2] Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                        [>2] Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    root(Property):\n" +
                "                        [>2] true instance Boolean\n" +
                "                    stores(Property):\n" +
                "                        [X] myDB instance Database\n" +
                "                    userDefinedPrimaryKey(Property):\n" +
                "                        [>2] false instance Boolean\n" +
                "        Anonymous_StripedId instance TableAliasColumn\n" +
                "            alias(Property):\n" +
                "                Anonymous_StripedId instance TableAlias\n" +
                "                    database(Property):\n" +
                "                        [~>] myDB instance Database\n" +
                "                    name(Property):\n" +
                "                        [>2] ACCOUNT_INFO instance String\n" +
                "                    relationalElement(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Table\n" +
                "            column(Property):\n" +
                "                Anonymous_StripedId instance Column\n" +
                "                    name(Property):\n" +
                "                        [>2] IF_NAME instance String\n" +
                "                    nullable(Property):\n" +
                "                        [>2] true instance Boolean\n" +
                "                    owner(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Table\n" +
                "                    type(Property):\n" +
                "                        [>2] Anonymous_StripedId instance Varchar\n" +
                "            columnName(Property):\n" +
                "                IF_NAME instance String\n" +
                "            setMappingOwner(Property):\n" +
                "                Anonymous_StripedId instance RootRelationalInstanceSetImplementation\n" +
                "                    class(Property):\n" +
                "                        [~>] IncomeFunction instance Class\n" +
                "                    distinct(Property):\n" +
                "                        [>2] false instance Boolean\n" +
                "                    groupBy(Property):\n" +
                "                        [>2] Anonymous_StripedId instance GroupByMapping\n" +
                "                    id(Property):\n" +
                "                        [>2] mapping_groupby_model_domain_IncomeFunction instance String\n" +
                "                    mainTableAlias(Property):\n" +
                "                        [>2] Anonymous_StripedId instance TableAlias\n" +
                "                    parent(Property):\n" +
                "                        [~>] mapping::testMapping instance Mapping\n" +
                "                    primaryKey(Property):\n" +
                "                        [>2] Anonymous_StripedId instance TableAliasColumn\n" +
                "                        [>2] Anonymous_StripedId instance TableAliasColumn\n" +
                "                    propertyMappings(Property):\n" +
                "                        [>2] Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                        [>2] Anonymous_StripedId instance RelationalPropertyMapping\n" +
                "                    root(Property):\n" +
                "                        [>2] true instance Boolean\n" +
                "                    stores(Property):\n" +
                "                        [X] myDB instance Database\n" +
                "                    userDefinedPrimaryKey(Property):\n" +
                "                        [>2] false instance Boolean", Printer.print(groupBy, 2));
    }

    @Test
    public void testDistinct()
    {
        Loader.parseM3("Class mapping::distinct::model::domain::IncomeFunction\n" +
                "{\n" +
                "   code:Integer[1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "\n" +
                "Database mapping::distinct::model::store::myDB\n" +
                "(\n" +
                "    Table ACCOUNT_INFO\n" +
                "    (\n" +
                "        ACC_NUM INT,\n" +
                "        IF_CODE INT  PRIMARY KEY\n" +
                "    )\n" +
                "\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "import mapping::distinct::model::domain::*;\n" +
                "import mapping::distinct::model::store::*;\n" +
                "\n" +
                "Mapping mapping::testMapping\n" +
                "(\n" +
                "    IncomeFunction: Relational\n" +
                "    {\n" +
                "       ~distinct\n" +
                "       scope([myDB]ACCOUNT_INFO)\n" +
                "       (\n" +
                "          code: IF_CODE\n" +
                "       )\n" +
                "       \n" +
                "    }\n" +
                ")\n" +
                "###Pure\n" +
                "import other::*;\n" +
                "import meta::relational::metamodel::*;\n" +
                "import meta::relational::metamodel::relation::*;\n" +
                "import mapping::distinct::model::domain::*;\n" +
                "import meta::relational::mapping::*;\n" +
                "\n" +
                "function test():Boolean[1]\n" +
                "{" +
                "   let distinct = mapping::testMapping.classMappingByClass(IncomeFunction)->cast(@RootRelationalInstanceSetImplementation).distinct;\n" +
                "   assert($distinct->toOne(), |'');\n" +
                "}\n" +
                "\n" +
                "", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = runtime.getCoreInstance("mapping::testMapping");
        CoreInstance classMapping = this.graphWalker.getMany(mapping, "classMappings").getFirst();
        CoreInstance distinct = this.graphWalker.getOne(classMapping, "distinct");
        Assert.assertEquals("true instance Boolean", Printer.print(distinct, 1));
    }

    @Test
    public void duplicatePropertyMappingCausesError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name,\n" +
                        "        firm: [db]@firmJoin,\n" +
                        "        firm: [db]@firmJoin\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Duplicate mappings found for the property 'firm' (targetId: other_Firm) in the mapping for class Person, the property should have one mapping.", "testSource.pure", 33, 9, e);
    }

    @Test
    public void testValidateEnumPropertiesHaveEnumMappings()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "Class Employee\n" +
                        "{\n" +
                        "    name: String[1];\n" +
                        "    type: EmployeeType[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Enum EmployeeType\n" +
                        "{\n" +
                        "    CONTRACT,\n" +
                        "    FULL_TIME\n" +
                        "}\n" +
                        "###Relational\n" +
                        "\n" +
                        "Database myDB\n" +
                        "(\n" +
                        "    Table employeeTable\n" +
                        "    (\n" +
                        "        type VARCHAR(20)\n" +
                        "    )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "\n" +
                        "Mapping employeeTestMapping\n" +
                        "(\n" +
                        "   Employee: Relational\n" +
                        "   {\n" +
                        "        scope([myDB]default.employeeTable)\n" +
                        "        (\n" +
                        "            type : type\n" +
                        "        )\n" +
                        "   }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Missing an EnumerationMapping for the enum property 'type'. Enum properties require an EnumerationMapping in order to transform the store values into the Enum.", "testSource.pure", 29, 13, e);
    }

    @Test
    public void testMappingIncludes()
    {
        String pureCode = "import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    employees:Person[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "import mappingPackage::*;\n" +
                "Mapping mappingPackage::subMapping1\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name\n" +
                "    }\n" +
                ")\n" +
                "Mapping mappingPackage::subMapping2\n" +
                "(\n" +
                "    Firm: Relational\n" +
                "    {\n" +
                "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                ")\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    include mappingPackage::subMapping1\n" +
                "    include subMapping2\n" +
                ")\n";
        Loader.parseM3(pureCode, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assert.assertNotNull(mapping);
        ListIterable<? extends CoreInstance> includes = this.graphWalker.getMany(mapping, M3Properties.includes);
        Assert.assertEquals(2, includes.size());
        MutableList<String> includedMappingPaths = includes.collect(include -> PackageableElement.getUserPathForPackageableElement(this.graphWalker.getOne(include, M3Properties.included)), Lists.mutable.ofInitialCapacity(2));
        Verify.assertListsEqual(Lists.fixedSize.with("mappingPackage::subMapping1", "mappingPackage::subMapping2"), includedMappingPaths);
    }

    @Test
    public void testInvalidMappingInclude()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::subMapping1\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        ")\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    include mappingPackage::subMapping1\n" +
                        "    include subMapping112\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "subMapping112 has not been defined!", "testSource.pure", 37, 13, 37, 13, 37, 25, e);
    }

    @Test
    public void testDuplicateMappingInclude()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::subMapping1\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        ")\n" +
                        "Mapping mappingPackage::subMapping2\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        ")\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    include mappingPackage::subMapping1\n" +
                        "    include mappingPackage::subMapping2\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'other_Person' in mapping mappingPackage::myMapping", "testSource.pure", 36, 5, 36, 5, 39, 5, e);
    }

    @Test
    public void testNestedDuplicateMappingInclude()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::subMapping1\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        ")\n" +
                        "Mapping mappingPackage::subMapping2\n" +
                        "(\n" +
                        "    include mappingPackage::subMapping1\n" +
                        ")\n" +
                        "Mapping mappingPackage::subMapping3\n" +
                        "(\n" +
                        "    include mappingPackage::subMapping2\n" +
                        ")\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    include mappingPackage::subMapping3\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'other_Person' in mapping mappingPackage::myMapping", "testSource.pure", 45, 5, 45, 5, 48, 5, e);
    }


    @Test
    public void testValidDuplicateEnumMapping()
    {
        String pureCode =
                "Enum TradeType\n" +
                        "{\n" +
                        "    BUY,\n" +
                        "    SELL\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping tradeMapping\n" +
                        "(\n" +
                        "   TradeType: EnumerationMapping TradeSource1\n" +
                        "   {\n" +
                        "       BUY:  ['BUY', 'B'],\n" +
                        "       SELL: ['SELL', 'S']\n" +
                        "   }\n" +
                        "   TradeType: EnumerationMapping TradeSource2\n" +
                        "   {\n" +
                        "       BUY:  ['CREDIT'],\n" +
                        "       SELL: ['DEBIT']\n" +
                        "   }\n" +
                        ")\n";
        Loader.parseM3(pureCode, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser(), new EnumerationMappingParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
    }

    @Test
    public void testReferredEnumMappingFromIncludes()
    {
        String pureCode =
                "Enum TradeType\n" +
                        "{\n" +
                        "    BUY,\n" +
                        "    SELL\n" +
                        "}\n" +
                        "Class Trade\n" +
                        "{\n" +
                        "    type: TradeType[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class EquityTrade extends Trade\n" +
                        "{\n" +
                        "    product: String[1];\n" +
                        "    quantity: Integer[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "\n" +
                        "Database tradeDB\n" +
                        "(\n" +
                        "    Table eqTradeTable\n" +
                        "    (\n" +
                        "        id INT PRIMARY KEY,\n" +
                        "        product VARCHAR(200),\n" +
                        "        type VARCHAR(10),\n" +
                        "        qty INTEGER\n" +
                        "    )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "\n" +
                        "Mapping tradeMapping1\n" +
                        "(\n" +
                        "    TradeType: EnumerationMapping TradeSource1\n" +
                        "    {\n" +
                        "       BUY:  ['BUY', 'B'],\n" +
                        "       SELL: ['SELL', 'S']\n" +
                        "    }\n" +
                        ")\n" +
                        "\n" +
                        "Mapping tradeMapping2\n" +
                        "(\n" +
                        "    include tradeMapping1\n" +
                        "\n" +
                        "    TradeType: EnumerationMapping TradeSource2\n" +
                        "    {\n" +
                        "       BUY:  ['CREDIT'],\n" +
                        "       SELL: ['DEBIT']\n" +
                        "    }\n" +
                        ")\n" +
                        "\n" +
                        "Mapping tradeMapping3\n" +
                        "(\n" +
                        "    include tradeMapping2\n" +
                        "\n" +
                        "    EquityTrade: Relational\n" +
                        "    {\n" +
                        "        scope( [tradeDB] default.eqTradeTable)\n" +
                        "        (\n" +
                        "            product: product,\n" +
                        "            quantity: qty,\n" +
                        "            type : EnumerationMapping TradeSource1 : type\n" +
                        "        )\n" +
                        "    }\n" +
                        ")\n" +
                        "\n";
        Loader.parseM3(pureCode, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser(), new EnumerationMappingParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
    }

    @Test
    public void testInValidDuplicateEnumMapping()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "Enum TradeType\n" +
                        "{\n" +
                        "    BUY,\n" +
                        "    SELL\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping tradeMapping1\n" +
                        "(\n" +
                        "   TradeType: EnumerationMapping \n" +
                        "   {\n" +
                        "       BUY:  ['BUY', 'B'],\n" +
                        "       SELL: ['SELL', 'S']\n" +
                        "   }\n" +
                        ")\n" +
                        "Mapping tradeMapping2\n" +
                        "(\n" +
                        "   include tradeMapping1\n" +
                        "   TradeType: EnumerationMapping \n" +
                        "   {\n" +
                        "       BUY:  ['CREDIT'],\n" +
                        "       SELL: ['DEBIT']\n" +
                        "   }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'default' in mapping tradeMapping2", "testSource.pure", 18, 4, 18, 4, 18, 12, e);
    }

    @Test
    public void wrongClassMappingFilterIdentifierCausesError()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testSource.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    firstName:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table personTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    firstName VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Table firmTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   View personFirmView\n" +
                        "   (\n" +
                        "    id : personTable.id,\n" +
                        "    firstName : personTable.firstName,\n" +
                        "    firmId : personTable.firmId\n" +
                        "   )\n" +
                        "   Filter GoldmanSachsFilter(firmTable.legalName = 'GoldmanSachs')\n" +
                        "   Join Firm_Person(firmTable.id = personTable.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        ~filter [mapping::db](Hello)@Firm_Person | [mapping::db] GoldmanSachsFilter \n" +
                        "        firstName : [db]personTable.firstName\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureParserException.class, "The joinType is not recognized. Valid join types are: [INNER, OUTER]", "testSource.pure", 43, 31, e);
    }

    @Test
    public void testClassMappingFilterWithInnerJoin()
    {
        compileTestSource("testSource.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    firstName:String[1];\n" +
                        "    firm:Firm[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table personTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    firstName VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Table firmTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   View personFirmView\n" +
                        "   (\n" +
                        "    id : personTable.id,\n" +
                        "    firstName : personTable.firstName,\n" +
                        "    firmId : personTable.firmId\n" +
                        "   )\n" +
                        "   Filter GoldmanSachsFilter(firmTable.legalName = 'GoldmanSachs')\n" +
                        "   Join Firm_Person(firmTable.id = personTable.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::myMapping\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        ~filter [mapping::db](INNER)@Firm_Person | [mapping::db] GoldmanSachsFilter \n" +
                        "        firstName : [db]personTable.firstName\n" +
                        "    }\n" +
                        ")\n");
    }

    @Test
    public void testNotNull()
    {
        compileTestSource("###Relational \n " +
                "Database test::TestDb \n" +
                "( \n" +
                "Table testTable \n" +
                "( \n" +
                "testColumn1 varchar(200), " +
                "testColumn2 varchar(200) NOT NULL " +
                ") \n" +
                ") \n");
        CoreInstance coreInstanceForDb = runtime.getCoreInstance("test::TestDb");
        CoreInstance table = Instance.getValueForMetaPropertyToOneResolved(coreInstanceForDb, M2RelationalProperties.schemas, M2RelationalProperties.tables, processorSupport);
        ListIterable<? extends CoreInstance> columns = Instance.getValueForMetaPropertyToManyResolved(table, M2RelationalProperties.columns, processorSupport);
        CoreInstance testColumn1 = columns.get(0);
        CoreInstance testColumn2 = columns.get(1);
        Assert.assertTrue((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn1, M2RelationalProperties.nullable, processorSupport))));
        Assert.assertFalse((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn2, M2RelationalProperties.nullable, processorSupport))));
    }

    @Test
    public void testPrimaryKeyIsNotNull()
    {
        compileTestSource("###Relational \n " +
                "Database test::TestDb \n" +
                "( \n" +
                "Table testTable \n" +
                "( \n" +
                "testColumn1 varchar(200), " +
                "testColumn2 varchar(200) PRIMARY KEY " +
                ") \n" +
                ") \n");
        CoreInstance coreInstanceForDb = runtime.getCoreInstance("test::TestDb");
        CoreInstance table = Instance.getValueForMetaPropertyToOneResolved(coreInstanceForDb, M2RelationalProperties.schemas, M2RelationalProperties.tables, processorSupport);
        ListIterable<? extends CoreInstance> columns = Instance.getValueForMetaPropertyToManyResolved(table, M2RelationalProperties.columns, processorSupport);
        CoreInstance testColumn1 = columns.get(0);
        CoreInstance testColumn2 = columns.get(1);
        Assert.assertTrue((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn1, M2RelationalProperties.nullable, processorSupport))));
        Assert.assertFalse((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn2, M2RelationalProperties.nullable, processorSupport))));
    }

    @Test
    public void testNotNullWithPrimaryKeyIsNotAllowed()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testFile.pure",
                "###Relational \n " +
                        "Database test::TestDb \n" +
                        "( \n" +
                        "Table testTable \n" +
                        "( \n" +
                        "testColumn1 varchar(200), \n" +
                        "testColumn2 varchar(200) PRIMARY KEY NOT NULL\n" +
                        ") \n" +
                        ") \n"));
        assertPureException(PureParserException.class, "expected: one of {')', ','} found: 'NOT NULL'", "testFile.pure", 7, 38, e);
    }

    @Test
    public void testMappingAssociationDirectlyIsNotAllowed()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFile.pure",
                "import other::*;\n" +
                        "\n" +
                        "Class other::Person\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n" +
                        "Class other::Firm\n" +
                        "{\n" +
                        "    legalName:String[1];\n" +
                        "}\n" +
                        "Association other::Firm_Person\n" +
                        "{\n" +
                        "    firm:Firm[1];\n" +
                        "    employees:Person[1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mapping::db(\n" +
                        "   Table employeeFirmDenormTable\n" +
                        "   (\n" +
                        "    id INT PRIMARY KEY,\n" +
                        "    name VARCHAR(200),\n" +
                        "    firmId INT,\n" +
                        "    legalName VARCHAR(200)\n" +
                        "   )\n" +
                        "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "import other::*;\n" +
                        "import mapping::*;\n" +
                        "Mapping mappingPackage::subMapping1\n" +
                        "(\n" +
                        "    Person: Relational\n" +
                        "    {\n" +
                        "        name : [db]employeeFirmDenormTable.name\n" +
                        "    }\n" +
                        "    Firm_Person: Relational\n" +
                        "    {\n" +
                        "        employees : [db]@firmJoin\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "Trying to map an unsupported type in Relational: Type Error: 'Association' not a subtype of 'Class<Any>'", e);
    }


    @Test
    public void testMappingAssociation()
    {
        Loader.parseM3("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "}\n" +
                "Association other::Firm_Person\n" +
                "{\n" +
                "    firm:Firm[1];\n" +
                "    employees:Person[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::subMapping1\n" +
                "(\n" +
                "    Person[per1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name\n" +
                "    }\n" +
                "    Firm[fir1]: Relational\n" +
                "    {\n" +
                "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                "\n" +
                "    Firm_Person: Relational\n" +
                "    {\n" +
                "        AssociationMapping\n" +
                "        (\n" +
                "           employees[fir1,per1] : [db]@firmJoin,\n" +
                "           firm[per1,fir1] : [db]@firmJoin\n" +
                "        )\n" +
                "    }\n" +
                ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);

        runtime.compile();

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::subMapping1");
        Assert.assertNotNull(mapping);
        ListIterable<? extends CoreInstance> associationMappings = this.graphWalker.getAssociationMappings(mapping);
        Assert.assertEquals(1, associationMappings.size());
        CoreInstance associationMapping = associationMappings.getFirst();
        Assert.assertNotNull(associationMapping);

        CoreInstance employeesMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "employees");

        Assert.assertNotNull(employeesMapping);
        Assert.assertEquals("fir1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assert.assertEquals("per1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());


        CoreInstance firmPropertyMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "firm");
        Assert.assertNotNull(firmPropertyMapping);
        Assert.assertEquals("per1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assert.assertEquals("fir1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());

    }

    @Test
    public void testMappingAssociationWithIncludes()
    {
        Loader.parseM3("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "}\n" +
                "Association other::Firm_Person\n" +
                "{\n" +
                "    firm:Firm[1];\n" +
                "    employees:Person[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::subMapping1\n" +
                "(\n" +
                "    Person[per1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name\n" +
                "    }\n" +
                ")\n" +
                "Mapping mappingPackage::subMapping2\n" +
                "(\n" +
                "    Firm[fir1]: Relational\n" +
                "    {\n" +
                "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                ")\n" +
                "Mapping mappingPackage::subMapping3\n" +
                "(\n" +
                "    include mappingPackage::subMapping1\n" +
                "    include mappingPackage::subMapping2\n" +
                "    Firm_Person: Relational\n" +
                "    {\n" +
                "        AssociationMapping\n" +
                "        (\n" +
                "           employees[fir1,per1] : [db]@firmJoin,\n" +
                "           firm[per1,fir1] : [db]@firmJoin\n" +
                "        )\n" +
                "    }\n" +
                ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);

        runtime.compile();

        CoreInstance mapping3 = this.graphWalker.getMapping("mappingPackage::subMapping3");
        Assert.assertNotNull(mapping3);
        ListIterable<? extends CoreInstance> associationMappings = this.graphWalker.getAssociationMappings(mapping3);
        Assert.assertEquals(1, associationMappings.size());
        CoreInstance associationMapping = associationMappings.getFirst();
        Assert.assertNotNull(associationMapping);

        CoreInstance employeesMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "employees");

        Assert.assertNotNull(employeesMapping);
        Assert.assertEquals("fir1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assert.assertEquals("per1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());

        CoreInstance firmPropertyMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "firm");
        Assert.assertNotNull(firmPropertyMapping);
        Assert.assertEquals("per1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assert.assertEquals("fir1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());
    }

    @Test
    public void testMappingAssociationDefaultIds()
    {
        Loader.parseM3("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "}\n" +
                "Association other::Firm_Person\n" +
                "{\n" +
                "    firm:Firm[1];\n" +
                "    employees:Person[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::subMapping1\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name\n" +
                "    }\n" +
                "    Firm: Relational\n" +
                "    {\n" +
                "        legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                "\n" +
                "    Firm_Person: Relational\n" +
                "    {\n" +
                "        AssociationMapping\n" +
                "        (\n" +
                "           employees : [db]@firmJoin,\n" +
                "           firm : [db]@firmJoin\n" +
                "        )\n" +
                "    }\n" +
                ")\n", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);

        runtime.compile();

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::subMapping1");
        Assert.assertNotNull(mapping);
        ListIterable<? extends CoreInstance> associationMappings = this.graphWalker.getAssociationMappings(mapping);
        Assert.assertEquals(1, associationMappings.size());
        CoreInstance associationMapping = associationMappings.getFirst();
        Assert.assertNotNull(associationMapping);

        CoreInstance employeesMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "employees");

        Assert.assertNotNull(employeesMapping);
        Assert.assertEquals("other_Firm", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assert.assertEquals("other_Person", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());


        CoreInstance firmPropertyMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "firm");
        Assert.assertNotNull(firmPropertyMapping);
        Assert.assertEquals("other_Person", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assert.assertEquals("other_Firm", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());
    }
}
