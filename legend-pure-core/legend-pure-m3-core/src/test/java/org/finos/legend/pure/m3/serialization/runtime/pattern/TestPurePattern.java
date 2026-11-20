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

package org.finos.legend.pure.m3.serialization.runtime.pattern;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class TestPurePattern extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testUrlArgumentIsDecodedAsUtf8()
    {
        compileTestSource("fromString.pure",
                "function {service.url='/test/greeting/{name}'} test::greeting(name:String[1]):String[1]\n" +
                        "{\n" +
                        "   'Hello, ' + $name\n" +
                        "}\n");

        // Percent-encoded UTF-8 for Greek and Chinese letters, which are in neither windows-1252 nor ISO-8859-1,
        // default charsets of JVMs before Java 18
        Pair<CoreInstance, Map<String, String[]>> result = runtime.getURLPatternLibrary().tryExecution(
                "/test/greeting/%CE%B1%CE%B2%CE%B3%E4%B8%AD%E6%96%87", processorSupport, Maps.mutable.empty());
        Assert.assertNotNull(result);
        Assert.assertArrayEquals(new String[]{"'\u03B1\u03B2\u03B3\u4E2D\u6587'"}, result.getTwo().get("param"));
    }
}
