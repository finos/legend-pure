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
import java.util.Optional;
import org.finos.legend.pure.lsp.protocol.PCTAdapterInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies PCTAdapterProvider mirrors meta::pure::ide::testing::getPCTAdapters(): every element
 * carrying the &lt;&lt;PCT.adapter&gt;&gt; stereotype is found, named by its 'adapterName' tagged
 * value (falling back to the element's own name), keyed by its Pure path.
 */
public class PCTAdapterProviderTest
{
    private static final String SOURCE_ID = "test_pct_adapter_provider.pure";

    private static LegendPureSession session;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        String code = "function <<meta::pure::test::pct::PCT.adapter>> "
                + "{meta::pure::test::pct::PCT.adapterName='Test-Custom'} "
                + "test::pct::adapters::myAdapter(f:Function<{->String[1]}>[1]):String[1]\n" +
                "{\n" +
                "  $f->eval() + '-adapted';\n" +
                "}\n" +
                "\n" +
                "function <<meta::pure::test::pct::PCT.adapter>> "
                + "test::pct::adapters::unnamedAdapter(f:Function<{->String[1]}>[1]):String[1]\n" +
                "{\n" +
                "  $f->eval();\n" +
                "}\n" +
                "\n" +
                "function test::pct::adapters::plainFunction(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n";

        LegendPureSession.CompileResult result = session.modifyAndCompile(SOURCE_ID, code);
        Assert.assertTrue("Test fixture should compile: "
                + (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void findsAdapterWithExplicitName()
    {
        List<PCTAdapterInfo> found = PCTAdapterProvider.getPCTAdapters(session.getPureRuntime());
        Optional<PCTAdapterInfo> custom = found.stream()
                .filter(a -> "test::pct::adapters::myAdapter__Function_1__String_1_".equals(a.getPath())
                        || a.getPath().startsWith("test::pct::adapters::myAdapter"))
                .findFirst();
        Assert.assertTrue("Expected the custom adapter to be found, found: " + found, custom.isPresent());
        Assert.assertEquals("Test-Custom", custom.get().getName());
    }

    @Test
    public void fallsBackToElementNameWhenAdapterNameTagAbsent()
    {
        List<PCTAdapterInfo> found = PCTAdapterProvider.getPCTAdapters(session.getPureRuntime());
        Optional<PCTAdapterInfo> unnamed = found.stream()
                .filter(a -> a.getPath().startsWith("test::pct::adapters::unnamedAdapter"))
                .findFirst();
        Assert.assertTrue("Expected the unnamed adapter to be found, found: " + found, unnamed.isPresent());
        Assert.assertEquals("unnamedAdapter", unnamed.get().getName());
    }

    @Test
    public void ignoresPlainFunction()
    {
        List<PCTAdapterInfo> found = PCTAdapterProvider.getPCTAdapters(session.getPureRuntime());
        boolean containsPlain = found.stream().anyMatch(a -> a.getPath().contains("plainFunction"));
        Assert.assertFalse("A function without the PCT.adapter stereotype must not be reported", containsPlain);
    }

    @Test
    public void includesTheBuiltInPlatformInMemoryAdapter()
    {
        // meta::pure::test::pct::testAdapterForInMemoryExecution is defined in the platform itself
        // (pct_core.pure), always loaded - a real end-to-end sanity check that this isn't only
        // finding elements from this test's own fixture source.
        List<PCTAdapterInfo> found = PCTAdapterProvider.getPCTAdapters(session.getPureRuntime());
        boolean containsBuiltIn = found.stream()
                .anyMatch(a -> "In-Memory".equals(a.getName())
                        && a.getPath().startsWith("meta::pure::test::pct::testAdapterForInMemoryExecution"));
        Assert.assertTrue("Expected the platform's built-in In-Memory PCT adapter to be found, found: " + found,
                containsBuiltIn);
    }
}
