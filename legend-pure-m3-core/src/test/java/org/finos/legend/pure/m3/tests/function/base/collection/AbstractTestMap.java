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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestMap extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testMapWithMultiplicityInferencePropertyOwner()
    {
        compileTestSource("fromString.pure","Class Employee<|m>\n" +
                "{\n" +
                "    prop:String[m];\n" +
                "}\n" +
                "\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "    let f = [^Employee<|*>(prop=['a','b']), ^Employee<|1>(prop='b')];\n" +
                "    print($f->map(e|$e.prop), 1);\n" +
                "}\n");
        this.execute("test():Nil[0]");
        Assert.assertEquals("[\n" +
                "   'a'\n" +
                "   'b'\n" +
                "   'b'\n" +
                "]", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testMapWithMultiplicityInferenceFunctionWhichIsNotAProperty()
    {
        compileTestSource("fromString.pure","function f<|m>(s:String[m]):String[m]\n" +
                "{\n" +
                "    $s\n" +
                "}\n" +
                "\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "    print([^List<String>(values='a'), ^List<String>(values=['b','c']), ^List<String>(values='c')]->map(i|f($i.values)), 1);\n" +
                "}\n");
        this.execute("test():Nil[0]");
        Assert.assertEquals("[\n" +
                "   'a'\n" +
                "   'b'\n" +
                "   'c'\n" +
                "   'c'\n" +
                "]", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testMapWithVariableThisAsParameter()
    {
        String code = "Class A \n" +
                "{\n" +
                "   func(valueFunc:Function<{A[1]->Float[1]}>[1])" +
                "   {" +
                "       if(true, | $this->map($valueFunc) , | 1.0);" +
                "   }:Float[1];  " +
                "\n}";
        compileTestSource("fromString.pure", code);
    }

    @Test
    public void testAutoMapWithZeroToOne()
    {
        String classes = "Class A \n { b: B[0..1];} \n" +
                "Class B { name: String[1]; }\n";
        String testFunction = "function test(a: A[1]): Any[*] \n {" +
                "    $a.b.name;\n" +
                "}";

        compileTestSource("classes.pure", classes);
        compileTestSource("fromString.pure", testFunction);
        CoreInstance autoMap = Automap.getAutoMapExpressionSequence(Instance.getValueForMetaPropertyToManyResolved(this.runtime.getCoreInstance("test_A_1__Any_MANY_"), M3Properties.expressionSequence, this.processorSupport).getFirst());
        Assert.assertNotNull(autoMap);
    }
}
