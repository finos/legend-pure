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
import org.finos.legend.pure.lsp.protocol.TestFunctionInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies legend/testFunctions finds functions by their real compiled &lt;&lt;test.Test&gt;&gt;
 * stereotype - not by scanning source text for the annotation. The negative cases (a different
 * stereotype, and the literal annotation text sitting inertly in a comment/string) exist
 * specifically to prove that: a text/regex scan would wrongly match both.
 */
public class TestFunctionProviderTest
{
    private static final String SOURCE_ID = "test_function_provider.pure";

    private static LegendPureSession session;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        String code = "Profile test::tfp::other::OtherProfile\n" +
                "{\n" +
                "  stereotypes: [NotATest];\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> test::tfp::realTestFunction(): Boolean[1]\n" +
                "{\n" +
                "  assert(true, |'')\n" +
                "}\n" +
                "\n" +
                "function test::tfp::plainFunction(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "\n" +
                "function <<test::tfp::other::OtherProfile.NotATest>> test::tfp::otherStereotypeFunction(): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "\n" +
                "// This one merely mentions <<test.Test>> as text - it must NOT be picked up:\n" +
                "// a regex/text scan would incorrectly match this comment and the string below.\n" +
                "function test::tfp::textOnlyFunction(): String[1]\n" +
                "{\n" +
                "  '<<test.Test>>'\n" +
                "}\n";

        LegendPureSession.CompileResult result = session.modifyAndCompile(SOURCE_ID, code);
        Assert.assertTrue("Test fixture should compile: " +
                (result.getError() != null ? result.getError().getMessage() : ""), result.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void getTestFunctions_findsOnlyRealTestStereotypedFunction()
    {
        List<TestFunctionInfo> found = TestFunctionProvider.getTestFunctions(session.getPureRuntime(), SOURCE_ID);

        Assert.assertEquals("Exactly one function should carry the real test.Test stereotype, found: " + found,
                1, found.size());

        TestFunctionInfo info = found.get(0);
        // Mangled id form (qualified path + signature suffix) - one of the accepted forms for
        // execution (see ExecuteFunctionParams), and precise even when a name is overloaded.
        Assert.assertEquals("test::tfp::realTestFunction__Boolean_1_", info.getFunctionPath());
        Assert.assertEquals("realTestFunction", info.getName());
        Assert.assertEquals("Function declaration is on line 6", 6, info.getLine());
    }

    @Test
    public void getTestFunctions_ignoresPlainFunction()
    {
        List<TestFunctionInfo> found = TestFunctionProvider.getTestFunctions(session.getPureRuntime(), SOURCE_ID);
        boolean containsPlain = found.stream()
                .anyMatch(f -> f.getName().equals("plainFunction"));
        Assert.assertFalse("Plain function without any stereotype must not be reported", containsPlain);
    }

    @Test
    public void getTestFunctions_ignoresDifferentStereotype()
    {
        List<TestFunctionInfo> found = TestFunctionProvider.getTestFunctions(session.getPureRuntime(), SOURCE_ID);
        boolean containsOther = found.stream()
                .anyMatch(f -> f.getName().equals("otherStereotypeFunction"));
        Assert.assertFalse("Function with an unrelated stereotype must not be reported", containsOther);
    }

    @Test
    public void getTestFunctions_ignoresLiteralAnnotationTextInCommentsAndStrings()
    {
        List<TestFunctionInfo> found = TestFunctionProvider.getTestFunctions(session.getPureRuntime(), SOURCE_ID);
        boolean containsTextOnly = found.stream()
                .anyMatch(f -> f.getName().equals("textOnlyFunction"));
        Assert.assertFalse("A function merely containing the annotation text in a comment/string "
                + "(no real stereotype) must not be reported - proves this is not text matching", containsTextOnly);
    }

    @Test
    public void getTestFunctions_unknownSource_returnsEmpty()
    {
        List<TestFunctionInfo> found = TestFunctionProvider.getTestFunctions(
                session.getPureRuntime(), "no_such_source_999.pure");
        Assert.assertTrue(found.isEmpty());
    }
}
