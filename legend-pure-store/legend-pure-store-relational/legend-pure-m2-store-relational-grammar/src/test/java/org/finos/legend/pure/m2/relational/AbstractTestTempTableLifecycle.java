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

public abstract class AbstractTestTempTableLifecycle extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_SOURCE_ID = "fromString.pure";

    @After
    public void cleanUp()
    {
        runtime.delete(TEST_SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testCreateTempTable()
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
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()),\n" +
                        "                   {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'},\n" +
                        "                   $dbConnection);\n" +
                        "   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);\n" +
                        "   let columnNames = $res.columnNames;\n" +
                        "   print($columnNames, 1);\n" +
                        "   assert('COL' == $columnNames, |'');\n" +
                        "   dropTempTable('tt', $dbConnection);\n" +
                        "   executeInDb('select * from tt', $dbConnection, 0, 1000);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("\\QError executing sql query; SQL reason: Table \"TT\" not found\\E[^;]*+\\Q; SQL statement:\nselect * from tt\\E[^;]*+\\Q; SQL error code: \\E\\d++\\Q; SQL state: \\E.*"), TEST_SOURCE_ID, 16, 4, e);
        Assert.assertEquals("'COL'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testTempTableDroppedInFinally()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()),\n" +
                        "                   {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'},\n" +
                        "                   $dbConnection);\n" +
                        "   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);\n" +
                        "   let columnNames = $res.columnNames;\n" +
                        "   print($columnNames, 1);\n" +
                        "   assert('COL' == $columnNames, |'');\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, "Error: Temporary table: tt should be dropped explicitly", e);
        Assert.assertEquals("'COL'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testRelyOnFinallyTempTableFlow()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "import meta::external::store::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\n" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()),\n" +
                        "   {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, true,\n" +
                        "   $dbConnection);\n" +
                        "   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);\n" +
                        "   let columnNames = $res.columnNames;\n" +
                        "   print($columnNames, 1);\n" +
                        "   assert('COL' == $columnNames, |'');\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );

        compileAndExecute("test():Any[0..1]");
        Assert.assertEquals("'COL'", functionExecution.getConsole().getLine(0));
    }
}
