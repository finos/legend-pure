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

import java.util.Collections;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsParams;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsResult;
import org.finos.legend.pure.lsp.protocol.TestStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PCTManifestResolverAbsentTest
{
    private static LegendPureSession session;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
        LegendPureSession.CompileResult result = session.modifyAndCompile("test_pct_no_resolver.pure",
                "function <<meta::pure::test::pct::PCT.adapter>> test::pctnoresolver::adapter<X|o>(f:Function<{->X[o]}>[1]):X[o]\n" +
                        "{\n" +
                        "  $f->eval();\n" +
                        "}\n" +
                        "\n" +
                        "function <<meta::pure::test::pct::PCT.test>> test::pctnoresolver::fails<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  assert(false, 'boom');\n" +
                        "}\n");
        Assert.assertTrue("Test fixture should compile: "
                + (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void testSessionWithoutResolverRunsWithoutExpectedFailuresAndWarns()
    {
        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setPackagePath("test::pctnoresolver");
        params.setPctAdapterPath("test::pctnoresolver::adapter_Function_1__X_o_");
        ExecuteTestsResult result = session.executeTests(params, null);

        Assert.assertNull(result.getError(), result.getError());
        Assert.assertEquals(TestStatus.FAILED.getProtocolValue(), result.getTests().get(0).getStatus());
        Assert.assertEquals(Collections.singletonList("No PCT expected failures applied: no function in this session carries <<PCT.manifestResolver>>"),
                result.getWarnings());
    }
}
