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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsParams;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsResult;
import org.finos.legend.pure.lsp.protocol.TestResult;
import org.finos.legend.pure.lsp.protocol.TestStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PCTManifestExclusionsTest
{
    private static final String REPO = "test_pct_manifests";
    private static final String ADAPTER = "test::pctmanifest::adapter_Function_1__X_o_";
    private static final String UNSCOPED_SOURCE_ID = "test_pct_unscoped.pure";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static LegendPureSession session;

    @BeforeClass
    public static void init() throws IOException
    {
        Path repoDir = createWorkspaceRepo();
        write(repoDir.resolve("pct/tests.pure"),
                "import meta::pure::test::pct::*;\n" +
                        "\n" +
                        "function <<PCT.manifestResolver>> test::pctmanifest::resolveManifest(filter:String[1], adapter:Function<Any>[1]):String[1]\n" +
                        "{\n" +
                        "  assert($filter->startsWith('/test_pct_manifests/'), | 'Cannot identify a manifest for ' + $filter);\n" +
                        "  '/test_pct_manifests/pct-manifests/manifest-test/RelationFunctions_manifest.json';\n" +
                        "}\n" +
                        "\n" +
                        "function <<PCT.adapter>> {PCT.adapterName='Manifest-Test'}\n" +
                        "  test::pctmanifest::adapter<X|o>(f:Function<{->X[o]}>[1]):X[o]\n" +
                        "{\n" +
                        "  $f->eval();\n" +
                        "}\n" +
                        "\n" +
                        "function <<PCT.test>> test::pctmanifest::passes<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  assertEquals(1, $f->eval(|1));\n" +
                        "}\n" +
                        "\n" +
                        "function <<PCT.test>> test::pctmanifest::failsAsExpected<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  assert(false, 'expected boom');\n" +
                        "}\n" +
                        "\n" +
                        "function <<PCT.test>> test::pctmanifest::failsDifferently<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  assert(false, 'actual boom');\n" +
                        "}\n" +
                        "\n" +
                        "function <<PCT.test>> test::pctmanifest::passesDespiteExclusion<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  true;\n" +
                        "}\n" +
                        "\n" +
                        "function <<PCT.test>> test::pctmanifest::failsAsQuotedExpected<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  assert(false, 'quoted boom');\n" +
                        "}\n");
        write(repoDir.resolve("pct-manifests/manifest-test/RelationFunctions_manifest.json"),
                "{\n" +
                        "  \"adapter\": \"" + ADAPTER + "\",\n" +
                        "  \"exclusions\": [\n" +
                        "    {\"test\": \"test::pctmanifest::failsAsExpected_Function_1__Boolean_1_\", \"expectedError\": \"expected boom\"},\n" +
                        "    {\"test\": \"test::pctmanifest::failsDifferently_Function_1__Boolean_1_\", \"expectedError\": \"recorded boom\"},\n" +
                        "    {\"test\": \"test::pctmanifest::passesDespiteExclusion_Function_1__Boolean_1_\", \"expectedError\": \"never raised\"},\n" +
                        "    {\"test\": \"test::pctmanifest::failsAsQuotedExpected_Function_1__Boolean_1_\", \"expectedError\": \"\\\"quoted boom\\\"\"}\n" +
                        "  ]\n" +
                        "}\n");

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(Collections.singletonList(tempFolder.getRoot().toPath()));
        session = new LegendPureSession();
        session.initialize(scanner);

        LegendPureSession.CompileResult result = session.modifyAndCompile(UNSCOPED_SOURCE_ID,
                "function <<meta::pure::test::pct::PCT.test>> test::pctunscoped::fails<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  assert(false, 'expected boom');\n" +
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
    public void testFailingWithItsExpectedErrorPasses()
    {
        TestResult test = runTest(runPackage("test::pctmanifest"), "failsAsExpected");
        Assert.assertEquals(TestStatus.PASSED.getProtocolValue(), test.getStatus());
        Assert.assertEquals("Expected failure: expected boom", test.getMessage());
    }

    @Test
    public void testFailingWithQuotedExpectedErrorPasses()
    {
        TestResult test = runTest(runPackage("test::pctmanifest"), "failsAsQuotedExpected");
        Assert.assertEquals(TestStatus.PASSED.getProtocolValue(), test.getStatus());
        Assert.assertEquals("Expected failure: quoted boom", test.getMessage());
    }

    @Test
    public void testFailingWithAnotherErrorFailsShowingBoth()
    {
        TestResult test = runTest(runPackage("test::pctmanifest"), "failsDifferently");
        Assert.assertEquals(TestStatus.FAILED.getProtocolValue(), test.getStatus());
        Assert.assertTrue(test.getMessage(), test.getMessage().contains("expected : recorded boom\n  actual   : actual boom"));
    }

    @Test
    public void testPassingDespiteExclusionFailsAsStale()
    {
        TestResult test = runTest(runPackage("test::pctmanifest"), "passesDespiteExclusion");
        Assert.assertEquals(TestStatus.FAILED.getProtocolValue(), test.getStatus());
        Assert.assertTrue(test.getMessage(), test.getMessage().contains("was expected to fail but now PASSES"));
    }

    @Test
    public void testWithoutExclusionIsUnaffected()
    {
        ExecuteTestsResult result = runPackage("test::pctmanifest");
        Assert.assertEquals(TestStatus.PASSED.getProtocolValue(), runTest(result, "passes").getStatus());
        Assert.assertEquals(Collections.emptyList(), result.getWarnings());
    }

    @Test
    public void testSourceTheResolverRejectsRunsWithoutExpectedFailuresAndWarns()
    {
        ExecuteTestsResult result = runPackage("test::pctunscoped");
        Assert.assertEquals(TestStatus.FAILED.getProtocolValue(), runTest(result, "fails").getStatus());
        Assert.assertEquals(1, result.getWarnings().size());
        Assert.assertTrue(result.getWarnings().get(0), result.getWarnings().get(0).startsWith(
                "No PCT expected failures applied to " + UNSCOPED_SOURCE_ID + ": Cannot identify a manifest for " + UNSCOPED_SOURCE_ID));
    }

    private static ExecuteTestsResult runPackage(String packagePath)
    {
        ExecuteTestsParams params = new ExecuteTestsParams();
        params.setPackagePath(packagePath);
        params.setPctAdapterPath(ADAPTER);
        ExecuteTestsResult result = session.executeTests(params, null);
        Assert.assertNull(result.getError(), result.getError());
        return result;
    }

    private static TestResult runTest(ExecuteTestsResult result, String name)
    {
        return result.getTests().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for " + name + " in " + result.getTests()));
    }

    private static Path createWorkspaceRepo() throws IOException
    {
        Path resourcesDir = tempFolder.getRoot().toPath().resolve(REPO + "-module/src/main/resources");
        write(resourcesDir.resolve(REPO + ".definition.json"),
                "{\"name\":\"" + REPO + "\",\"pattern\":\"(test::pctmanifest)(::.*)?\",\"dependencies\":[\"platform\"]}");
        return resourcesDir.resolve(REPO);
    }

    private static void write(Path file, String content) throws IOException
    {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }
}
