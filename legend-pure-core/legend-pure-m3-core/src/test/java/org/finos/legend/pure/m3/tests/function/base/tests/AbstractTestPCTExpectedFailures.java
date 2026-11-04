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

package org.finos.legend.pure.m3.tests.function.base.tests;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Test;

public abstract class AbstractTestPCTExpectedFailures extends AbstractPureTestWithCoreCompiled
{
    protected static final String TEST_SOURCE_ID = "/test/testFile.pure";
    protected static final String TEST_SOURCE_CODE = "import meta::pure::test::pct::*;\n" +
            "import meta::pure::test::surveyor::*;\n" +
            "\n" +
            "function <<PCT.test>> test::pct::failsWithBoom<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1]\n" +
            "{\n" +
            "  assert(false, 'boom');\n" +
            "}\n" +
            "\n" +
            "function test::passCountExpecting(expectedError:String[1]):Integer[1]\n" +
            "{\n" +
            "  let exclusions = newMap(pair(test::pct::failsWithBoom_Function_1__Boolean_1_->cast(@Function<Any>), $expectedError));\n" +
            "  'test::pct'->pathToElement()->runPCTTests('', testAdapterForInMemoryExecution_Function_1__X_o_, $exclusions).passCount;\n" +
            "}\n" +
            "\n" +
            "function test::bareExpectedErrorMatches():Any[*]\n" +
            "{\n" +
            "  assertEquals(1, test::passCountExpecting('boom'));\n" +
            "}\n" +
            "\n" +
            "function test::quotedExpectedErrorMatches():Any[*]\n" +
            "{\n" +
            "  assertEquals(1, test::passCountExpecting('\"boom\"'));\n" +
            "}\n";

    @Test
    public void testAssertionInfoMatchesBareExpectedError()
    {
        execute("test::bareExpectedErrorMatches():Any[*]");
    }

    @Test
    public void testAssertionMessageMatchesQuotedExpectedError()
    {
        execute("test::quotedExpectedErrorMatches():Any[*]");
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(TEST_SOURCE_ID, TEST_SOURCE_CODE);
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(), GenericCodeRepository.build("test", "test(::.*)?", "platform"))));
    }
}
