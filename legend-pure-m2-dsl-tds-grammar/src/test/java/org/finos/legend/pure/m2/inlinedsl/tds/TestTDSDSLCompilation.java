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

package org.finos.legend.pure.m2.inlinedsl.tds;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTDSDSLCompilation extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
    }

    @Test
    public void testSimple()
    {
        try
        {
            runtime.createInMemorySource("file.pure",
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#EEW#,2);\n" +
                    "}\n");
            runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Parser error at (resource:file.pure line:3 column:15), expected: one of {'as', '{', '<'} found: '<EOF>'", e.getMessage());
        }

        this.runtime.modify("file.pure",
                "import meta::pure::metamodel::relation::*;" +
                        "native function <<functionType.SideEffectFunction>> rows<T>(type:TDS<T>[1]):T[*];" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   print(" +
                        "       #TDS\n" +
                        "         value, other, name\n" +
                        "         1, 3, A\n" +
                        "         2, 4, B\n" +
                        "       #->rows()" +
                        ", 2);\n" +
                        //"    print(A.all()->map(x|$x.a), 2);\n" +
                        "}\n");
        this.runtime.compile();

        this.runtime.modify("file.pure",
                "import meta::pure::metamodel::relation::*;" +
                        "native function <<functionType.SideEffectFunction>> rows<T>(type:TDS<T>[1]):T[*];" +
                        "function test():Any[*]\n" +
                "{\n" +
                "    print(" +
                        "   #TDS\n" +
                        "       value, other, name\n" +
                        "       1, 3, A\n" +
                        "       2, 4, B\n" +
                        "   #" +
                        "   ->rows()->map(x|$x.value + $x.other)" +
                        ", 2);\n" +
                //"    print(A.all()->map(x|$x.a), 2);\n" +
                "}\n");
        this.runtime.compile();

        this.runtime.modify("file.pure",
                "import meta::pure::metamodel::relation::*;" +
                        "native function <<functionType.SideEffectFunction>> rows<T>(type:Relation<T>[1]):T[*];" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "    print(" +
                        "   #TDS\n" +
                        "       value, other, name\n" +
                        "       1, 3, A\n" +
                        "       2, 4, B\n" +
                        "   #" +
                        "   ->rows()->map(x|$x.value + $x.other)" +
                        ", 2);\n" +
                        //"    print(A.all()->map(x|$x.a), 2);\n" +
                        "}\n");
        this.runtime.compile();
    }
}
