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

package org.finos.legend.pure.lsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsParams;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsResult;
import org.finos.legend.pure.lsp.protocol.TestInvocation;
import org.finos.legend.pure.lsp.protocol.TestResult;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers invoking a function with String[1] arguments, both one-at-a-time (legend/execute) and as a
 * fan-out over one parameterised function (legend/executeTests invocations) - the case an explicit
 * function list cannot express, since every entry would name the same function.
 */
public class StringArgumentExecutionTest
{
    private static final String ECHO_SOURCE =
            "function echo(first:String[1], second:String[1]):Any[*]\n"
                    + "{\n"
                    + "  print($first + '/' + $second, 1)\n"
                    + "}\n"
                    + "function failUnless(expected:String[1], actual:String[1]):Boolean[1]\n"
                    + "{\n"
                    + "  assert($expected == $actual, | 'wanted ' + $expected + ' got ' + $actual);\n"
                    + "  true;\n"
                    + "}\n";

    private static LegendPureSession session;

    @BeforeClass
    public static void initSession()
    {
        session = new LegendPureSession();
        session.initialize();
        Assert.assertTrue("Session should be initialized", session.isInitialized());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    private static void loadEchoSource()
    {
        session.reinitialize();
        LegendPureSession.CompileResult r = session.modifyAndCompile("string_args.pure", ECHO_SOURCE);
        Assert.assertTrue("fixture should compile, got: "
                + (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());
    }

    @Test
    public void executeFunction_withStringArguments_bindsThemInOrder()
    {
        loadEchoSource();

        LegendPureSession.ExecuteResult result = session.executeFunction(
                "echo_String_1__String_1__Any_MANY_", null, null, null, Arrays.asList("alpha", "beta"));

        Assert.assertTrue("should succeed, got: " + result.getError(), result.isSuccess());
        Assert.assertTrue("arguments should bind in order, got: " + result.getOutput(),
                result.getOutput().contains("alpha/beta"));
    }

    @Test
    public void executeFunction_withWrongArgumentCount_fails()
    {
        loadEchoSource();

        LegendPureSession.ExecuteResult result = session.executeFunction(
                "echo_String_1__String_1__Any_MANY_", null, null, null, Collections.singletonList("alpha"));

        Assert.assertFalse("a mis-arity call must fail rather than bind silently", result.isSuccess());
    }

    @Test
    public void executeFunction_withArgumentsAndPctAdapter_isRejected()
    {
        loadEchoSource();

        LegendPureSession.ExecuteResult result = session.executeFunction(
                "echo_String_1__String_1__Any_MANY_", "some::adapter", null, null,
                Arrays.asList("alpha", "beta"));

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue("error should say which two are exclusive, got: " + result.getError(),
                result.getError().contains("arguments") && result.getError().contains("pctAdapterPath"));
    }

    @Test
    public void executeTests_invocationsOfOneFunction_reportIndependentlyUnderTheirLabels()
    {
        loadEchoSource();

        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setInvocations(Arrays.asList(
                invocation("failUnless_String_1__String_1__Boolean_1_", "same", Arrays.asList("x", "x")),
                invocation("failUnless_String_1__String_1__Boolean_1_", "different", Arrays.asList("x", "y"))));

        ExecuteTestsResult result = session.executeTests(params, null);

        Assert.assertEquals(2, result.getTests().size());
        Assert.assertEquals(1, result.getPassed());
        Assert.assertEquals(1, result.getFailed());
        Assert.assertEquals(Arrays.asList("same", "different"), names(result.getTests()));
        Assert.assertTrue("the failing entry should carry the assert message, got: "
                        + result.getTests().get(1).getMessage(),
                result.getTests().get(1).getMessage().contains("wanted x got y"));
    }

    @Test
    public void executeTests_unlabelledInvocation_fallsBackToTheFunctionName()
    {
        loadEchoSource();

        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setInvocations(Collections.singletonList(
                invocation("failUnless_String_1__String_1__Boolean_1_", null, Arrays.asList("x", "x"))));

        ExecuteTestsResult result = session.executeTests(params, null);

        Assert.assertEquals(Collections.singletonList("failUnless"), names(result.getTests()));
    }

    private static TestInvocation invocation(String path, String label, List<String> arguments)
    {
        TestInvocation invocation = new TestInvocation();
        invocation.setPath(path);
        invocation.setLabel(label);
        invocation.setArguments(arguments);
        return invocation;
    }

    private static List<String> names(List<TestResult> tests)
    {
        List<String> names = new ArrayList<>(tests.size());
        for (TestResult test : tests)
        {
            names.add(test.getName());
        }
        return names;
    }
}
