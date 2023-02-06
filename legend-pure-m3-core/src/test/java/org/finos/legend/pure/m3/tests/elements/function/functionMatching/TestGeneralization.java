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
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGeneralization extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testMultipleGeneralizationFunctionMatchingError()
    {
        try
        {
            compileTestSource("Class A\n" +
                    "{\n" +
                    "   propA:String[1];\n" +
                    "}\n" +
                    "Class C\n" +
                    "{\n" +
                    "   propC:String[1];\n" +
                    "}\n" +
                    "Class B extends A,C\n" +
                    "{\n" +
                    "   propB:String[1];\n" +
                    "}\n" +
                    "Class D\n" +
                    "{\n" +
                    "}\n" +
                    "function simpleTest():String[1]\n" +
                    "{\n" +
                    "   let b = ^B(propA='iA',propB='iB',propC='iC');\n" +
                    "   callWithA($b);\n" +
                    "   callWithC($b);\n" +
                    "   callWithD($b);\n" +
                    "}\n" +
                    "function callWithA(a:A[1]):String[1]\n" +
                    "{\n" +
                    "   $a.propA;\n" +
                    "}\n" +
                    "function callWithC(c:C[1]):String[1]\n" +
                    "{\n" +
                    "   $c.propC;\n" +
                    "}\n" +
                    "function callWithD(d:D[1]):String[1]\n" +
                    "{\n" +
                    "   'D';\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "callWithD(_:B[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tcallWithD(D[1]):String[1]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, 21, 4, e);
        }
    }

}
