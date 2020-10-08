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

package org.finos.legend.pure.m3.tests.literal;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public class TestNestedCollection extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testCollectionWithNoNesting()
    {
        compileTestSource("fromString.pure", "function test():String[*]\n" +
                                                   "{\n" +
                                                   "    let a = ['a', 'b', 'c', 'd'];\n" +
                                                   "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithDirectNesting()
    {
        try
        {
            compileTestSource("fromString.pure", "function test():String[*]\n" +
                    "{\n" +
                    "    let a = ['e', 'f', ['g', 'h']];\n" +
                    "}\n");
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 2", "fromString.pure", 3, 24, e);
        }
    }

    @Test
    public void testCollectionWithIllusoryNesting()
    {
        compileTestSource("fromString.pure", "function test():String[*]\n" +
                                                   "{\n" +
                                                   "    let a = ['i', ['j']];\n" +
                                                   "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithNonCollectionVariableExpression()
    {
        compileTestSource("fromString.pure", "function test():String[*]\n" +
                                                   "{\n" +
                                                   "    let x = 'k';" +
                                                   "    let a = [$x, 'l'];\n" +
                                                   "}\n");
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithCollectionVariableExpression()
    {
        try
        {
            compileTestSource("fromString.pure", "function test():String[*]\n" +
                    "{\n" +
                    "    let x = ['m', 'n'];\n" +
                    "    let a = [$x, 'o', 'p'];\n" +
                    "}\n");
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 2", "fromString.pure", 4, 15, 4, 15, 4, 15, e);
        }
    }

    @Test
    public void testCollectionWithCollectionVariableExpressionFromFunction()
    {
        try
        {
            runtime.createInMemorySource("fromString.pure", "function test():String[*]\n" +
                    "{\n" +
                    "    let x = 'm, n'->split(', ');\n" +
                    "    let a = [$x, 'o', 'p'];\n" +
                    "}\n");
            runtime.compile();
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: *", "fromString.pure", 4, 15, 4, 15, 4, 15, e);
        }
    }

    @Test
    public void testCollectionWithNonCollectionFunctionExpression()
    {
        runtime.createInMemorySource("fromString.pure", "function test():String[*]\n" +
                                                   "{\n" +
                                                   "    let a = [trim('q'), 'r'];\n" +
                                                   "}\n");
        runtime.compile();
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithCollectionFunctionExpression()
    {
        try
        {
            runtime.createInMemorySource("fromString.pure", "function test():String[*]\n" +
                    "{\n" +
                    "    let a = [split('s', ', '), 't', 'u'];\n" +
                    "}\n");
            runtime.compile();
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: *", "fromString.pure", 3, 14, e);
        }
    }

    @Test
    public void testCollectionWithZeroOneExpression()
    {
        try
        {
            compileTestSource("fromString.pure", "function test(x:String[0..1]):String[*]\n" +
                    "{\n" +
                    "    ['a', $x, 'c'];\n" +
                    "}");
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 12, e);
        }
    }

    @Test
    public void testCollectionWithZeroOneWrappedInCollection()
    {
        try
        {
            compileTestSource("fromString.pure", "function test():String[*]\n" +
                    "{\n" +
                    "    let a = [[['a', 'b']->first()], 't', 'u'];\n" +
                    "}\n");
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 14, e);
        }
    }


    @Test
    public void testCollectionWithZeroOneExpressionReturnedFromFunction()
    {
        try
        {
            compileTestSource("fromString.pure", "function test(x:String[0..1]):String[*]\n" +
                    "{\n" +
                    "    ['a', doSomething(), 'c'];\n" +
                    "}" +
                    "function doSomething():String[0..1]\n" +
                                        "{\n" +
                                        "    [];\n" +
                                        "}");
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 11, e);
        }
    }

    @Test
    public void testCollectionWithZeroOneExpressionReturnedFromFunctionWithLet()
    {
        try
        {
            compileTestSource("fromString.pure", "function test(x:String[0..1]):String[*]\n" +
                    "{\n" +
                    "    let a = [doSomething()];\n" +
                    "    ['a', $a];\n" +
                    "}" +
                    "function doSomething():String[0..1]\n" +
                                        "{\n" +
                                        "    [];\n" +
                                        "}");
            Assert.fail("Expected a compilation exception, got none");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 4, 12, e);
        }
    }
}
