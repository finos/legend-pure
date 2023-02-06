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

package org.finos.legend.pure.m3.tests.validation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAllowedPackages extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME),
                GenericCodeRepository.build("model_validation", "(model::producers)(::.*)?", PlatformCodeRepository.NAME),
                GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system"));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/platform/testSource1.pure");
        runtime.delete("/platform/testSource2.pure");
        runtime.delete("/test/testSource1.pure");
        runtime.delete("/test/testSource2.pure");
        runtime.delete("/model_validation/testFile2.pure");
        runtime.compile();
    }

    @Test
    public void testNoModelPackageInPlatform()
    {
        // This should compile
        compileTestSource("/platform/testSource1.pure",
                "Class meta::pure::MyTestClass\n" +
                        "{\n" +
                        "}\n");

        // This should not compile
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/platform/testSource2.pure",
                "Class model::test::MyTestClass\n" +
                        "{\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Package model::test is not allowed in platform; only packages matching ((meta)|(system)|(apps::pure))(::.*)? are allowed", "/platform/testSource2.pure", 1, 1, 1, 20, 3, 1, e);
    }

    @Test
    public void testTestOnlyInTestRepo()
    {
        // This should compile
        compileTestSource("/test/testSource1.pure",
                "Class test::MyTestClass\n" +
                        "{\n" +
                        "}\n");

        // This should not compile
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource2.pure",
                "Class meta::pure::MyTestClass\n" +
                        "{\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Package meta::pure is not allowed in test; only packages matching test(::.*)? are allowed", "/test/testSource2.pure", 1, 1, 1, 19, 3, 1, e);
    }

    @Test
    public void testPackagePatternOfModelInModelValidationRepo()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/model_validation/testFile2.pure",
                "function model::somepk::producers::bu::validationFunc():Boolean[1]\n" +
                        "{\n" +
                        "    true;\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Package model::somepk::producers::bu is not allowed in model_validation; only packages matching (model::producers)(::.*)? are allowed", "/model_validation/testFile2.pure", 1, 1, 1, 40, 4, 1, e);
    }
}
