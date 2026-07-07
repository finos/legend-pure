// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct;

import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class TestPCTTools
{
    @Test
    public void unwrapStripsWrapperWithParams()
    {
        Throwable real = new RuntimeException("Table 'FOO' not found");
        Throwable wrapped = new RuntimeException("Unexpected error executing function with params [x, y]", real);
        Assert.assertSame(real, PCTTools.unwrapExecutionError(wrapped));
        Assert.assertEquals("Table 'FOO' not found", PCTTools.getMessageFromError(PCTTools.unwrapExecutionError(wrapped)));
    }

    @Test
    public void unwrapStripsNoParamsAndValidateVariants()
    {
        Throwable real = new RuntimeException("real error");
        Assert.assertSame(real, PCTTools.unwrapExecutionError(new RuntimeException("Unexpected error executing function", real)));
        Assert.assertSame(real, PCTTools.unwrapExecutionError(new RuntimeException("Unexpected error executing function _validate", real)));
    }

    @Test
    public void unwrapDescendsThroughReflectionPlumbingWithNullMessage()
    {
        Throwable real = new RuntimeException("Division by zero");
        // wrapper -> InvocationTargetException (null message) -> real error
        Throwable wrapped = new RuntimeException("Unexpected error executing function with params [1]", new InvocationTargetException(real));
        Assert.assertSame(real, PCTTools.unwrapExecutionError(wrapped));
        Assert.assertEquals("Division by zero", PCTTools.getMessageFromError(PCTTools.unwrapExecutionError(wrapped)));
    }

    @Test
    public void unwrapStripsNestedWrapperVariants()
    {
        Throwable real = new RuntimeException("boom");
        Throwable inner = new RuntimeException("Unexpected error executing function _validate", real);
        Throwable outer = new RuntimeException("Unexpected error executing function with params [z]", inner);
        Assert.assertSame(real, PCTTools.unwrapExecutionError(outer));
    }

    @Test
    public void unwrapLeavesRealErrorUntouched()
    {
        Throwable real = new RuntimeException("Table 'FOO' not found");
        Assert.assertSame(real, PCTTools.unwrapExecutionError(real));
    }

    @Test
    public void genericWrapperMessageIsDetected()
    {
        Assert.assertTrue(PCTTools.isGenericExecutionErrorMessage("Unexpected error executing function with params [a]"));
        Assert.assertTrue(PCTTools.isGenericExecutionErrorMessage("Unexpected error executing function"));
        Assert.assertTrue(PCTTools.isGenericExecutionErrorMessage("Unexpected error executing function _validate"));
        Assert.assertFalse(PCTTools.isGenericExecutionErrorMessage("Table 'FOO' not found"));
    }

    @Test
    public void messageCleaned()
    {
        String msg = PCTTools.getMessageFromError(new Exception("Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\n" +
                "expected: '#TDS\\n   p,o,i,newCol\\n   300,2,20,30\\n   300,1,10,30\\n   200,3,30,80\\n   200,3,30,80\\n   200,1,10,80\\n   200,1,10,80\\n   100,3,30,60\\n   100,2,20,60\\n   100,1,10,60\\n   0,1,10,20\\n   0,1,10,20\\n#'\n" +
                "actual:   '#TDS\\n   p,o,i,newCol\\n   300,2,20,30\\n   300,1,10,30\\n   200,3,30,110\\n   200,3,30,110\\n   200,1,10,110\\n   200,1,10,110\\n   100,3,30,110\\n   100,2,20,110\\n   100,1,10,110\\n   0,1,10,50\\n   0,1,10,50\\n#'\""));

        Assert.assertEquals("Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\n" +
                "expected: '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,3,30,80\n   200,3,30,80\n   200,1,10,80\n   200,1,10,80\n   100,3,30,60\n   100,2,20,60\n   100,1,10,60\n   0,1,10,20\n   0,1,10,20\n#'\n" +
                "actual:   '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,3,30,110\n   200,3,30,110\n   200,1,10,110\n   200,1,10,110\n   100,3,30,110\n   100,2,20,110\n   100,1,10,110\n   0,1,10,50\n   0,1,10,50\n#'\"", msg);
    }

    @Test
    public void manifestSnippetProducesPrettyJson()
    {
        // Locks in the exact snippet format: {\n  "test" : "...",\n  "expectedError" : "..."\n}
        // Two-space indent, spaces around the colon, and Unix "\n" line endings on every platform.
        Assert.assertEquals(
                "{\n" +
                        "  \"test\" : \"meta::pure::functions::foo\",\n" +
                        "  \"expectedError\" : \"Table FOO not found\"\n" +
                        "}",
                PCTTools.buildManifestExclusionSnippet("meta::pure::functions::foo", "Table FOO not found"));
    }

    @Test
    public void manifestSnippetEscapesSpecialCharacters()
    {
        // Jackson must escape quotes, backslashes, newlines, tabs and carriage returns just like
        // the old hand-written jsonEscape did.
        String snippet = PCTTools.buildManifestExclusionSnippet("meta::pure::functions::bar",
                "line1 with \"quotes\"\nline2 \\ backslash\ttab\rcr");
        Assert.assertEquals(
                "{\n" +
                        "  \"test\" : \"meta::pure::functions::bar\",\n" +
                        "  \"expectedError\" : \"line1 with \\\"quotes\\\"\\nline2 \\\\ backslash\\ttab\\rcr\"\n" +
                        "}",
                snippet);
    }

    @Test
    public void manifestSnippetTreatsNullExpectedErrorAsEmptyString()
    {
        // Preserves the previous jsonEscape(null) contract: null -> empty string in the JSON.
        Assert.assertEquals(
                "{\n" +
                        "  \"test\" : \"meta::pure::functions::baz\",\n" +
                        "  \"expectedError\" : \"\"\n" +
                        "}",
                PCTTools.buildManifestExclusionSnippet("meta::pure::functions::baz", null));
    }

    @Test
    public void manifestSnippetEscapesControlCharacters()
    {
        // The old hand-written jsonEscape silently emitted raw control characters, which produced
        // invalid JSON. Jackson escapes them as \\uXXXX — this test locks in the safer behaviour.
        String snippet = PCTTools.buildManifestExclusionSnippet("meta::pure::functions::qux", "bell\u0007text");
        Assert.assertEquals(
                "{\n" +
                        "  \"test\" : \"meta::pure::functions::qux\",\n" +
                        "  \"expectedError\" : \"bell\\u0007text\"\n" +
                        "}",
                snippet);
    }
}
