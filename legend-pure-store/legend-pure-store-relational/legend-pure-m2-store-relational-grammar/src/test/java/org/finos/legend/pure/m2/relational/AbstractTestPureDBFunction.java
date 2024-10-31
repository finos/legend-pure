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

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public abstract class AbstractTestPureDBFunction extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_SOURCE_ID = "fromString.pure";

    @After
    public void cleanUp()
    {
        runtime.delete(TEST_SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testCreateTempTableError()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), \n" +
                        "                   {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE (col INT)'}, \n" +
                        "                   $dbConnection);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("Error executing sql query; SQL reason: Syntax error in SQL statement \"Create LOCAL TEMPORARY TABLE \\[\\*]\\(col INT\\) ?\"; expected \"identifier\"; SQL statement:\n" +
                "Create LOCAL TEMPORARY TABLE \\(col INT\\) \\[42001-\\d++]; SQL error code: 42001; SQL state: 42001"), 8, 4, e);
    }

    @Test
    public void testDropTempTableError()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   dropTempTable('tt', $dbConnection);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("Error executing sql query; SQL reason: Table \"TT\" not found; SQL statement:\n" +
                "drop table tt \\[42102-\\d++]; SQL error code: 42102; SQL state: 42S02"), 8, 4, e);
    }

    @Test
    public void testExecuteInDbError()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   executeInDb('select * from tt', $dbConnection, 0, 1000);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("Error executing sql query; SQL reason: Table \"TT\" not found \\(this database is empty\\); SQL statement:\n" +
                "select \\* from tt \\[42104-\\d++]; SQL error code: 42104; SQL state: 42S04"), 8, 4, e);
    }

    @Test
    public void testExecuteInDb_H2()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   let res  = executeInDb('select H2VERSION();', $dbConnection, 0, 1000);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        compileAndExecute("test():Any[0..1]");
    }

    @Test
    public void testInsertFloatInDb_H2()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   executeInDb('drop table myTable IF EXISTS;', $dbConnection, 0, 1000);\n" +
                        "   executeInDb('create table myTable(col1 FLOAT)', $dbConnection, 0, 1000);\n" +
                        "   executeInDb('insert into myTable(col1) values (0.9)', $dbConnection, 0, 1000);\n" +
                        "   let res  = executeInDb('select * from myTable', $dbConnection, 0, 1000);\n" +
                        "    assertEquals(0.9 , $res.rows.values);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        compileAndExecute("test():Any[0..1]");
    }

    @Test
    public void testExecuteInb_DuckDB()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.DuckDB);\n" +
                        "   let res  = executeInDb('select * from duckdb_settings();', $dbConnection, 0, 1000);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
       compileAndExecute("test():Any[0..1]");
    }

    // Duck db implicitly converts result of int sum to hugeint >> test to ensure we can read duckdb specific hugeint into pure (as int)
    @Test
    public void testExecuteInb_DuckDB_HugeInt()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.DuckDB);\n" +
                        "   let res  = executeInDb('select 1+1;', $dbConnection, 0, 1000);\n" +
                        "   assertEquals(2, $res.rows->at(0).values->at(0));\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        compileAndExecute("test():Any[0..1]");
    }
}
