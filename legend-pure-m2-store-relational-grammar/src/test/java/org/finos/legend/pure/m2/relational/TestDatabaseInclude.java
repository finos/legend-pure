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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDatabaseInclude extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(GenericCodeRepository.build("test", "((test)|(meta))(::.*)?", PlatformCodeRepository.NAME));
        return repositories;
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("/test/testDB.pure");
        runtime.delete("/test/testDB1.pure");
        runtime.delete("/test/testDB2.pure");
        runtime.compile();
    }

    @Test
    public void testDoubleInclude()
    {
        compileTestSource("/test/testDB1.pure",
                "###Relational\n" +
                        "Database test::TestDB1 ()");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testDB2.pure",
                "###Relational\n" +
                        "Database test::TestDB2\n" +
                        "(\n" +
                        "    include test::TestDB1\n" +
                        "    include test::TestDB1\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "test::TestDB1 is included multiple times in test::TestDB2", "/test/testDB2.pure", 2, 1, 2, 16, 6, 1, e);
    }

    @Test
    public void testSelfInclude()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testDB.pure",
                    "###Relational\n" +
                            "Database test::TestDB\n" +
                            "(\n" +
                            "    include test::TestDB\n" +
                            ")\n"));
        assertPureException(PureCompilationException.class, "Circular include in test::TestDB: test::TestDB -> test::TestDB", "/test/testDB.pure", 2, 1, 2, 16, 5, 1, e);
    }

    @Test
    public void testIncludeLoop()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testDB.pure",
                    "###Relational\n" +
                            "Database test::TestDB1\n" +
                            "(\n" +
                            "    include test::TestDB2\n" +
                            ")\n" +
                            "\n" +
                            "###Relational\n" +
                            "Database test::TestDB2\n" +
                            "(\n" +
                            "    include test::TestDB3\n" +
                            ")\n" +
                            "\n" +
                            "###Relational\n" +
                            "Database test::TestDB3\n" +
                            "(\n" +
                            "    include test::TestDB1\n" +
                            ")\n"));
        switch (e.getInfo())
        {
            case "Circular include in test::TestDB1: test::TestDB1 -> test::TestDB2 -> test::TestDB3 -> test::TestDB1":
            {
                assertSourceInformation("/test/testDB.pure", 2, 1, 2, 16, 5, 1, e.getSourceInformation());
                break;
            }
            case "Circular include in test::TestDB2: test::TestDB2 -> test::TestDB3 -> test::TestDB1 -> test::TestDB2":
            {
                assertSourceInformation("/test/testDB.pure", 8, 1, 8, 16, 11, 1, e.getSourceInformation());
                break;
            }
            case "Circular include in test::TestDB3: test::TestDB3 -> test::TestDB1 -> test::TestDB2 -> test::TestDB3":
            {
                assertSourceInformation("/test/testDB.pure", 14, 1, 14, 16, 17, 1, e.getSourceInformation());
                break;
            }
            default:
            {
                Assert.assertEquals("Circular include in test::TestDB1: test::TestDB1 -> test::TestDB2 -> test::TestDB3 -> test::TestDB1", e.getInfo());
            }
        }
    }
}
