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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class TestGeneralization extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testClassGeneralizationToEnum()
    {
        compileTestSource("Enum test::TestEnum {A, B, C}");
        try
        {
            compileTestSource("testSource.pure", "Class test::TestClass extends test::TestEnum {}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid generalization: test::TestClass cannot extend test::TestEnum as it is not a Class", "testSource.pure", 1, 1, 1, 13, 1, 47, e);
        }
    }

    @Test
    public void testEnumGeneralizationToEnum()
    {
        compileTestSource("Enum test::TestEnum {A, B, C}");
        try
        {
            compileTestSource("testSource.pure", "Enum test::TestEnum2 extends test::TestEnum {}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: '{' found: 'extends'", "testSource.pure", 1, 22, 1, 22, 1, 29, e);
        }
    }

    @Test
    public void testClassGeneralizationToPrimitiveType()
    {
        for (String typeName : ModelRepository.PRIMITIVE_TYPE_NAMES)
        {
            String sourceFile = String.format("test%s.pure", typeName);
            try
            {
                compileTestSource(sourceFile, String.format("Class test::TestClass extends %s {}", typeName));
                Assert.fail("Expected compilation error");
            }
            catch (Exception e)
            {
                String expectedMessage = String.format("Invalid generalization: test::TestClass cannot extend %s as it is not a Class", typeName);
                assertPureException(PureCompilationException.class, expectedMessage, sourceFile, 1, 1, 1, 13, 1, 33 + typeName.length(), e);
            }
        }
    }

    @Test
    public void testDiamondWithGenericIssue()
    {
        try
        {
            compileTestSource("Class A<T>{prop:T[1];}\n" +
                    "Class B extends A<String>{}\n" +
                    "Class C extends A<Integer>{}\n" +
                    "Class D extends B,C{}\n" +
                    "function simpleTest():D[1]\n" +
                    "{\n" +
                    "   ^D(prop=333);\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, Pattern.compile("^Diamond inheritance error! (('Integer' is not compatible with 'String')|('String' is not compatible with 'Integer')) going from 'D' to 'A<T>'$"), 7, 4, e);
        }
    }

    @Test
    public void testGeneralizationWithSelfReferenceInGenerics()
    {
        try
        {
            compileTestSource("/test/testModel.pure",
                    "import test::*;\n" +
                            "Class test::A<T> {}\n" +
                            "Class test::B extends A<B> {}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Class B extends A<B> which contains a reference to B itself", "/test/testModel.pure", 3, 13, e);
        }
    }

    @Test
    public void testGeneralizationWithNestedSelfReferenceInGenerics()
    {
        try
        {
            compileTestSource("/test/testModel.pure",
                    "import test::*;\n" +
                            "Class test::A<T> {}\n" +
                            "Class test::B<U> {}\n" +
                            "Class test::C extends A<B<C>> {}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Class C extends A<B<C>> which contains a reference to C itself", "/test/testModel.pure", 4, 13, e);
        }
    }

    @Test
    public void testGeneralizationWithSubtypeReferenceInGenerics()
    {
        try
        {
            compileTestSource("/test/testModel.pure",
                    "import test::*;\n" +
                            "Class test::A<T> {}\n" +
                            "Class test::B extends A<C> {}\n" +
                            "Class test::C extends B {}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Class B extends A<C> which contains a reference to C which is a subtype of B", "/test/testModel.pure", 3, 13, e);
        }
    }

    @Test
    public void testGeneralizationWithNestedSubtypeReferenceInGenerics()
    {
        try
        {
            compileTestSource("/test/testModel.pure",
                    "import test::*;\n" +
                            "Class test::A<T> {}\n" +
                            "Class test::B<U> {}\n" +
                            "Class test::C extends A<B<D>> {}\n" +
                            "Class test::D extends C {}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Class C extends A<B<D>> which contains a reference to D which is a subtype of C", "/test/testModel.pure", 4, 13, e);
        }
    }

}
