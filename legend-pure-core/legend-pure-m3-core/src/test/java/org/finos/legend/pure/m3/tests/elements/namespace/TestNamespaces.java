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

package org.finos.legend.pure.m3.tests.elements.namespace;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNamespaces extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("testModel1.pure");
        runtime.delete("testModel2.pure");
        runtime.compile();
    }

    @Test
    public void testClassNameConflict()
    {
        compileTestSource("testModel1.pure",
                "Class test::MyClass {}");
        CoreInstance myClass = runtime.getCoreInstance("test::MyClass");
        Assert.assertNotNull(myClass);
        Assert.assertTrue(Instance.instanceOf(myClass, M3Paths.Class, processorSupport));

        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testModel2.pure",
                "Class test::MyClass {}"));
        assertPureException(PureParserException.class, "The element 'MyClass' already exists in the package 'test'", "testModel2.pure", 1, 13, 1, 13, 1, 19, e);
    }

    @Test
    public void testEnumerationNameConflict()
    {
        compileTestSource("testModel1.pure",
                "Enum test::MyEnum {VALUE}");
        CoreInstance myEnum = runtime.getCoreInstance("test::MyEnum");
        Assert.assertNotNull(myEnum);
        Assert.assertTrue(Instance.instanceOf(myEnum, M3Paths.Enumeration, processorSupport));

        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testModel2.pure",
                "Enum test::MyEnum {VALUE}"));
        assertPureException(PureParserException.class, "The element 'MyEnum' already exists in the package 'test'", "testModel2.pure", 1, 12, 1, 12, 1, 17, e);
    }

    @Test
    public void testAssociationNameConflict()
    {
        compileTestSource("testModel1.pure",
                "Class test::TestClass {}\n" +
                        "Association test::MyAssociation" +
                        "{\n" +
                        "  prop1 : test::TestClass[*];\n" +
                        "  prop2 : test::TestClass[*];\n" +
                        "}");
        CoreInstance myAssoc = runtime.getCoreInstance("test::MyAssociation");
        Assert.assertNotNull(myAssoc);
        Assert.assertTrue(Instance.instanceOf(myAssoc, M3Paths.Association, processorSupport));

        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testModel2.pure",
                "Association test::MyAssociation" +
                        "{\n" +
                        "  prop1 : test::TestClass[*];\n" +
                        "  prop2 : test::TestClass[*];\n" +
                        "}"));
        assertPureException(PureParserException.class, "The element 'MyAssociation' already exists in the package 'test'", "testModel2.pure", 1, 19, 1, 19, 1, 31, e);
    }

    @Test
    public void testProfileNameConflict()
    {
        compileTestSource("testModel1.pure",
                "Profile test::MyProfile\n" +
                        "{\n" +
                        "  stereotypes: [st1, st2];\n" +
                        "  tags: [t1, t2, t3];\n" +
                        "}");
        CoreInstance myProfile = runtime.getCoreInstance("test::MyProfile");
        Assert.assertNotNull(myProfile);
        Assert.assertTrue(Instance.instanceOf(myProfile, M3Paths.Profile, processorSupport));

        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testModel2.pure",
                "Profile test::MyProfile\n" +
                        "{\n" +
                        "  stereotypes: [st1, st2];\n" +
                        "  tags: [t1, t2, t3];\n" +
                        "}"));
        assertPureException(PureParserException.class, "The element 'MyProfile' already exists in the package 'test'", "testModel2.pure", 1, 15, 1, 15, 1, 23, e);
    }
}
