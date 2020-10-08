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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class TestAllowedPackages extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testNoModelPackageInPlatform()
    {
        // This should compile
        compileTestSource("/platform/testSource1.pure",
                "Class meta::pure::MyTestClass\n" +
                        "{\n" +
                        "}\n");

        // This should not compile
        try
        {
            compileTestSource("/platform/testSource2.pure",
                    "Class model::test::MyTestClass\n" +
                            "{\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Package model::test is not allowed in platform; only packages matching ((meta)|(system)|(apps::pure))(::.*)? are allowed", "/platform/testSource2.pure", 1, 1, 1, 20, 3, 1, e);
        }
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
        try
        {
            compileTestSource("/test/testSource2.pure",
                    "Class meta::pure::MyTestClass\n" +
                            "{\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Package meta::pure is not allowed in test; only packages matching test(::*)? are allowed", "/test/testSource2.pure", 1, 1, 1, 19, 3, 1, e);
        }
    }
    @Test
    public void testPackagePatternOfModelInModelValidationRepo()
    {
        try
        {
            compileTestSource(
                    "/model_validation/testFile2.pure",
                            "function model::somepk::producers::bu::validationFunc():Boolean[1]\n" +
                            "{\n" +
                            "    true;\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Package model::somepk::producers::bu is not allowed in model_validation; only packages matching (model::producers)(::.*)? are allowed", "/model_validation/testFile2.pure", 1, 1, 1, 40, 4, 1, e);
        }
    }

    @Override
    protected MutableCodeStorage getCodeStorage()
    {
        return new PureCodeStorage(getCodeStorageRoot(), new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository(), new TestCodeRepository("test", Pattern.compile("test(::*)?")), SVNCodeRepository.newModelValidationCodeRepository()));
    }
}
