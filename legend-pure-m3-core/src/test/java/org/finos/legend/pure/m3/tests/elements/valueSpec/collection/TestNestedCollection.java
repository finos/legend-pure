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

package org.finos.legend.pure.m3.tests.elements.valueSpec.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNestedCollection extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testCollectionWithNoNesting()
    {
        compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let a = ['a', 'b', 'c', 'd'];\n" +
                        "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithDirectNesting()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let a = ['e', 'f', ['g', 'h']];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 2", "fromString.pure", 3, 24, e);
    }

    @Test
    public void testCollectionWithIllusoryNesting()
    {
        compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let a = ['i', ['j']];\n" +
                        "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithNonCollectionVariableExpression()
    {
        compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let x = 'k';" +
                        "    let a = [$x, 'l'];\n" +
                        "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithCollectionVariableExpression()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let x = ['m', 'n'];\n" +
                        "    let a = [$x, 'o', 'p'];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 2", "fromString.pure", 4, 15, 4, 15, 4, 15, e);
    }

    @Test
    public void testCollectionWithCollectionVariableExpressionFromFunction()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let x = 'm, n'->split(', ');\n" +
                        "    let a = [$x, 'o', 'p'];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: *", "fromString.pure", 4, 15, 4, 15, 4, 15, e);
    }

    @Test
    public void testCollectionWithNonCollectionFunctionExpression()
    {
        compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let a = [trim('q'), 'r'];\n" +
                        "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithCollectionFunctionExpression()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let a = [split('s', ', '), 't', 'u'];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: *", "fromString.pure", 3, 14, e);
    }

    @Test
    public void testCollectionWithZeroOneExpression()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test(x:String[0..1]):String[*]\n" +
                        "{\n" +
                        "    ['a', $x, 'c'];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 12, e);
    }

    @Test
    public void testCollectionWithZeroOneWrappedInCollection()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():String[*]\n" +
                        "{\n" +
                        "    let a = [[['a', 'b']->first()], 't', 'u'];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 14, e);
    }


    @Test
    public void testCollectionWithZeroOneExpressionReturnedFromFunction()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test(x:String[0..1]):String[*]\n" +
                        "{\n" +
                        "    ['a', doSomething(), 'c'];\n" +
                        "}" +
                        "function doSomething():String[0..1]\n" +
                        "{\n" +
                        "    [];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 11, e);
    }

    @Test
    public void testCollectionWithZeroOneExpressionReturnedFromFunctionWithLet()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test(x:String[0..1]):String[*]\n" +
                        "{\n" +
                        "    let a = [doSomething()];\n" +
                        "    ['a', $a];\n" +
                        "}" +
                        "function doSomething():String[0..1]\n" +
                        "{\n" +
                        "    [];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 4, 12, e);
    }

    @Test
    public void testCollectionInClassConstraint()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "Class TestClass\n" +
                        "[\n" +
                        "  nonEmpty: length($this.name + $this.optionalName) > 0\n" +
                        "]\n" +
                        "{\n" +
                        "    name : String[1];\n" +
                        "    optionalName : String[0..1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 39, e);
    }

    @Test
    public void testCollectionInFunctionPreConstraint()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function testFn(name:String[1], optionalName:String[0..1]):String[1]\n" +
                        "[\n" +
                        "  nonEmpty: length($name + $optionalName) > 0\n" +
                        "]\n" +
                        "{\n" +
                        "    $name + if($optionalName->isEmpty(), |'', |' ' + $optionalName->toOne());\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 29, e);
    }

    @Test
    public void testCollectionInFunctionPostConstraint()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function testFn(name:String[1], optionalName:String[0..1]):String[1]\n" +
                        "[\n" +
                        "  nonEmpty: size([$return, $optionalName]) > 0\n" +
                        "]\n" +
                        "{\n" +
                        "    $name + if($optionalName->isEmpty(), |'', |' ' + $optionalName->toOne());\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 29, e);
    }

    @Test
    public void testCollectionInConstraintMessageFn()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "Class TestClass\n" +
                        "[\n" +
                        "  nonEmpty\n" +
                        "  (\n" +
                        "      ~function : ($this.name->length() > 0) || (!$this.optionalName->isEmpty() && ($this.optionalName->toOne()->length() > 0))\n" +
                        "      ~message  : 'name (' + $this.name + ') or optionalName (' + $this.optionalName + ') must be non-empty'\n" +
                        "  )\n" +
                        "]\n" +
                        "{\n" +
                        "    name : String[1];\n" +
                        "    optionalName : String[0..1];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 6, 73, e);
    }
}
