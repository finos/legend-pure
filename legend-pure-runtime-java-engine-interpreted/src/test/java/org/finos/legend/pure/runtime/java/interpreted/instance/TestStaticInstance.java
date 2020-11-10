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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestStaticInstance extends AbstractPureTestWithCoreCompiled
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
    public void testStaticInstance()
    {
        compileTestSource("fromString.pure","Class Person\n" +
                          "{\n" +
                          "   lastName:String[1];\n" +
                          "}\n" +
                          "^Person p (lastName='last')\n" +
                          "function testGet():Nil[0]\n" +
                          "{\n" +
                          "    print(p, 1);\n" +
                          "}\n");
        this.execute("testGet():Nil[0]");
        Assert.assertEquals("p instance Person\n" +
                            "    lastName(Property):\n" +
                            "        last instance String", this.functionExecution.getConsole().getLine(0));
    }


    @Test
    public void testGetterFromStaticInstance()
    {
        compileTestSource("fromString.pure","Class Person\n" +
                "{\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "^Person a (lastName='last')\n" +
                "function testGet():Nil[0]\n" +
                "{\n" +
                "    print(a.lastName, 1);\n" +
                "}\n");
        this.execute("testGet():Nil[0]");
        Assert.assertEquals("'last'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testGetterFromStaticInstanceWithWrongProperty()
    {
        try
        {
            compileTestSource("fromString.pure","Class test::Person\n" +
                    "{\n" +
                    "   lastName:String[1];\n" +
                    "}\n" +
                    "^test::Person p (lastName='last')\n" +
                    "function testGet():Nil[0]\n" +
                    "{\n" +
                    "    print(p.wrongProperty);\n" +
                    "}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Can't find the property 'wrongProperty' in the class test::Person", 8, 13, e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}