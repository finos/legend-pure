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

package org.finos.legend.pure.runtime.java.compiled.modeling;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMultiplicityArgument extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
    }

    @Test
    public void testMulti()
    {
        compileTestSource("fromString.pure", "Class JSONResult<P|m> extends ServiceResult<P|m>\n" +
                "{" +
                " \n" +
                "}" +
                "" +
                "function meta::pure::functions::meta::hasUpperBound(multiplicity:Multiplicity[1]):Boolean[1]\n" +
                "{\n" +
                "    let upperBound = $multiplicity.upperBound;\n" +
                "    !$upperBound->isEmpty() && !$upperBound->toOne().value->isEmpty() && $upperBound->toOne().value != -1;\n" +
                "}" +
                "function meta::pure::functions::meta::hasToOneUpperBound(multiplicity:Multiplicity[1]):Boolean[1]\n" +
                "{\n" +
                "    $multiplicity->hasUpperBound() && eq($multiplicity->getUpperBound(), 1)\n" +
                "}" +
                "function meta::pure::functions::meta::getUpperBound(multiplicity:Multiplicity[1]):Integer[1]\n" +
                "{\n" +
                "    $multiplicity.upperBound->toOne().value->toOne()\n" +
                "}" +
                "\n" +
                "function meta::pure::functions::meta::getLowerBound(multiplicity:Multiplicity[1]):Integer[1]\n" +
                "{\n" +
                "    $multiplicity.lowerBound->toOne().value->toOne()\n" +
                "}" +
                "function meta::pure::functions::meta::isToOne(multiplicity:Multiplicity[1]):Boolean[1]\n" +
                "{\n" +
                "    hasToOneUpperBound($multiplicity) && eq($multiplicity->getLowerBound(), 1)\n" +
                "}" +
                "function test(v:ServiceResult<Any|*>[1]):Boolean[1]\n" +
                "{\n" +
                "   $v->match(j:JSONResult<Any|*>[1]| $j.value->match([a:Any[*]|$j.classifierGenericType.multiplicityArguments->at(0)->isToOne()]))\n" +
                "}" +
                "function test():Any[*]\n" +
                "{" +
                "  assert(^JSONResult<String|1>(value='hello')->test(), |'');" +
                "  assert(!^JSONResult<String|0..1>(value='hello')->test(), |'');" +
                "  assert(!^JSONResult<String|*>(value='hello')->test(), |'');" +
                "  assert(!^JSONResult<String|1..2>(value='hello')->test(), |'');" +
                "  assert(!^JSONResult<String|0..*>(value='hello')->test(), |'');" +
                "}\n");
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testFunctionWithReturnMultiplicityParameter()
    {
        String source = "function test::foo<T|m>(x:T[m], y:String[1]):T[m]{$x}\n" +
                "function test::bar(s:String[1]):String[1] { $s + 'bar' }\n" +
                "function test::testFn():Any[*] {test::foo('one string', 'two string')->test::bar()}\n";
        this.compileTestSource("fromString.pure", source);
        this.compileAndExecute("test::testFn():Any[*]");
    }

    @Test
    public void testGenericTypeWithMultiplicityArgument()
    {
        compileTestSource("fromString.pure", "Class test::TestClass<|m>\n" +
                "{\n" +
                "  names : String[m];\n" +
                "}\n");

        compileTestSource("fromString3.pure", "import test::*;\n" +
                "function test::testClass1():TestClass<|1>[1]\n" +
                "{\n" +
                "  ^TestClass<|1>(names='one name');\n" +
                "}\n" +
                "\n" +
                "function test::testFn1():String[1]" +
                "{\n" +
                "  let name = testClass1().names;\n" +
                "  assert('one name' == $name, |'');\n" +
                "  $name;" +
                "}\n");

        compileTestSource("fromString2.pure", "import test::*;\n" +
                "function test::testClass0_1():TestClass<|0..1>[1]\n" +
                "{\n" +
                "  ^TestClass<|0..1>(names=[]);\n" +
                "}\n" +
                "\n" +
                "function test::testFn0_1():String[0..1]" +
                "{\n" +
                "  let name = testClass0_1().names;\n" +
                "  assert($name->isEmpty(), |'');\n" +
                "  $name;" +
                "}\n");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}