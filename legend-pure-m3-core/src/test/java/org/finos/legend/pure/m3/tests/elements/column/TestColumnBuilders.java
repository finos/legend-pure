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

package org.finos.legend.pure.m3.tests.elements.column;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

public class TestColumnBuilders extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSimpleColumn()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test():Boolean[1]" +
                            "{" +
                            "   let x = ~name;" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Parser error at (resource:fromString.pure line:1 column:44), expected: ':' found: ';'", e.getMessage());
        }
    }

    @Test
    public void testSimpleColumnWithType()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpec<(name:String)>[1]" +
                        "{" +
                        "   ~name:String;" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeArray()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpecArray<(name:String, id:Integer)>[1]" +
                        "{" +
                        "   ~[name:String, id:Integer];" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeFail()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test():meta::pure::metamodel::relation::ColSpec<(name:Integer)>[1]" +
                            "{" +
                            "   ~name:String;" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:1 column:80), \"Return type error in function 'test'; found: meta::pure::metamodel::relation::ColSpec<(name:String)>; expected: meta::pure::metamodel::relation::ColSpec<(name:Integer)>\"", e.getMessage());
        }
    }

    @Test
    public void testSimpleColumnWithFunction()
    {
        compileTestSource("fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]" +
                        "{" +
                        "   ~name:x|'ok';" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionArray()
    {
        compileTestSource("fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpecArray<{U[1]->Any[1]}, (name:String, val:Integer)>[1]" +
                        "{" +
                        "   ~[name:x|'ok', val:x|1];" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionFail()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]" +
                            "{" +
                            "   ~name:x|1;" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:1 column:102), \"Return type error in function 'test'; found: meta::pure::metamodel::relation::FuncColSpec<{U[1]->meta::pure::metamodel::type::Any[1]}, (name:Integer)>; expected: meta::pure::metamodel::relation::FuncColSpec<{U[1]->meta::pure::metamodel::type::Any[1]}, (name:String)>\"", e.getMessage());
        }
    }

    @Test
    public void testMixColumnsFail()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]" +
                            "{" +
                            "   ~[name:x|1, id:Integer];" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Parser error at (resource:fromString.pure line:-2), (Compilation error at ??, \"Can't mix column types\") in\n" +
                    "'\n" +
                    "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]{   ~[name:x|1, id:Integer];}'", e.getMessage());
        }
    }

}