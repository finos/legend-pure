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

package org.finos.legend.pure.m3.inlinedsl.graph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestVisibilityAndAccessibilityInGraph extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getCodeStorage(), getCodeRepositories(), null);
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/datamart_datamt/testFile1.pure");
        runtime.delete("/datamart_datamt/testFile2.pure");
        runtime.delete("/datamart_datamt/testFile3.pure");
        runtime.delete("/model/testFile1.pure");
        runtime.delete("/model/testFile3.pure");

        try
        {
            runtime.compile();
        } catch (PureCompilationException e)
        {
            setUp();
        }
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(
                SVNCodeRepository.newDatamartCodeRepository("dtm"),
                SVNCodeRepository.newDatamartCodeRepository("datamt"),
                SVNCodeRepository.newModelCodeRepository(""),
                SVNCodeRepository.newModelCodeRepository("candidate", Sets.immutable.with("")),
                SVNCodeRepository.newModelCodeRepository("legacy", Sets.immutable.with("")),
                SVNCodeRepository.newModelValidationCodeRepository(),
                SVNCodeRepository.newSystemCodeRepository(),
                CodeRepository.newCoreCodeRepository(),
                CodeRepository.newPlatformCodeRepository()
        );
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void testClassReferenceVisibilityInGraph()
    {
        this.assertRepoExists("datamart_datamt");
        this.assertRepoExists("datamart_dtm");

        this.compileTestSource(
                "/datamart_datamt/testFile1.pure",
                "Class datamarts::datamt::domain::A\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n");
        Assert.assertNotNull(this.runtime.getCoreInstance("datamarts::datamt::domain::A"));
        try
        {
            this.compileTestSource(
                    "/datamart_dtm/testFile3.pure",
                            "function datamarts::dtm::domain::testFn1():Any[*]\n" +
                            "{\n" +
                            "  #{datamarts::datamt::domain::A{name}}#\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /datamart_dtm/testFile3.pure", "/datamart_dtm/testFile3.pure", 3, 5, 3, 32, 3, 32, e);
        }
    }

    @Test
    public void testSubTypeClassReferenceVisibilityInGraph()
    {
        this.assertRepoExists("datamart_datamt");
        this.assertRepoExists("model");

        this.compileTestSource(
                "/model/testFile1.pure",
                "Class model::domain::A\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  b : model::domain::B[1];\n" +
                        "}\n" +
                        "Class model::domain::B {}"
        );
        this.compileTestSource(
                "/datamart_datamt/testFile2.pure",
                "Class datamarts::datamt::domain::C extends model::domain::B {}"
        );
        try
        {
            this.compileTestSource(
                    "/model/testFile3.pure",
                            "function model::domain::testFn1():Any[*]\n" +
                            "{\n" +
                            "  #{model::domain::A{name,b->subType(@datamarts::datamt::domain::C)}}#\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "datamarts::datamt::domain::C is not visible in the file /model/testFile3.pure", "/model/testFile3.pure", 3, 39, 3, 39, 3, 66, e);
            this.runtime.delete("/model/testFile3.pure");
        }

        this.compileTestSource(
                "/datamart_datamt/testFile3.pure",
                "function datamarts::datamt::domain::testFn1():Any[*]\n" +
                        "{\n" +
                        "  #{model::domain::A{name,b->subType(@datamarts::datamt::domain::C)}}#\n" +
                        "}\n"
        );
    }

    @Test
    public void testReferenceAccessibilityInGraph()
    {
        this.assertRepoExists("datamart_datamt");
        this.assertRepoExists("datamart_dtm");

        this.compileTestSource(
                "/datamart_datamt/testFile1.pure",
                "Class <<access.private>> datamarts::datamt::domain::A\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n");
        Assert.assertNotNull(this.runtime.getCoreInstance("datamarts::datamt::domain::A"));
        try
        {
            this.compileTestSource(
                    "/datamart_dtm/testFile3.pure",
                            "function datamarts::dtm::domain::testFn1():Any[*]\n" +
                            "{\n" +
                            "  #{datamarts::datamt::domain::A{name}}#\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not accessible in datamarts::dtm::domain", "/datamart_dtm/testFile3.pure", 3, 5, 3, 32, 3, 32, e);
            this.runtime.delete("/datamart_dtm/testFile3.pure");
        }

        this.compileTestSource(
                "/datamart_datamt/testFile3.pure",
                        "function datamarts::datamt::domain::testFn1():Any[*]\n" +
                        "{\n" +
                        "  #{datamarts::datamt::domain::A{name}}#\n" +
                        "}\n"
        );
    }
}
