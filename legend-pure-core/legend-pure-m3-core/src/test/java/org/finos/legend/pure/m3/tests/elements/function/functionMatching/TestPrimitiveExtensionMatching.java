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

package org.finos.legend.pure.m3.tests.elements.function.functionMatching;

import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPrimitiveExtensionMatching extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testFunc.pure");
    }


    @Test
    public void testPrimitiveExtensionMatch()
    {
        compileTestSourceM3("testFunc.pure",
                "Primitive I(x:Integer[1]) extends Integer\n" +
                        "function test(var:I(3)[1]):Nil[0]\n" +
                        "{\n" +
                        "   []\n" +
                        "}\n" +
                        "function testMany():Nil[0]\n" +
                        "{\n" +
                        "    test(1->cast(@I(3)));\n" +
                        "}");
    }


    @Test
    public void testPrimitiveExtensionMismatch()
    {
        try
        {
            compileTestSourceM3("testFunc.pure",
                    "Primitive I(x:Integer[1]) extends Integer\n" +
                            "function test(var:I(3)[1]):Nil[0]\n" +
                            "{\n" +
                            "   []\n" +
                            "}\n" +
                            "function testMany():Nil[0]\n" +
                            "{\n" +
                            "    test(1->cast(@I(2)));\n" +
                            "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "test(_:I(2)[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\ttest(I(3)[1]):Nil[0]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "testFunc.pure", 8, 5, 8, 5, 8, 8, e);
        }
    }
}
