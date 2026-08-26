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

import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
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

    /**
     * Fix 2 — dynamic lambda list path (hits the ConcreteFunctionDefinition else-branch in Pure.evaluate).
     * Checks both side-effect count (print fires once) AND the return value — if the lambda ran twice
     * the accumulating +1 would produce 3 instead of 2.
     */
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
        CoreInstance result = execute("testDynamicMatch():Integer[1]");
        String out = baos.toString();
        Assert.assertEquals("dynamic lambda must execute exactly once", 1, countOccurrences(out, "'dynamic'"));
        Assert.assertEquals("return value must be 2 (1+1), not 3 from a double execution", 2, PrimitiveUtilities.getIntegerValue(result.getValueForMetaPropertyToOne(M3Properties.values)).intValue());
    }

    /**
     * Fix 2 — named function reference passed as the match handler.
     * A named ConcreteFunctionDefinition resolves through Pure.evaluate()'s else-branch;
     * without the missing return, the function body executes twice.
     */
    @Test
    public void testMatchWithNamedFunctionReferenceExecutedOnce()
    {
        compileTestSource("fromString.pure",
                "function sideEffectingIncrement(i:Integer[1]):Integer[1]\n" +
                        "{\n" +
                        "    print('namedFn', 1);\n" +
                        "    $i + 5;\n" +
                        "}\n" +
                        "function testNamedFnMatch():Integer[1]\n" +
                        "{\n" +
                        "    let fns = [sideEffectingIncrement_Integer_1__Integer_1_];\n" +
                        "    10->match($fns);\n" +
                        "}\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        functionExecution.getConsole().setPrintStream(new PrintStream(baos));
        functionExecution.getConsole().setConsole(true);
        CoreInstance result = execute("testNamedFnMatch():Integer[1]");
        String out = baos.toString();
        Assert.assertEquals("named function body must execute exactly once", 1, countOccurrences(out, "'namedFn'"));
        Assert.assertEquals("return value must be 15 (10+5), not 20 from double execution", 15, PrimitiveUtilities.getIntegerValue(result.getValueForMetaPropertyToOne(M3Properties.values)).intValue());
    }

    /**
     * Fix 2 — dynamic match where the first clause does NOT match (type mismatch).
     * Forces the runtime to skip the first handler and invoke the second via Pure.evaluate().
     * Without the return fix, the fallback path in evaluate() re-runs the matched handler.
     */
    @Test
    public void testDynamicMatchSecondClauseExecutedOnce()
    {
        compileTestSource("fromString.pure",
                "function testDynamicMultiClauseMatch():String[1]\n" +
                        "{\n" +
                        "    let fns = [\n" +
                        "        {s:String[1] | print('stringClause', 1); $s + '_str';},\n" +
                        "        {i:Integer[1] | print('intClause', 1); 'was_int';}\n" +
                        "    ];\n" +
                        "    42->match($fns);\n" +
                        "}\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        functionExecution.getConsole().setPrintStream(new PrintStream(baos));
        functionExecution.getConsole().setConsole(true);
        CoreInstance result = execute("testDynamicMultiClauseMatch():String[1]");
        String out = baos.toString();
        Assert.assertEquals("string clause must not fire", 0, countOccurrences(out, "'stringClause'"));
        Assert.assertEquals("int clause must fire exactly once", 1, countOccurrences(out, "'intClause'"));
        Assert.assertEquals("return value must be 'was_int'", "was_int", PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
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
