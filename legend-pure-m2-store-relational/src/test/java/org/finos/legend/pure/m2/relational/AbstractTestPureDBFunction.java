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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Test;

public abstract class AbstractTestPureDBFunction extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testCreateTempTableError() throws Exception
    {
        this.compileTestSource(
                "import meta::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(element = mydb, type = DatabaseType.H2);\n" +
                        "   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), \n" +
                        "                   {ttName:String[1], cols: Column[*], dbType: DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE (col INT)'}, \n" +
                        "                   $dbConnection);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        try
        {
            this.compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            assertPureException(PureExecutionException.class, "Error executing sql query; SQL reason: Syntax error in SQL statement \"CREATE LOCAL TEMPORARY TABLE ([*]COL INT) \"; expected \"identifier\"; SQL statement:\n" +
                    "Create LOCAL TEMPORARY TABLE (col INT) [42001-197]; SQL error code: 42001; SQL state: 42001", 8, 4, ex);
        }
    }

    @Test
    public void testDropTempTableError() throws Exception
    {
        this.compileTestSource(
                "import meta::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(element = mydb, type = DatabaseType.H2);\n" +
                        "   dropTempTable('tt', $dbConnection);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        try
        {
            this.compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            this.assertPureException(PureExecutionException.class, "Error executing sql query; SQL reason: Table \"TT\" not found; SQL statement:\n" +
                    "drop table tt [42102-197]; SQL error code: 42102; SQL state: 42S02", 8, 4, ex);
        }
    }

    @Test
    public void testExecuteInDbError() throws Exception
    {
        this.compileTestSource(
                "import meta::relational::runtime::*;\n" +
                        "import meta::relational::metamodel::*;\n" +
                        "import meta::relational::metamodel::execute::*;\n" +
                        "import meta::relational::functions::toDDL::*;\n" +
                        "function test():Any[0..1]\n" +
                        "{\n" +
                        "   let dbConnection = ^TestDatabaseConnection(element = mydb, type = DatabaseType.H2);\n" +
                        "   executeInDb('select * from tt', $dbConnection, 0, 1000);\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database mydb()\n"
        );
        try
        {
            this.compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            this.assertPureException(PureExecutionException.class, "Error executing sql query; SQL reason: Table \"TT\" not found; SQL statement:\n" +
                    "select * from tt [42102-197]; SQL error code: 42102; SQL state: 42S02", 8, 4, ex);
        }
    }
}
