// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements.relation;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRelationType extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleColumnWithTypeSuccess()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::Relation<(name:Number[0..1])>[1]\n" +
                        "{\n" +
                        "   @meta::pure::metamodel::relation::Relation<(name:Integer[1])>;\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeFailing()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::Relation<(name:Integer[1])>[1]\n" +
                        "{\n" +
                        "   @meta::pure::metamodel::relation::Relation<(name:Number[1])>;\n" +
                        "}")
        );

        assertPureException(
                PureCompilationException.class,
                "Return type error in function 'test'; found: meta::pure::metamodel::relation::Relation<(name:Number[1])>; expected: meta::pure::metamodel::relation::Relation<(name:Integer[1])>",
                e);
    }

    @Test
    public void testSimpleColumnWithMultiplicityFailing()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::Relation<(name:Number[1])>[1]\n" +
                        "{\n" +
                        "   @meta::pure::metamodel::relation::Relation<(name:Integer[0..1])>;\n" +
                        "}")
        );

        assertPureException(
                PureCompilationException.class,
                "Return type error in function 'test'; found: meta::pure::metamodel::relation::Relation<(name:Integer)>; expected: meta::pure::metamodel::relation::Relation<(name:Number[1])>",
                e);
    }

    @Test
    public void testMerge()
    {
        compileTestSource("fromString.pure",
                "Primitive Varchar(x:Integer[1]) extends String\n" +
                        "[\n" +
                        "  $this->length() <= $x\n" +
                        "]" +
                        "function func1():meta::pure::metamodel::relation::Relation<(num:Number[1], other:Varchar(222))>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}" +
                        "function func2():meta::pure::metamodel::relation::Relation<(num:Number[1], other:Varchar(222))>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}");
        FunctionType fType1 = (FunctionType) Function.computeFunctionType(runtime.getFunction("func1__Relation_$0_1$_"), runtime.getProcessorSupport());
        FunctionType fType2 = (FunctionType) Function.computeFunctionType(runtime.getFunction("func2__Relation_$0_1$_"), runtime.getProcessorSupport());
        Assert.assertEquals("(num:Number[1], other:Varchar(222))", GenericType.print(_RelationType.merge(fType1._returnType()._typeArguments().getFirst(), fType2._returnType()._typeArguments().getFirst(), true, runtime.getProcessorSupport()), processorSupport));
    }

    @Test
    public void testNonExistentColumnTypeInParameter()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "\n" +
                        "function test::testFn(x:Relation<(c1:test::FakeInt)>[1]):Any[1]\n" +
                        "{\n" +
                        "  $x\n" +
                        "}\n"));
        assertPureException(
                PureCompilationException.class,
                "test::FakeInt has not been defined!", "fromString.pure", 3, 44, 3, 44, 3, 50, e);
    }

    @Test
    public void testNonExistentColumnTypeInReturnType()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "\n" +
                        "function test::testFn():Relation<(c1:test::FakeInt)>[*]\n" +
                        "{\n" +
                        "  []\n" +
                        "}\n"));
        assertPureException(
                PureCompilationException.class,
                "test::FakeInt has not been defined!", "fromString.pure", 3, 44, 3, 44, 3, 50, e);
    }

    @Test
    public void testNonExistentColumnTypeInCast()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "\n" +
                        "function test::testFn(x:Any[1]):Any[1]\n" +
                        "{\n" +
                        "  $x->cast(@Relation<(c1:test::FakeInt)>)\n" +
                        "}\n"));
        assertPureException(
                PureCompilationException.class,
                "test::FakeInt has not been defined!", "fromString.pure", 5, 32, 5, 32, 5, 38, e);
    }
}
