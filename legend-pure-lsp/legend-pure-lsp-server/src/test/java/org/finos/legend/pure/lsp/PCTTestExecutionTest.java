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

import java.util.List;
import org.finos.legend.pure.lsp.protocol.PCTAdapterInfo;
import org.finos.legend.pure.lsp.protocol.TestFunctionInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies LegendPureSession#executeFunction(String, String) resolves the given pctAdapterPath and
 * substitutes it as the sole argument to a &lt;&lt;PCT.test&gt;&gt; function - mirroring, for a single
 * function invoked by path, the substitution org.finos.legend.pure.m3.execution.test.TestRunner
 * (legend-pure-core) already performs for a bulk PCT run via _Package.getByUserPath(pctAdapter, ...).
 */
public class PCTTestExecutionTest
{
    private static final String SOURCE_ID = "test_pct_execution.pure";

    private static LegendPureSession session;
    private static String pctTestFunctionPath;
    private static String pctAdapterPath;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        String code = "function <<meta::pure::test::pct::PCT.adapter>> "
                + "{meta::pure::test::pct::PCT.adapterName='Test-Custom'} "
                + "test::pct::exec::myAdapter(f:Function<{->String[1]}>[1]):String[1]\n" +
                "{\n" +
                "  $f->eval() + '-adapted';\n" +
                "}\n" +
                "\n" +
                "function <<meta::pure::test::pct::PCT.test>> "
                + "test::pct::exec::myPctTest(f:Function<{Function<{->String[1]}>[1]->String[1]}>[1]):Boolean[1]\n" +
                "{\n" +
                "  print($f->eval(|'hello'));\n" +
                "  true;\n" +
                "}\n" +
                "\n" +
                "function test::pct::exec::notAnAdapter(): String[1]\n" +
                "{\n" +
                "  'not-an-adapter';\n" +
                "}\n";

        LegendPureSession.CompileResult result = session.modifyAndCompile(SOURCE_ID, code);
        Assert.assertTrue("Test fixture should compile: "
                + (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());

        // The precise, execution-ready path (mangled id form) for a non-zero-arg function like a PCT
        // test isn't guessable from its bare path - it's exactly what legend/testFunctions already
        // hands back to a client for this purpose (see TestFunctionInfo#getFunctionPath), so resolve
        // it the same way here instead of hand-guessing Pure's name-mangling scheme.
        List<TestFunctionInfo> testFunctions = TestFunctionProvider.getTestFunctions(session.getPureRuntime(), SOURCE_ID);
        pctTestFunctionPath = testFunctions.stream()
                .filter(f -> f.getName().equals("myPctTest"))
                .findFirst()
                .map(TestFunctionInfo::getFunctionPath)
                .orElseThrow(() -> new AssertionError("myPctTest should be discovered as a test function: " + testFunctions));

        // Likewise, the adapter's precise path (mangled id form, since it's not zero-arg either) is
        // exactly what legend/getPCTAdapters hands back - resolve it the same way a real client would,
        // rather than hand-guessing Pure's name-mangling scheme.
        List<PCTAdapterInfo> adapters = PCTAdapterProvider.getPCTAdapters(session.getPureRuntime());
        pctAdapterPath = adapters.stream()
                .filter(a -> a.getPath().startsWith("test::pct::exec::myAdapter"))
                .findFirst()
                .map(PCTAdapterInfo::getPath)
                .orElseThrow(() -> new AssertionError("myAdapter should be discovered as a PCT adapter: " + adapters));
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void discoveredAsPCTTest()
    {
        List<TestFunctionInfo> testFunctions = TestFunctionProvider.getTestFunctions(session.getPureRuntime(), SOURCE_ID);
        TestFunctionInfo info = testFunctions.stream()
                .filter(f -> f.getName().equals("myPctTest"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("myPctTest should be discovered: " + testFunctions));
        Assert.assertTrue("A <<PCT.test>> function must be flagged isPCTTest so a client can offer "
                + "a \"Run PCTs\" action for it", info.isPCTTest());
    }

    @Test
    public void executesPCTTestWithResolvedAdapter()
    {
        LegendPureSession.ExecuteResult result =
                session.executeFunction(pctTestFunctionPath, pctAdapterPath);

        Assert.assertTrue("Execution should succeed: " + result.getError(), result.isSuccess());
        Assert.assertTrue("Adapter should have been invoked, wrapping the test's lambda result: "
                + result.getOutput(), result.getOutput().contains("hello-adapted"));
    }

    @Test
    public void unknownAdapterPathFailsWithClearError()
    {
        LegendPureSession.ExecuteResult result =
                session.executeFunction(pctTestFunctionPath, "test::pct::exec::noSuchAdapter999");

        Assert.assertFalse("Execution should fail for an adapter path that does not resolve", result.isSuccess());
        Assert.assertTrue("Error should name the missing adapter: " + result.getError(),
                result.getError().contains("noSuchAdapter999"));
    }

    @Test
    public void nullAdapterPathBehavesAsZeroArgumentExecution()
    {
        // notAnAdapter() takes no parameters - executeFunction(path) and executeFunction(path, null)
        // must behave identically for a function that isn't a PCT test.
        LegendPureSession.ExecuteResult withNullAdapter =
                session.executeFunction("test::pct::exec::notAnAdapter", null);
        LegendPureSession.ExecuteResult plain =
                session.executeFunction("test::pct::exec::notAnAdapter");

        Assert.assertTrue("Execution should succeed: " + withNullAdapter.getError(), withNullAdapter.isSuccess());
        Assert.assertEquals(plain.getOutput(), withNullAdapter.getOutput());
    }
}
