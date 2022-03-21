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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.generated.CoreJavaModelFactoryRegistry;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionReturnMultiplicity extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution(), getFactoryRegistryOverride());
        compileTestSource("Class A{" +
                "   b:Integer[1];" +
                "}" +
                "Class Result<T|m>\n" +
                "{\n" +
                "   values:T[m];\n" +
                "}\n");
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testReturnNull()
    {
        compileTestSource("fromString.pure","function process():String[*]\n" +
                "{\n" +
                "    ['a','b']\n" +
                "}\n" +
                "function makeString(any:Any[*], s:String[1]):String[1]\n" +
                "{\n" +
                "    $any->map(x | $x->toString())->joinStrings('', $s, '')\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   print(process()->makeString('__'),1);\n" +
                "}\n");
        this.compileAndExecute("test():Nil[0]");
        Assert.assertEquals("'a__b'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testReturnManyTypeConversion()
    {
        compileTestSource("fromString.pure","function process():Any[*]\n" +
                "{\n" +
                "    ['a', 1, 2.0, %2015-03-12, %2015-03-12T23:59:00, true, Class]\n" +
                "}\n" +
                "\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "   process();\n" +
                "}\n");
        CoreInstance result = this.compileAndExecute("test():Any[*]");
        ListIterable<? extends CoreInstance> values = result.getValueForMetaPropertyToMany("values");
        Assert.assertEquals(7, values.size());
        Assert.assertEquals("a instanceOf String,1 instanceOf Integer,2.0 instanceOf Float,2015-03-12 instanceOf StrictDate,2015-03-12T23:59:00+0000 instanceOf DateTime,true instanceOf Boolean,Class(1809) instanceOf Class", values.makeString(","));
    }


    @Test
    public void testReturnExactlyOnePrimitiveTypeConversion()
    {
        compileTestSource("fromString.pure","function process():Any[*]\n" +
                "{\n" +
                "    ['a']\n" +
                "}\n" +
                "\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "   process();\n" +
                "}\n");
        CoreInstance result = this.compileAndExecute("test():Any[*]");
        CoreInstance value = result.getValueForMetaPropertyToOne("values");
        Assert.assertEquals("a instanceOf String", value.toString());
    }

    @Test
    public void testReturnExactlyOneNonPrimitiveTypeConversion()
    {
        compileTestSource("fromString.pure","function process():Any[*]\n" +
                "{\n" +
                "    [Class]\n" +
                "}\n" +
                "\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "   process();\n" +
                "}\n");
        CoreInstance result = this.compileAndExecute("test():Any[*]");
        CoreInstance value = result.getValueForMetaPropertyToOne("values");
        Assert.assertEquals("Class(1809) instanceOf Class", value.toString());
    }

    @Test
    public void testReturnTypeMultiplicityArg()
    {

        compileTestSource("fromString.pure",
                "function process<T|m>(f:FunctionDefinition<{->T[m]}>[1]):Result<T|m>[1]\n" +
                "{\n" +
                "    let vals = $f->eval();\n" +
                "    ^Result<T|m>(values=$vals);\n" +
                "}\n" +
                "\n" +
                "function testOne():A[1]\n" +
                "{\n" +
                "   process({| ^A(b=1)}).values;\n" +
                "}\n" +
                "\n" +
                "function testMany():A[*]\n" +
                "{\n" +
                "   process({| [^A(b=1),^A(b=2)]}).values;\n" +
                "}\n");
        CoreInstance result = this.compileAndExecute("testOne():A[1]");
        CoreInstance value = result.getValueForMetaPropertyToOne("values");
        Assert.assertEquals("A", this.functionExecution.getProcessorSupport().getClassifier(value).getName());
        Assert.assertEquals("1", value.getValueForMetaPropertyToOne("b").getName());

        CoreInstance result2 = this.execute("testMany():A[*]");
        ListIterable<? extends CoreInstance> value2 = result2.getValueForMetaPropertyToMany("values");
        Assert.assertEquals("1,2", value2.collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance object)
            {
                return object.getValueForMetaPropertyToOne("b").getName();
            }
        }).makeString(","));
    }

    @Test
    public void testReturnTypeMultiplicityArgWithLet()
    {

        compileTestSource("fromString.pure",
                "function process<T|m>(f:FunctionDefinition<{->T[m]}>[1]):Result<T|m>[1]\n" +
                "{\n" +
                "    let vals = $f->eval();\n" +
                "    ^Result<T|m>(values=$vals);\n" +
                "}\n" +
                "\n" +
                "function testOne():A[1]\n" +
                "{\n" +
                "   let v = process({| ^A(b=1)}).values;\n" +
                "   $v;\n" +
                "}\n" +
                "function testOne2():A[1]\n" +
                "{\n" +
                "   let v = process({| ^A(b=1)});\n" +
                "   $v.values;\n" +
                "}\n" +
                "\n" +
                "function testMany():A[*]\n" +
                "{\n" +
                "   let v = process({| [^A(b=1),^A(b=2)]}).values;\n" +
                "   $v;\n" +
                "}\n");
        CoreInstance result = this.compileAndExecute("testOne():A[1]");
        CoreInstance value = result.getValueForMetaPropertyToOne("values");
        Assert.assertEquals("A", this.functionExecution.getProcessorSupport().getClassifier(value).getName());
        Assert.assertEquals("1", value.getValueForMetaPropertyToOne("b").getName());

        CoreInstance result2 = this.execute("testMany():A[*]");
        ListIterable<? extends CoreInstance> value2 = result2.getValueForMetaPropertyToMany("values");
        Assert.assertEquals("1,2", value2.collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance object)
            {
                return object.getValueForMetaPropertyToOne("b").getName();
            }
        }).makeString(","));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    protected static CoreInstanceFactoryRegistry getFactoryRegistryOverride()
    {
        return CoreJavaModelFactoryRegistry.REGISTRY;
    }
}
