// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Regression tests for the match expression double-execution bug.
 */
public abstract class AbstractTestMatchSideEffects extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testNestedMatchInnerLambdaExecutedOnce()
    {
        compileTestSource("fromString.pure",
                "function testNestedMatch():Integer[1]\n" +
                "{\n" +
                "    'hello'->match([\n" +
                "        s:String[1] | print('inner', 1); 42;\n" +
                "    ])->match([\n" +
                "        i:Integer[1] | print('outer', 1); $i;\n" +
                "    ])\n" +
                "}\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        functionExecution.getConsole().setPrintStream(new PrintStream(baos));
        functionExecution.getConsole().setConsole(true);
        execute("testNestedMatch():Integer[1]");
        String out = baos.toString();
        Assert.assertEquals("inner lambda must execute exactly once", 1, countOccurrences(out, "'inner'"));
        Assert.assertEquals("outer lambda must execute exactly once", 1, countOccurrences(out, "'outer'"));
    }

    @Test
    public void testTripleNestedMatchEachLambdaExecutedOnce()
    {
        compileTestSource("fromString.pure",
                "function testTripleMatch():Integer[1]\n" +
                "{\n" +
                "    'x'->match([\n" +
                "        s:String[1] | print('first', 1); 1;\n" +
                "    ])->match([\n" +
                "        i:Integer[1] | print('second', 1); $i + 10;\n" +
                "    ])->match([\n" +
                "        i:Integer[1] | print('third', 1); $i + 100;\n" +
                "    ])\n" +
                "}\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        functionExecution.getConsole().setPrintStream(new PrintStream(baos));
        functionExecution.getConsole().setConsole(true);
        execute("testTripleMatch():Integer[1]");
        String out = baos.toString();
        Assert.assertEquals("first lambda must execute exactly once", 1, countOccurrences(out, "'first'"));
        Assert.assertEquals("second lambda must execute exactly once", 1, countOccurrences(out, "'second'"));
        Assert.assertEquals("third lambda must execute exactly once", 1, countOccurrences(out, "'third'"));
    }

    @Test
    public void testMatchWithVariableLambdaExecutedOnce()
    {
        compileTestSource("fromString.pure",
                "function testDynamicMatch():Integer[1]\n" +
                "{\n" +
                "    let fns = [{i:Integer[1] | print('dynamic', 1); $i + 1;}];\n" +
                "    1->match($fns);\n" +
                "}\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        functionExecution.getConsole().setPrintStream(new PrintStream(baos));
        functionExecution.getConsole().setConsole(true);
        execute("testDynamicMatch():Integer[1]");
        String out = baos.toString();
        Assert.assertEquals("dynamic lambda must execute exactly once", 1, countOccurrences(out, "'dynamic'"));
    }

    private static int countOccurrences(String text, String sub)
    {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1)
        {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
