// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestPathToElement extends AbstractPureTestWithCoreCompiled
{
    protected static final String TEST_SOURCE_ID = "/test/testFile.pure";
    protected static final String TEST_SOURCE_CODE = "Class test::model::classes::MyClass\n" +
            "{\n" +
            "}\n" +
            "\n" +
            "Enum test::model::enums::MyEnumeration\n" +
            "{\n" +
            "    VAL\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongClassName():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test::model::classes::MyClass', '::'));\n" +
            "  pathToElement('test::model::classes::YourClass', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongEnumName():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::enums::MyEnumeration, pathToElement('test::model::enums::MyEnumeration', '::'));\n" +
            "  pathToElement('test::model::enums::YourEnumeration', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongFunctionName():Any[*]\n" +
            "{\n" +
            "  assertIs(test::functions::testWrongFunctionName__Any_MANY_, pathToElement('test::functions::testWrongFunctionName__Any_MANY_', '::'));\n" +
            "  pathToElement('test::functions::testRightFunctionName__Any_MANY_', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongPackage1():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test::model::classes::MyClass', '::'));\n" +
            "  pathToElement('test::model::class::MyClass', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongPackage2():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test::model::classes::MyClass', '::'));\n" +
            "  pathToElement('test::models::classes::MyClass', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongPackage3():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test::model::classes::MyClass', '::'));\n" +
            "  pathToElement('tests::model::classes::MyClass', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongClassName_NonStandardSeparator():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test.model.classes.MyClass', '.'));\n" +
            "  pathToElement('test.model.classes.YourClass', '.');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testWrongPackage_NonStandardSeparator():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test.model.classes.MyClass', '.'));\n" +
            "  pathToElement('test.model.class.MyClass', '.');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testInvalidTopLevel():Any[*]\n" +
            "{\n" +
            "  pathToElement('Stringy', '::');\n" +
            "}\n" +
            "\n" +
            "function test::functions::testSeparatorTypo():Any[*]\n" +
            "{\n" +
            "  assertIs(test::model::classes::MyClass, pathToElement('test.model.classes.MyClass', '.'));\n" +
            "  pathToElement('test.model.classes.MyClass', '_');\n" +
            "}\n";

    @Test
    public void testWrongClassName()
    {
        assertElementToPathException(
                "test::functions::testWrongClassName():Any[*]",
                "'test::model::classes::YourClass' is not a valid PackageableElement: could not find 'YourClass' in test::model::classes",
                13, 3);
    }

    @Test
    public void testWrongEnumName()
    {
        assertElementToPathException(
                "test::functions::testWrongEnumName():Any[*]",
                "'test::model::enums::YourEnumeration' is not a valid PackageableElement: could not find 'YourEnumeration' in test::model::enums",
                19, 3);
    }

    @Test
    public void testWrongFunctionName()
    {
        assertElementToPathException(
                "test::functions::testWrongFunctionName():Any[*]",
                "'test::functions::testRightFunctionName__Any_MANY_' is not a valid PackageableElement: could not find 'testRightFunctionName__Any_MANY_' in test::functions",
                25, 3);
    }

    @Test
    public void testWrongPackage1()
    {
        assertElementToPathException(
                "test::functions::testWrongPackage1():Any[*]",
                "'test::model::class::MyClass' is not a valid PackageableElement: could not find 'class' in test::model",
                31, 3);
    }

    @Test
    public void testWrongPackage2()
    {
        assertElementToPathException(
                "test::functions::testWrongPackage2():Any[*]",
                "'test::models::classes::MyClass' is not a valid PackageableElement: could not find 'models' in test",
                37, 3);
    }

    @Test
    public void testWrongPackage3()
    {
        assertElementToPathException(
                "test::functions::testWrongPackage3():Any[*]",
                "'tests::model::classes::MyClass' is not a valid PackageableElement: could not find 'tests' in Root",
                43, 3);
    }

    @Test
    public void testWrongClassName_NonStandardSeparator()
    {
        assertElementToPathException(
                "test::functions::testWrongClassName_NonStandardSeparator():Any[*]",
                "'test.model.classes.YourClass' is not a valid PackageableElement: could not find 'YourClass' in test.model.classes",
                49, 3);
    }

    @Test
    public void testWrongPackage_NonStandardSeparator()
    {
        assertElementToPathException(
                "test::functions::testWrongPackage_NonStandardSeparator():Any[*]",
                "'test.model.class.MyClass' is not a valid PackageableElement: could not find 'class' in test.model",
                55, 3);
    }

    @Test
    public void testInvalidTopLevel()
    {
        assertElementToPathException(
                "test::functions::testInvalidTopLevel():Any[*]",
                "'Stringy' is not a valid PackageableElement",
                60, 3);
    }

    @Test
    public void testSeparatorTypo()
    {
        assertElementToPathException(
                "test::functions::testSeparatorTypo():Any[*]",
                "'test.model.classes.MyClass' is not a valid PackageableElement",
                66, 3);
    }

    private void assertElementToPathException(String function, String expectedMessage, int line, int column)
    {
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute(function));
        assertPureException(PureExecutionException.class, expectedMessage, TEST_SOURCE_ID, line, column, e);
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(TEST_SOURCE_ID, TEST_SOURCE_CODE);
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(), GenericCodeRepository.build("test", "test(::.*)?", "platform"))));
    }
}
