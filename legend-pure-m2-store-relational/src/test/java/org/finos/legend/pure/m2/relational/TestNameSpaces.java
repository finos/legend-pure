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

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestNameSpaces extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testDatabaseNameConflict()
    {
        compileTestSource("db1.pure",
                "###Relational\n" +
                        "Database test::MyDB ()");
        CoreInstance myDB = this.runtime.getCoreInstance("test::MyDB");
        Assert.assertNotNull(myDB);
        Assert.assertTrue(Instance.instanceOf(myDB, M2RelationalPaths.Database, this.processorSupport));

        try
        {
            compileTestSource("db2.pure",
                    "###Relational\n" +
                            "Database test::MyDB ()");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyDB' already exists in the package 'test'", "db2.pure", 2, 1, 2, 16, 2, 22, e);
        }
    }

    @Test
    public void testColumnNameConflict()
    {
        try
        {
            compileTestSource("/test/testDB.pure",
                    "###Relational\n" +
                            "\n" +
                            "Database test::MyTestDB\n" +
                            "(\n" +
                            "  Table T1 (col1 INT, col2 VARCHAR(32), col1 INT)\n" +
                            ")");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Multiple columns named 'col1' found in table T1", "/test/testDB.pure", 5, 41, 5, 41, 5, 44, e);
        }
    }


}
