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

package org.finos.legend.pure.runtime.java.interpreted.instance;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCollectionMixedTypes extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSimple()
    {
        compileTestSource("fromString.pure","Class Employee{lastName:String[1];}\n" +
                          "function test():GenericType[1]\n" +
                          "{\n" +
                          "    let a = [^Employee(lastName='William'),'a string',123,false];\n" +
                          "    $a->genericType();\n" +
                          "}\n");
        CoreInstance result = execute("test():GenericType[1]");
        CoreInstance genericType = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertEquals("Any", GenericType.print(genericType, this.processorSupport));
    }

    @Test
    public void testWithTypeArguments()
    {
        compileTestSource("fromString.pure","Class MyType<T>{}\n" +
                          "Class A{}\n" +
                          "Class B extends A{}\n" +
                          "Class C{}\n" +
                          "function test():GenericType[2]\n" +
                          "{\n" +
                          "    let a = [^MyType<A>(), ^MyType<B>()];\n" +
                          "    let b = [^MyType<A>(), ^MyType<B>(), ^MyType<C>()];\n" +
                          "    [$a->genericType(), $b->genericType()];\n" +
                          "}\n");
        CoreInstance result = execute("test():GenericType[2]");
        ListIterable<? extends CoreInstance> genericTypes = result.getValueForMetaPropertyToMany(M3Properties.values);
        Assert.assertEquals("MyType<A>", GenericType.print(genericTypes.get(0), this.processorSupport));
        Assert.assertEquals("MyType<Any>", GenericType.print(genericTypes.get(1), this.processorSupport));
    }

    @Test
    public void testWithTypeMultiplicities()
    {
        compileTestSource("fromString.pure","Class MyType<|m>{}\n" +
                "function test():GenericType[1]\n" +
                "{\n" +
                "    let argpa = [^MyType<|1..2>(), ^MyType<|3..5>()];\n" +
                "    $argpa->genericType();\n" +
                "}\n");
        CoreInstance result = execute("test():GenericType[1]");
        CoreInstance genericType = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertEquals("MyType<|1..5>", GenericType.print(genericType, this.processorSupport));
    }

    @Test
    public void testWithTypeMultiplicitiesWithMany()
    {
        compileTestSource("fromString.pure","Class MyType<|m>{}\n" +
                          "function test():GenericType[1]\n" +
                          "{\n" +
                          "    let argpa = [^MyType<|1..2>(), ^MyType<|3..5>(), ^MyType<|*>()];\n" +
                          "    $argpa->genericType();\n" +
                          "}\n");
        CoreInstance result = execute("test():GenericType[1]");
        CoreInstance genericType = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertEquals("MyType<|*>", GenericType.print(genericType, this.processorSupport));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
