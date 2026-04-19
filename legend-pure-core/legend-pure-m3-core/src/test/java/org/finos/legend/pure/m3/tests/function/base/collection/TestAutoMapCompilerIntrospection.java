// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

// White-box compiler-introspection test that cannot be expressed in Pure:
// it uses the internal Automap API to assert the post-processor inserted
// an automap node when dereferencing a [0..1] property. The functional
// behavior is covered by the Pure tests testAutoMapWithZeroOnePropertyPresent
// and testAutoMapWithZeroOnePropertyInEvaluate in map.pure, but this test
// guards the specific AST transform directly.
public class TestAutoMapCompilerIntrospection extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("classes.pure");
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testAutoMapWithZeroToOne()
    {
        compileTestSource(
                "classes.pure",
                "Class A\n" +
                        "{\n" +
                        "    b: B[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    name: String[1];\n" +
                        "}\n");
        compileTestSource(
                "fromString.pure",
                "function test(a:A[1]):Any[*]\n" +
                        "{\n" +
                        "    $a.b.name;\n" +
                        "}\n");
        CoreInstance autoMap = Automap.getAutoMapExpressionSequence(Instance.getValueForMetaPropertyToManyResolved(runtime.getCoreInstance("test_A_1__Any_MANY_"), M3Properties.expressionSequence, processorSupport).getFirst());
        Assert.assertNotNull(autoMap);
    }
}
