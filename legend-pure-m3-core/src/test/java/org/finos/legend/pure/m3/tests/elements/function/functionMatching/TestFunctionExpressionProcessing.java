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

package org.finos.legend.pure.m3.tests.elements.function.functionMatching;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionExpressionProcessing extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("sample.pure");
    }

    @Test
    public void testFunctionExpressionProcessing1()
    {
        try
        {
            this.runtime.createInMemorySource(
                    "sample.pure",
                            "function go():Any[*]\n" +
                            "{\n" +
                            "   helloX('1', '2')->println();\n" +
                            "}\n" +
                            "\n" +
                            "function a::helloX(a:Integer[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::d::helloX(a:String[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "###Pure\n" +
                            "function goX():Any[*]\n" +
                            "{\n" +
                            "   'goX'->print(1)\n" +
                            "}\n"
            );
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "helloX(_:String[1],_:String[1])\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\ta::helloX(Integer[1], String[1]):String[*]\n" +
                    "\tb::c::helloX(String[1], Integer[1]):String[*]\n" +
                    "\tb::d::helloX(String[1], String[1]):String[*]\n", "sample.pure", 3, 4, e);
        }
    }

    @Test
    public void testFunctionExpressionProcessing2()
    {
        try
        {
            this.runtime.createInMemorySource(
                    "sample.pure",
                    "function go():Any[*]\n" +
                            "{\n" +
                            "   helloX('1', '2')->print(1);\n" +
                            "}\n" +
                            "\n" +
                            "function helloX(a:Integer[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function a::helloX(a:Integer[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::d::helloX(a:String[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "###Pure\n" +
                            "function goX():Any[*]\n" +
                            "{\n" +
                            "   'goX'->print(1)\n" +
                            "}\n"
            );
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "helloX(_:String[1],_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\thelloX(Integer[1], Integer[1]):String[*]\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\ta::helloX(Integer[1], String[1]):String[*]\n" +
                    "\tb::c::helloX(String[1], Integer[1]):String[*]\n" +
                    "\tb::d::helloX(String[1], String[1]):String[*]\n", "sample.pure", 3, 4, e);
        }
    }

    @Test
    public void testFunctionExpressionProcessing3()
    {
        try
        {
            this.runtime.createInMemorySource(
                    "sample.pure",
                    "import b::c::*;\n" +
                            "function go():Any[*]\n" +
                            "{\n" +
                            "   helloX('1', '2')->print(1);\n" +
                            "}\n" +
                            "\n" +
                            "function helloX(a:Integer[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function a::helloX(a:Integer[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::d::helloX(a:String[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "###Pure\n" +
                            "function goX():Any[*]\n" +
                            "{\n" +
                            "   'goX'->print(1)\n" +
                            "}\n"
            );
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "helloX(_:String[1],_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tb::c::helloX(String[1], Integer[1]):String[*]\n" +
                    "\thelloX(Integer[1], Integer[1]):String[*]\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\ta::helloX(Integer[1], String[1]):String[*]\n" +
                    "\tb::d::helloX(String[1], String[1]):String[*]\n", "sample.pure", 4, 4, e);
        }
    }

    @Test
    public void testFunctionExpressionProcessing4()
    {
        try
        {
            this.runtime.createInMemorySource(
                    "sample.pure",
                    "import a::*;\n" +
                            "import b::c::*;\n" +
                            "function go():Any[*]\n" +
                            "{\n" +
                            "   helloX('1', '2')->print(1);\n" +
                            "}\n" +
                            "\n" +
                            "function helloX(a:Integer[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function a::helloX(a:Integer[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::d::helloX(a:String[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "###Pure\n" +
                            "function goX():Any[*]\n" +
                            "{\n" +
                            "   'goX'->print(1)\n" +
                            "}\n"
            );
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "helloX(_:String[1],_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\ta::helloX(Integer[1], String[1]):String[*]\n" +
                    "\tb::c::helloX(String[1], Integer[1]):String[*]\n" +
                    "\thelloX(Integer[1], Integer[1]):String[*]\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\tb::d::helloX(String[1], String[1]):String[*]\n", "sample.pure", 5, 4, e);
        }
    }

    @Test
    public void testFunctionExpressionProcessing5()
    {
        try
        {
            this.runtime.createInMemorySource(
                    "sample.pure",
                    "import a::*;\n" +
                            "import b::c::*;\n" +
                            "import b::d::*;\n" +
                            "function go():Any[*]\n" +
                            "{\n" +
                            "   helloX('1')->print(1);\n" +
                            "}\n" +
                            "\n" +
                            "function a::helloX(a:Integer[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::d::helloX(a:String[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "###Pure\n" +
                            "function goX():Any[*]\n" +
                            "{\n" +
                            "   'goX'->print(1)\n" +
                            "}\n"
            );
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "helloX(_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\ta::helloX(Integer[1], String[1]):String[*]\n" +
                    "\tb::c::helloX(String[1], Integer[1]):String[*]\n" +
                    "\tb::d::helloX(String[1], String[1]):String[*]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "sample.pure", 6, 4, e);
        }
    }

    @Test
    public void testFunctionExpressionProcessing6()
    {
        try
        {
            this.compileTestSource(
                    "sample.pure",
                    "###Pure\n" +
                            "function goX():Any[*]\n" +
                            "{\n" +
                            "   'goX'->print(1)\n" +
                            "}\n" +
                            "\n" +
                            "###Pure\n" +
                            "import b::c::*;\n" +
                            "function go():Any[*]\n" +
                            "{\n" +
                            "   helloX('1')->print(1);\n" +
                            "}\n" +
                            "\n" +
                            "function a::helloX(a:Integer[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:Integer[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}\n" +
                            "\n" +
                            "function b::c::helloX(a:String[1], b:String[1]):String[*]\n" +
                            "{\n" +
                            "   'hello_str_str';\n" +
                            "}"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "helloX(_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tb::c::helloX(String[1], Integer[1]):String[*]\n" +
                    "\tb::c::helloX(String[1], String[1]):String[*]\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\ta::helloX(Integer[1], String[1]):String[*]\n", "sample.pure", 11, 4, e);
        }
    }
}
