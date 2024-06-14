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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRelationDatabaseAccessor extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private MutableMap<String, String> sources = Maps.mutable.empty();

    @Before
    public void setUp()
    {
        this.sources.put(MODEL_SOURCE_ID,
                "import test::*;\n" +
                        "\n" +
                        "function myFunc():Any[1]\n" +
                        "{\n" +
                        "   #>{test::TestDB.personTb}#\n" +
                        "}");
        this.sources.put(STORE_SOURCE_ID,
                "###Relational\n" +
                        "Database test::TestDB\n" +
                        "(\n" +
                        "   Table personTb(name VARCHAR(200), firmId INT)\n" +
                        ")");
    }

    @Test
    public void testCompileAndDeleteModel()
    {
        compileSources(STORE_SOURCE_ID);
        int expectedSize = this.repository.serialize().length;
        ImmutableSet<CoreInstance> expectedInstances = Sets.immutable.withAll(this.context.getAllInstances());
        for (int i = 0; i < TEST_COUNT; i++)
        {
            compileSource(MODEL_SOURCE_ID);
            Assert.assertNotNull(this.runtime.getCoreInstance("myFunc__Any_1_"));
            deleteSource(MODEL_SOURCE_ID);
            Assert.assertNull(this.runtime.getCoreInstance("myFunc__Any_1_"));
            Assert.assertEquals(expectedInstances, this.context.getAllInstances());
            Assert.assertEquals("Failed on iteration #" + i, expectedSize, this.repository.serialize().length);
        }
    }

    @Test
    public void testCompileAndDeleteStore()
    {
        String INITIAL_DATA = "import test::*;\n" +
                "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                "\n" +
                "function myFunc():Any[1]\n" +
                "{ \n" +
                "   #>{test::TestDB.personTb}#->filter(t|$t.name == 'ee')\n" +
                "}";

        String STORE = "###Relational\n" +
                "Database test::TestDB\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firmId INT)\n" +
                ")";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source3.pure")
                        .compileWithExpectedCompileFailure("The store 'test::TestDB' can't be found", "source1.pure", 5, 6)
                        .createInMemorySource("source3.pure", STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testCompileAndDeleteMutateStore()
    {
        String INITIAL_DATA = "import test::*;\n" +
                "\n" +
                "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                "function myFunc():Any[1]\n" +
                "{ \n" +
                "   #>{test::TestDB.personTb}#" +
                "       ->filter(t|$t.name == 'ee')" +
                "}";

        String STORE = "###Relational\n" +
                "Database test::TestDB\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firmId INT)\n" +
                ")";

        String STORE2 = "###Relational\n" +
                "Database test::TestDB\n" +
                "(\n" +
                "   Table personTb(name22 VARCHAR(200), firmId INT)\n" +
                ")";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", STORE2)
                        .compileWithExpectedCompileFailure("The system can't find the column name in the Relation (name22:String, firmId:Integer)", "source1.pure", 6, 51)
                        .updateSource("source3.pure", STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testCompileAndDeleteRenameTable()
    {
        String INITIAL_DATA = "import test::*;\n" +
                "\n" +
                "function myFunc():Any[1]\n" +
                "{ \n" +
                "   #>{test::mainDb.PersonTable}#" +
                "}";

        String STORE1 = "###Relational\n" +
                "Database test::incDb\n" +
                "( \n" +
                "   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                ")\n";

        String STORE1_CHANGED = "###Relational\n" +
                "Database test::incDb\n" +
                "( \n" +
                "   Table PersonTable_Renamed(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)\n" +
                ")\n";

        String STORE2 = "###Relational\n" +
                "Database test::mainDb\n" +
                "( \n" +
                "   include test::incDb\n" +
                "   Table FirmTable(legalName VARCHAR(200), firmId INTEGER)\n" +
                ")\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", STORE1, "source3.pure", STORE2))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", STORE1_CHANGED)
                        .compileWithExpectedCompileFailure("The table 'PersonTable' can't be found in the schema 'default' in the database 'test::mainDb'", "source1.pure", 5, 5)
                        .updateSource("source2.pure", STORE1)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testCompileAndDeleteFull()
    {
        String INITIAL_DATA = "import test::*;\n" +
                "\n" +
                "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                "function myFunc():Any[1]\n" +
                "{ \n" +
                "   #>{test::TestDB.personTb}#" +
                "       ->filter(t|$t.name == 'ee')" +
                "}";

        String STORE = "\n###Relational\n" +
                "Database test::TestDB\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firmId INT)\n" +
                ")";

        String STORE2 = "\n###Relational\n" +
                "Database test::TestDB\n" +
                "(\n" +
                "   Table personTb(name22 VARCHAR(200), firmId INT)\n" +
                ")";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA + STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", INITIAL_DATA + STORE2)
                        .compileWithExpectedCompileFailure("The system can't find the column name in the Relation (name22:String, firmId:Integer)", "source1.pure", 6, 51)
                        .updateSource("source1.pure", INITIAL_DATA + STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    private void compileSources(String... sourceIds)
    {
        for (String sourceId : sourceIds)
        {
            this.runtime.createInMemorySource(sourceId, this.sources.get(sourceId));
        }
        this.runtime.compile();
    }

    private void compileSource(String sourceId)
    {
        compileSources(sourceId);
    }

    private void deleteSource(String sourceId)
    {
        this.runtime.delete(sourceId);
        this.runtime.compile();
    }
}
