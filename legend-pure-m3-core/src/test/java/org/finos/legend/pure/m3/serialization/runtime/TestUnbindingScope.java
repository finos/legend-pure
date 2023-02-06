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

package org.finos.legend.pure.m3.serialization.runtime;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.*;

@Ignore
public class TestUnbindingScope extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    // Class deletion / should unbind

    @Test
    public void testScopeOfFunctionUndbindingForClass_ReturnType()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn():A[*]\n" +
                        "{\n" +
                        "    []\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn():A[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_ReturnTypeGeneric()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn():List<A>[*]\n" +
                        "{\n" +
                        "    []\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn():List[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_ParameterType()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(a:A[1]):String[*]\n" +
                        "{\n" +
                        "    []\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(A[1]):String[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_ParameterTypeGeneric()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(a:List<A>[1]):String[*]\n" +
                        "{\n" +
                        "    []\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(List[1]):String[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_New()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(name:String[1]):Any[1]\n" +
                        "{\n" +
                        "    ^A(name=$name)\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):Any[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_NewGeneric()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(name:String[1]):Any[1]\n" +
                        "{\n" +
                        "    ^List<A>()\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):Any[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_Cast()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(a:Any[1]):String[1]\n" +
                        "{\n" +
                        "    $a->cast(@A).name\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(Any[1]):String[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_CastGeneric()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(a:Any[1]):String[*]\n" +
                        "{\n" +
                        "    $a->cast(@List<A>).values->map(v | $v.name)\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(Any[1]):String[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Inferred()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::newA(name:String[1]):A[1]\n" +
                        "{\n" +
                        "    ^A(name=$name)\n" +
                        "}\n" +
                        "\n" +
                        "function test::getAName(a:A[1]):String[1]\n" +
                        "{\n" +
                        "    $a.name\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn(name:String[1]):String[1]\n" +
                        "{\n" +
                        "    $name->newA()->getAName()\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):String[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Inferred2()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::newA(name:String[1]):A[1]\n" +
                        "{\n" +
                        "    ^A(name=$name)\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn(name:String[1]):Any[1]\n" +
                        "{\n" +
                        "    $name->newA()\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):Any[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Explicit_New()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(names:String[*]):Any[*]\n" +
                        "{\n" +
                        "    $names->map(name | ^A(name=$name))\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[*]):Any[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Explicit_Cast()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(things:Any[*]):String[*]\n" +
                        "{\n" +
                        "    $things->map(thing | $thing->cast(@A).name)\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(Any[*]):String[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Inferred()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::newA(name:String[1]):A[1]\n" +
                        "{\n" +
                        "    ^A(name=$name)\n" +
                        "}\n" +
                        "\n" +
                        "function test::getAName(a:A[1]):String[1]\n" +
                        "{\n" +
                        "    $a.name\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn(names:String[*]):String[*]\n" +
                        "{\n" +
                        "    $names->map(name | $name->newA()->getAName())\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[*]):String[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Inferred2()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::newA(name:String[1]):A[1]\n" +
                        "{\n" +
                        "    ^A(name=$name)\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn(names:String[*]):Any[*]\n" +
                        "{\n" +
                        "    $names->map(name | $name->newA())\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[*]):Any[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    // Class deletion / should NOT unbind

    @Test
    public void testScopeOfFunctionUndbindingForClass_Unrelated()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(string:String[1]):String[1]\n" +
                        "{\n" +
                        "    $string\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):String[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertTrue(testFn.hasBeenValidated());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Indirect()
    {
        compileTestSource("/test/source1.pure",
                "Class test::A\n" +
                        "{\n" +
                        "    name:String[1];\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::getName(name:String[1]):String[1]\n" +
                        "{\n" +
                        "    ^A(name=$name).name\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn(string:String[1]):String[1]\n" +
                        "{\n" +
                        "    $string->getName()\n" +
                        "}\n");
        CoreInstance getName = this.runtime.getFunction("test::getName(String[1]):String[1]");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):String[1]");
        Assert.assertTrue(getName.hasBeenValidated());
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(getName.hasBeenProcessed());
        Assert.assertTrue(testFn.hasBeenValidated());
    }

    // Function deletion / should unbind

    @Test
    public void testScopeOfFunctionUnbindingForFunction_Direct()
    {
        compileTestSource("/test/source1.pure",
                "function test::testJoinStrings(strings:String[*]):String[1]\n" +
                        "{\n" +
                        "    $strings->joinStrings(' ')\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(string:String[1]):String[1]\n" +
                        "{\n" +
                        "    $string->split('\\t')->testJoinStrings()\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):String[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUnbindingForFunction_DirectInLambda()
    {
        compileTestSource("/test/source1.pure",
                "function test::testJoinStrings(strings:String[*]):String[1]\n" +
                        "{\n" +
                        "    $strings->joinStrings(' ')\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(strings:String[*]):String[*]\n" +
                        "{\n" +
                        "    $strings->map(string | $string->split('\\t')->testJoinStrings())\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[*]):String[*]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(testFn.hasBeenProcessed());
    }

    // Function deletion / should NOT unbind

    @Test
    public void testScopeOfFunctionUnbindingForFunction_Unrelated()
    {
        compileTestSource("/test/source1.pure",
                "function test::testJoinStrings(strings:String[*]):String[1]\n" +
                        "{\n" +
                        "    $strings->joinStrings(' ')\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::testFn(string:String[1]):String[1]\n" +
                        "{\n" +
                        "    $string->replace('a', 'b')\n" +
                        "}\n");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):String[1]");
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertTrue(testFn.hasBeenValidated());
    }

    @Test
    public void testScopeOfFunctionUnbindingForFunction_Indirect()
    {
        compileTestSource("/test/source1.pure",
                "function test::testJoinStrings(strings:String[*]):String[1]\n" +
                        "{\n" +
                        "    $strings->joinStrings(' ')\n" +
                        "}\n");
        compileTestSource("/test/source2.pure",
                "import test::*;\n" +
                        "function test::replaceTabsWithSpaces(strings:String[*]):String[*]\n" +
                        "{\n" +
                        "    $strings->map(s | $s->split('\\t')->testJoinStrings())\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn(string:String[1]):String[*]\n" +
                        "{\n" +
                        "    $string->split('\\n')->replaceTabsWithSpaces()\n" +
                        "}\n");
        CoreInstance replaceTabsWithSpaces = this.runtime.getFunction("test::replaceTabsWithSpaces(String[*]):String[*]");
        CoreInstance testFn = this.runtime.getFunction("test::testFn(String[1]):String[*]");
        Assert.assertTrue(replaceTabsWithSpaces.hasBeenValidated());
        Assert.assertTrue(testFn.hasBeenValidated());
        this.runtime.delete("/test/source1.pure");
        this.runtime.getIncrementalCompiler().unload();
        Assert.assertFalse(replaceTabsWithSpaces.hasBeenProcessed());
        Assert.assertTrue(testFn.hasBeenValidated());
    }
}
