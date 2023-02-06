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

package org.finos.legend.pure.m3.tests.elements.function.inference;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReturnInference extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());

        //set observer
//        System.setProperty("pure.typeinference.test", "true");
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testReturnInferenceFail()
    {
        try
        {
            runtime.createInMemorySource("fromString.pure", "function test(s:String[1]):Any[*]\n" +
                    "{\n" +
                    "   let r = '10';\n" +
                    "   ex(w:String[1]|$w+$s+$r);\n" +
                    "}\n" +
                    "\n" +
                    "function ex(f:Function<Any>[1]):Any[*]\n" +
                    "{\n" +
                    "   print($f->eval('ee'));\n" +
                    "}\n" +
                    "\n" +
                    "function go():Any[*]\n" +
                    "{\n" +
                    "   print(test('www'),5);\n" +
                    "}");
            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "eval(_:Function<Any>[1],_:String[1])\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\tmeta::pure::functions::lang::eval(Function[1]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], S[n], T[o], U[p], W[q], X[r], Y[s], Z[t]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], T[n]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], T[n], U[p]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], T[n], U[p], W[q]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], T[n], U[p], W[q], X[r]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], T[n], U[p], W[q], X[r], Y[s]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function[1], T[n], U[p], W[q], X[r], Y[s], Z[t]):V[m]\n", "fromString.pure", 9, 14, e);
        }
    }
}
