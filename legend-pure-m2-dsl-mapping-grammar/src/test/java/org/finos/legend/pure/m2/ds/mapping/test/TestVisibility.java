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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepositoryWithDependencies;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestVisibility extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getCodeStorage());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/datamart_datamt/testFile.pure");
        runtime.delete("/datamart_datamt/testFile2.pure");
        runtime.delete("/system/testFile.pure");
        runtime.delete("/system/testFile1.pure");
        runtime.delete("/system/testFile2.pure");
        runtime.compile();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        RichIterable<? extends CodeRepository> repositories = AbstractPureTestWithCoreCompiled.getCodeRepositories();
        CodeRepository core = new TestCodeRepositoryWithDependencies("core", null, Sets.mutable.with(repositories.detect(r -> r.getName().equals("platform"))));
        CodeRepository system = new TestCodeRepositoryWithDependencies("system", null, Sets.mutable.with(repositories.detect(r -> r.getName().equals("platform")), repositories.detect(r -> r.getName().equals("platform_dsl_mapping")), core));
        CodeRepository model = new TestCodeRepositoryWithDependencies("model", null, Sets.mutable.with(repositories.detect(r -> r.getName().equals("platform")), core, system));
        CodeRepository other = new TestCodeRepositoryWithDependencies("datamart_datamt", null, Sets.mutable.with(repositories.detect(r -> r.getName().equals("platform")), repositories.detect(r -> r.getName().equals("platform_dsl_mapping")), core, system, model));
        MutableList<CodeRepository> r = Lists.mutable.withAll(repositories);
        r.add(system);
        r.add(model);
        r.add(other);
        return r;
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void testClassMapping()
    {
        compileTestSource(
                "/datamart_datamt/testFile2.pure",
                "Class datamarts::datamt::domain::TestClass2 {}\n");
        Assert.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestClass2"));

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile.pure",
                "function meta::pure::a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                        "###Mapping\n" +
                        "Mapping system::myMap(\n" +
                        "   datamarts::datamt::domain::TestClass2[ppp]: Operation\n" +
                        "           {\n" +
                        "               meta::pure::a__SetImplementation_MANY_()\n" +
                        "           }\n" +
                        ")\n"));

        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::TestClass2 is not visible in the file /system/testFile.pure", "/system/testFile.pure", 4, 31, e);
    }

    @Test
    public void testEnumMapping()
    {
        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Enum datamarts::datamt::domain::TestEnum1{ VAL }\n");
        Assert.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestEnum1"));

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile2.pure",
                "###Mapping\n" +
                        "Mapping meta::pure::TestMapping1\n" +
                        "(\n" +
                        "\n" +
                        "    datamarts::datamt::domain::TestEnum1: EnumerationMapping Foo\n" +
                        "    {\n" +
                        "        VAL:  'a'\n" +
                        "    }\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::TestEnum1 is not visible in the file /system/testFile2.pure", "/system/testFile2.pure", 5, 32, e);
    }

    @Test
    public void testMappingIncludes()
    {
        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Class datamarts::datamt::domain::TestClass2 {}\n" +
                        "function datamarts::datamt::mapping::a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                        "###Mapping\n" +
                        "Mapping datamarts::datamt::mapping::myMap1(\n" +
                        "   datamarts::datamt::domain::TestClass2[ppp]: Operation\n" +
                        "           {\n" +
                        "               datamarts::datamt::mapping::a__SetImplementation_MANY_()\n" +
                        "           }\n" +
                        ")\n");
        Assert.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestClass2"));

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile1.pure",
                "###Mapping\n" +
                        "Mapping system::myMap(\n" +
                        "  include datamarts::datamt::mapping::myMap1\n" +
                        ")\n"));
        assertPureException(PureCompilationException.class, "datamarts::datamt::mapping::myMap1 is not visible in the file /system/testFile1.pure", "/system/testFile1.pure", 2, 17, e);
    }
}
