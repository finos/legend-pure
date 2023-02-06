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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class AbstractTestTempTableLifecycle extends AbstractPureTestWithCoreCompiled
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testCreateTempTable()
    {
        exception.expect(PureExecutionException.class);
        exception.expectMessage("Table \"TT\" not found");

        compileTestSource(
                "import meta::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;" +
                        "import meta::relational::functions::toDDL::*;" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(element = mydb, type = DatabaseType.H2);" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), " +
                        "                   {ttName:String[1], cols: Column[*], dbType: DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, " +
                        "                   $dbConnection);" +
                        "   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);" +
                        "   let columnNames = $res.columnNames;" +
                        "   print($columnNames, 1);" +
                        "   assert('COL' == $columnNames, |'');" +
                        "   dropTempTable('tt', $dbConnection);" +
                        "   executeInDb('select * from tt', $dbConnection, 0, 1000);" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        try
        {
            compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            //expected
            Assert.assertEquals("'COL'", this.functionExecution.getConsole().getLine(0));
            throw ex;
        }
    }

    @Test
    public void testTempTableDroppedInFinally()
    {
        exception.expect(PureExecutionException.class);
        exception.expectMessage("Temporary table: tt should be dropped explicitly");

        compileTestSource(
                "import meta::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(element = mydb, type = DatabaseType.H2);" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), " +
                        "   {ttName:String[1], cols: Column[*], dbType: DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, " +
                        "   $dbConnection);" +
                        "   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);" +
                        "   let columnNames = $res.columnNames;" +
                        "   print($columnNames, 1);" +
                        "   assert('COL' == $columnNames, |'');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        try
        {
            compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            //expected
            Assert.assertEquals("'COL'", this.functionExecution.getConsole().getLine(0));
            throw ex;
        }
    }

    @Test
    public void testRelyOnFinallyTempTableFlow()
    {
        compileTestSource(
                "import meta::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(element = mydb, type = DatabaseType.H2);" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), " +
                        "   {ttName:String[1], cols: Column[*], dbType: DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, true," +
                        "   $dbConnection);" +
                        "   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);" +
                        "   let columnNames = $res.columnNames;" +
                        "   print($columnNames, 1);" +
                        "   assert('COL' == $columnNames, |'');" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        try
        {
            compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            //expected
            Assert.assertEquals("'COL'", this.functionExecution.getConsole().getLine(0));
            throw ex;
        }
    }
}
