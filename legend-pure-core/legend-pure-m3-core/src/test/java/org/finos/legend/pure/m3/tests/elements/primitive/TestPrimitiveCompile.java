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

package org.finos.legend.pure.m3.tests.elements.primitive;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPrimitiveCompile extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPrimitive()
    {
        compileTestSource("fromString.pure",
                "Primitive test::Int8 extends Integer");
        Assert.assertEquals("Int8", ((PrimitiveType) runtime.getCoreInstance("test::Int8"))._name());
    }

    @Test
    public void testPrimitiveFunctionMatching()
    {
        assertCompileError("Primitive test::Int8 extends Integer\n" +
                        "native function x(p:test::Int8[1]):Any[1];\n" +
                        "function test():Any[1]\n" +
                        "{\n" +
                        "   x(1);\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:5 column:4), \"The system can't find a match for the function: x(_:Integer[1])\n" +
                        "\n" +
                        "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                        "\tx(Int8[1]):Any[1]\n" +
                        "\n" +
                        "No functions, in packages not imported, match the function name.\n" +
                        "\"");
    }

    @Test
    public void testPrimitiveWithConstraints()
    {
        compileTestSource("fromString.pure",
                "Primitive test::Int8 extends Integer" +
                        "[" +
                        " $this < 255" +
                        "]");
        Assert.assertEquals("Int8", ((PrimitiveType) runtime.getCoreInstance("test::Int8"))._name());
    }

    @Test
    public void testPrimitiveWithVariable()
    {
        assertCompileError(
                "Primitive test::IntCap(x:Integer[1], z:String[1]) extends Integer\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "\n" +
                        "function test::x():test::IntCap(1)[1]\n" +
                        "{\n" +
                        " 2->cast(@test::IntCap(1));\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:6 column:26), \"Type variable mismatch for test::IntCap(x:Integer,z:String) (expected 2, got 1): test::IntCap(1)\"");
    }

    @Test
    public void testPrimitiveWrongVariableType()
    {
        assertCompileError(
                "Primitive test::IntCap(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "\n" +
                        "function test::x():test::IntCap('w')[0..1]\n" +
                        "{\n" +
                        " [];\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:6 column:26), \"Type variable type mismatch for test::IntCap(x:Integer) (expected Integer, got String): test::IntCap('w')\"");
    }

    @Test
    public void testPrimitiveVariableTypeInheritance()
    {
        compileTestSource("fromString.pure",
                "Primitive test::IntCap(x:Number[1]) extends Integer\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "\n" +
                        "function test::x():test::IntCap(1)[0..1]\n" +
                        "{\n" +
                        " [];\n" +
                        "}");
    }

    @Test
    public void testPrimitiveWithVariableExtend()
    {
        compileTestSource("fromString.pure",
                "Primitive test::IntCap(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "Primitive test::Int8 extends test::IntCap(255)");
        Assert.assertEquals("IntCap", ((PrimitiveType) runtime.getCoreInstance("test::IntCap"))._name());
        Assert.assertEquals("Int8", ((PrimitiveType) runtime.getCoreInstance("test::Int8"))._name());
    }

    @Test
    public void testPrimitiveComplexConstraint()
    {
        compileTestSource("fromString.pure",
                "Primitive test::IntCap(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        " id(~function:$this < $x)\n" +
                        "]\n" +
                        "Primitive test::Int8 extends test::IntCap(255)");
        Assert.assertEquals("IntCap", ((PrimitiveType) runtime.getCoreInstance("test::IntCap"))._name());
        Assert.assertEquals("Int8", ((PrimitiveType) runtime.getCoreInstance("test::Int8"))._name());
    }

    @Test
    public void testPrimitiveWithParameterInClass()
    {
        compileTestSource("fromString.pure",
                "Primitive test::IntCap(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "Class x::A\n" +
                        "{\n" +
                        " v : test::IntCap(2)[1];\n" +
                        "}");
    }

    @Test
    public void testPrimitiveWithParameterInClassError()
    {
        assertCompileError(
                "Primitive test::IntCap(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "Class x::A\n" +
                        "{\n" +
                        " v : test::IntCap()[1];\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:7 column:12), \"Type variable mismatch for test::IntCap(x:Integer) (expected 1, got 0): test::IntCap\"");
    }

    @Test
    public void testWrongPrimitiveTypeError()
    {
        assertCompileError(
                "Primitive x::Decimal(x:Integer[1]) extends Decimal\n" +
                        "[\n" +
                        " $this < $x\n" +
                        "]\n" +
                        "Class x::A\n" +
                        "{\n" +
                        " v : Decimal(1)[1];\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:7 column:6), \"Type variable mismatch for Decimal (expected 0, got 1): Decimal(1)\"");
    }

    @Test
    public void testPrimitiveExtendsNonPrimitive()
    {
        assertCompileError(
                "Class x::NotPrimitive\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Primitive x::MyPrimitive(x:Integer[1]) extends x::NotPrimitive\n" +
                        "[\n" +
                        " $x < 10\n" +
                        "]",
                "Compilation error at (resource:fromString.pure line:6 column:51), \"Invalid generalization: x::MyPrimitive cannot extend x::NotPrimitive as it is not a PrimitiveType or Any\"");
    }

    @Test
    public void testPrimitiveNoExtends()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource("fromString.pure", "Primitive x::MyPrimitive(x:Integer[1])\n"));
        Assert.assertEquals("Parser error at (resource:fromString.pure line:2 column:1), expected: 'extends' found: '<EOF>'", e.getMessage());
    }

    @Test
    public void testTypeVariableNonExistentType()
    {
        assertCompileError(
                "Primitive test::IntCap(x:NonExistentType[1]) extends Integer\n",
                "Compilation error at (resource:fromString.pure line:1 column:26), \"NonExistentType has not been defined!\"");
    }

    @Test
    public void testTypeVariableConflict()
    {
        assertCompileError(
                "Primitive test::IntCap(x:Integer[1], x:Integer[1]) extends Integer\n",
                "Compilation error at (resource:fromString.pure line:1 column:38), \"Type variable 'x' is already defined (at fromString.pure:1c24)\"");
    }

    @Test
    public void testMissingTypeVariableInExtends()
    {
        assertCompileError(
                        "Primitive test::IntLessThan(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        "   $this < $x\n" +
                        "]\n" +
                        "\n" +
                        "Primitive test::IntBetween(y:Integer[1]) extends test::IntLessThan\n" +
                        "[\n" +
                        " $this > $y\n" +
                        "]",
                "Compilation error at (resource:fromString.pure line:6 column:56), \"Type variable mismatch for test::IntLessThan(x:Integer) (expected 1, got 0): test::IntLessThan\"");
    }

    @Test
    public void testVariableReferenceInSubtype()
    {
        // We should consider allowing this
        assertCompileError(
                "Primitive test::IntLessThan(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        "   $this < $x\n" +
                        "]\n" +
                        "\n" +
                        "Primitive test::IntBetweenYAndFive(y:Integer[1]) extends test::IntLessThan(5)\n" +
                        "[\n" +
                        " $y < $x,\n" +
                        " $this > $y\n" +
                        "]",
                "Compilation error at (resource:fromString.pure line:8 column:8), \"The variable 'x' is unknown!\"");
    }

    @Test
    public void testTypeVariableConflictWithSuperType()
    {
        // We should consider allowing this
        assertCompileError(
                "Primitive test::IntLessThan(x:Integer[1]) extends Integer\n" +
                        "[\n" +
                        "   $this < $x\n" +
                        "]\n" +
                        "\n" +
                        "Primitive test::IntBetweenYAndFive(x:Integer[1]) extends test::IntLessThan(5)",
                "Compilation error at (resource:fromString.pure line:6 column:36), \"Type variable 'x' is already defined in supertype test::IntLessThan at fromString.pure:1c29\"");
    }

    @Test
    public void testGeneralizationResolutionOrder()
    {
        compileTestSource("fromString.pure",
                        "Primitive test::IntBounded(l:Integer[1], u:Integer[1]) extends Integer\n" +
                        "[\n" +
                        "  $this >= $l," +
                        "  $this <= $u\n" +
                        "]\n" +
                        "\n" +
                        "Primitive test::OneToTen extends test::IntBounded(1, 10)\n");
        Class<?> any = (Class<?>) processorSupport.type_TopType();
        PrimitiveType number = (PrimitiveType) processorSupport.repository_getTopLevel("Number");
        PrimitiveType integer = (PrimitiveType) processorSupport.repository_getTopLevel("Integer");
        PrimitiveType bounded = (PrimitiveType) runtime.getCoreInstance("test::IntBounded");
        PrimitiveType oneToTen = (PrimitiveType) runtime.getCoreInstance("test::OneToTen");
        Assert.assertEquals(Lists.immutable.with(oneToTen, bounded, integer, number, any), Type.getGeneralizationResolutionOrder(oneToTen, processorSupport));
    }

    public static void assertCompileError(String code, String message)
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure", code));
        Assert.assertEquals(message, e.getMessage());
    }
}
