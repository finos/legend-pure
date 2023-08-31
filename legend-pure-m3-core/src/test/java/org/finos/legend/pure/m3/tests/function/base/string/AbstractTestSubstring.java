// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.string;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestSubstring extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testStart()
    {
           compileTestSource("substring.pure",
                    "function testStart():Boolean[1]\n" +
                            "{\n" +
                            "    let string = 'the quick brown fox jumps over the lazy dog';\n" +
                            "    assertEquals('the quick brown fox jumps over the lazy dog', substring($string, 0));\n" +
                            "    assertEquals('he quick brown fox jumps over the lazy dog', substring($string, 1));\n" +
                            "    assertEquals('e quick brown fox jumps over the lazy dog', substring($string, 2));\n" +
                            "    assertEquals(' quick brown fox jumps over the lazy dog', substring($string, 3));\n" +
                            "    assertEquals('quick brown fox jumps over the lazy dog', substring($string, 4));\n" +
                            "}");
            this.execute("testStart():Boolean[1]");
    }

    @Test
    public void testStartEnd()
    {
       compileTestSource("substring.pure",
                "function testStartEnd():Boolean[1]\n" +
                        "{\n" +
                        "    let string = 'the quick brown fox jumps over the lazy dog';\n" +
                        "    assertEquals('the quick brown fox jumps over the lazy dog', substring($string, 0, 43));\n" +
                        "    assertEquals('he quick brown fox jumps over the lazy do', substring($string, 1, 42));\n" +
                        "    assertEquals('e quick brown fox jumps over the lazy d', substring($string, 2, 41));\n" +
                        "    assertEquals(' quick brown fox jumps over the lazy ', substring($string, 3, 40));\n" +
                        "    assertEquals('quick brown fox jumps over the lazy', substring($string, 4, 39));\n" +
                        "}\n");
        this.execute("testStartEnd():Boolean[1]");
    }

    @Test
    public void testStartWithReflection()
    {
        compileTestSource("substring.pure",
                "function testStart():Boolean[1]\n" +
                        "{\n" +
                        "    let string = 'the quick brown fox jumps over the lazy dog';\n" +
                        "    assertEquals('the quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 0));\n" +
                        "    assertEquals('he quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 1));\n" +
                        "    assertEquals('e quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 2));\n" +
                        "    assertEquals(' quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 3));\n" +
                        "    assertEquals('quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 4));\n" +
                        "}");
        this.execute("testStart():Boolean[1]");
    }

    @Test
    public void testStartEndWithReflection()
    {
        compileTestSource("substring.pure",
                "function testStartEnd():Boolean[1]\n" +
                        "{\n" +
                        "    let string = 'the quick brown fox jumps over the lazy dog';\n" +
                        "    assertEquals('the quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 0, 43));\n" +
                        "    assertEquals('he quick brown fox jumps over the lazy do', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 1, 42));\n" +
                        "    assertEquals('e quick brown fox jumps over the lazy d', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 2, 41));\n" +
                        "    assertEquals(' quick brown fox jumps over the lazy ', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 3, 40));\n" +
                        "    assertEquals('quick brown fox jumps over the lazy', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 4, 39));\n" +
                        "}\n");
        this.execute("testStartEnd():Boolean[1]");
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("substring.pure");
        runtime.compile();
    }
}
