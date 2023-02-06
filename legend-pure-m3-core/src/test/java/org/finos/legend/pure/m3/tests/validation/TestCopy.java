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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCopy extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testModel.pure");
        runtime.delete("testFunc.pure");
    }

    @Test
    public void testIncompatiblePrimitiveTypes()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(prop='the quick brown fox');\n" +
                        "    ^$a(prop=1);" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: Integer not a subtype of String", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleClasses()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(prop=^B());\n" +
                        "    ^$a(prop=^C());\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: C not a subtype of B", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleMixedTypes1()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(prop=^B());\n" +
                        "    ^$a(prop='the quick brown fox');\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: String not a subtype of B", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleMixedTypes2()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(prop='the quick brown fox');\n" +
                        "    ^$a(prop=^B());\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: B not a subtype of String", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleInstanceValueMultiplicity()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(prop='one string');\n" +
                        "    ^$a(prop=['one string', 'two string', 'red string', 'blue string']);\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [4] is not compatible with [1]", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleExpressionMultiplicity()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function someStrings():String[*]\n" +
                        "{\n" +
                        "    ['one string', 'two string', 'red string', 'blue string'];\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(prop='one string');\n" +
                        "    ^$a(prop=someStrings());\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [*] is not compatible with [1]", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleExpressionMultiplicity_Deep()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    toB:B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function someStrings():String[*]\n" +
                        "{\n" +
                        "    ['one string', 'two string', 'red string', 'blue string'];\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    let a = ^A(toB=^B(prop='one string'));\n" +
                        "    ^$a(toB.prop=someStrings());\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [*] is not compatible with [1]", "testFunc.pure", 4, 17, 4, 17, 4, 17, e);
    }
}
