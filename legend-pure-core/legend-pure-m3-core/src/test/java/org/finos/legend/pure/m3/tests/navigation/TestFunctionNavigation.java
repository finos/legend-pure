// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.navigation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFunctionNavigation extends AbstractPureTestWithCoreCompiledPlatform
{

    @Before
    public void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void tearDown()
    {
        tearDownRuntime();
    }

    @Test
    public void testPrettyPrintFunction()
    {
        runtime.createInMemorySource(
                "test.pure",
                "function test1::A<T,V|m>(value: T[m]): V[*] \n" +
                        "{\n" +
                        "  print('ok', 1);\n" +
                        "}\n" +
                        "Class test::A {}\n" +
                        "\n" +
                        "function test2::doSomething(param1: String[1], param2: test::A[1]): Any[*]\n" +
                        "{\n" +
                        "  ''\n" +
                        "}\n"
        );
        runtime.compile();

        String result = Function.prettyPrint(runtime.getCoreInstance("test1").getValueForMetaPropertyToMany(M3Properties.children).get(0), runtime.getProcessorSupport());
        Assert.assertEquals(result, "A<T,V|m>(value: T[m]): V[*]");

        result = Function.prettyPrint(runtime.getCoreInstance("test2").getValueForMetaPropertyToMany(M3Properties.children).get(0), runtime.getProcessorSupport());
        Assert.assertEquals(result, "doSomething(param1: String[1], param2: A[1]): Any[*]");
    }
}
