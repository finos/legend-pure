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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNamespaces extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testClassNameConflict()
    {
        compileTestSource("class1.pure",
                "Class test::MyClass {}");
        CoreInstance myClass = this.runtime.getCoreInstance("test::MyClass");
        Assert.assertNotNull(myClass);
        Assert.assertTrue(Instance.instanceOf(myClass, M3Paths.Class, this.processorSupport));

        try
        {
            compileTestSource("class2.pure",
                    "Class test::MyClass {}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyClass' already exists in the package 'test'", "class2.pure", 1, 13, 1, 13, 1, 19, e);
        }
    }

    @Test
    public void testEnumerationNameConflict()
    {
        compileTestSource("enum1.pure",
                "Enum test::MyEnum {VALUE}");
        CoreInstance myEnum = this.runtime.getCoreInstance("test::MyEnum");
        Assert.assertNotNull(myEnum);
        Assert.assertTrue(Instance.instanceOf(myEnum, M3Paths.Enumeration, this.processorSupport));

        try
        {
            compileTestSource("enum2.pure",
                    "Enum test::MyEnum {VALUE}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyEnum' already exists in the package 'test'", "enum2.pure", 1, 12, 1, 12, 1, 17, e);
        }
    }

    @Test
    public void testAssociationNameConflict()
    {
        compileTestSource("assoc1.pure",
                "Class test::TestClass {}\n" +
                        "Association test::MyAssociation" +
                        "{\n" +
                        "  prop1 : test::TestClass[*];\n" +
                        "  prop2 : test::TestClass[*];\n" +
                        "}");
        CoreInstance myAssoc = this.runtime.getCoreInstance("test::MyAssociation");
        Assert.assertNotNull(myAssoc);
        Assert.assertTrue(Instance.instanceOf(myAssoc, M3Paths.Association, this.processorSupport));

        try
        {
            compileTestSource("assoc2.pure",
                    "Association test::MyAssociation" +
                            "{\n" +
                            "  prop1 : test::TestClass[*];\n" +
                            "  prop2 : test::TestClass[*];\n" +
                            "}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyAssociation' already exists in the package 'test'", "assoc2.pure", 1, 19, 1, 19, 1, 31, e);
        }
    }




}
