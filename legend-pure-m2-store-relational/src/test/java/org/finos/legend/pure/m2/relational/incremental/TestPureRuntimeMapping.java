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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestPureRuntimeMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";

    private MutableMap<String, String> sources = Maps.mutable.empty();

    @Before
    public void setUp()
    {
        this.sources.put(MODEL_SOURCE_ID,
                "import test::*;\n" +
                        "\n" +
                        "Class test::Person\n" +
                        "{\n" +
                        "   name:String[1];\n" +
                        "}");
        this.sources.put(STORE_SOURCE_ID,
                "###Relational\n" +
                        "Database test::TestDB\n" +
                        "(\n" +
                        "   Table personTb(name VARCHAR(200), firmId INT)\n" +
                        ")");
        this.sources.put(MAPPING_SOURCE_ID,
                "###Mapping\n" +
                        "import test::*;\n" +
                        "\n" +
                        "Mapping test::TestMapping\n" +
                        "(\n" +
                        "   Person : Relational\n" +
                        "            {\n" +
                        "               name:[TestDB]personTb.name\n" +
                        "            }\n" +
                        ")");
    }

    @Test
    public void testCompileAndDeleteMapping()
    {
        compileSources(MODEL_SOURCE_ID, STORE_SOURCE_ID);
        int expectedSize = this.repository.serialize().length;
        ImmutableSet<CoreInstance> expectedInstances = Sets.immutable.withAll(this.context.getAllInstances());
        for (int i = 0; i < TEST_COUNT; i++)
        {
            compileSource(MAPPING_SOURCE_ID);
            Assert.assertNotNull(this.runtime.getCoreInstance("test::TestMapping"));
            deleteSource(MAPPING_SOURCE_ID);
            Assert.assertNull(this.runtime.getCoreInstance("test::TestMapping"));

            Assert.assertEquals(expectedInstances, this.context.getAllInstances());
            Assert.assertEquals("Failed on iteration #" + i, expectedSize, this.repository.serialize().length);
        }
    }

    @Test
    public void testDeleteModel()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(MODEL_SOURCE_ID);
                Assert.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Person has not been defined!", MAPPING_SOURCE_ID, 6, 4, e);
            }
            compileSource(MODEL_SOURCE_ID);
            Assert.assertEquals("Failed on iteration #" + i, expectedSize, this.repository.serialize().length);
        }
    }

    @Test
    public void testDeleteStore()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(STORE_SOURCE_ID);
                Assert.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "TestDB has not been defined!", MAPPING_SOURCE_ID, 8, 22, e);
            }
            compileSource(STORE_SOURCE_ID);
            Assert.assertEquals("Failed on iteration #" + i, expectedSize, this.repository.serialize().length);
        }
    }

    private void compileAllSources()
    {
        this.runtime.createInMemoryAndCompile(this.sources);
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
