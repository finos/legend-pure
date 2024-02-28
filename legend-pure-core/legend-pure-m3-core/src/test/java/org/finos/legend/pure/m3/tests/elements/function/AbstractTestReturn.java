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

package org.finos.legend.pure.m3.tests.elements.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestReturn extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testReturn()
    {
        compileTestSource("fromString.pure","function funcWithReturn():String[1]\n" +
                "{\n" +
                "   'Hello';\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   assertEquals('Hello', funcWithReturn());" +
                "   [];\n" +
                "}");
        this.execute("test():Nil[0]");
     }

    @Test
    public void testReturnWithInheritance()
    {
        compileTestSource("fromString.pure","Class TypeA\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "Class TypeB extends TypeA\n" +
                "{\n" +
                "   moreName : String[1];\n" +
                "}\n" +
                "function funcWithReturn():TypeA[1]\n" +
                "{\n" +
                "   ^TypeB(moreName='xxx', name='aaa');\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   assertEquals('aaa', funcWithReturn().name);" +
                "   [];\n" +
                "}");
        this.execute("test():Nil[0]");
    }

    @Test
    public void testReturnWithMultiplicityMany()
    {
        compileTestSource("fromString.pure","function process():String[*]\n" +
                "{\n" +
                "    ['a','b']\n" +
                "}\n" +
                "\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   assertEquals('a__b', process()->joinStrings('__'));" +
                "   [];\n" +
                "}\n");
        this.execute("test():Nil[0]");
    }
}
