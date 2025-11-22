// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements._class;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestDispatchQualifiedProperties extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testWithoutOverrides()
    {
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   f(){'a'}:String[1];\n" +
                        "   f(s:String[1]){'a'+$s}:String[1];\n" +
                        "}\n" +
                        "Class B extends A\n" +
                        "{\n" +
                        "}\n" +
                        "function n():A[1]\n" +
                        "{\n" +
                        "   ^B();\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   assertEquals('a', ^A().f);\n" +
                        "   assertEquals('a', ^B().f);\n" +
                        "   assertEquals('a', ^B()->cast(@A).f);\n" +
                        "   assertEquals('aok', n().f('ok'));\n" +
                        "   assertEquals('a', n().f);\n" +
                        "   assertEquals(['a','a','a','a'], [^A(),^B(),^A(),^B()]->cast(@A).f);\n" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testWithoutOverridesFromAssociation()
    {
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   f(s:String[1]){'a'+$s}:String[1];\n" +
                        "}\n" +
                        "Class B extends A\n" +
                        "{\n" +
                        "}\n" +
                        "Class C\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "Association A_C\n" +
                        "{\n" +
                        "  a:A[*];\n" +
                        "  c:C[*];\n" +
                        "  cByName(name:String[1])\n" +
                        "  {\n" +
                        "    $this.c->filter(c | $name == $c.name)\n" +
                        "  }:C[*];\n" +
                        "}\n" +
                        "function n():A[1]\n" +
                        "{\n" +
                        "   ^B(c=[^C(name='c1'), ^C(name='c2')]);\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let a = ^A(c=[^C(name='c1'), ^C(name='c2')]);\n" +
                        "   assertEquals('c1', $a.cByName('c1')->map(c | $c.name));\n" +
                        "   let b = ^B(c=[^C(name='c1'), ^C(name='c2')]);\n" +
                        "   assertEquals('c1', $b.cByName('c1')->map(c | $c.name));\n" +
                        "   assertEquals('c2', $b->cast(@A).cByName('c2')->map(c | $c.name));\n" +
                        "   let n = n();\n" +
                        "   assertEquals('c1', $n.cByName('c1')->map(c | $c.name));\n" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testWithOverrides()
    {
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   f(){'a'}:String[1];\n" +
                        "   f(s:String[1]){'a'+$s}:String[1];\n" +
                        "}\n" +
                        "Class B extends A\n" +
                        "{\n" +
                        "   f(s:String[1]){'b'+$s}:String[1];\n" +
                        "   f(){'b'}:String[1];\n" +
                        "}\n" +
                        "function n():A[1]\n" +
                        "{\n" +
                        "   ^B();\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   assertEquals('a', ^A().f);\n" +
                        "   assertEquals('b', ^B().f);\n" +
                        "   assertEquals('b', ^B()->cast(@A).f);\n" +
                        "   assertEquals('bok', n().f('ok'));\n" +
                        "   assertEquals('b', n().f);\n" +
                        "   assertEquals(['a','b','a','b'], [^A(),^B(),^A(),^B()]->cast(@A).f);\n" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testWithOverridesFromAssociation()
    {
        compileTestSource("fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   f(s:String[1]){'a'+$s}:String[1];\n" +
                        "}\n" +
                        "Class B extends A\n" +
                        "{\n" +
                        "}\n" +
                        "Class C\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "Association A_C\n" +
                        "{\n" +
                        "  a:A[*];\n" +
                        "  c:C[*];\n" +
                        "  cByName(name:String[1])\n" +
                        "  {\n" +
                        "    $this.c->filter(c | $name == $c.name)\n" +
                        "  }:C[*];\n" +
                        "}\n" +
                        "Association B_C\n" +
                        "{\n" +
                        "  b:B[*];\n" +
                        "  c:C[*];\n" +
                        "  cByName(name:String[1])\n" +
                        "  {\n" +
                        "    $this.c->filter(c | $name != $c.name)\n" +
                        "  }:C[*];\n" +
                        "}\n" +
                        "function n():A[1]\n" +
                        "{\n" +
                        "   ^B(c=[^C(name='c1'), ^C(name='c2')]);\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let a = ^A(c=[^C(name='c1'), ^C(name='c2')]);\n" +
                        "   assertEquals('c1', $a.cByName('c1')->map(c | $c.name));\n" +
                        "   let b = ^B(c=[^C(name='c1'), ^C(name='c2')]);\n" +
                        "   assertEquals('c2', $b.cByName('c1')->map(c | $c.name));\n" +
                        "   assertEquals('c1', $b->cast(@A).cByName('c2')->map(c | $c.name));\n" +
                        "   let n = n();\n" +
                        "   assertEquals('c2', $n.cByName('c1')->map(c | $c.name));\n" +
                        "   assertEquals('c1', $n.cByName('c2')->map(c | $c.name));\n" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testWithEvaluateAndDeactivate()
    {
        compileTestSource("fromString.pure",
                        "Class D\n" +
                        "{\n" +
                        "  i : Integer[1];\n" +
                        "}\n" +
                        "Class C\n" +
                        "{\n" +
                        "   f : Function<{Integer[1]->Integer[1]}>[1];\n" +
                        "   f(i:Integer[1]){^D(i=$this.f->eval($i));}:D[1];\n" +
                        "}\n" +
                        "function myfunc(i:Integer[1]):Integer[1]\n" +
                        "{\n" +
                        "   $i + 1;\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   assertEquals(2, ^C(f=myfunc_Integer_1__Integer_1_)->evaluateAndDeactivate().f(1).i);\n" +
                        "}\n");
        execute("testNew():Any[*]");
    }
}
