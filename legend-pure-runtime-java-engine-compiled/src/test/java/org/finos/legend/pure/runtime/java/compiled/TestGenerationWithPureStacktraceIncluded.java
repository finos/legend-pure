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

package org.finos.legend.pure.runtime.java.compiled;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.PureCompiledExecutionException;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestGenerationWithPureStacktraceIncluded extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testExceptionStacktrace()
    {
        String genericFunc = "function generic():Any[1]\n" +
                "{\n" +
                "   55;\n" +
                "}\n";
        compileTestSource("genericFunc.pure", genericFunc);

        String icFunc = "function invalidCast():String[1]\n" +
                "{\n" +
                "   generic()->cast(@String);" +
                "}\n";
        compileTestSource("invalidCast.pure", icFunc);
        String noOPFunc = "function noOP():Any[*]\n" +
                "{\n" +
                "   invalidCast();" +
                "}\n";
        compileTestSource("noOP.pure", noOPFunc);
        compileTestSource("pureStacktaceTest.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "   'whatever';noOP();\n" +
                        "}\n");
        PureCompiledExecutionException e = Assert.assertThrows(PureCompiledExecutionException.class, () -> execute("test():Any[*]"));
        StringBuilder sb = new StringBuilder();
        e.printPureStackTrace(sb);
        Assert.assertEquals("resource:invalidCast.pure line:3 column:15\n" +
                "resource:noOP.pure line:3 column:4\n" +
                "resource:pureStacktaceTest.pure line:3 column:15\n", sb.toString());
    }

    @Test
    public void testCompileWithThisReference()
    {
        String code =
                "Class A { qp(){ func55($this); }:Number[1]; } \n" +
                        "function func55(a:A[1]):Number[1] { 55; }";
        compileTestSource("fromString.pure", code);
    }

    @Test
    public void testToOneCompilation()
    {
        String code = "Class JsiProperty{ name:String[1]; } \n" +
                "\n" +
                "function jsiProperty(prop: AbstractProperty<Any>[*]):JsiProperty[1] { ^JsiProperty(name='ta da'); }\n" +
                "\n" +
                "function getJsiProperties(class: Class<Any>[1], pathToRoot:AbstractProperty<Any>[*]): JsiProperty[*]\n" +
                "{   \n" +
                "   let current =  \n" +
                "      $class.properties->map(pt | $pt.genericType.rawType->toOne()->match([\n" +
                "         {pr:PrimitiveType[1] | jsiProperty($pathToRoot->add($pt)->toOneMany()) },                        \n" +
                "         {et:Enumeration<Any>[1] | jsiProperty($pathToRoot->add($pt)->toOneMany()) },\n" +
                "         {a:Class<Any>[1] | \n" +
                "          let newPath = $pathToRoot->add($pt);\n" +
                "          let keyProps = $a.properties; \n" +
                "          let props = if(true, | jsiProperty($pt), | if($a.properties->size() > 1, \n" +
                "                                                        | if($keyProps->isEmpty(), | $a->getJsiProperties($newPath), \n" +
                "                  | $keyProps->toOneMany()->map(aapt | $aapt->jsiProperty())),\n" +
                "                  | $a.properties->cast(@AbstractProperty<Any>)->toOne()->jsiProperty()));\n" +
                "                  \n" +
                "          $props;\n" +
                "         }          \n" +
                "         ]));      \n" +
                "      \n" +
                "   $current;            \n" +
                "}" +
                "";
        compileTestSource("fromString.pure", code);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().shouldIncludePureStackTrace().build();
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        //target\generated-sources\
        Path pureCodeDirectory = Paths.get("target\\generated-sources\\");
        return PureCodeStorage.createCodeStorage(pureCodeDirectory, getCodeRepositories());
    }
}
